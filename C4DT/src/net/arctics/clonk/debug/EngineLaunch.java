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
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
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

class EngineLaunch implements ILaunchesListener2, IResourceChangeListener {
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

	public static class MultipleLaunchesException extends CoreException {
		private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
		public MultipleLaunchesException(IStatus status) { super(status); }
	}

	public EngineLaunch(ILaunchConfiguration configuration, ILaunch launch, IFolder scenarioFolder, File engineFile, String mode) throws CoreException {
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
		// Don't launch engine multiple times
		synchronized (list) {
			if (list.get(scenarioFolderPath) != null)
				throw new MultipleLaunchesException(new Status(IStatus.ERROR, Core.PLUGIN_ID,
					String.format("Already launched: %s", scenarioFolderPath)));
			else
				list.put(scenarioFolderPath, this);
		}
		this.nature = n;
		this.args = new LinkedList<String>();
		File tf = null;
		try {
			if (nature.settings().typing.allowsNonParameterAnnotations()) {
				tf = Files.createTempDirectory("c4dt").toFile(); //$NON-NLS-1$
				StaticTypingUtil.mirrorDirectoryWithTypingAnnotationsRemoved(StaticTypingUtil.toFile(nature.getProject()), tf, true);
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
		this.tempFolder = tf;
		DebugPlugin.getDefault().getLaunchManager().addLaunchListener(this);
		record();
	}

	void addWorkspaceDependency(IContainer res) {
		if (tempFolder != null)
			args.add(Path.fromOSString(tempFolder.getAbsolutePath()).append(res.getProjectRelativePath()).toOSString());
		else
			args.add(ClonkLaunchConfigurationDelegate.resFilePath(res));
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
							final GroupType gType = engine.groupTypeForFileName(res.getName());
							if (gType == GroupType.DefinitionGroup || gType == GroupType.ResourceGroup)
								if (!Utilities.resourceInside(scenarioFolder, (IContainer) res))
									this.addWorkspaceDependency((IContainer) res);
						}
			}

		// Full screen/console
		if (configuration.getAttribute(ClonkLaunchConfigurationDelegate.ATTR_FULLSCREEN, false))
			this.addArgument(ClonkLaunchConfigurationDelegate.cmdLineOptionString(engine, "fullscreen")); //$NON-NLS-1$
		else {
			this.addArgument(ClonkLaunchConfigurationDelegate.cmdLineOptionString(engine,
					engine.settings().editorCmdLineOption));
			this.addArgument(ClonkLaunchConfigurationDelegate.cmdLineOptionString(engine, "noleague")); //$NON-NLS-1$
		}

		// Record
		if (configuration.getAttribute(ClonkLaunchConfigurationDelegate.ATTR_RECORD, false))
			this.addArgument(ClonkLaunchConfigurationDelegate.cmdLineOptionString(engine, "record")); //$NON-NLS-1$

		// Debug
		if (mode.equals(ILaunchManager.DEBUG_MODE)) {
			this.addArgument(String
					.format(ClonkLaunchConfigurationDelegate.cmdLineOptionString(engine, "debug", "%d"), ClonkLaunchConfigurationDelegate.DEFAULT_DEBUG_PORT)); //$NON-NLS-1$ //$NON-NLS-2$
			this.addArgument(ClonkLaunchConfigurationDelegate.cmdLineOptionString(engine, "debugwait")); //$NON-NLS-1$
		}

		final String custom = configuration.getAttribute(ClonkLaunchConfigurationDelegate.ATTR_CUSTOMARGS, (String) null);
		if (custom != null) {
			// FIXME: doesn't take into account '\ ' and such..
			final String[] split = custom.split(" "); //$NON-NLS-1$
			for (final String s : split)
				this.addArgument(s);
		}
	}

	@Override
	public void launchesAdded(ILaunch[] launches) {}
	@Override
	public void launchesChanged(ILaunch[] launches) {}
	@Override
	public void launchesRemoved(ILaunch[] launches) {}

	@Override
	public void launchesTerminated(ILaunch[] launches) {
		synchronized (list) {
			list.remove(scenarioFolder.getFullPath());
		}
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
		if (tempFolder != null)
			ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
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
				final Process process = Runtime.getRuntime().exec(arguments(), null, workDirectory);
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

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		try {
			if (tempFolder == null)
				return;
			event.getDelta().accept(new IResourceDeltaVisitor() {
				@Override
				public boolean visit(IResourceDelta delta) throws CoreException {
					switch (delta.getKind()) {
					case IResourceDelta.CHANGED:
						if ((delta.getFlags() & IResourceDelta.CONTENT) == IResourceDelta.CONTENT && delta.getResource() instanceof IFile) {
							final String purged = StaticTypingUtil.purgeTyping(StaticTypingUtil.toFile(delta.getResource()));
							if (purged != null) {
								final List<String> breadcrump = new LinkedList<>();
								breadcrump.add(delta.getResource().getName());
								for (IContainer c = delta.getResource().getParent(); c != null; c = c.getParent())
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