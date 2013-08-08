package net.arctics.clonk.command;

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
import java.util.HashMap;
import java.util.Map;

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
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.typing.StaticTypingUtil;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.Scenario;
import net.arctics.clonk.ui.editors.EntityHyperlink;
import net.arctics.clonk.util.SelfcontainedStorage;
import net.arctics.clonk.util.Sink;

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

	public static ExecutableScript executableScriptFromCommand(String command) {
		return new ExecutableScript("command", String.format("func Main() {%s;}", command), Command.INDEX);
	}

	public static void registerCommandsFromClass(Script script, Class<?> classs) {
		for (final Method m : classs.getMethods())
			if (m.getAnnotation(CommandFunction.class) != null)
				addCommand(script, m);
	}

	private static class NativeCommandFunction extends Function {

		private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

		private final transient Method method;

		@Override
		public Object invoke(IEvaluationContext context) {
			try {
				final Object[] args = new Object[method.getParameterTypes().length];
				args[0] = context;
				System.arraycopy(context.arguments(), 0, args, 1, context.arguments().length);
				return method.invoke(context, args);
			} catch (final Exception e) {
				e.printStackTrace();
				return null;
			}
		}

		public NativeCommandFunction(Script parent, Method method) {
			super(parent, FunctionScope.PUBLIC, method.getName());
			this.method = method;
		}

	}

	public static void addCommand(Script script, Method method) {
		script.addDeclaration(new NativeCommandFunction(script, method));
	}

	public static void addCommand(Method method) {
		addCommand(BASE, method);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void setFieldValue(Object obj, String name, Object value) {
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
		public static void Log(Object context, String message) {
			System.out.println(message);
		}

		@CommandFunction
		public static String Format(Object context, String format, Object... args) {
			return String.format(format, args);
		}

		@CommandFunction
		public static void OpenDoc(Object context, String funcName) {
			try {
				EntityHyperlink.openDocumentationForFunction(funcName, Core.instance().activeEngine());
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static class CodeConversionCommands {
		@CommandFunction
		public static void SetCodeConversionOption(Object context, String option, Object value) {
			setFieldValue(Conf.class, option, value);
		}
		@CommandFunction
		public static void WriteEngineScript(Object context, String engineName) throws IOException {
			Core.instance().loadEngine(engineName).writeEngineScript();
		}
		private static void _WriteDescriptionsToFile(String writeToFile, Engine engine) throws FileNotFoundException, IOException {
			final OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(writeToFile));
			writer.append("[Descriptions]\n"); //$NON-NLS-1$
			for (final Function f : engine.functions()) {
				final String escaped = f.obtainUserDescription() != null ? f.obtainUserDescription().replace("\n", "|||") : ""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				writer.append(String.format("%s=%s\n", f.name(), escaped)); //$NON-NLS-1$
			}
			writer.close();
		}
		@CommandFunction
		public static void WriteDescriptionsToFile(Object context, String writeToFile, String engineName) throws FileNotFoundException, IOException {
			final Engine engine = Core.instance().loadEngine(engineName);
			if (engine != null)
				_WriteDescriptionsToFile(writeToFile, engine);
		}
		@CommandFunction
		public static void convertProject(Object context, String source, String dest) {
			final ProjectConverter converter = new ProjectConverter(
				ResourcesPlugin.getWorkspace().getRoot().getProject(source),
				ResourcesPlugin.getWorkspace().getRoot().getProject(dest)
			);
			converter.convert(new NullProgressMonitor());
		}
	}

	public static class EngineConfiguration {
		@CommandFunction
		public static void SetEngineProperty(Object context, String name, Object value) {
			setFieldValue(Core.instance().activeEngine().settings(), name, value);
		}
		@CommandFunction
		public static void IntrinsicizeEngineProperty(Object context, String name) throws IllegalArgumentException, SecurityException, IllegalAccessException, NoSuchFieldException {
			final Engine engine = Core.instance().activeEngine();
			setFieldValue(
				engine.intrinsicSettings(), name,
				engine.settings().getClass().getField(name).get(engine.settings())
			);
		}
	}

	public static class Diagnostics {
		@CommandFunction
		public static void ReadIndex(Object context, String path, String engine) {
			final Index index = Index.loadShallow(Index.class, new File(path), null, Core.instance().loadEngine(engine));
			try {
				index.postLoad();
			} catch (final CoreException e) {
				e.printStackTrace();
				return;
			}
			System.out.println("===Objects==="); //$NON-NLS-1$
			index.allDefinitions(new Sink<Definition>() {
				@Override
				public void receivedObject(Definition item) {
					System.out.println(item.toString());
				}
			});
			System.out.println("===Scripts==="); //$NON-NLS-1$
			for (final Script script : index.scripts())
				System.out.println(script.toString());
			System.out.println("===Scenarios==="); //$NON-NLS-1$
			for (final Scenario scen : index.scenarios())
				System.out.println(scen.toString());
		}
		@CommandFunction
		public static void GC(Object context) { System.gc(); }
		@CommandFunction
		public static void ReloadIndex(Object context, String projectName) {
			final ClonkProjectNature nature = ClonkProjectNature.get(projectName);
			if (nature != null)
				nature.reloadIndex();
		}
		@CommandFunction
		public static void PrintHashCodes(Object context, String projectName) {
			final ClonkProjectNature nature = ClonkProjectNature.get(projectName);
			final Map<Integer, Declaration> m = new HashMap<>();
			if (nature != null)
				nature.index().allScripts(new Sink<Script>() {
					@Override
					public void receivedObject(Script item) {
						System.out.println(item.toString());
						for (final Declaration d : item.subDeclarations(nature.index(), DeclMask.ALL)) {
							if (m.containsKey(d.hashCode()))
								System.out.println(String.format("\tconflict:%d", d.hashCode()));
							m.put(d.hashCode(), d);
						}
					}
				});
		}
		@CommandFunction
		public static void CountNodes(Object context, String projectName) {
			class Counter {
				int result;
				public int count(ASTNode node) {
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
		public static void ToggleFlag(Object context, String flag) {
			try {
				final Field f = Flags.class.getField(flag);
				f.set(null, !(Boolean) f.get(null));
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
		@CommandFunction
		public static void OutputTree(Object context, String projectName, final String outputFile) {
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
				public TraversalContinuation visitNode(ASTNode node, Void context) {
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
	}

}
