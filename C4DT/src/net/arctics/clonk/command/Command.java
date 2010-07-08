package net.arctics.clonk.command;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.C4Engine;
import net.arctics.clonk.index.C4Object;
import net.arctics.clonk.index.C4Scenario;
import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.SimpleScriptStorage;
import net.arctics.clonk.parser.c4script.C4Function;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.ControlFlowException;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.ExprElm;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.IEvaluationContext;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.ExpressionListener;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.IVariableValueProvider;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.ReturnException;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.Statement;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.TraversalContinuation;
import net.arctics.clonk.resource.ClonkIndexStream;
import net.arctics.clonk.ui.editors.ClonkHyperlink;
import net.arctics.clonk.util.Utilities;

/**
 * Macro execution engine based on the C4Script parser. Naturally, macros are written in C4Script.
 *
 */
public class Command {
	public static final C4ScriptBase COMMAND_BASESCRIPT;
	public static final ClonkIndex COMMANDS_INDEX = new ClonkIndex();
	public static final String COMMAND_SCRIPT_TEMPLATE = "func Main() {%s;}"; //$NON-NLS-1$

	public static class C4CommandScript extends C4ScriptBase {

		private static class C4CommandFunction extends C4Function {
			private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

			private transient Statement[] statements;

			@Override
			public Object invoke(final Object... args) {
				final IVariableValueProvider variableProvider = args != null && args.length > 0 && args[0] instanceof IVariableValueProvider ? (IVariableValueProvider)args[0] : null;
				IEvaluationContext context = new IEvaluationContext() {

					@Override
					public Object[] getArguments() {
						return args;
					}

					@Override
					public C4Function getFunction() {
						return C4CommandFunction.this;
					}

					@Override
					public Object getValueForVariable(String varName) {
						return variableProvider != null ? variableProvider.getValueForVariable(varName) : null;
					}

				};
				for (Statement s : statements) {
					try {
						s.evaluate(context);
					} catch (ReturnException e) {
						return e.getResult();
					} catch (ControlFlowException e) {
						switch (e.getControlFlow()) {
						case BreakLoop:
							return null;
						case Continue:
							break;
						default:
							return null;
						}
					}
				}
				return null;
			}
		}

		private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

		private String script;
		private C4CommandFunction main;

		@Override
		public ClonkIndex getIndex() {
			return COMMANDS_INDEX;
		}

		@Override
		public String getScriptText() {
			return script;
		}

		@Override
		public Object getScriptFile() {
			try {
				return new SimpleScriptStorage(getName(), script);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				return null;
			}
		}

		public C4CommandScript(String name, String script) {
			super();
			setName(name);
			this.script = script;
			C4ScriptParser parser = new C4ScriptParser(script, this, null) {
				@Override
				protected C4Function newFunction() {
					return new C4CommandFunction();
				}
				@Override
				public void parseCodeOfFunction(C4Function function) throws ParsingException {
					if (function.getName().equals("Main")) { //$NON-NLS-1$
						main = (C4CommandFunction)function;
					}
					final List<Statement> statements = new LinkedList<Statement>();
					this.setExpressionListener(new ExpressionListener() {
						@Override
						public TraversalContinuation expressionDetected(ExprElm expression, C4ScriptParser parser) {
							if (expression instanceof Statement)
								statements.add((Statement)expression);
							return TraversalContinuation.Continue;
						}
					});
					super.parseCodeOfFunction(function);
					((C4CommandFunction)function).statements = statements.toArray(new Statement[statements.size()]);
					this.setExpressionListener(null);
				}
			};
			try {
				parser.parse();
			} catch (ParsingException e) {
				e.printStackTrace();
			}
		}

		@Override
		public C4ScriptBase[] getIncludes(ClonkIndex index) {
			return new C4ScriptBase[] {
					COMMAND_BASESCRIPT
			};
		}

		public C4CommandFunction getMain() {
			return main;
		}

		public Object invoke(Object... args) {
			return main.invoke(args);
		}

	}

	static {
		COMMAND_BASESCRIPT = new C4ScriptBase() {
			private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

			@Override
			public ClonkIndex getIndex() {
				return COMMANDS_INDEX;
			}
			@Override
			public Object getScriptFile() {
				try {
					return new SimpleScriptStorage("CommandBase", ""); //$NON-NLS-1$ //$NON-NLS-2$
				} catch (UnsupportedEncodingException e) {
					return null;
				}
			}

			@Override
			public String getName() {
				return "CommandBaseScript"; //$NON-NLS-1$
			};

			@Override
			public String getNodeName() {
				return getName();
			};

		};

		for (Class<?> c : Command.class.getDeclaredClasses())
			registerCommandsFromClass(c);
	}

	private static void registerCommandsFromClass(Class<?> classs) {
		for (Method m : classs.getMethods()) {
			if (m.getAnnotation(CommandFunction.class) != null)
				addCommand(m);
		}
	}

	private static class C4CommandFunction extends C4Function {

		private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

		private final transient Method method;

		@Override
		public Object invoke(Object... args) {
			try {
				return method.invoke(null, Utilities.concat(this, args));
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}

		public C4CommandFunction(C4ScriptBase parent, Method method) {
			super(method.getName(), parent, C4FunctionScope.FUNC_PUBLIC);
			this.method = method;
		}

	}

	public static void addCommand(Method method) {
		COMMAND_BASESCRIPT.addDeclaration(new C4CommandFunction(COMMAND_BASESCRIPT, method));
	}

	public static void setFieldValue(Object obj, String name, Object value) {
		Class<?> c = obj instanceof Class<?> ? (Class<?>)obj : obj.getClass();
		try {
			Field f = c.getField(name);
			if (value instanceof Long && f.getType() == Integer.TYPE) {
				value = ((Long)value).intValue();
			}
			else if (value instanceof String && f.getType().getSuperclass() == Enum.class) {
				value = f.getType().getMethod("valueOf", String.class).invoke(f.getClass(), value); //$NON-NLS-1$
			}
			f.set(obj, value);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

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
			ClonkHyperlink.openDocumentationForFunction(funcName, ClonkCore.getDefault().getActiveEngine());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static class CodeConversionCommands {
		@CommandFunction
		public static void SetCodeConversionOption(Object context, String option, Object value) {
			setFieldValue(C4ScriptExprTree.class, option, value);
		}
		@CommandFunction
		public static void WriteEngineScript(Object context, String engineName, String fileName) throws IOException {
			C4Engine engine = ClonkCore.getDefault().loadEngine(engineName);
			FileOutputStream stream = new FileOutputStream(fileName);
			Writer writer = new OutputStreamWriter(stream);
			engine.writeEngineScript(writer);
			writer.flush();
			writer.close();
			stream.close();
		}
		@CommandFunction
		public static void ImportDescriptionsFromSerializedIndex(Object context, String engineName, String indexPath, String writeToFile) throws IOException, ClassNotFoundException {
			InputStream engineStream = new FileInputStream(indexPath);
			try {
				ObjectInputStream objStream = new ClonkIndexStream(engineStream);
				C4Engine result = (C4Engine)objStream.readObject();
				result.setName(engineName); // for good measure
				result.postSerialize(null);
				_WriteDescriptionsToFile(writeToFile, result);
			} finally {
				engineStream.close();
			}
		}
		private static void _WriteDescriptionsToFile(String writeToFile, C4Engine engine) throws FileNotFoundException, IOException {
			OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(writeToFile));
			writer.append("[Descriptions]\n"); //$NON-NLS-1$
			for (C4Function f : engine.functions()) {
				String escaped = f.getUserDescription() != null ? f.getUserDescription().replace("\n", "|||") : ""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				writer.append(String.format("%s=%s\n", f.getName(), escaped)); //$NON-NLS-1$
			}
			writer.close();
		}
		@CommandFunction
		public static void WriteDescriptionsToFile(Object context, String writeToFile, String engineName) throws FileNotFoundException, IOException {
			C4Engine engine = ClonkCore.getDefault().loadEngine(engineName);
			if (engine != null)
				_WriteDescriptionsToFile(writeToFile, engine);
		}
	}

	public static class EngineConfiguration {
		@CommandFunction
		public static void SetEngineProperty(Object context, String name, Object value) {
			setFieldValue(ClonkCore.getDefault().getActiveEngine().getCurrentSettings(), name, value);
		}
		@CommandFunction
		public static void IntrinsicizeEngineProperty(Object context, String name) throws IllegalArgumentException, SecurityException, IllegalAccessException, NoSuchFieldException {
			C4Engine engine = ClonkCore.getDefault().getActiveEngine();
			setFieldValue(
					engine.getIntrinsicSettings(), name,
					engine.getCurrentSettings().getClass().getField(name).get(engine.getCurrentSettings())
			);
		}
	}

	public static class Diagnostics {
		@CommandFunction
		public static void ReadIndex(Object context, String path) {
			ClonkIndex index = ClonkIndex.load(ClonkIndex.class, new File(path), null);
			try {
				index.postSerialize();
			} catch (CoreException e) {
				e.printStackTrace();
				return;
			}
			System.out.println("===Objects==="); //$NON-NLS-1$
			for (C4Object obj : index) {
				System.out.println(obj.toString());
			}
			System.out.println("===Scripts==="); //$NON-NLS-1$
			for (C4ScriptBase script : index.getIndexedScripts()) {
				System.out.println(script.toString());
			}
			System.out.println("===Scenarios==="); //$NON-NLS-1$
			for (C4Scenario scen : index.getIndexedScenarios()) {
				System.out.println(scen.toString());
			}
		}
		@CommandFunction
		public static void GC(Object context) {
			System.gc();
		}
	}

}
