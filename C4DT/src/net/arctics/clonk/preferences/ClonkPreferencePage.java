package net.arctics.clonk.preferences;

import java.io.File;
import java.util.List;
import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.C4Engine;
import net.arctics.clonk.util.UI;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.ListEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.util.Util;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Clonk preferences
 */
public class ClonkPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private final class ExternalLibsEditor extends ListEditor {
		private ExternalLibsEditor(String name, String labelText,
				Composite parent) {
			super(name, labelText, parent);
		}

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

	private DirectoryFieldEditor gamePathEditor;
	private ExternalLibsEditor externalLibsEditor;
	private String previousGamePath;
	private FileFieldEditor c4GroupEditor;
	private FileFieldEditor engineExecutableEditor;
	
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
			) {
				
				private String c4GroupAccordingToOS() {
					if (Util.isWindows())
						return "c4group.exe"; //$NON-NLS-1$
					else
						return "c4group"; //$NON-NLS-1$
				}
				
				private void setFile(IPath gamePath, String gamePathText, FileFieldEditor editor, String... values) {
					String val = editor.getStringValue();
					if (val.equals("")) { //$NON-NLS-1$
						for (String s : values) {
							File f;
							if ((f = gamePath.append(s).toFile()).exists()) {
								editor.setStringValue(f.getAbsolutePath());
								break;
							}
						}
					}
					else if (val.toLowerCase().startsWith(previousGamePath)) {
						editor.setStringValue(gamePathText + val.substring(previousGamePath.length()));
					}
				}
				
				@Override
				protected void valueChanged() {
					super.valueChanged();
					String gamePathText = getTextControl().getText();
					IPath gamePath = new Path(gamePathText);
					adjustExternalLibsToGamePath(gamePath);
					setFile(gamePath, gamePathText, c4GroupEditor, c4GroupAccordingToOS());
					setFile(gamePath, gamePathText, engineExecutableEditor, C4Engine.possibleEngineNamesAccordingToOS());
					previousGamePath = gamePathText.toLowerCase();
				}

				private void adjustExternalLibsToGamePath(IPath gamePath) {
					String[] externalLibs = externalLibsEditor.getValues();
					if (externalLibs.length == 0) {
						externalLibs = new String[] {
							gamePath.append("System.c4g").toPortableString(), //$NON-NLS-1$
							gamePath.append("Objects.c4d").toPortableString() //$NON-NLS-1$
						};
						externalLibsEditor.setValues(externalLibs);
					}
					else {
						String oldGamePath = previousGamePath;
						for (int i = 0; i < externalLibs.length; i++) {
							String s = externalLibs[i];
							if (s.toLowerCase().startsWith(oldGamePath)) {
								s = gamePath + s.substring(oldGamePath.length());
							}
							externalLibs[i] = s;
						}
						externalLibsEditor.setValues(externalLibs);
					}
				};
			}
		);
		addField(
			c4GroupEditor = new FileFieldEditor(
				ClonkPreferences.C4GROUP_EXECUTABLE,
				Messages.C4GroupExecutable,
				getFieldEditorParent()
			)
		);
		addField(
			engineExecutableEditor = new FileFieldEditor(
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
		List<String> engines = ClonkCore.getDefault().getAvailableEngines();
		String[][] engineChoices = new String[engines.size()][2];
		int i = 0;
		for (String s : engines) {
			engineChoices[i][1] = s;
			engineChoices[i][0] = makeUserFriendlyEngineName(s);
			i++;
		}
		addField(
			new ComboFieldEditor(
				ClonkPreferences.ACTIVE_ENGINE,
				Messages.EngineVersion,
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
		addField(externalLibsEditor = new ExternalLibsEditor(ClonkPreferences.STANDARD_EXT_LIBS, Messages.ExternalObjectsAndScripts, getFieldEditorParent()));
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
	
	@Override
	protected void initialize() {
		super.initialize();
		previousGamePath = ClonkPreferences.getPreferenceOrDefault(ClonkPreferences.GAME_PATH).toLowerCase();
	}

	public void init(IWorkbench workbench) {
	}
	
}