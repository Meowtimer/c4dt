package net.arctics.clonk.debug;

import static net.arctics.clonk.util.StreamUtil.ofType;
import static net.arctics.clonk.util.Utilities.tri;
import static net.arctics.clonk.util.Utilities.walk;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import net.arctics.clonk.Core;
import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.c4group.FileExtension;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.typing.StaticTypingUtil;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.ProjectIndex;
import net.arctics.clonk.index.Scenario;
import net.arctics.clonk.util.StreamUtil;
import net.arctics.clonk.util.StringUtil;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;

public class EngineLaunch implements ILaunchesListener2 {
	private final ILaunchConfiguration configuration;
	private final IFolder scenarioFolder;
	private final IPath scenarioFolderPath;
	private final File engineFile;
	private final String mode;
	private final Scenario scenario;
	private final ClonkProjectNature nature;
	private final File tempFolder;
	private final Collection<String> args;
	private final ILaunch launch;

	static final Map<IPath, EngineLaunch> list = new HashMap<IPath, EngineLaunch>();

	private static EngineLaunch get(final IPath scenarioPath) {
		synchronized (list) {
			return list.get(scenarioPath);
		}
	}

	public static void scriptsBuilt(final Script[] scripts) {
		synchronized (list) {
			if (list.isEmpty())
				return;
		}
		Arrays.stream(scripts).forEach(s -> {
			final Scenario scen = s.scenario();
			final EngineLaunch l = scen != null ? get(scen.resource().getFullPath()) : null;
			if (l != null)
				l.eraseTypeAnnotations(s);
		});
	}

	public static class MultipleLaunchesException extends CoreException {
		private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
		public MultipleLaunchesException(final IStatus status) { super(status); }
	}

	public EngineLaunch(final ILaunchConfiguration configuration, final ILaunch launch, final IFolder scenarioFolder, final File engineFile, final String mode) throws CoreException {
		this.launch = launch;
		this.configuration = configuration;
		this.scenarioFolder = scenarioFolder;
		this.scenarioFolderPath = scenarioFolder.getFullPath();;
		this.engineFile = engineFile;
		this.mode = mode;
		Scenario s = null;
		ClonkProjectNature n = null;
		try {
			s = Scenario.get(scenarioFolder);
			n = ClonkProjectNature.get(scenarioFolder);
		} catch (final Exception e) {
			final String err = s == null ? Messages.ClonkLaunchConfigurationDelegate_NoScenario
					: Messages.ClonkLaunchConfigurationDelegate_SomethingWentWrong;
			throw new CoreException(new Status(IStatus.ERROR, Core.PLUGIN_ID, err));
		}
		this.scenario = s;
		this.nature = n;
		this.tempFolder = maybeTempFolder();
		this.args = determineArguments();
		// Don't launch engine multiple times
		register();
		DebugPlugin.getDefault().getLaunchManager().addLaunchListener(this);
	}

	private void register() throws MultipleLaunchesException {
		synchronized (list) {
			if (list.get(scenarioFolderPath) != null)
				throw new MultipleLaunchesException(new Status(IStatus.ERROR, Core.PLUGIN_ID,
					String.format("Already launched: %s", scenarioFolderPath)));
			else
				list.put(scenarioFolderPath, this);
		}
	}

	public File maybeTempFolder() {
		File tf = null;
		if (nature.settings().typing.allowsNonParameterAnnotations())
			try {
				tf = Files.createTempDirectory("c4dt").toFile().getCanonicalFile(); //$NON-NLS-1$
				StaticTypingUtil.mirrorDirectoryWithTypingAnnotationsRemoved(StaticTypingUtil.toFile(nature.getProject()), tf, true);
			} catch (final IOException e) {
				e.printStackTrace();
			}
		return tf;
	}

	void addWorkspaceDependency(Collection<String> args, final IContainer res) {
		if (tempFolder != null)
			args.add(Path.fromOSString(tempFolder.getAbsolutePath()).append(res.getProjectRelativePath()).toOSString());
		else
			args.add(ClonkLaunchConfigurationDelegate.resFilePath(res));
	}

	public String[] arguments() {
		return args.toArray(new String[args.size()]);
	}

	public Collection<String> determineArguments() throws CoreException {
		final Engine engine = nature.index().engine();
		final Collection<String> args = new LinkedList<>();
		// Engine
		args.add(engineFile.getAbsolutePath());

		// Scenario
		this.addWorkspaceDependency(args, scenarioFolder);

		// add stuff from the project so Clonk does not fail to find them
		ofType(ClonkProjectNature.get(scenarioFolder).index().relevantIndexes().stream(), ProjectIndex.class)
			.flatMap(index -> {
				final IContainer projectLevel = index.nature().getProject();
				return walk(scenarioFolder.getParent(), c -> c != null && c != projectLevel, c -> c.getParent());
			})
			.flatMap(c -> {
				final IResource[] mems = tri(() -> c.members(), CoreException.class, e -> e.printStackTrace());
				return mems != null
					? Arrays.stream(mems)
						.filter(res -> {
							final FileExtension ext = engine.extensionForFileName(res.getName());
							return
								!res.getName().startsWith(".") && res instanceof IContainer &&
								(ext == FileExtension.DefinitionGroup || ext == FileExtension.ResourceGroup) &&
								!Utilities.resourceInside(scenarioFolder, (IContainer)res);
						})
					: Stream.empty();
			})
			.forEach(res -> this.addWorkspaceDependency(args, (IContainer) res));

		// Full screen/console
		if (configuration.getAttribute(ClonkLaunchConfigurationDelegate.ATTR_FULLSCREEN, false))
			args.add(ClonkLaunchConfigurationDelegate.cmdLineOptionString(engine, "fullscreen")); //$NON-NLS-1$
		else {
			args.add(ClonkLaunchConfigurationDelegate.cmdLineOptionString(engine, engine.settings().editorCmdLineOption));
			args.add(ClonkLaunchConfigurationDelegate.cmdLineOptionString(engine, "noleague")); //$NON-NLS-1$
		}

		// Record
		if (configuration.getAttribute(ClonkLaunchConfigurationDelegate.ATTR_RECORD, false))
			args.add(ClonkLaunchConfigurationDelegate.cmdLineOptionString(engine, "record")); //$NON-NLS-1$

		// Debug
		if (mode.equals(ILaunchManager.DEBUG_MODE)) {
			args.add(String.format(ClonkLaunchConfigurationDelegate.cmdLineOptionString(engine, "debug", "%d"), ClonkLaunchConfigurationDelegate.DEFAULT_DEBUG_PORT)); //$NON-NLS-1$ //$NON-NLS-2$
			args.add(ClonkLaunchConfigurationDelegate.cmdLineOptionString(engine, "debugwait")); //$NON-NLS-1$
		}

		final String custom = configuration.getAttribute(ClonkLaunchConfigurationDelegate.ATTR_CUSTOMARGS, (String) null);
		if (custom != null)
			// FIXME: doesn't take into account '\ ' and such..
			Arrays.stream(custom.split(" ")).forEach(args::add);
		
		return args;
	}

	@Override
	public void launchesAdded(final ILaunch[] launches) {}
	@Override
	public void launchesChanged(final ILaunch[] launches) {}
	@Override
	public void launchesRemoved(final ILaunch[] launches) {}

	@Override
	public void launchesTerminated(final ILaunch[] launches) {
		synchronized (list) {
			list.remove(scenarioFolder.getFullPath());
		}
		for (final ILaunch l : launches)
			if (l == launch) {
				DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(this);
				try {
					if (tempFolder != null)
						Utilities.removeRecursively(tempFolder);
				} catch (final Exception e) {}
			}
	}

	public void launch(final IProgressMonitor monitor) throws CoreException {
		try {
			// Working directory (work around a bug in early Linux engines)
			final File workDirectory = engineFile.getParentFile();

			// Progress
			if (monitor.isCanceled())
				return;
			monitor.worked(1);
			monitor.subTask(Messages.StartingClonkEngine);

			// Run the engine
			try {
				if (mode.equals(ILaunchManager.DEBUG_MODE))
					if (scenario != null && !scenario.engine().settings().supportsDebugging)
						Utilities.abort(IStatus.ERROR,
							String.format(Messages.EngineDoesNotSupportDebugging, scenario.engine().name()));
				final Process process = new ProcessBuilder(arguments()).directory(workDirectory).start();
				final Map<String, String> processAttributes = new HashMap<String, String>();
				processAttributes.put(IProcess.ATTR_PROCESS_TYPE, "clonkEngine"); //$NON-NLS-1$
				processAttributes.put(IProcess.ATTR_PROCESS_LABEL, scenarioFolder.getProjectRelativePath().toOSString());
				final IProcess p = DebugPlugin.newProcess(launch, process, configuration.getName(), processAttributes);
				if (mode.equals(ILaunchManager.DEBUG_MODE))
					try {
						final IDebugTarget target = new Target(launch, p, ClonkLaunchConfigurationDelegate.DEFAULT_DEBUG_PORT, scenarioFolder);
						launch.addDebugTarget(target);
					} catch (final Exception e) {
						e.printStackTrace();
					}
			} catch (final IOException e) {
				Utilities.abort(IStatus.ERROR, Messages.CouldNotStartEngine, e);
			}
		} finally {
			monitor.done();
		}
	}

	private void eraseTypeAnnotations(final Script script) {
		if (tempFolder == null)
			return;
		final String purged = StaticTypingUtil.eraseTypeAnnotations(script);
		if (purged != null) {
			final IResource res = script.file();
			final List<String> breadcrumb = new LinkedList<>();
			breadcrumb.add(res.getName());
			for (IContainer c = res.getParent(); c != null; c = c.getParent())
				if (c == nature.getProject()) {
					final File dest = new File(tempFolder, StringUtil.blockString("", "", File.separator, breadcrumb));
					try {
						StreamUtil.writeToFile(dest, (file, stream, writer) -> writer.write(purged));
					} catch (final IOException e) {
						e.printStackTrace();
					}
					break;
				} else
					breadcrumb.add(0, c.getName());
		}
	}

}