package net.arctics.clonk.debug;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.ProjectIndex;
import net.arctics.clonk.index.Scenario;
import net.arctics.clonk.parser.c4script.statictyping.StaticTypingUtil;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.resource.c4group.C4Group.GroupType;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
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
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;

public class ClonkLaunchConfigurationDelegate extends LaunchConfigurationDelegate {
	
	public static final String LAUNCH_TYPE = Core.id("debug.ClonkLaunch"); //$NON-NLS-1$
	
	public static final String ATTR_PROJECT_NAME = Core.id("debug.ProjectNameAttr"); //$NON-NLS-1$
	public static final String ATTR_SCENARIO_NAME = Core.id("debug.ScenarioNameAttr"); //$NON-NLS-1$
	public static final String ATTR_FULLSCREEN = Core.id("debug.FullscreenAttr"); //$NON-NLS-1$
	public static final String ATTR_RECORD = Core.id("debug.RecordAttr"); //$NON-NLS-1$
	public static final String ATTR_CUSTOMARGS = Core.id("debug.CustomArgs"); //$NON-NLS-1$
	
	public static int DEFAULT_DEBUG_PORT = 10464;
	
	@Override
	public synchronized void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
			
		// Get scenario and engine
		final IFolder scenario = verifyScenario(configuration);
		final File engine = verifyClonkInstall(configuration, scenario);
		final String[] launchArgs = verifyLaunchArguments(configuration, scenario, engine, mode);
		
		// Don't launch engine multiple times
		if (Target.existingDebugTargetForScenario(scenario) != null)
			return;
		
		// Set up monitor
		if(monitor == null)
			monitor = new NullProgressMonitor();
		monitor.beginTask(String.format(Messages.LaunchConf, configuration.getName()), 2);
		
		try {
			
			// Working directory (work around a bug in early Linux engines)
			final File workDirectory = engine.getParentFile();
			
			// Progress
			if(monitor.isCanceled())
				return;
			monitor.worked(1);
			monitor.subTask(Messages.StartingClonkEngine);
			
			// Run the engine
			try {
				if (mode.equals(ILaunchManager.DEBUG_MODE)) {
					final Scenario scenarioObj = Scenario.get(scenario);
					if (scenarioObj != null && !scenarioObj.engine().settings().supportsDebugging)
						abort(IStatus.ERROR, String.format(Messages.EngineDoesNotSupportDebugging, scenarioObj.engine().name()));
				}
				final Process process = Runtime.getRuntime().exec(launchArgs, null, workDirectory);
				final Map<String, Object> processAttributes = new HashMap<String, Object>();
				processAttributes.put(IProcess.ATTR_PROCESS_TYPE, "clonkEngine"); //$NON-NLS-1$
				processAttributes.put(IProcess.ATTR_PROCESS_LABEL, scenario.getProjectRelativePath().toOSString());
				final IProcess p = DebugPlugin.newProcess(launch, process, configuration.getName(), processAttributes);
				if (mode.equals(ILaunchManager.DEBUG_MODE))
					try {
						final IDebugTarget target = new Target(launch, p, DEFAULT_DEBUG_PORT, scenario);
						launch.addDebugTarget(target);
					} catch (final Exception e) {
						e.printStackTrace();
					}
			} catch(final IOException e) {
				abort(IStatus.ERROR, Messages.CouldNotStartEngine, e);
			}
				
		} finally {
			monitor.done();
		}
		
	}
	
	/** Helper for throwing CoreException objects */
	public void abort(int severity, String message) throws CoreException {
		throw new CoreException(new Status(severity, Core.PLUGIN_ID, message));
	}
	public void abort(int severity, String message, Throwable nested) throws CoreException {
		throw new CoreException(new Status(severity, Core.PLUGIN_ID, message, nested));
	}
	
	/** 
	 * Searches the scenario to launch
	 */
	public IFolder verifyScenario(ILaunchConfiguration configuration) throws CoreException {
		
		// Get project and scenario name from configuration
		final String projectName = configuration.getAttribute(ATTR_PROJECT_NAME, ""); //$NON-NLS-1$
		final String scenarioName = configuration.getAttribute(ATTR_SCENARIO_NAME, ""); //$NON-NLS-1$
		
		// Get project
		final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		final IProject project = root.getProject(projectName);
		if(project == null || !project.isOpen())
			abort(IStatus.ERROR, String.format(Messages.ProjectNotOpen, projectName));
		
		// Get scenario
		final IFolder scenario = project.getFolder(scenarioName);
		if(scenario == null || !scenario.exists())
			abort(IStatus.ERROR, String.format(Messages.ScenarioNotFound, projectName));
		
		return scenario;
	}
	
	/** 
	 * Searches an appropriate Clonk installation for launching the scenario.
	 * @param configuration The launch configuration
	 * @param scenario Scenario folder
	 * @return The path of the Clonk engine executable
	 */
	public File verifyClonkInstall(ILaunchConfiguration configuration, IFolder scenario) throws CoreException {
		
		final Index index = ProjectIndex.fromResource(scenario);
		final String gamePath = index != null ? index.engine().settings().gamePath : null;

		File enginePath = new File("Unspecified"); //$NON-NLS-1$
		String enginePref = index != null ? index.engine().settings().engineExecutablePath : null;
		if (enginePref == null)
			enginePref = ""; //$NON-NLS-1$
		if (!enginePref.equals("")) //$NON-NLS-1$
			enginePath = new File(enginePref);
		else {
			// Try some variants in an attempt to find the engine (ugh...)
			final String[] engineNames = Engine.possibleEngineNamesAccordingToOS(); 
			for(final String name : engineNames) {
				final File path = new File(gamePath, name);
				if(path.exists()) {
					enginePath = path;
					break;
				}
			}
		}
		if(!enginePath.exists())
			abort(IStatus.ERROR, String.format(Messages.CouldNotFindEngine, enginePath.getAbsolutePath()));

		// TODO: Do some more verification? Check engine version?
	
		return enginePath;
	}
	
	public static String resFilePath(IResource res) {
		return new Path(res.getRawLocationURI().getSchemeSpecificPart()).toOSString();
	}
	
	private static String cmdLineOptionString(Engine engine, String option) {
		return String.format(engine.settings().cmdLineOptionFormat, option);
	}
	
	private static String cmdLineOptionString(Engine engine, String option, String argument) {
		return String.format(engine.settings().cmdLineOptionWithArgumentFormat, option, argument);
	}
	
	/**
	 * Collects arguments to pass to the engine at launch
	 * @param configuration Launch configuration
	 * @param scenario Scenario resource
	 * @param engine Engine executable
	 * @param mode (either {@link ILaunchManager#DEBUG_MODE} or {@link ILaunchManager#RUN_MODE}
	 * @return Command-line arguments to run the engine with
	 * @throws CoreException
	 */
	public String[] verifyLaunchArguments(ILaunchConfiguration configuration, IFolder scenario, File engine, String mode) throws CoreException {
		
		Scenario scenarioObj = null;
		Engine engineObj = null;
		ClonkProjectNature nature = null;
		try {
			scenarioObj = Scenario.get(scenario);
			engineObj = scenarioObj.engine();
			nature = ClonkProjectNature.get(scenario);
		} catch (final Exception e) {
			final String s = scenarioObj == null ? Messages.ClonkLaunchConfigurationDelegate_NoScenario : Messages.ClonkLaunchConfigurationDelegate_SomethingWentWrong;
			throw new CoreException(new Status(IStatus.ERROR, Core.PLUGIN_ID, s));
		}
		if (engineObj == null)
			return null;
		
		File tempFolder = null;
		try {
			if (nature.settings().typing.allowsNonParameterAnnotations()) {
				tempFolder = Files.createTempDirectory("c4dt").toFile(); //$NON-NLS-1$
				StaticTypingUtil.mirrorDirectoryWithTypingAnnotationsRemoved(nature.getProject(), tempFolder, true);
			}
		} catch (final IOException e) {
			e.printStackTrace();
			return null;
		}
		
		final Collection<String> args = new LinkedList<String>();  
			
		// Engine
		args.add(engine.getAbsolutePath());
		
		// Scenario
		addWorkspaceDependency(scenario, nature, args, tempFolder);
		
		// add stuff from the project so Clonk does not fail to find them
		for (final Index index : ClonkProjectNature.get(scenario).index().relevantIndexes())
			if (index instanceof ProjectIndex) {
				final IContainer projectLevel = ((ProjectIndex)index).nature().getProject();
				for (IContainer c = scenario.getParent(); c != null && c != projectLevel.getParent(); c = c.getParent())
					for (final IResource res : c.members())
						if (!res.getName().startsWith(".") && res instanceof IContainer) { //$NON-NLS-1$
							final GroupType gType = engineObj.groupTypeForFileName(res.getName());
							if (gType == GroupType.DefinitionGroup || gType == GroupType.ResourceGroup)
								if (!Utilities.resourceInside(scenario, (IContainer) res))
									addWorkspaceDependency((IContainer)res, ((ProjectIndex)index).nature(), args, tempFolder);
						}
			}
		
		// Full screen/console
		if(configuration.getAttribute(ATTR_FULLSCREEN, false))
			args.add(cmdLineOptionString(engineObj, "fullscreen")); //$NON-NLS-1$
		else {
			args.add(cmdLineOptionString(engineObj, engineObj.settings().editorCmdLineOption));
			args.add(cmdLineOptionString(engineObj, "noleague")); //$NON-NLS-1$
		}
		
		// Record
		if(configuration.getAttribute(ATTR_RECORD, false))
			args.add(cmdLineOptionString(engineObj, "record")); //$NON-NLS-1$
	
		// Debug
		if (mode.equals(ILaunchManager.DEBUG_MODE)) {
			args.add(String.format(cmdLineOptionString(engineObj, "debug", "%d"), DEFAULT_DEBUG_PORT)); //$NON-NLS-1$ //$NON-NLS-2$
			args.add(cmdLineOptionString(engineObj, "debugwait")); //$NON-NLS-1$
		}
		
		final String custom = configuration.getAttribute(ATTR_CUSTOMARGS, (String)null);
		if (custom != null) {
			// FIXME: doesn't take into account '\ ' and such..
			final String[] split = custom.split(" "); //$NON-NLS-1$
			for (final String s : split)
				args.add(s);
		}
		
		return args.toArray(new String [args.size()]);
	}

	private void addWorkspaceDependency(IContainer res, ClonkProjectNature nature, Collection<String> args, File tempFolder) {
		if (tempFolder != null)
			args.add(Path.fromOSString(tempFolder.getAbsolutePath()).append(res.getProjectRelativePath()).toOSString());
		else
			args.add(resFilePath(res));
	}
	
}
