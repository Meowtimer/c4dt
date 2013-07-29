package net.arctics.clonk.debug;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.arctics.clonk.Core;
import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.c4group.C4Group.GroupType;
import net.arctics.clonk.c4script.typing.StaticTypingUtil;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.ProjectIndex;
import net.arctics.clonk.index.Scenario;
import net.arctics.clonk.util.StreamUtil;
import net.arctics.clonk.util.StreamUtil.StreamWriteRunnable;
import net.arctics.clonk.util.StringUtil;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;

public class ClonkLaunchConfigurationDelegate extends
		LaunchConfigurationDelegate {

	public static final String LAUNCH_TYPE = Core.id("debug.ClonkLaunch"); //$NON-NLS-1$

	public static final String ATTR_PROJECT_NAME = Core
			.id("debug.ProjectNameAttr"); //$NON-NLS-1$
	public static final String ATTR_SCENARIO_NAME = Core
			.id("debug.ScenarioNameAttr"); //$NON-NLS-1$
	public static final String ATTR_FULLSCREEN = Core
			.id("debug.FullscreenAttr"); //$NON-NLS-1$
	public static final String ATTR_RECORD = Core.id("debug.RecordAttr"); //$NON-NLS-1$
	public static final String ATTR_CUSTOMARGS = Core.id("debug.CustomArgs"); //$NON-NLS-1$

	public static int DEFAULT_DEBUG_PORT = 10464;

	@Override
	public synchronized void launch(ILaunchConfiguration configuration,
			String mode, ILaunch launch, IProgressMonitor monitor)
			throws CoreException {

		// Get scenario and engine
		final IFolder scenario = verifyScenario(configuration);
		final File engine = verifyClonkInstall(configuration, scenario);

		final Launchling launchling = new Launchling(configuration, launch,
				scenario, engine, mode);
		launchling.record();
		launchling.launch(monitor);

		// Set up monitor
		if (monitor == null)
			monitor = new NullProgressMonitor();
		monitor.beginTask(
				String.format(Messages.LaunchConf, configuration.getName()), 2);

	}

	/** Helper for throwing CoreException objects */
	public void abort(int severity, String message) throws CoreException {
		throw new CoreException(new Status(severity, Core.PLUGIN_ID, message));
	}

	public void abort(int severity, String message, Throwable nested)
			throws CoreException {
		throw new CoreException(new Status(severity, Core.PLUGIN_ID, message,
				nested));
	}

	/**
	 * Searches the scenario to launch
	 */
	public IFolder verifyScenario(ILaunchConfiguration configuration)
			throws CoreException {

		// Get project and scenario name from configuration
		final String projectName = configuration.getAttribute(
				ATTR_PROJECT_NAME, ""); //$NON-NLS-1$
		final String scenarioName = configuration.getAttribute(
				ATTR_SCENARIO_NAME, ""); //$NON-NLS-1$

		// Get project
		final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		final IProject project = root.getProject(projectName);
		if (project == null || !project.isOpen())
			abort(IStatus.ERROR,
					String.format(Messages.ProjectNotOpen, projectName));

		// Get scenario
		final IFolder scenario = project.getFolder(scenarioName);
		if (scenario == null || !scenario.exists())
			abort(IStatus.ERROR,
					String.format(Messages.ScenarioNotFound, projectName));

		return scenario;
	}

	/**
	 * Searches an appropriate Clonk installation for launching the scenario.
	 * 
	 * @param configuration
	 *            The launch configuration
	 * @param scenario
	 *            Scenario folder
	 * @return The path of the Clonk engine executable
	 */
	public File verifyClonkInstall(ILaunchConfiguration configuration,
			IFolder scenario) throws CoreException {

		final Index index = ProjectIndex.fromResource(scenario);
		final String gamePath = index != null ? index.engine().settings().gamePath
				: null;

		File enginePath = new File("Unspecified"); //$NON-NLS-1$
		String enginePref = index != null ? index.engine().settings().engineExecutablePath
				: null;
		if (enginePref == null)
			enginePref = ""; //$NON-NLS-1$
		if (!enginePref.equals("")) //$NON-NLS-1$
			enginePath = new File(enginePref);
		else {
			// Try some variants in an attempt to find the engine (ugh...)
			final String[] engineNames = Engine
					.possibleEngineNamesAccordingToOS();
			for (final String name : engineNames) {
				final File path = new File(gamePath, name);
				if (path.exists()) {
					enginePath = path;
					break;
				}
			}
		}
		if (!enginePath.exists())
			abort(IStatus.ERROR,
					String.format(Messages.CouldNotFindEngine,
							enginePath.getAbsolutePath()));

		// TODO: Do some more verification? Check engine version?

		return enginePath;
	}

	public static String resFilePath(IResource res) {
		return new Path(res.getRawLocationURI().getSchemeSpecificPart())
				.toOSString();
	}

	private static String cmdLineOptionString(Engine engine, String option) {
		return String.format(engine.settings().cmdLineOptionFormat, option);
	}

	private static String cmdLineOptionString(Engine engine, String option,
			String argument) {
		return String.format(engine.settings().cmdLineOptionWithArgumentFormat,
				option, argument);
	}

	class Launchling implements ILaunchesListener2, IResourceChangeListener {
		final ILaunchConfiguration configuration;
		final IFolder scenarioFolder;
		final File engineFile;
		final String mode;
		final Scenario scenario;
		final ClonkProjectNature nature;
		final File tempFolder;
		final Collection<String> args;
		final ILaunch launch;

		Launchling(ILaunchConfiguration configuration, ILaunch launch,
				IFolder scenarioFolder, File engineFile, String mode)
				throws CoreException {
			this.launch = launch;
			this.configuration = configuration;
			this.scenarioFolder = scenarioFolder;
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
				throw new CoreException(new Status(IStatus.ERROR,
						Core.PLUGIN_ID, err));
			}
			this.scenario = s;
			// Don't launch engine multiple times
			if (Target.existingDebugTargetForScenario(scenarioFolder) != null)
				throw new CoreException(new Status(IStatus.ERROR,
						Core.PLUGIN_ID, "Already launched"));
			this.nature = n;
			this.args = new LinkedList<String>();
			File tf = null;
			try {
				if (nature.settings().typing.allowsNonParameterAnnotations()) {
					tf = Files.createTempDirectory("c4dt").toFile(); //$NON-NLS-1$
					StaticTypingUtil.mirrorDirectoryWithTypingAnnotationsRemoved(nature.getProject(), tf, true);
					DebugPlugin.getDefault().getLaunchManager().addLaunchListener(this);
				}
			} catch (final IOException e) {
				e.printStackTrace();
			}
			this.tempFolder = tf;
		}

		void addWorkspaceDependency(IContainer res) {
			if (tempFolder != null)
				args.add(Path.fromOSString(tempFolder.getAbsolutePath()).append(res.getProjectRelativePath()).toOSString());
			else
				args.add(resFilePath(res));
		}

		public void addArgument(String arg) {
			args.add(arg);
		}

		public String[] arguments() {
			return args.toArray(new String[args.size()]);
		}

		public void record() throws CoreException {
			final Engine engine = nature.index().engine();
			// Engine
			this.args.add(engineFile.getAbsolutePath());

			// Scenario
			this.addWorkspaceDependency(scenarioFolder);

			// add stuff from the project so Clonk does not fail to find them
			for (final Index index : ClonkProjectNature.get(scenarioFolder).index().relevantIndexes())
				if (index instanceof ProjectIndex) {
					final IContainer projectLevel = ((ProjectIndex) index)
							.nature().getProject();
					for (IContainer c = scenarioFolder.getParent(); c != null
							&& c != projectLevel.getParent(); c = c.getParent())
						for (final IResource res : c.members())
							if (!res.getName().startsWith(".") && res instanceof IContainer) { //$NON-NLS-1$
								final GroupType gType = engine
										.groupTypeForFileName(res.getName());
								if (gType == GroupType.DefinitionGroup
										|| gType == GroupType.ResourceGroup)
									if (!Utilities.resourceInside(
											scenarioFolder, (IContainer) res))
										this.addWorkspaceDependency((IContainer) res);
							}
				}

			// Full screen/console
			if (configuration.getAttribute(ATTR_FULLSCREEN, false))
				this.addArgument(cmdLineOptionString(engine, "fullscreen")); //$NON-NLS-1$
			else {
				this.addArgument(cmdLineOptionString(engine,
						engine.settings().editorCmdLineOption));
				this.addArgument(cmdLineOptionString(engine, "noleague")); //$NON-NLS-1$
			}

			// Record
			if (configuration.getAttribute(ATTR_RECORD, false))
				this.addArgument(cmdLineOptionString(engine, "record")); //$NON-NLS-1$

			// Debug
			if (mode.equals(ILaunchManager.DEBUG_MODE)) {
				this.addArgument(String
						.format(cmdLineOptionString(engine, "debug", "%d"), DEFAULT_DEBUG_PORT)); //$NON-NLS-1$ //$NON-NLS-2$
				this.addArgument(cmdLineOptionString(engine, "debugwait")); //$NON-NLS-1$
			}

			final String custom = configuration.getAttribute(ATTR_CUSTOMARGS, (String) null);
			if (custom != null) {
				// FIXME: doesn't take into account '\ ' and such..
				final String[] split = custom.split(" "); //$NON-NLS-1$
				for (final String s : split)
					this.addArgument(s);
			}
		}

		@Override
		public void launchesAdded(ILaunch[] arg0) {}
		@Override
		public void launchesChanged(ILaunch[] arg0) {}
		@Override
		public void launchesRemoved(ILaunch[] arg0) {}

		@Override
		public void launchesTerminated(ILaunch[] launches) {
			for (final ILaunch l : launches)
				if (l == launch) {
					DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(this);
					ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
					try {
						if (tempFolder != null)
							Utilities.removeRecursively(tempFolder);
					} catch (final Exception e) {}
				}
		}

		public void launch(IProgressMonitor monitor) throws CoreException {
			if (tempFolder != null) {
				ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
			}
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
					if (mode.equals(ILaunchManager.DEBUG_MODE)) {
						if (scenario != null && !scenario.engine().settings().supportsDebugging)
							abort(IStatus.ERROR, String.format(
								Messages.EngineDoesNotSupportDebugging,
								scenario.engine().name()));
					}
					final Process process = Runtime.getRuntime().exec(arguments(), null, workDirectory);
					final Map<String, Object> processAttributes = new HashMap<String, Object>();
					processAttributes.put(IProcess.ATTR_PROCESS_TYPE, "clonkEngine"); //$NON-NLS-1$
					processAttributes.put(IProcess.ATTR_PROCESS_LABEL, scenarioFolder.getProjectRelativePath().toOSString());
					final IProcess p = DebugPlugin.newProcess(launch, process, configuration.getName(), processAttributes);
					if (mode.equals(ILaunchManager.DEBUG_MODE))
						try {
							final IDebugTarget target = new Target(launch, p, DEFAULT_DEBUG_PORT, scenarioFolder);
							launch.addDebugTarget(target);
						} catch (final Exception e) {
							e.printStackTrace();
						}
				} catch (final IOException e) {
					abort(IStatus.ERROR, Messages.CouldNotStartEngine, e);
				}
			} finally {
				monitor.done();
			}
		}

		@Override
		public void resourceChanged(IResourceChangeEvent event) {
			try {
				event.getDelta().accept(new IResourceDeltaVisitor() {
					@Override
					public boolean visit(IResourceDelta delta) throws CoreException {
						switch (delta.getKind()) {
						case IResourceDelta.CHANGED:
							if ((delta.getFlags() & IResourceDelta.CONTENT) == IResourceDelta.CONTENT && delta.getResource() instanceof IFile) {
								final String purged = StaticTypingUtil.purgeTyping((IFile) delta.getResource());
								if (purged != null) {
									final List<String> breadcrump = new LinkedList<>();
									breadcrump.add(delta.getResource().getName());
									for (IContainer c = delta.getResource().getParent(); c != null; c = c.getParent()) {
										if (c == nature.getProject()) {
											final File dest = new File(tempFolder, StringUtil.blockString("", "", File.separator, breadcrump));
											try {
												StreamUtil.writeToFile(dest, new StreamWriteRunnable() {
													@Override
													public void run(File file, OutputStream stream, OutputStreamWriter writer) throws IOException {
														writer.write(purged);
													}
												});
											} catch (final IOException e) {
												e.printStackTrace();
											}
										} else
											breadcrump.add(0, c.getName());
									}
								}
							}
							break;
						}
						return true;
					}
				});
			} catch (final CoreException e) {
				e.printStackTrace();
			}
		}
	}
}
