package net.arctics.clonk.preferences;

import java.lang.reflect.Field;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.ui.editors.ClonkColorConstants;
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.PreferenceConverter;
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
				PreferenceConverter.setDefault(getPreferenceStore(), ClonkColorConstants.actualPrefName(colorField.getName()), ClonkColorConstants.getDefaultColor(colorField.getName()));
				addField(new ColorFieldEditor(ClonkColorConstants.actualPrefName(colorField.getName()), (String) ClonkColorConstants.ColorHumanReadable.class.getField(colorField.getName()).get(null), getFieldEditorParent()));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public boolean performOk() {
		if (super.performOk()) {
			/*
			FIXME: refresh immediately
			for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
				for (IWorkbenchPage page : window.getPages()) {
					for (IEditorReference edRef : page.getEditorReferences()) {
						IEditorPart editor = edRef.getEditor(false);
						if (editor instanceof ClonkTextEditor) {
							((ClonkTextEditor)editor).refreshSyntaxColoring();
						}
					}
				}
			} */
			return true;
		}
		else
			return false;
	}

	@Override
	public void init(IWorkbench workbench) {
	}

}
