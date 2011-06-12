package net.arctics.clonk.preferences;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

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
		for (Entry<String, String> entry : toggleHumanMap.entrySet())
			addField(new BooleanFieldEditor(entry.getKey(), entry.getValue(), getFieldEditorParent()));
	}

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(ClonkCore.getDefault().getPreferenceStore());
	}
	
	@Override
	public boolean performOk() {
		boolean r = super.performOk();
		if (r)
			for (IWorkbenchWindow w : PlatformUI.getWorkbench().getWorkbenchWindows())
				for (IProject proj : ClonkProjectNature.getClonkProjects())
					Utilities.getProjectExplorer(w).getCommonViewer().refresh(proj);
		return r;
	}

}
