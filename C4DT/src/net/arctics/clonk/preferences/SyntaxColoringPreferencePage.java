package net.arctics.clonk.preferences;

import net.arctics.clonk.Core;
import net.arctics.clonk.ui.editors.ClonkTextEditor;
import net.arctics.clonk.ui.editors.ColorManager;
import net.arctics.clonk.ui.editors.EditorUtil;
import net.arctics.clonk.ui.editors.ClonkRuleBasedScanner.ScannerPerEngine;
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
			ColorManager manager = ColorManager.instance();
			for (SyntaxElementStyle syntaxElement : manager.syntaxElementStyles.values()) {
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
	
	@Override
	public boolean performOk() {
		if (super.performOk()) {
			ScannerPerEngine.refreshScanners();
			for (ClonkTextEditor part : EditorUtil.clonkTextEditors(ClonkTextEditor.class, false))
				part.reconfigureSourceViewer();
			return true;
		} else
			return false;
	}

}
