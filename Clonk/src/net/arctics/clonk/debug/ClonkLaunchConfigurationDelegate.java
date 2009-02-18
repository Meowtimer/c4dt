package net.arctics.clonk.debug;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.preferences.PreferenceConstants;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.jface.preference.IPreferenceStore;

public class ClonkLaunchConfigurationDelegate implements
		ILaunchConfigurationDelegate {
	
	public static final String LAUNCH_TYPE = ClonkCore.PLUGIN_ID + ".debug.ClonkLaunch";
	
	public static final String ATTR_PROJECT_NAME = ClonkCore.PLUGIN_ID + ".debug.ProjectNameAttr";
	public static final String ATTR_SCENARIO_NAME = ClonkCore.PLUGIN_ID + ".debug.ScenarioNameAttr";
	public static final String ATTR_FULLSCREEN = ClonkCore.PLUGIN_ID + ".debug.FullscreenAttr";
	public static final String ATTR_RECORD = ClonkCore.PLUGIN_ID + ".debug.RecordAttr";
	
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch,
			IProgressMonitor monitor) throws CoreException {

		// Run only for now
		if(!mode.equals(ILaunchManager.RUN_MODE))
			abort(IStatus.ERROR, "Launcher only supports run mode!");
		
		// Set up monitor
		if(monitor == null)
			monitor = new NullProgressMonitor();
		monitor.beginTask("Launch " + configuration.getName() + "...", 2);
		
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
			monitor.subTask("Starting Clonk engine...");
			
			// Run the engine
			try {
				Process process = Runtime.getRuntime().exec(launchArgs, null, workDirectory);
				DebugPlugin.newProcess(launch, process, configuration.getName());
			} catch(IOException e) {
				abort(IStatus.ERROR, "Could not start engine!", e);
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
		String projectName = configuration.getAttribute(ATTR_PROJECT_NAME, "");
		String scenarioName = configuration.getAttribute(ATTR_SCENARIO_NAME, "");
		
		// Get project
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject project = root.getProject(projectName);
		if(project == null || !project.isOpen())
			abort(IStatus.ERROR, projectName + " is not an open project!");
		
		// Get scenario
		IFolder scenario = project.getFolder(scenarioName);
		if(scenario == null || !scenario.exists())
			abort(IStatus.ERROR, "Scenario " + projectName + " not found!");
		
		return scenario;
	}
	
	private String[] possibleEngineNamesAccordingToOS(String OS) {
		if (OS.equals("Mac OS X")) {
			return new String[] { "Clonk.app/Contents/MacOS/Clonk" };
		}
		if (OS.contains("Windows")) {
			return new String[] { "Clonk.c4x", "Clonk.exe" };
		}
		if (OS.contains("Linux")) {
			return new String[] { "clonk" };
		}
		return possibleEngineNamesAccordingToOS("Windows"); // default to what the majority wants!
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
		String gamePath = prefs.getString(PreferenceConstants.GAME_PATH);

		// Try some variants in an attempt to find the engine (ugh...)
		final String[] engineNames = possibleEngineNamesAccordingToOS(System.getProperty("os.name"));
		File enginePath = null;
		for(String name : engineNames) {
			File path = new File(gamePath, name);
			if(path.exists()) {
				enginePath = path;
				break;
			}
		}
		if(enginePath == null)
			abort(IStatus.ERROR, "Could not find engine excutable!");
		
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
			args.add("/fullscreen");
		else
			args.add("/console");
		
		// Record
		if(configuration.getAttribute(ATTR_RECORD, false))
			args.add("/record");
		
		return args.toArray(new String [] {});
	}
	
}
