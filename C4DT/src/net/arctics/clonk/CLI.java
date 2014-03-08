package net.arctics.clonk;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Scanner;
import java.util.Stack;

import net.arctics.clonk.c4script.Function.FunctionInvocation;
import net.arctics.clonk.c4script.ScriptParser;
import net.arctics.clonk.command.Command;
import net.arctics.clonk.command.CommandFunction;
import net.arctics.clonk.command.ExecutableScript;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.StreamUtil;

import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

/**
 * Commandline interface to some of the c4dt features, like verification of scripts.
 * @author madeen
 *
 */
public class CLI implements IApplication, AutoCloseable {
	{
		Command.registerCommandsFromClass(Command.BASE, CLI.class);
	}
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Callable {}
	public static void main(final String[] args) throws Exception {
		try {
			new CLI().run(args);
			System.exit(2);
		} catch (final Exception e) {
			System.out.println(e.getMessage());
			System.exit(0);
		}
	}
	public String engine;
	public String engineConfigurationRoot;
	private final Scanner input = new Scanner(System.in);
	@Override
	public void close() throws Exception { input.close(); }
	private int parseOptions(final String[] args) {
		readSettingsFromHome();
		for (int i = 0; i < args.length; i++) {
			final String a = args[i];
			if (a.equals("-application")) {
				i++; // ignore standard Equinox application argument
				continue;
			}
			if (a.startsWith("--")) {
				final String option = a.substring(2);
				++i;
				if (i >= args.length)
					throw new IllegalArgumentException("Value required for " + option);
				final String value = args[i];
				try {
					getClass().getField(option).set(this, value);
				} catch (final Exception e) {
					throw new IllegalArgumentException(String.format("Invalid value for '%s': '%s'", option, value));
				}
			} else
				return i;
		}
		return args.length;
	}
	private void readSettingsFromHome() {
		final File settingsFile = new File(new File(System.getenv().get("HOME")), ".c4dt");
		if (settingsFile.exists()) {
			final String[] settings = StreamUtil.stringFromFile(settingsFile).split("\n");
			for (final String s : settings) {
				final String[] split = s.split("=");
				if (split.length != 2)
					continue;
				try {
					getClass().getField(split[0]).set(this, split[1]);
				} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
					e.printStackTrace();
				}
			}
		}
	}
	/**
	 * Main entry point. Will interpret arguments of the form --<option>=<value> as assignment to the instance field <option>
	 * and the rest of the arguments as <method> <parameters...>
	 * @param args Arguments to interpret. Passed from {@link #main(String[])}
	 */
	public void run(String[] args) {
		if (args == null || args.length == 0)
			args = Platform.getCommandLineArgs();
		final int methodIndex = parseOptions(args);
		if (methodIndex == args.length)
			throw new IllegalArgumentException("Missing command");
		final String methodName = args[methodIndex];
		for (final Method method : getClass().getMethods())
			if (method.getName().equals(methodName) && method.getAnnotation(Callable.class) != null)
				try {
					initialize();
					method.invoke(this, ArrayUtil.concat(
						(Object[])Arrays.copyOfRange(args, methodIndex+1, args.length),
						new Object[method.getParameterTypes().length-(args.length-(methodIndex+1))])
					);
					return;
				} catch (final IllegalArgumentException e) {
					throw e;
				} catch (final Exception e) {
					e.printStackTrace();
				}
		throw new IllegalArgumentException(String.format("Invalid command: '%s'", methodName));
	}
	private void initialize() {
		if (engine == null || engineConfigurationRoot == null)
			throw new IllegalArgumentException("--engine and --engineConfigurationRoot command required");
		Core.headlessInitialize(engineConfigurationRoot, engine);
	}
	private class DoneToken implements AutoCloseable {
		public DoneToken() { doneTokens.push(this); }
		public boolean done = false;
		@Override
		public void close() {
			if (doneTokens.peek() != this)
				throw new UnsupportedOperationException();
			else
				doneTokens.pop();
		}
	}
	private final Stack<DoneToken> doneTokens = new Stack<>();
	@CommandFunction
	public static void exit(FunctionInvocation context) {
		((CLI)context.self()).exit();
	}
	@Callable
	public void exit() {
		doneTokens.peek().done = true;
	}
	/**
	 * repl interface using c4script expressions
	 */
	@Callable
	public void repl() {
		try (final DoneToken done = new DoneToken()) {
			while (!done.done) {
				final String command = input.nextLine();
				final ExecutableScript script = Command.executableScriptFromCommand(command);
				if (script != null) try {
					final Object result = script.main().invoke(script.main().new FunctionInvocation(new Object[0], null, this));
					if (result != null)
						System.out.println(result.toString());
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	@Callable
	public void interactive() {
		try (final DoneToken done = new DoneToken()) {
			while (!done.done) {
				final String command = input.nextLine();
				try {
					run(command.split("\\s"));
				} catch (final Exception e) {
					System.err.println(e.getMessage());
				}
			}
		}
	}
	@Callable
	public void verifyScript(final String fileName) {
		final ScriptParser parser = new ScriptParser(new ExecutableScript(fileName, StreamUtil.stringFromFile(new File(fileName)), new Index() {
			private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
			@Override
			public Engine engine() {
				return Core.instance().activeEngine();
			}
		}));
		try {
			parser.parse();
		} catch (final ProblemException e) {
			e.printStackTrace();
		}
	}
	private static String readFile() {
		return StreamUtil.stringFromInputStream(System.in);
	}
	@Callable
	public void printAST(final String fileName) throws ProblemException {
		final String scriptText = fileName != null ? StreamUtil.stringFromFile(new File(fileName)) : readFile();
		final ScriptParser parser = new ScriptParser(new ExecutableScript(fileName, scriptText, new Index() {
			private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
			@Override
			public Engine engine() {
				return Core.instance().activeEngine();
			}
		}));
		parser.parse();
		System.out.println(parser.script().printed());
	}
	@Callable
	public void help(String on) {
		System.out.println("I dunno");
	}
	@Override
	public Object start(final IApplicationContext context) throws Exception {
		main(new String[0]);
		return null;
	}
	@Override
	public void stop() {}
}
