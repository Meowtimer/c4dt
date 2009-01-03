package net.arctics.clonk.ui.debug;

import java.util.Collection;
import java.util.LinkedList;

import net.arctics.clonk.debug.ClonkLaunchConfigurationDelegate;
import net.arctics.clonk.resource.ResourceTester;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;

public class ClonkLaunchShortcut implements ILaunchShortcut {

	/**
	 * Create launch from a selection. In practice, this should be a selection
	 * of resources.
	 */
	public void launch(ISelection sel, String mode) {
		if (sel instanceof IStructuredSelection)
			// Note: The selection will only have one element because this was
			//       checked beforehand (see extension point declaration)
			for (Object obj : ((IStructuredSelection) sel).toArray())
				if (obj instanceof IResource)
					launchResource((IResource) obj, mode);
	}

	/**
	 * Create launch from an open editor tab.
	 */
	public void launch(IEditorPart editor, String mode) {
		// Note: This is also guaranteed to work because of prior checks
		IResource res = (IResource) editor.getEditorInput().getAdapter(IResource.class);
		if(res != null)
			launchResource(res, mode);
	}
	
	private void launchResource(IResource res, String mode) {
		
		// Search for containing scenario
		while(res != null && !ResourceTester.isScenario(res))
			res = res.getParent();
		if(res == null)
			return;
		
		try {
		
			// Get launch configuration to use (might be created)
			ILaunchConfiguration config = getLaunchConfigurationFor(res);
			
			// Start it
			DebugUITools.launch(config, mode);

		} catch(CoreException e) {
			// No need to succeed...
		}
	}
	
	private ILaunchConfiguration getLaunchConfigurationFor(IResource res) throws CoreException {

		// Get existing launch configurations
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType configType =
			launchManager.getLaunchConfigurationType(ClonkLaunchConfigurationDelegate.LAUNCH_TYPE);
		ILaunchConfiguration[] configs = launchManager.getLaunchConfigurations(configType);
		
		// Select those that use the selected scenario (by project name + scenario name)
		Collection<ILaunchConfiguration> candidates = new LinkedList<ILaunchConfiguration>();
		String expectProjectName = res.getProject().getName();
		String expectScenarioName = res.getProjectRelativePath().toString();
		for(ILaunchConfiguration config : configs) {
			String projectName = config.getAttribute(ClonkLaunchConfigurationDelegate.ATTR_PROJECT_NAME, "");
			String scenarioName = config.getAttribute(ClonkLaunchConfigurationDelegate.ATTR_SCENARIO_NAME, "");
			if(projectName.equals(expectProjectName) && scenarioName.equals(expectScenarioName))
				candidates.add(config);
		}
		
		// Select one
		if(candidates.size() > 0)
			return candidates.iterator().next();
		
		// Otherwise: Create new
		String configName = launchManager.generateUniqueLaunchConfigurationNameFrom(res.getName());
		ILaunchConfigurationWorkingCopy wc = configType.newInstance(null, configName);
		wc.setAttribute(ClonkLaunchConfigurationDelegate.ATTR_PROJECT_NAME, expectProjectName);
		wc.setAttribute(ClonkLaunchConfigurationDelegate.ATTR_SCENARIO_NAME, expectScenarioName);
		wc.setMappedResources(new IResource[] {res});
		return wc.doSave();
	}

}
