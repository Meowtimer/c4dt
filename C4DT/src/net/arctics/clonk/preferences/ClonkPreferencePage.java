package net.arctics.clonk.preferences;

import java.beans.Beans;
import java.util.List;

import net.arctics.clonk.Core;

import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Preferences page where global settings can be set, like author using the workbench, the default engine used for projects and the preferred language id.
 */
public class ClonkPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public ClonkPreferencePage() {
		super(GRID);
		setPreferenceStore(Core.instance().getPreferenceStore());
	}
	
	@Override
	public void createFieldEditors() {
		addField(
			new ComboFieldEditor(
				ClonkPreferences.ACTIVE_ENGINE,
				Messages.ClonkPreferencePage_DefaultEngine,
				engineComboValues(false),
				getFieldEditorParent()
			)
		);
		addField(
			new ComboFieldEditor(
				ClonkPreferences.PREFERRED_LANGID,
				Messages.PreferredLangID,
				new String[][] {
					{Messages.German, "DE"}, //$NON-NLS-1$
					{Messages.USEnglish, "US"}, //$NON-NLS-1$
					{Messages.Finnish, "FI"} //$NON-NLS-1$
				},
				getFieldEditorParent()
			)
		);
		addField(new StringFieldEditor(ClonkPreferences.AUTHOR, Messages.ClonkPreferencePage_Author, getFieldEditorParent()));
	}

	public static String[][] engineComboValues(final boolean includeDefault) {
		if (Beans.isDesignTime())
			return new String[][] {
					new String[] {
							"OpenClonk", //$NON-NLS-1$
							"OpenClonk" //$NON-NLS-1$
					},
					new String[] {
							"ClonkRage", //$NON-NLS-1$
							"ClonkRage" //$NON-NLS-1$
					}
			};
		final List<String> engines = Core.instance().namesOfAvailableEngines();
		final String[][] engineChoices = new String[engines.size() + (includeDefault ? 1 : 0)][2];
		int i = 0;
		if (includeDefault) {
			engineChoices[i][1] = null;
			engineChoices[i][0] = Messages.ClonkPreferencePage_DefaultEngine;
			++i;
		}
		for (final String s : engines) {
			engineChoices[i][1] = s;
			engineChoices[i][0] = makeUserFriendlyEngineName(s);
			i++;
		}
		return engineChoices;
	}

	private static String makeUserFriendlyEngineName(final String s) {
	    final StringBuilder builder = new StringBuilder(s.length()*2);
	    for (int i = 0; i < s.length(); i++) {
	    	final char c = s.charAt(i);
	    	if (i > 0 && Character.isUpperCase(c))
	    		builder.append(' ');
	    	builder.append(c);
	    }
	    return builder.toString();
    }

	@Override
	public void init(final IWorkbench workbench) {
	}

}