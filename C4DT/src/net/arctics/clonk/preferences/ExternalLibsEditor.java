package net.arctics.clonk.preferences;

import java.io.File;

import net.arctics.clonk.util.UI;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.ListEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;

public final class ExternalLibsEditor extends ListEditor {
	
	StringFieldEditor gamePathEditor;
	
	public ExternalLibsEditor(String name, String labelText, Composite parent) {
		super(name, labelText, parent);
	}

	public String[] parseString(String stringList) {
		if (stringList.length() == 0) return new String[] {};
		return stringList.split("<>"); //$NON-NLS-1$
	}

	@Override
	protected String getNewInputObject() {
		String gamePath = getPreferenceStore().getString(ClonkPreferences.GAME_PATH);
		// not yet saved -> look in field editor
		if (gamePath == null || gamePath.length() == 0 && gamePathEditor != null) {
			gamePath = gamePathEditor.getStringValue();
		}
		if (gamePath == null || !new File(gamePath).exists()) {
			gamePath = null;
		}
		MessageDialog msgDialog = new MessageDialog(
			getShell(), Messages.ClonkPreferencePage_GroupFileOrFolder, null, Messages.ClonkPreferencePage_SelectRegularFolder,
			MessageDialog.INFORMATION, new String[] {
				Messages.ClonkPreferencePage_Nope,
				Messages.ClonkPreferencePage_YesIndeed
			}, 0
		);
		switch (msgDialog.open()) {
		case 0:
			FileDialog dialog = new FileDialog(getShell());
			dialog.setText(Messages.ChooseExternalObject);
			dialog.setFilterExtensions(new String[] { UI.FILEDIALOG_CLONK_FILTER });
			dialog.setFilterPath(gamePath);
			return dialog.open();
		case 1:
			DirectoryDialog dirDialog = new DirectoryDialog(getShell());
			dirDialog.setText(Messages.ClonkPreferencePage_SelectExternalFolder);
			dirDialog.setFilterPath(gamePath);
			return dirDialog.open();
		default:
			return null;
		}
	}

	@Override
	protected String createList(String[] items) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < items.length; i++) {
			if (i > 0)
				result.append("<>"); //$NON-NLS-1$
			result.append(items[i]);
		}
		return result.toString();
	}
	
	public String[] getValues() {
		return getList().getItems();
	}
	
	public void setValues(String[] items) {
		getList().setItems(items);
		setPresentsDefaultValue(false);
	}
	
}