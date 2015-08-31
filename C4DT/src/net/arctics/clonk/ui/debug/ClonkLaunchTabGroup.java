package net.arctics.clonk.ui.debug;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;

/** Represents a group of tabs that describe properties of a launch */
public class ClonkLaunchTabGroup extends AbstractLaunchConfigurationTabGroup {

	@Override
	public void createTabs(final ILaunchConfigurationDialog arg0, final String arg1) {
		final ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] {
			new LaunchMainTab(),
			new CommonTab() // by Eclipse
		};
		setTabs(tabs);
	}

}
