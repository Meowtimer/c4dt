package net.arctics.clonk.preferences;

import java.io.File;
import java.util.Map;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.C4Engine;
import net.arctics.clonk.util.UI;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.ListEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Clonk preferences
 */
public class ClonkPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private DirectoryFieldEditor gamePathEditor;
	
	public ClonkPreferencePage() {
		super(GRID);
		setPreferenceStore(ClonkCore.getDefault().getPreferenceStore());
		setDescription(Messages.ClonkPreferences);
	}

	public void createFieldEditors() {
		
		// FIXME: not the best place to set that
		getPreferenceStore().setDefault(ClonkPreferences.EXTERNAL_INDEX_ENCODING, ClonkPreferences.EXTERNAL_INDEX_ENCODING_DEFAULT);
		getPreferenceStore().setDefault(ClonkPreferences.DOC_URL_TEMPLATE, ClonkPreferences.DOC_URL_TEMPLATE_DEFAULT);
		
		addField(
			gamePathEditor = new DirectoryFieldEditor(
				ClonkPreferences.GAME_PATH,
				Messages.GamePath,
				getFieldEditorParent()
			)
		);
		addField(
			new FileFieldEditor(
				ClonkPreferences.C4GROUP_EXECUTABLE,
				Messages.C4GroupExecutable,
				getFieldEditorParent()
			)
		);
		addField(
			new FileFieldEditor(
				ClonkPreferences.ENGINE_EXECUTABLE,
				Messages.EngineExecutable,
				getFieldEditorParent()
			)
		);
		addField(
			new DirectoryFieldEditor(
				ClonkPreferences.OPENCLONK_REPO,
				Messages.OpenClonkRepo,
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
		Map<String, C4Engine> engines = ClonkCore.getDefault().getEngines();
		String[][] engineChoices = new String[engines.size()][2];
		int i = 0;
		for (String s : engines.keySet()) {
			engineChoices[i][1] = s;
			engineChoices[i][0] = makeUserFriendlyEngineName(s);
			i++;
		}
		addField(
			new ComboFieldEditor(
				ClonkPreferences.ACTIVE_ENGINE,
				"Engine Version",
				engineChoices,
				getFieldEditorParent()
			)
		);
		addField(
			new StringFieldEditor(
				ClonkPreferences.DOC_URL_TEMPLATE,
				Messages.DocumentURLTemplate,
				getFieldEditorParent()
			)
		);
		addField(new ListEditor(ClonkPreferences.STANDARD_EXT_LIBS, Messages.ExternalObjectsAndScripts,getFieldEditorParent()) {
			@Override
			protected String[] parseString(String stringList) {
				if (stringList.length() == 0) return new String[] {};
				return stringList.split("<>"); //$NON-NLS-1$
			}

			@Override
			protected String getNewInputObject() {
				String gamePath = getPreferenceStore().getString(ClonkPreferences.GAME_PATH);
				// not yet saved -> look in field editor
				if (gamePath == null || gamePath.length() == 0) {
					gamePath = gamePathEditor.getStringValue();
				}
				if (gamePath == null || !new File(gamePath).exists()) {
					gamePath = null;
				}
				FileDialog dialog = new FileDialog(getShell());
				dialog.setText(Messages.ChooseExternalObject);
				dialog.setFilterExtensions(new String[] { UI.FILEDIALOG_CLONK_FILTER });
				dialog.setFilterPath(gamePath);
				return dialog.open();
			}
		
			@Override
			protected String createList(String[] items) {
				StringBuilder result = new StringBuilder();
				for(String item : items) {
					result.append(item);
					result.append("<>"); //$NON-NLS-1$
				}
				if (result.length() > 2)
					return result.substring(0, result.length() - 2);
				else
					return null;
			}
		});
		addField(
			new ExceptionlessEncodingFieldEditor(
				ClonkPreferences.EXTERNAL_INDEX_ENCODING,
				"", //$NON-NLS-1$
				Messages.EncodingForExternalObjects,
				getFieldEditorParent()
			)
		);
		addField(
			new BooleanFieldEditor(
				ClonkPreferences.SHOW_EXPORT_LOG,
				Messages.ShowExportLog,
				getFieldEditorParent()
			)
		);
//
//		addField(new RadioGroupFieldEditor(
//				PreferenceConstants.P_CHOICE,
//			"An example of a multiple-choice preference",
//			1,
//			new String[][] { { "&Choice 1", "choice1" }, {
//				"C&hoice 2", "choice2" }
//		}, getFieldEditorParent()));
//		addField(
//			new StringFieldEditor(PreferenceConstants.P_STRING, "A &text preference:", getFieldEditorParent()));
	}

	private String makeUserFriendlyEngineName(String s) {
	    StringBuilder builder = new StringBuilder(s.length()*2);
	    for (int i = 0; i < s.length(); i++) {
	    	char c = s.charAt(i);
	    	if (i > 0 && Character.isUpperCase(c))
	    		builder.append(' ');
	    	builder.append(c);
	    }
	    return builder.toString();
    }

	public void init(IWorkbench workbench) {
	}
	
}