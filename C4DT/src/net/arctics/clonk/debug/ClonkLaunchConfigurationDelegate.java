package net.arctics.clonk.debug;

import java.io.File;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.ProjectIndex;
import net.arctics.clonk.util.Utilities;

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
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
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
		if (monitor == null)
			monitor = new NullProgressMonitor();
		monitor.beginTask(String.format(Messages.LaunchConf, configuration.getName()), 2);
		try {
			// Get scenario and engine
			final IFolder scenario = verifyScenario(configuration);
			final File engine = verifyClonkInstall(configuration, scenario);
			final EngineLaunch launchling = new EngineLaunch(configuration, launch, scenario, engine, mode);
			launchling.launch(monitor);
		} finally {
			monitor.done();
		}
	}

	/**
	 * Searches the scenario to launch
	 */
	public IFolder verifyScenario(ILaunchConfiguration configuration) throws CoreException {

		// Get project and scenario name from configuration
		final String projectName = configuration.getAttribute(
				ATTR_PROJECT_NAME, ""); //$NON-NLS-1$
		final String scenarioName = configuration.getAttribute(
				ATTR_SCENARIO_NAME, ""); //$NON-NLS-1$

		// Get project
		final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		final IProject project = root.getProject(projectName);
		if (project == null || !project.isOpen())
			Utilities.abort(IStatus.ERROR,
					String.format(Messages.ProjectNotOpen, projectName));

		// Get scenario
		final IFolder scenario = project.getFolder(scenarioName);
		if (scenario == null || !scenario.exists())
			Utilities.abort(IStatus.ERROR, String.format(Messages.ScenarioNotFound, projectName));

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
			Utilities.abort(IStatus.ERROR,
					String.format(Messages.CouldNotFindEngine,
							enginePath.getAbsolutePath()));

		// TODO: Do some more verification? Check engine version?

		return enginePath;
	}

	public static String resFilePath(IResource res) {
		return new Path(res.getRawLocationURI().getSchemeSpecificPart())
				.toOSString();
	}

	static String cmdLineOptionString(Engine engine, String option) {
		return String.format(engine.settings().cmdLineOptionFormat, option);
	}

	static String cmdLineOptionString(Engine engine, String option,
			String argument) {
		return String.format(engine.settings().cmdLineOptionWithArgumentFormat,
				option, argument);
	}
}
