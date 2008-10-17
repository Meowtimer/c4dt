package net.arctics.clonk.preferences;

import java.io.File;

import net.arctics.clonk.ClonkCore;

import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.ListEditor;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * This class represents a preference page that
 * is contributed to the Preferences dialog. By 
 * subclassing <samp>FieldEditorPreferencePage</samp>, we
 * can use the field support built into JFace that allows
 * us to create a page that is small and knows how to 
 * save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They
 * are stored in the preference store that belongs to
 * the main plug-in class. That way, preferences can
 * be accessed directly via the preference store.
 */

public class ClonkPreferencePage
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage {

	public ClonkPreferencePage() {
		super(GRID);
		setPreferenceStore(ClonkCore.getDefault().getPreferenceStore());
		setDescription("Global Clonk specific preferences");
	}
	
	/**
	 * Creates the field editors. Field editors are abstractions of
	 * the common GUI blocks needed to manipulate various types
	 * of preferences. Each field editor knows how to save and
	 * restore itself.
	 */
	public void createFieldEditors() {
		addField(new DirectoryFieldEditor(PreferenceConstants.GAME_PATH, 
				"&Clonk game path:", getFieldEditorParent()));
		addField(new ListEditor(PreferenceConstants.STANDARD_EXT_LIBS,"External objects and scripts:",getFieldEditorParent()) {
		
			@Override
			protected String[] parseString(String stringList) {
				if (stringList.length() == 0) return new String[] {};
				return stringList.split("<>");
			}

			@Override
			protected String getNewInputObject() {
				String gamePath = getPreferenceStore().getString(PreferenceConstants.GAME_PATH);
				if (gamePath == null || !new File(gamePath).exists()) {
					gamePath = "C:\\";
				}
				FileDialog dialog = new FileDialog(getShell());
				dialog.setText("Choose external object");
				dialog.setFilterExtensions(new String[] { "*.c4g;*.c4d" });
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
//		addField(
//			new BooleanFieldEditor(
//				PreferenceConstants.P_BOOLEAN,
//				"&An example of a boolean preference",
//				getFieldEditorParent()));
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