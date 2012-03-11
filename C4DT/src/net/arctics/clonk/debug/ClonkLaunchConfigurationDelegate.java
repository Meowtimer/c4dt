package net.arctics.clonk.debug;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.ProjectIndex;
import net.arctics.clonk.index.Scenario;
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
		IFolder scenario = verifyScenario(configuration);
		File engine = verifyClonkInstall(configuration, scenario);
		String[] launchArgs = verifyLaunchArguments(configuration, scenario, engine, mode);
		
		// Don't launch engine multiple times
		if (ClonkDebugTarget.existingDebugTargetForScenario(scenario) != null)
			return;
		
		// Set up monitor
		if(monitor == null)
			monitor = new NullProgressMonitor();
		monitor.beginTask(String.format(Messages.LaunchConf, configuration.getName()), 2);
		
		try {
			
			// Working directory (work around a bug in early Linux engines)
			File workDirectory = engine.getParentFile();
			
			// Progress
			if(monitor.isCanceled()) return;
			monitor.worked(1);
			monitor.subTask(Messages.StartingClonkEngine);
			
			// Run the engine
			try {
				if (mode.equals(ILaunchManager.DEBUG_MODE)) {
					Scenario scenarioObj = Scenario.get(scenario);
					if (scenarioObj != null && !scenarioObj.engine().settings().supportsDebugging)
						abort(IStatus.ERROR, String.format(Messages.EngineDoesNotSupportDebugging, scenarioObj.engine().name()));
				}
				Process process = Runtime.getRuntime().exec(launchArgs, null, workDirectory);
				Map<String, Object> processAttributes = new HashMap<String, Object>();
				processAttributes.put(IProcess.ATTR_PROCESS_TYPE, "clonkEngine");
				processAttributes.put(IProcess.ATTR_PROCESS_LABEL, scenario.getProjectRelativePath().toOSString());
				IProcess p = DebugPlugin.newProcess(launch, process, configuration.getName(), processAttributes);
				if (mode.equals(ILaunchManager.DEBUG_MODE)) {
					try {
						IDebugTarget target = new ClonkDebugTarget(launch, p, DEFAULT_DEBUG_PORT, scenario);
						launch.addDebugTarget(target);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} catch(IOException e) {
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
		String projectName = configuration.getAttribute(ATTR_PROJECT_NAME, ""); //$NON-NLS-1$
		String scenarioName = configuration.getAttribute(ATTR_SCENARIO_NAME, ""); //$NON-NLS-1$
		
		// Get project
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject project = root.getProject(projectName);
		if(project == null || !project.isOpen())
			abort(IStatus.ERROR, String.format(Messages.ProjectNotOpen, projectName));
		
		// Get scenario
		IFolder scenario = project.getFolder(scenarioName);
		if(scenario == null || !scenario.exists())
			abort(IStatus.ERROR, String.format(Messages.ScenarioNotFound, projectName));
		
		return scenario;
	}
	
	/** 
	 * Searches an appropriate Clonk installation for launching the scenario.
	 * @return The path of the Clonk engine executable
	 */
	public File verifyClonkInstall(ILaunchConfiguration configuration, IFolder scenario) throws CoreException {
		
		Index index = Utilities.indexFromResource(scenario);
		String gamePath = index != null ? index.engine().settings().gamePath : null;

		File enginePath = new File("Unspecified");
		String enginePref = index != null ? index.engine().settings().engineExecutablePath : null;
		if (enginePref == null)
			enginePref = ""; //$NON-NLS-1$
		if (!enginePref.equals("")) //$NON-NLS-1$
			enginePath = new File(enginePref);
		else {
			// Try some variants in an attempt to find the engine (ugh...)
			final String[] engineNames = Engine.possibleEngineNamesAccordingToOS(); 
			for(String name : engineNames) {
				File path = new File(gamePath, name);
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
	 * @param mode 
	 */
	public String[] verifyLaunchArguments(ILaunchConfiguration configuration, IResource scenario, File engine, String mode) throws CoreException {
		Collection<String> args = new LinkedList<String>();  
			
		// Engine
		args.add(engine.getAbsolutePath());
		
		// Scenario
		args.add(resFilePath(scenario));
		
		Engine engineObj;
		try {
			engineObj = Scenario.get((IContainer) scenario).engine();
		} catch (Exception e) {
			return null;
		}
		if (engineObj == null)
			return null;
		
		// add stuff from the project so Clonk does not fail to find them
		for (Index index : ClonkProjectNature.get(scenario).index().relevantIndexes()) {
			if (index instanceof ProjectIndex) {
				IContainer projectLevel = ((ProjectIndex)index).project();
				for (IContainer c = scenario.getParent(); c != null && c != projectLevel.getParent(); c = c.getParent()) {
					for (IResource res : c.members()) {
						if (!res.getName().startsWith(".") && res instanceof IContainer) { //$NON-NLS-1$
							GroupType gType = engineObj.groupTypeForFileName(res.getName());
							if (gType == GroupType.DefinitionGroup || gType == GroupType.ResourceGroup)
								if (!Utilities.resourceInside(scenario, (IContainer) res))
									args.add(resFilePath(res));
						}
					}
				}
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
			args.add(String.format(cmdLineOptionString(engineObj, "debug", "%d"), DEFAULT_DEBUG_PORT)); //$NON-NLS-1$
			args.add(cmdLineOptionString(engineObj, "debugwait")); //$NON-NLS-1$
		}
		
		String custom = configuration.getAttribute(ATTR_CUSTOMARGS, (String)null);
		if (custom != null) {
			// FIXME: doesn't take into account '\ ' and such..
			String[] split = custom.split(" "); //$NON-NLS-1$
			for (String s : split)
				args.add(s);
		}
		
		return args.toArray(new String [args.size()]);
	}
	
}
