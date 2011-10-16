package net.arctics.clonk.command;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Scenario;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.SimpleScriptStorage;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.parser.c4script.ast.Conf;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.ui.editors.ClonkHyperlink;
import net.arctics.clonk.util.ArrayUtil;

/**
 * Macro execution engine based on the C4Script parser. Naturally, macros are written in C4Script.
 *
 */
public class Command {
	public static final Script COMMAND_BASESCRIPT;
	public static final Index COMMANDS_INDEX = new Index();
	public static final String COMMAND_SCRIPT_TEMPLATE = "func Main() {%s;}"; //$NON-NLS-1$

	static {
		COMMAND_BASESCRIPT = new Script(COMMANDS_INDEX) {
			private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
			@Override
			public IStorage getScriptStorage() {
				try {
					return new SimpleScriptStorage("CommandBase", ""); //$NON-NLS-1$ //$NON-NLS-2$
				} catch (UnsupportedEncodingException e) {
					return null;
				}
			}
			@Override
			public String name() {
				return "CommandBaseScript"; //$NON-NLS-1$
			};
			@Override
			public String getNodeName() {
				return name();
			};
		};

		for (Class<?> c : Command.class.getDeclaredClasses())
			registerCommandsFromClass(c);
	}
	
	public static ExecutableScript executableScriptFromCommand(String command) {
		return new ExecutableScript("command", String.format(Command.COMMAND_SCRIPT_TEMPLATE, command), Command.COMMANDS_INDEX);
	}

	private static void registerCommandsFromClass(Class<?> classs) {
		for (Method m : classs.getMethods()) {
			if (m.getAnnotation(CommandFunction.class) != null)
				addCommand(m);
		}
	}

	private static class NativeCommandFunction extends Function {

		private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

		private final transient Method method;

		@Override
		public Object invoke(Object... args) {
			try {
				return method.invoke(null, ArrayUtil.concat(this, args));
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}

		public NativeCommandFunction(Script parent, Method method) {
			super(method.getName(), parent, FunctionScope.PUBLIC);
			this.method = method;
		}

	}

	public static void addCommand(Method method) {
		COMMAND_BASESCRIPT.addDeclaration(new NativeCommandFunction(COMMAND_BASESCRIPT, method));
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
				ClonkHyperlink.openDocumentationForFunction(funcName, ClonkCore.getDefault().getActiveEngine());
			} catch (Exception e) {
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
		public static void WriteEngineScript(Object context, String engineName, String fileName) throws IOException {
			Engine engine = ClonkCore.getDefault().loadEngine(engineName);
			FileOutputStream stream = new FileOutputStream(fileName);
			Writer writer = new OutputStreamWriter(stream);
			engine.writeEngineScript(writer);
			writer.flush();
			writer.close();
			stream.close();
		}
		private static void _WriteDescriptionsToFile(String writeToFile, Engine engine) throws FileNotFoundException, IOException {
			OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(writeToFile));
			writer.append("[Descriptions]\n"); //$NON-NLS-1$
			for (Function f : engine.functions()) {
				String escaped = f.obtainUserDescription() != null ? f.obtainUserDescription().replace("\n", "|||") : ""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				writer.append(String.format("%s=%s\n", f.name(), escaped)); //$NON-NLS-1$
			}
			writer.close();
		}
		@CommandFunction
		public static void WriteDescriptionsToFile(Object context, String writeToFile, String engineName) throws FileNotFoundException, IOException {
			Engine engine = ClonkCore.getDefault().loadEngine(engineName);
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
			Engine engine = ClonkCore.getDefault().getActiveEngine();
			setFieldValue(
					engine.getIntrinsicSettings(), name,
					engine.getCurrentSettings().getClass().getField(name).get(engine.getCurrentSettings())
			);
		}
	}

	public static class Diagnostics {
		@CommandFunction
		public static void ReadIndex(Object context, String path) {
			Index index = Index.loadShallow(Index.class, new File(path), null);
			try {
				index.postLoad();
			} catch (CoreException e) {
				e.printStackTrace();
				return;
			}
			System.out.println("===Objects==="); //$NON-NLS-1$
			for (Definition obj : index) {
				System.out.println(obj.toString());
			}
			System.out.println("===Scripts==="); //$NON-NLS-1$
			for (Script script : index.indexedScripts()) {
				System.out.println(script.toString());
			}
			System.out.println("===Scenarios==="); //$NON-NLS-1$
			for (Scenario scen : index.indexedScenarios()) {
				System.out.println(scen.toString());
			}
		}
		@CommandFunction
		public static void GC(Object context) {
			System.gc();
		}
		@CommandFunction
		public static void ReloadIndex(Object context, String projectName) {
			ClonkProjectNature nature = ClonkProjectNature.get(projectName);
			if (nature != null) {
				nature.reloadIndex();
			}
		}
	}

}
