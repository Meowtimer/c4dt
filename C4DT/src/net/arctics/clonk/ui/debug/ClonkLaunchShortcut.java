package net.arctics.clonk.ui.debug;

import java.util.Collection;
import java.util.LinkedList;

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

import net.arctics.clonk.builder.ResourceTester;
import net.arctics.clonk.debug.ClonkLaunchConfigurationDelegate;

public class ClonkLaunchShortcut implements ILaunchShortcut {

	/**
	 * Create launch from a selection. In practice, this should be a selection
	 * of resources.
	 */
	@Override
	public void launch(final ISelection sel, final String mode) {
		if (sel instanceof IStructuredSelection) {
			// Note: The selection will only have one element because this was
			//       checked beforehand (see extension point declaration)
			for (final Object obj : ((IStructuredSelection) sel).toArray()) {
				if (obj instanceof IResource) {
					launchResource((IResource) obj, mode);
				}
			}
		}
	}

	/**
	 * Create launch from an open editor tab.
	 */
	@Override
	public void launch(final IEditorPart editor, final String mode) {
		// Note: This is also guaranteed to work because of prior checks
		final IResource res = editor.getEditorInput().getAdapter(IResource.class);
		if(res != null) {
			launchResource(res, mode);
		}
	}
	
	private void launchResource(IResource res, final String mode) {
		
		// Search for containing scenario
		while(res != null && !ResourceTester.isScenario(res)) {
			res = res.getParent();
		}
		if(res == null) {
			return;
		}
		
		try {
		
			// Get launch configuration to use (might be created)
			final ILaunchConfiguration config = getLaunchConfigurationFor(res);
			
			// Start it
			DebugUITools.launch(config, mode);

		} catch(final CoreException e) {
			// No need to succeed...
		}
	}
	
	@SuppressWarnings("deprecation")
	private ILaunchConfiguration getLaunchConfigurationFor(final IResource res) throws CoreException {

		// Get existing launch configurations
		final ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		final ILaunchConfigurationType configType =
			launchManager.getLaunchConfigurationType(ClonkLaunchConfigurationDelegate.LAUNCH_TYPE);
		final ILaunchConfiguration[] configs = launchManager.getLaunchConfigurations(configType);
		
		// Select those that use the selected scenario (by project name + scenario name)
		final Collection<ILaunchConfiguration> candidates = new LinkedList<ILaunchConfiguration>();
		final String expectProjectName = res.getProject().getName();
		final String expectScenarioName = res.getProjectRelativePath().toString();
		for(final ILaunchConfiguration config : configs) {
			final String projectName = config.getAttribute(ClonkLaunchConfigurationDelegate.ATTR_PROJECT_NAME, ""); //$NON-NLS-1$
			final String scenarioName = config.getAttribute(ClonkLaunchConfigurationDelegate.ATTR_SCENARIO_NAME, ""); //$NON-NLS-1$
			if(projectName.equals(expectProjectName) && scenarioName.equals(expectScenarioName)) {
				candidates.add(config);
			}
		}
		
		// Select one
		if(candidates.size() > 0) {
			return candidates.iterator().next();
		}
		
		// Otherwise: Create new
		final String configName = launchManager.generateUniqueLaunchConfigurationNameFrom(res.getName());
		final ILaunchConfigurationWorkingCopy wc = configType.newInstance(null, configName);
		wc.setAttribute(ClonkLaunchConfigurationDelegate.ATTR_PROJECT_NAME, expectProjectName);
		wc.setAttribute(ClonkLaunchConfigurationDelegate.ATTR_SCENARIO_NAME, expectScenarioName);
		wc.setMappedResources(new IResource[] {res});
		return wc.doSave();
	}

}
