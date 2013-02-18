package net.arctics.clonk.cli;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Scanner;

import net.arctics.clonk.Core;
import net.arctics.clonk.command.Command;
import net.arctics.clonk.command.ExecutableScript;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.util.StreamUtil;

import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

/**
 * Commandline interface to some of the c4dt features, like verification of scripts.
 * @author madeen
 *
 */
public class CLI implements IApplication {

	public static void main(String[] args) throws Exception {
		new CLI().run(args);
	}

	public String engine;
	public String engineConfigurationRoot;

	private int parseOptions(String[] args) {
		for (int i = 0; i < args.length; i++) {
			String a = args[i];
			if (a.equals("-application")) {
				i++; // ignore standard Equinox application argument
				continue;
			}
			if (a.startsWith("--")) {
				String option = a.substring(2);
				++i;
				if (i >= args.length)
					throw new IllegalArgumentException("Value required for " + option);
				String value = args[i];
				try {
					getClass().getField(option).set(this, value);
				} catch (Exception e) {
					throw new IllegalArgumentException(String.format("Invalid value for '%s': '%s'", option, value));
				}
			} else
				return i;
		}
		return args.length;
	}

	/**
	 * Main entry point. Will interpret arguments of the form --<option>=<value> as assignment to the instance field <option>
	 * and the rest of the arguments as <method> <parameters...>
	 * @param args Arguments to interpret. Passed from {@link #main(String[])}
	 */
	public void run(String[] args) {
		if (args == null || args.length == 0)
			args = Platform.getCommandLineArgs();
		int methodIndex = parseOptions(args);
		if (methodIndex == args.length)
			throw new IllegalArgumentException("Missing command");
		String methodName = args[methodIndex];
		for (Method method : getClass().getMethods())
			if (method.getName().equals(methodName))
				try {
					initialize();
					method.invoke(this, (Object[])Arrays.copyOfRange(args, methodIndex+1, args.length));
					return;
				} catch (IllegalArgumentException e) {
					throw e;
				} catch (Exception e) {
					e.printStackTrace();
					// fallthrough to invalid command
				}
		throw new IllegalArgumentException(String.format("Invalid command: '%s'", methodName));
	}

	private void initialize() {
		if (engine == null || engineConfigurationRoot == null)
			throw new IllegalArgumentException("--engine and --engineConfigurationRoot command required");
		Core.headlessInitialize(engineConfigurationRoot, engine);
	}

	/**
	 * repl interface using c4script expressions
	 */
	public void repl() {
		boolean done = false;
		Scanner scanner = new Scanner(System.in);
		try {
			while (!done) {
				String command = scanner.nextLine();
				ExecutableScript script = Command.executableScriptFromCommand(command);
				if (script != null) try {
					Object result = script.main().invoke(script.main().new FunctionInvocation(new Object[0], null));
					if (result != null)
						System.out.println(result.toString());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} finally {
			scanner.close();
		}
	}

	public void verifyScript(String fileName) {
		C4ScriptParser parser = new C4ScriptParser(new ExecutableScript(fileName, StreamUtil.stringFromFile(new File(fileName)), new Index() {
			private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
			@Override
			public Engine engine() {
				return Core.instance().activeEngine();
			}
		}));
		try {
			parser.parse();
		} catch (ParsingException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Object start(IApplicationContext context) throws Exception {
		main(new String[0]);
		return null;
	}

	@Override
	public void stop() {}

}
