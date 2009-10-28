package net.arctics.clonk.debug;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.preferences.ClonkPreferences;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.jface.preference.IPreferenceStore;

public class ClonkLaunchConfigurationDelegate implements
		ILaunchConfigurationDelegate {
	
	public static final String LAUNCH_TYPE = ClonkCore.id("debug.ClonkLaunch"); //$NON-NLS-1$
	
	public static final String ATTR_PROJECT_NAME = ClonkCore.id("debug.ProjectNameAttr"); //$NON-NLS-1$
	public static final String ATTR_SCENARIO_NAME = ClonkCore.id("debug.ScenarioNameAttr"); //$NON-NLS-1$
	public static final String ATTR_FULLSCREEN = ClonkCore.id("debug.FullscreenAttr"); //$NON-NLS-1$
	public static final String ATTR_RECORD = ClonkCore.id("debug.RecordAttr"); //$NON-NLS-1$
	
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch,
			IProgressMonitor monitor) throws CoreException {

		// Run only for now
		if(!mode.equals(ILaunchManager.RUN_MODE))
			abort(IStatus.ERROR, Messages.LauncherOnlySupportsRunMode);
		
		// Set up monitor
		if(monitor == null)
			monitor = new NullProgressMonitor();
		monitor.beginTask(String.format(Messages.LaunchConf, configuration.getName()), 2);
		
		try {
			
			// Get scenario and engine
			IResource scenario = verifyScenario(configuration);
			File engine = verifyClonkInstall(configuration);
			String[] launchArgs = verifyLaunchArguments(configuration, scenario, engine);
			
			// Working directory (work around a bug in early Linux engines)
			File workDirectory = engine.getParentFile();
			
			// Progress
			if(monitor.isCanceled()) return;
			monitor.worked(1);
			monitor.subTask(Messages.StartingClonkEngine);
			
			// Run the engine
			try {
				Process process = Runtime.getRuntime().exec(launchArgs, null, workDirectory);
				DebugPlugin.newProcess(launch, process, configuration.getName());
			} catch(IOException e) {
				abort(IStatus.ERROR, Messages.CouldNotStartEngine, e);
			}
				
		} finally {
			monitor.done();
		}
		
	}
	
	/** Helper for throwing CoreException objects */
	public void abort(int severity, String message) throws CoreException {
		throw new CoreException(new Status(severity, ClonkCore.PLUGIN_ID, message));
	}
	public void abort(int severity, String message, Throwable nested) throws CoreException {
		throw new CoreException(new Status(severity, ClonkCore.PLUGIN_ID, message, nested));
	}
	
	/** 
	 * Searches the scenario to launch
	 */
	public IResource verifyScenario(ILaunchConfiguration configuration)
			throws CoreException {
		
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
	
	private String[] possibleEngineNamesAccordingToOS(String OS) {
		if (OS.equals("Mac OS X")) { //$NON-NLS-1$
			return new String[] { "Clonk.app/Contents/MacOS/Clonk" }; //$NON-NLS-1$
		}
		if (OS.contains("Windows")) { //$NON-NLS-1$
			return new String[] { "Clonk.c4x", "Clonk.exe" }; //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (OS.contains("Linux")) { //$NON-NLS-1$
			return new String[] { "clonk" }; //$NON-NLS-1$
		}
		return possibleEngineNamesAccordingToOS("Windows"); // default to what the majority wants! //$NON-NLS-1$
	}
	
	/** 
	 * Searches an appropriate Clonk installation for launching the scenario.
	 * @return The path of the Clonk engine executable
	 */
	public File verifyClonkInstall(ILaunchConfiguration configuration)
			throws CoreException {
		
		// TODO: Lots of guesswork, should be more configurable in the future
		
		// Clonk path from configuration
		IPreferenceStore prefs = ClonkCore.getDefault().getPreferenceStore();
		String gamePath = prefs.getString(ClonkPreferences.GAME_PATH);

		File enginePath = null;
		String enginePref = Platform.getPreferencesService().getString(ClonkCore.PLUGIN_ID, ClonkPreferences.ENGINE_EXECUTABLE, "", null); //$NON-NLS-1$
		if (enginePref != "") { //$NON-NLS-1$
			enginePath = new File(enginePref);
			if (!enginePath.exists())
				enginePath = null;
		}
		else {
			// Try some variants in an attempt to find the engine (ugh...)
			final String[] engineNames = possibleEngineNamesAccordingToOS(System.getProperty("os.name")); //$NON-NLS-1$
			for(String name : engineNames) {
				File path = new File(gamePath, name);
				if(path.exists()) {
					enginePath = path;
					break;
				}
			}
		}
		if(enginePath == null)
			abort(IStatus.ERROR, Messages.CouldNotFindEngine);

		// TODO: Do some more verification? Check engine version?
	
		return enginePath;
	}
	
	/** 
	 * Collects arguments to pass to the engine at launch
	 */
	public String[] verifyLaunchArguments(ILaunchConfiguration configuration,
			IResource scenario, File engine) throws CoreException {
		Collection<String> args = new LinkedList<String>();  
			
		// Engine
		args.add(engine.getAbsolutePath());
		
		// Scenario
		args.add(scenario.getRawLocation().toOSString());
		
		// Full screen/console
		if(configuration.getAttribute(ATTR_FULLSCREEN, false))
			args.add("/fullscreen"); //$NON-NLS-1$
		else
			args.add("/console"); //$NON-NLS-1$
		
		// Record
		if(configuration.getAttribute(ATTR_RECORD, false))
			args.add("/record"); //$NON-NLS-1$
		
		return args.toArray(new String [] {});
	}
	
}
