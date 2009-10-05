package net.arctics.clonk.preferences;

import java.io.File;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.util.UI;

import org.eclipse.jface.preference.BooleanFieldEditor;
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
		setDescription("Global Clonk specific preferences");
	}

	public void createFieldEditors() {
		
		// FIXME: not the best place to set that
		getPreferenceStore().setDefault(PreferenceConstants.EXTERNAL_INDEX_ENCODING, PreferenceConstants.EXTERNAL_INDEX_ENCODING_DEFAULT);
		
		addField(
			gamePathEditor = new DirectoryFieldEditor(
				PreferenceConstants.GAME_PATH,
				"&Game path:",
				getFieldEditorParent()
			)
		);
		addField(
			new FileFieldEditor(
				PreferenceConstants.C4GROUP_EXECUTABLE,
				"C4&Group executable:",
				getFieldEditorParent()
			)
		);
		addField(
			new FileFieldEditor(
				PreferenceConstants.ENGINE_EXECUTABLE,
				"&Engine:",
				getFieldEditorParent()
			)
		);
		addField(
			new DirectoryFieldEditor(
				PreferenceConstants.OPENCLONK_REPO,
				"&OpenClonk Repository",
				getFieldEditorParent()
			)
		);
		addField(
			new StringFieldEditor(
				PreferenceConstants.PREFERRED_LANGID,
				"Preferred language",
				getFieldEditorParent()
			)
		);
		addField(
			new StringFieldEditor(
				PreferenceConstants.DOC_URL_TEMPLATE,
				"Documentation URL Template",
				getFieldEditorParent()
			)
		);
		addField(new ListEditor(PreferenceConstants.STANDARD_EXT_LIBS, "External objects and scripts:",getFieldEditorParent()) {
			@Override
			protected String[] parseString(String stringList) {
				if (stringList.length() == 0) return new String[] {};
				return stringList.split("<>");
			}

			@Override
			protected String getNewInputObject() {
				String gamePath = getPreferenceStore().getString(PreferenceConstants.GAME_PATH);
				// not yet saved -> look in field editor
				if (gamePath == null || gamePath.length() == 0) {
					gamePath = gamePathEditor.getStringValue();
				}
				if (gamePath == null || !new File(gamePath).exists()) {
					gamePath = null;
				}
				FileDialog dialog = new FileDialog(getShell());
				dialog.setText("Choose external object");
				dialog.setFilterExtensions(new String[] { UI.FILEDIALOG_CLONK_FILTER });
				dialog.setFilterPath(gamePath);
				return dialog.open();
			}
		
			@Override
			protected String createList(String[] items) {
				StringBuilder result = new StringBuilder();
				for(String item : items) {
					result.append(item);
					result.append("<>");
				}
				if (result.length() > 2)
					return result.substring(0, result.length() - 2);
				else
					return null;
			}
		});
		addField(
			new ExceptionlessEncodingFieldEditor(
				PreferenceConstants.EXTERNAL_INDEX_ENCODING,
				"",
				"Encoding for external scripts",
				getFieldEditorParent()
			)
		);
		addField(
			new BooleanFieldEditor(
				PreferenceConstants.SHOW_EXPORT_LOG,
				"Show &export log in console",
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

	public void init(IWorkbench workbench) {
	}
	
}