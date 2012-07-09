package net.arctics.clonk.preferences;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.arctics.clonk.Core;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.util.UI;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.navigator.CommonNavigator;

public class TogglesPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	
	@Override
	protected void createFieldEditors() {
		// toggles
		Map<String, String> toggleHumanMap = new HashMap<String, String>();
		toggleHumanMap.put(ClonkPreferences.SHOW_EXPORT_LOG, Messages.ShowExportLog);
		toggleHumanMap.put(ClonkPreferences.SHOW_ERRORS_WHILE_TYPING, Messages.ClonkPreferencePage_ShowErrorsWhileTyping);
		toggleHumanMap.put(ClonkPreferences.OPEN_EXTERNAL_BROWSER, Messages.ClonkPreferencePage_OpenExternalBrowser);
		toggleHumanMap.put(ClonkPreferences.STRUCTURE_OUTLINES_IN_PROJECT_EXPLORER, Messages.ClonkPreferencePage_StructureOutlinesInProjectExplorer);
		toggleHumanMap.put(ClonkPreferences.NO_AUTOBRACKETPAIRS, Messages.ClonkPreferencePage_NoAutoBrackets);
		toggleHumanMap.put(ClonkPreferences.IGNORE_SIMPLE_FUNCTION_DUPES, Messages.TogglesPreferencePage_IgnoreSimpleFunctionDupes);
		toggleHumanMap.put(ClonkPreferences.ANALYZE_CODE, Messages.TogglesPreferencePage_AnalyzeCode);
		for (Entry<String, String> entry : toggleHumanMap.entrySet())
			addField(new BooleanFieldEditor(entry.getKey(), entry.getValue(), getFieldEditorParent()));
	}

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(Core.instance().getPreferenceStore());
	}
	
	@Override
	public boolean performOk() {
		boolean r = super.performOk();
		if (r)
			for (CommonNavigator nav : UI.projectExplorers())
				for (IProject proj : ClonkProjectNature.clonkProjectsInWorkspace())
					nav.getCommonViewer().refresh(proj);
		return r;
	}

}
