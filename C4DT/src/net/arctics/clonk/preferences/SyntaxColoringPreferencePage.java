package net.arctics.clonk.preferences;

import net.arctics.clonk.Core;
import net.arctics.clonk.ui.editors.ColorManager;
import net.arctics.clonk.ui.editors.ColorManager.SyntaxElementStyle;

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
			for (SyntaxElementStyle syntaxElement : ColorManager.syntaxElementStyles.values()) {
				PreferenceConverter.setDefault(getPreferenceStore(),
					syntaxElement.prefName(SyntaxElementStyle.RGB),
					syntaxElement.defaultRGB
				);
				addField(new ColorFieldEditor(syntaxElement.prefName(SyntaxElementStyle.RGB),
					syntaxElement.localizedName, getFieldEditorParent()));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void init(IWorkbench workbench) {
	}

}
