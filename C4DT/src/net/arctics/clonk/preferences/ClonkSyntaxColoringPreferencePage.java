package net.arctics.clonk.preferences;

import java.lang.reflect.Field;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.ui.editors.ClonkColorConstants;

import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class ClonkSyntaxColoringPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public ClonkSyntaxColoringPreferencePage() {
		super (GRID);
		setPreferenceStore(ClonkCore.getDefault().getPreferenceStore());
	}
	
	@Override
	protected void createFieldEditors() {
		try {
			for (Field colorField : ClonkColorConstants.Defaults.class.getFields()) {
				addField(new ColorFieldEditor(ClonkColorConstants.actualPrefName(colorField.getName()), (String) ClonkColorConstants.ColorHumanReadable.class.getField(colorField.getName()).get(null), getFieldEditorParent()));
			}
		} catch (Exception e) {

		}
	}

	@Override
	public void init(IWorkbench workbench) {
	}

}
