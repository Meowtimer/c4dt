package net.arctics.clonk.preferences;

import java.lang.reflect.Field;

import net.arctics.clonk.Core;
import net.arctics.clonk.ui.editors.ClonkColorConstants;
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class SyntaxColoringPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public SyntaxColoringPreferencePage() {
		super (GRID);
		setPreferenceStore(Core.instance().getPreferenceStore());
	}
	
	@Override
	protected void createFieldEditors() {
		try {
			for (Field colorField : ClonkColorConstants.Defaults.class.getFields()) {
				PreferenceConverter.setDefault(getPreferenceStore(), ClonkColorConstants.actualPrefName(colorField.getName()), ClonkColorConstants.defaultColor(colorField.getName()));
				addField(new ColorFieldEditor(ClonkColorConstants.actualPrefName(colorField.getName()), (String) ClonkColorConstants.ColorHumanReadable.class.getField(colorField.getName()).get(null), getFieldEditorParent()));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void init(IWorkbench workbench) {
	}

}
