package net.arctics.clonk.preferences;

import net.arctics.clonk.Core;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {
		ClonkPreferences.commitDefaultsToPrefStore(Core.instance().getPreferenceStore());
	}

}
