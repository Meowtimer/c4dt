package net.arctics.clonk;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Scanner;
import java.util.Stack;

import net.arctics.clonk.ast.IEvaluationContext;
import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.builder.ProjectConverter;
import net.arctics.clonk.builder.ProjectSettings;
import net.arctics.clonk.c4group.C4GroupFileSystem;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.Function.Invocation;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.ScriptParser;
import net.arctics.clonk.c4script.typing.Typing;
import net.arctics.clonk.command.Command;
import net.arctics.clonk.command.CommandFunction;
import net.arctics.clonk.command.ExecutableScript;
import net.arctics.clonk.command.SelfContainedScript;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.Index.Built;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.StreamUtil;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

/**
 * Commandline interface to some of the c4dt features, like verification of scripts.
 * @author madeen
 *
 */
public class CLI implements IApplication, AutoCloseable {
	private static final String CLONK_RAGE = "ClonkRage";
	private static final String OPEN_CLONK = "OpenClonk";
	private static class CLIFunction<T> extends Function {
		private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
		private final transient Method method;
		@Override
		public Object invoke(final IEvaluationContext context) {
			try {
				return method.invoke(context.self(), context.arguments());
			} catch (final Exception e) {
				System.err.println(e.getMessage());
				return null;
			}
		}
		public CLIFunction(final Script parent, final Method method) {
			super(parent, FunctionScope.PUBLIC, method.getName());
			this.method = method;
		}
		public static <C> void register(Script script, Class<C> cls) {
			Arrays.asList(cls.getMethods())
				.stream()
				.filter(m -> m.getAnnotation(Callable.class) != null)
				.forEach(m -> script.addDeclaration(new CLIFunction<C>(script, m)));
		}
	}
	{ CLIFunction.register(Command.BASE, CLI.class); }
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Callable {}
	public static void main(final String[] args) throws Exception {
		try {
			new CLI().mainImpl(args);
			System.exit(0);
		} catch (final Exception e) {
			System.out.println(e.getMessage());
			System.exit(2);
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
	private void mainImpl(String[] args) {
		final int methodIndex = parseOptions(args);
		if (methodIndex == args.length)
			throw new IllegalArgumentException("Missing command");
		final String methodName = args[methodIndex];
		final Method method = Arrays.stream(getClass().getMethods())
			.filter(m -> m.getName().equals(methodName) && m.getAnnotation(Callable.class) != null)
			.findFirst().orElse(null);
		if (method != null)
			try {
				initialize();
				method.invoke(this,
					ArrayUtil.concat(
						(Object[])Arrays.copyOfRange(args, methodIndex+1, args.length),
						new Object[method.getParameterTypes().length-(args.length-(methodIndex+1))]
					)
				);
				return;
			} catch (final IllegalArgumentException e) {
				throw e;
			} catch (final Exception e) {
				e.printStackTrace();
			}
		else
			throw new IllegalArgumentException(String.format("Invalid command: '%s'", methodName));
	}
	private void restore() {
		oc = existingProj(OPEN_CLONK);
		cr = existingProj(CLONK_RAGE);
	}
	public IProject existingProj(String projName) {
		final IProject proj = ResourcesPlugin.getWorkspace().getRoot().getProject(projName);
		return proj.exists() ? proj : null;
	}
	private void initialize() {
		if (engine == null || engineConfigurationRoot == null)
			throw new IllegalArgumentException("--engine and --engineConfigurationRoot command required");
		Core.headlessInitialize(engineConfigurationRoot, engine);
		restore();
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
	public static void exit(Invocation context) {
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
					final Object result = script.main().invoke(script.main().new Invocation(new Object[0], null, this));
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
			while (!done.done && input.hasNextLine()) {
				final String command = input.nextLine();
				try {
					mainImpl(command.split("\\s"));
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
	private IProject oc, cr;
	@Callable
	public void setupWorkspace(String ocRepo, String crFolder) {
		try {
			if (ocRepo != null)
				setupOCProject(ocRepo);
			if (crFolder != null)
				setupCRProject(crFolder);
		} catch (final CoreException e) { e.printStackTrace(); }
		System.out.println("Set up workspace");
	}
	@Callable
	public void setupCRProject(String crFolder) throws CoreException {
		final NullProgressMonitor npm = new NullProgressMonitor();
		IProjectDescription desc = ResourcesPlugin.getWorkspace().newProjectDescription(OPEN_CLONK);
		desc.setNatureIds(new String[0]);
		desc.setBuildSpec(new ICommand[0]);

		cr = ResourcesPlugin.getWorkspace().getRoot().getProject(CLONK_RAGE);
		if (cr.exists())
			cr.delete(true, true, npm);
		cr.create(desc, npm);
		cr.open(npm);
		final String[] packs = new String[] {
			"Objects.c4d",
			"System.c4g",
			"Fantasy.c4d",
			"FarWorlds.c4d",
			"Graphics.c4g",
			"Knights.c4d",
			"Material.c4g",
			"MetalMagic.c4d",
			"Music.c4g",
			"Sound.c4g",
			"Western.c4d",
		};
		Arrays.stream(packs).forEach(p -> link(cr, crFolder, p, npm));
		cr.refreshLocal(IResource.DEPTH_INFINITE, npm);

		desc = cr.getDescription();
		desc.setNatureIds(new String[] {Core.NATURE_ID});
		final ICommand command = desc.newCommand();
		command.setBuilderName(Core.id("builder")); //$NON-NLS-1$
		desc.setBuildSpec(new ICommand[] {command});
		cr.setDescription(desc, npm);

		final ClonkProjectNature nature = ClonkProjectNature.get(cr);
		nature.forceIndexRecreation().built(Built.LeaveAlone);
		final ProjectSettings settings = nature.settings();
		settings.engineName = OPEN_CLONK;
		settings.typing = Typing.INFERRED;
		nature.saveSettings();
		cr.build(IncrementalProjectBuilder.CLEAN_BUILD, npm);
		cr.build(IncrementalProjectBuilder.FULL_BUILD, npm);
	}
	private void link(final IProject cr, String crFolder, String name, NullProgressMonitor npm) {
		final IFolder linkedFolder = cr.getFolder(name);
		try {
			linkedFolder.createLink(new URI(C4GroupFileSystem.SCHEME, C4GroupFileSystem.replaceSpecialChars(
				new File(crFolder, name).getAbsolutePath()), null), 0, npm);
		} catch (final URISyntaxException e) {
			e.printStackTrace();
		} catch (final CoreException e) {
			e.printStackTrace();
		}
	}
	@Callable
	public void setupOCProject(String ocRepo) throws CoreException {
		final NullProgressMonitor npm = new NullProgressMonitor();
		IProjectDescription desc = ResourcesPlugin.getWorkspace().newProjectDescription(OPEN_CLONK);
		desc.setLocation(new Path(ocRepo).append("planet"));
		desc.setNatureIds(new String[0]);
		desc.setBuildSpec(new ICommand[0]);

		oc = ResourcesPlugin.getWorkspace().getRoot().getProject(OPEN_CLONK);
		if (oc.exists())
			oc.delete(false, true, npm);
		oc.create(desc, npm);
		oc.open(npm);
		oc.refreshLocal(IResource.DEPTH_INFINITE, npm);

		desc = oc.getDescription();
		desc.setNatureIds(new String[] {Core.NATURE_ID});
		final ICommand command = desc.newCommand();
		command.setBuilderName(Core.id("builder")); //$NON-NLS-1$
		desc.setBuildSpec(new ICommand[] {command});
		oc.setDescription(desc, npm);

		final ClonkProjectNature nature = ClonkProjectNature.get(oc);
		nature.forceIndexRecreation().built(Built.LeaveAlone);
		final ProjectSettings settings = nature.settings();
		settings.engineName = OPEN_CLONK;
		settings.typing = Typing.INFERRED;
		nature.saveSettings();
		oc.build(IncrementalProjectBuilder.CLEAN_BUILD, npm);
		oc.build(IncrementalProjectBuilder.FULL_BUILD, npm);
	}
	@Callable
	public void linkFolderAsProject(String path, String projectName, String engineName) throws CoreException {
		final NullProgressMonitor npm = new NullProgressMonitor();
		final IProject proj = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		if (proj.exists())
			proj.delete(false, true, npm);

		IProjectDescription desc = ResourcesPlugin.getWorkspace().newProjectDescription(projectName);
		desc.setLocation(new Path(path));
		desc.setNatureIds(new String[0]);
		desc.setBuildSpec(new ICommand[0]);

		proj.create(desc, npm);
		proj.open(npm);
		proj.refreshLocal(IResource.DEPTH_INFINITE, npm);

		desc = proj.getDescription();
		desc.setNatureIds(new String[] {Core.NATURE_ID});
		final ICommand command = desc.newCommand();
		command.setBuilderName(Core.id("builder")); //$NON-NLS-1$
		desc.setBuildSpec(new ICommand[] {command});
		final IProject baseProj = engineName.equals(OPEN_CLONK) ? oc : cr;
		if (baseProj != null)
			desc.setReferencedProjects(new IProject[] {baseProj});
		proj.setDescription(desc, npm);

		final ClonkProjectNature nature = ClonkProjectNature.get(proj);
		nature.forceIndexRecreation().built(Built.LeaveAlone);
		final ProjectSettings settings = nature.settings();
		settings.engineName = engineName;
		settings.typing = Typing.INFERRED;
		nature.saveSettings();
		proj.build(IncrementalProjectBuilder.CLEAN_BUILD, npm);
		proj.build(IncrementalProjectBuilder.FULL_BUILD, npm);
	}
	@Callable
	public void run(String fileName) {
		final File f = new File(fileName);
		final Index ndx = new Index() {
			private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
			@Override
			public Engine engine() {
				return Core.instance().loadEngine(OPEN_CLONK);
			}
		};
		final Script script = new SelfContainedScript(f.getName(), StreamUtil.stringFromFile(f), ndx) {
			private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
			@Override
			public boolean gatherIncludes(Index contextIndex, Script origin, Collection<Script> set, int options) {
				set.add(Command.BASE);
				return super.gatherIncludes(contextIndex, origin, set, options);
			}
		};
		final Function main = script.findFunction("Main");
		if (main != null)
			main.invoke(main.new Invocation(new Object[0], null, this));
	}
	@Callable
	public void convertProject(final String source, final String dest, Map<String, Object> conf) {
		final ProjectConverter converter = new ProjectConverter(
			ResourcesPlugin.getWorkspace().getRoot().getProject(source),
			ResourcesPlugin.getWorkspace().getRoot().getProject(dest)
		);
		if (conf != null)
			converter.configuration().apply(conf);
		converter.convert(new NullProgressMonitor());
	}
	@Override
	public Object start(final IApplicationContext context) throws Exception {
		try {
			mainImpl(Platform.getApplicationArgs());
		} catch (final Exception e) {
			e.printStackTrace();
			return 2;
		}
		return EXIT_OK;
	}
	@Override
	public void stop() {}
}
