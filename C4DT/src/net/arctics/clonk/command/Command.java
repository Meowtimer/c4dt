package net.arctics.clonk.command;

import static net.arctics.clonk.util.StringUtil.blockString;
import static net.arctics.clonk.util.StringUtil.multiply;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.arctics.clonk.Core;
import net.arctics.clonk.Flags;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.DeclMask;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ast.IASTVisitor;
import net.arctics.clonk.ast.IEvaluationContext;
import net.arctics.clonk.ast.TraversalContinuation;
import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.builder.ProjectConverter;
import net.arctics.clonk.c4script.Conf;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.Function.FunctionScope;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.c4script.typing.ITypeable;
import net.arctics.clonk.c4script.typing.PrimitiveType;
import net.arctics.clonk.c4script.typing.StaticTypingUtil;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.Scenario;
import net.arctics.clonk.index.XMLDocImporter;
import net.arctics.clonk.index.XMLDocImporter.ExtractedDeclarationDocumentation;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.ui.editors.EntityHyperlink;
import net.arctics.clonk.util.SelfcontainedStorage;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

/**
 * Macro execution engine based on the C4Script parser. Naturally, macros are written in C4Script.
 *
 */
public class Command {
	public static final Script BASE;
	public static final Index INDEX = new Index();
	static {
		BASE = new Script(INDEX) {
			private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
			@Override
			public IStorage source() {
				return new SelfcontainedStorage("CommandBase", ""); //$NON-NLS-1$ //$NON-NLS-2$
			}
			@Override
			public String name() {
				return "CommandBaseScript"; //$NON-NLS-1$
			};
			@Override
			public String nodeName() {
				return name();
			};
		};

		for (final Class<?> c : Command.class.getDeclaredClasses())
			registerCommandsFromClass(BASE, c);
		registerCommandsFromClass(BASE, StaticTypingUtil.class);
	}

	public static ExecutableScript executableScriptFromCommand(final String command) {
		return new ExecutableScript("command", String.format("func Main() {%s;}", command), Command.INDEX);
	}

	public static void registerCommandsFromClass(final Script script, final Class<?> classs) {
		for (final Method m : classs.getMethods())
			if (m.getAnnotation(CommandFunction.class) != null)
				addCommand(script, m);
	}

	private static class NativeCommandFunction extends Function {
		private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
		private final transient Method method;
		@Override
		public Object invoke(final IEvaluationContext context) {
			final Object[] args = new Object[method.getParameterTypes().length];
			args[0] = context;
			final Object[] evaluatedArgs = Arrays.stream(context.arguments()).map(ASTNode::evaluateVariable).toArray();
			System.arraycopy(evaluatedArgs, 0, args, 1, context.arguments().length);
			try {
				return method.invoke(context, args);
			} catch (final IllegalArgumentException iae) {
				System.out.println(String.format("Passed: %s; Expected: %s",
					Arrays.stream(args)
						.map(a -> a != null ? a.getClass().getSimpleName() : "null")
						.collect(Collectors.joining(", ")),
					Arrays.stream(method.getParameterTypes())
						.map(t -> t.getSimpleName())
						.collect(Collectors.joining(", "))
				));
				iae.printStackTrace();
				return null;
			} catch (final Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		public NativeCommandFunction(final Script parent, final Method method) {
			super(parent, FunctionScope.PUBLIC, method.getName());
			this.method = method;
		}
	}

	public static void addCommand(final Script script, final Method method) {
		script.addDeclaration(new NativeCommandFunction(script, method));
	}

	public static void addCommand(final Method method) {
		addCommand(BASE, method);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void setFieldValue(final Object obj, final String name, Object value) {
		final Class<?> c = obj instanceof Class<?> ? (Class<?>)obj : obj.getClass();
		try {
			final Field f = c.getField(name);
			if (value instanceof Long && f.getType() == Integer.TYPE)
				value = ((Long)value).intValue();
			else if (value instanceof String && f.getType().isEnum())
				value = Enum.valueOf((Class<Enum>)f.getType(), (String)value);
			f.set(obj, value);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	public static class BaseCommands {
		@CommandFunction
		public static void Log(final Object context, final String message) {
			System.out.println(message);
		}

		@CommandFunction
		public static String Format(final Object context, final String format, final List<?> args) {
			return String.format(format, args.toArray());
		}

		@CommandFunction
		public static void OpenDoc(final Object context, final String funcName) {
			try {
				EntityHyperlink.openDocumentationForFunction(funcName, Core.instance().activeEngine());
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static class CodeConversionCommands {
		@CommandFunction
		public static void SetCodeConversionOption(final Object context, final String option, final Object value) {
			setFieldValue(Conf.class, option, value);
		}
		@CommandFunction
		public static void WriteEngineScript(final Object context, final String engineName) throws IOException {
			Core.instance().loadEngine(engineName).writeEngineScript();
		}
		private static void _WriteDescriptionsToFile(final String writeToFile, final Engine engine) throws FileNotFoundException, IOException {
			final OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(writeToFile));
			writer.append("[Descriptions]\n"); //$NON-NLS-1$
			for (final Function f : engine.functions()) {
				final String escaped = f.obtainUserDescription() != null ? f.obtainUserDescription().replace("\n", "|||") : ""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				writer.append(String.format("%s=%s\n", f.name(), escaped)); //$NON-NLS-1$
			}
			writer.close();
		}
		@CommandFunction
		public static void WriteDescriptionsToFile(final Object context, final String writeToFile, final String engineName) throws FileNotFoundException, IOException {
			final Engine engine = Core.instance().loadEngine(engineName);
			if (engine != null)
				_WriteDescriptionsToFile(writeToFile, engine);
		}
		@CommandFunction
		public static void ConvertProject(final Object context, final String source, final String dest) {
			final ProjectConverter converter = new ProjectConverter(
				ResourcesPlugin.getWorkspace().getRoot().getProject(source),
				ResourcesPlugin.getWorkspace().getRoot().getProject(dest)
			);
			converter.convert(new NullProgressMonitor());
		}
	}

	public static class EngineConfiguration {
		@CommandFunction
		public static void SetEngineProperty(final Object context, final String name, final Object value) {
			setFieldValue(Core.instance().activeEngine().settings(), name, value);
		}
		@CommandFunction
		public static void IntrinsicizeEngineProperty(final Object context, final String name) throws IllegalArgumentException, SecurityException, IllegalAccessException, NoSuchFieldException {
			final Engine engine = Core.instance().activeEngine();
			setFieldValue(
				engine.intrinsicSettings(), name,
				engine.settings().getClass().getField(name).get(engine.settings())
			);
		}
	}

	public static class Diagnostics {
		@CommandFunction
		public static void ReadIndex(final Object context, final String path, final String engine) {
			final Index index = Index.loadShallow(Index.class, new File(path), null, Core.instance().loadEngine(engine));
			try {
				index.postLoad();
			} catch (final CoreException e) {
				e.printStackTrace();
				return;
			}
			System.out.println("===Objects==="); //$NON-NLS-1$
			index.allDefinitions(item -> System.out.println(item.toString()));
			System.out.println("===Scripts==="); //$NON-NLS-1$
			for (final Script script : index.scripts())
				System.out.println(script.toString());
			System.out.println("===Scenarios==="); //$NON-NLS-1$
			for (final Scenario scen : index.scenarios())
				System.out.println(scen.toString());
		}
		@CommandFunction
		public static void GC(final Object context) { System.gc(); }
		@CommandFunction
		public static void ReloadIndex(final Object context, final String projectName) {
			final ClonkProjectNature nature = ClonkProjectNature.get(projectName);
			if (nature != null)
				nature.reloadIndex();
		}
		@CommandFunction
		public static void PrintHashCodes(final Object context, final String projectName) {
			final ClonkProjectNature nature = ClonkProjectNature.get(projectName);
			final Map<Integer, Declaration> m = new HashMap<>();
			if (nature != null)
				nature.index().allScripts(item -> {
					System.out.println(item.toString());
					for (final Declaration d : item.subDeclarations(nature.index(), DeclMask.ALL)) {
						if (m.containsKey(d.hashCode()))
							System.out.println(String.format("\tconflict:%d", d.hashCode()));
						m.put(d.hashCode(), d);
					}
				});
		}
		@CommandFunction
		public static void CountNodes(final Object context, final String projectName) {
			class Counter {
				int result;
				public int count(final ASTNode node) {
					if (node != null) {
						result++;
						for (final ASTNode sn : node.subElements())
							count(sn);
					}
					return result;
				}
			}
			System.out.println(String.format("%d", new Counter().count(ClonkProjectNature.get(projectName).index())));
		}
		@CommandFunction
		public static void ToggleFlag(final Object context, final String flag) {
			try {
				final Field f = Flags.class.getField(flag);
				f.set(null, !(Boolean) f.get(null));
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
		@CommandFunction
		public static void OutputTree(final Object context, final String projectName, final String outputFile) {
			final ClonkProjectNature nat = ClonkProjectNature.get(ResourcesPlugin.getWorkspace().getRoot().getProject(projectName));
			class Output implements IASTVisitor<Void>, AutoCloseable {
				Writer writer;
				int depth;
				Output() throws IOException {
					this.writer = new FileWriter(new File(outputFile));
				}
				@Override
				public void close() {
					try {
						writer.close();
					} catch (final IOException e) {
						e.printStackTrace();
					}
				}
				@Override
				public TraversalContinuation visitNode(final ASTNode node, final Void context) {
					final ASTNode[] subs = node.subElements();
					try {
						writer.append(multiply("\t", depth));
						if (node instanceof Declaration)
							writer.append(String.format("%s (%s)", ((Declaration) node).name(), node.getClass().getSimpleName()));
						else if (subs.length == 0)
							writer.append(node.printed());
						else
							writer.append(node.getClass().getSimpleName());
						writer.append('\n');
					} catch (final Exception e) {
						return TraversalContinuation.Cancel;
					}
					depth++;
					for (final ASTNode s : subs)
						if (s != null)
							s.traverse(this, context);
					depth--;
					return TraversalContinuation.SkipSubElements;
				}
			}
			try (Output output = new Output()) {
				nat.index().traverse(output, null);
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
		@CommandFunction
		public static void XmlDefinition(final Object context, final String _name) {
			final Engine oc = Core.instance().loadEngine("OpenClonk");
			final XMLDocImporter importer = oc.docImporter();
			importer.initialize();
			for (final String name : _name.split(";")) {
				final ExtractedDeclarationDocumentation doc = importer
					.extractDeclarationInformationFromFunctionXml(name, ClonkPreferences.languagePref(), XMLDocImporter.SIGNATURE);
				final Declaration declaration = doc.isVariable ? new Variable(name, PrimitiveType.ANY) : new Function(name, FunctionScope.GLOBAL);
				@SuppressWarnings("serial")
				final SelfContainedScript s = new SelfContainedScript("_", "", new Index() {
					@Override
					public Engine engine() { return oc; }
				});
				s.addDeclaration(declaration);
				if (declaration instanceof Function) {
					final Function f = (Function)declaration;
					if (doc.parameters != null)
						f.setParameters(doc.parameters);
				}
				if (doc.returnType != null && declaration instanceof ITypeable)
					((ITypeable)declaration).forceType(doc.returnType);
				System.out.println(declaration.printed());
			}
		}
		@CommandFunction
		public static void AllDefinitions(final Object context, final String proj, final String format) {
			final ClonkProjectNature cpn = ClonkProjectNature.get(proj);
			final List<String> defs = new LinkedList<>();
			cpn.index().allDefinitions((Definition item) -> defs.add(String.format(format, item.id().stringValue())));
			System.out.println(blockString("", "", ";", defs));
		}
	}

}
