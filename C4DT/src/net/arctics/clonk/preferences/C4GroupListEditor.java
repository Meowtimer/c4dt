package net.arctics.clonk.preferences;

import java.io.File;

import net.arctics.clonk.ui.navigator.FullPathConverter;
import net.arctics.clonk.util.ArrayUtil;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.ListEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.List;

public final class C4GroupListEditor extends ListEditor {

	StringFieldEditor gamePathEditor;
	IEngineProvider engineProvider;

	public C4GroupListEditor(String name, String labelText, Composite parent, IEngineProvider engineProvider) {
		super(name, labelText, parent);
		this.engineProvider = engineProvider;
	}

	@Override
	public String[] parseString(String stringList) {
		if (stringList.length() == 0)
			return new String[] {};
		return stringList.split("<>"); //$NON-NLS-1$
	}

	private void addFiles(File[] files) {
		List list = getList();
		int index = Math.max(list.getSelectionIndex(), 0);
		for (int i = 0; i < files.length; i++) {
			list.add(files[i].getAbsolutePath(), index + i);
		}
		selectionChanged();
	}

	@Override
	protected String getNewInputObject() {
		String gamePath = engineProvider.getEngine(true).currentSettings().gamePath;
		// not yet saved -> look in field editor
		if (gamePath == null || gamePath.length() == 0 && gamePathEditor != null) {
			gamePath = gamePathEditor.getStringValue();
		}
		if (gamePath == null || !new File(gamePath).exists()) {
			gamePath = null;
		}
		MessageDialog msgDialog = new MessageDialog(getShell(), Messages.ClonkPreferencePage_GroupFileOrFolder, null, Messages.ClonkPreferencePage_SelectRegularFolder, MessageDialog.INFORMATION, new String[] { Messages.ClonkPreferencePage_Nope, Messages.ClonkPreferencePage_YesIndeed }, 0);
		switch (msgDialog.open()) {
		case 0:
			FileDialog dialog = new FileDialog(getShell(), SWT.SHEET + SWT.MULTI + SWT.OPEN);
			dialog.setText(Messages.ChooseExternalObject);
			dialog.setFilterExtensions(new String[] { engineProvider.getEngine(true).currentSettings().getFileDialogFilterForGroupFiles() });
			dialog.setFilterPath(gamePath);
			// add multiple files instead of returning one file to be added by
			// the super class
			if (dialog.open() != null) {
				addFiles(ArrayUtil.map(dialog.getFileNames(), File.class, new FullPathConverter(dialog)));
			}
			return null;
		case 1:
			DirectoryDialog dirDialog = new DirectoryDialog(getShell(), SWT.SHEET + SWT.MULTI + SWT.OPEN);
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