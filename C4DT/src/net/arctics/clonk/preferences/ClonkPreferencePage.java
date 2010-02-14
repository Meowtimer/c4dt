package net.arctics.clonk.preferences;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.C4Engine;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.util.Util;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Clonk preferences
 */
public class ClonkPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private C4GroupListEditor externalLibsEditor;
	private String previousGamePath;
	private FileFieldEditor c4GroupEditor;
	private FileFieldEditor engineExecutableEditor;
	private List<FieldEditor> enginePrefs = new ArrayList<FieldEditor>(10);
	
	public ClonkPreferencePage() {
		super(GRID);
		setPreferenceStore(ClonkCore.getDefault().getPreferenceStore());
		setDescription(Messages.ClonkPreferences);
	}
	
	private String currentEngine = ClonkCore.getDefault().getPreferenceStore().getString(ClonkPreferences.ACTIVE_ENGINE);
	
	private class EngineConfigPrefStore extends PreferenceStore {
		
		private Map<String, C4Engine.EngineSettings> settings = new HashMap<String, C4Engine.EngineSettings>(); 
		
		@Override
		public void setValue(String name, String value) {
			C4Engine.EngineSettings e = getSettings();
			try {
				e.getClass().getField(name).set(e, value);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
		
		@Override
		public String getString(String name) {
			C4Engine.EngineSettings e = getSettings();
			try {
				String result = (String) e.getClass().getField(name).get(e);
				if (result == null)
					result = Messages.ClonkPreferencePage_0;
				return result;
			} catch (Exception e1) {
				e1.printStackTrace();
				return null;
			}
		}

		public C4Engine.EngineSettings getSettings() {
			C4Engine.EngineSettings e = settings.get(currentEngine);
			if (e == null) {
				C4Engine engine = ClonkCore.getDefault().loadEngine(currentEngine);
				try {
					e = engine.getCurrentSettings().clone();
				} catch (CloneNotSupportedException e1) {
					e1.printStackTrace();
				}
				settings.put(currentEngine, e);
			}
			return e;
		}
		
		public void apply() {
			for (Map.Entry<String, C4Engine.EngineSettings> entry : settings.entrySet()) {
				ClonkCore.getDefault().loadEngine(entry.getKey()).setCurrentSettings(entry.getValue());
			}
		}
		
	};
	
	@Override
	public boolean performOk() {
		engineConfigPrefStore.apply();
		return super.performOk();
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(FieldEditor.VALUE) && ((FieldEditor)event.getSource()).getPreferenceName().equals(ClonkPreferences.ACTIVE_ENGINE)) {
			currentEngine = (String) event.getNewValue();
			for (FieldEditor ed : enginePrefs) {
				ed.load();
			}
		}
		super.propertyChange(event);
	}
	
	private boolean addingEnginePrefs;
	private EngineConfigPrefStore engineConfigPrefStore;
	
	@Override
	protected void addField(FieldEditor editor) {
		super.addField(editor);
		if (addingEnginePrefs)
			enginePrefs.add(editor);
	}
	
	public void createFieldEditors() {
		
		// FIXME: not the best place to set that
		getPreferenceStore().setDefault(ClonkPreferences.EXTERNAL_INDEX_ENCODING, ClonkPreferences.EXTERNAL_INDEX_ENCODING_DEFAULT);
		getPreferenceStore().setDefault(ClonkPreferences.DOC_URL_TEMPLATE, ClonkPreferences.DOC_URL_TEMPLATE_DEFAULT);
		
		String[][] engineChoices = engineComboValues(false);
		addField(
			new ComboFieldEditor(
				ClonkPreferences.ACTIVE_ENGINE,
				Messages.EngineVersion,
				engineChoices,
				getFieldEditorParent()
			)
		);

		// per-engine
		{
			addingEnginePrefs = true;
			Group engineConfigurationComposite = new Group(getFieldEditorParent(), SWT.DEFAULT);
			engineConfigurationComposite.setText(Messages.ClonkPreferencePage_EngineConfigurationTitle);
			engineConfigurationComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
			
			IPreferenceStore realStore = getPreferenceStore();
			engineConfigPrefStore = new EngineConfigPrefStore();
			setPreferenceStore(engineConfigPrefStore);
			try {
				addField(
						new DirectoryFieldEditor(
								"gamePath", //$NON-NLS-1$
								Messages.GamePath,
								engineConfigurationComposite
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
									/* better not
									externalLibs = new String[] {
										gamePath.append("System.c4g").toPortableString(), //$NON-NLS-1$
										gamePath.append("Objects.c4d").toPortableString() //$NON-NLS-1$
									};
									externalLibsEditor.setValues(externalLibs);
									 */
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
								"c4GroupPath", //$NON-NLS-1$
								Messages.C4GroupExecutable,
								engineConfigurationComposite
						)
				);
				addField(
						engineExecutableEditor = new FileFieldEditor(
								"engineExecutablePath", //$NON-NLS-1$
								Messages.EngineExecutable,
								engineConfigurationComposite
						) {
							
							// this class is not approriate for overridal :C
							
							private String[] extensions;
							
							@Override
							protected String changePressed() {
								File f = new File(getTextControl().getText());
						        if (!f.exists()) {
									f = null;
								}
						        File d = getFile(f);
						        if (d == null) {
									return null;
								}
						        
						        if (Util.isMac() && d.isDirectory() && d.getName().endsWith(".app")) {
						        	d = new File(d.getAbsolutePath()+"/Contents/MacOS/"+d.getName().substring(0, d.getName().length()-".app".length()));
						        }

						        return d.getAbsolutePath();
							};
							
							@Override
							public void setFileExtensions(String[] extensions) {
								super.setFileExtensions(extensions);
								this.extensions = extensions;
							};
							
							private File getFile(File startingDirectory) {

						        FileDialog dialog = new FileDialog(getShell(), SWT.OPEN | SWT.SHEET);
						        if (startingDirectory != null) {
									dialog.setFileName(startingDirectory.getPath());
								}
						        if (extensions != null) {
									dialog.setFilterExtensions(extensions);
								}
						        String file = dialog.open();
						        if (file != null) {
						            file = file.trim();
						            if (file.length() > 0) {
										return new File(file);
									}
						        }

						        return null;
						    }
						}
				);
				addField(
						new DirectoryFieldEditor(
								"repositoryPath", //$NON-NLS-1$
								Messages.OpenClonkRepo,
								engineConfigurationComposite
						)
				);
				addField(
						new StringFieldEditor(
								"docURLTemplate", //$NON-NLS-1$
								Messages.DocumentURLTemplate,
								engineConfigurationComposite
						)
				);
			} finally {
				setPreferenceStore(realStore);
			}
			
			GridLayout gridLayout = new GridLayout();
			gridLayout.numColumns = 3;
			engineConfigurationComposite.setLayout(gridLayout);
			
			addingEnginePrefs = false;
		}
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
		/*
		addField(externalLibsEditor = new C4GroupListEditor(ClonkPreferences.STANDARD_EXT_LIBS, Messages.ExternalObjectsAndScripts, getFieldEditorParent()));
		externalLibsEditor.gamePathEditor = gamePathEditor;
		addField(
			new ExceptionlessEncodingFieldEditor(
				ClonkPreferences.EXTERNAL_INDEX_ENCODING,
				"", //$NON-NLS-1$
				Messages.EncodingForExternalObjects,
				getFieldEditorParent()
			)
		);*/
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

	public static String[][] engineComboValues(boolean includeDefault) {
		List<String> engines = ClonkCore.getDefault().getAvailableEngines();
		String[][] engineChoices = new String[engines.size() + (includeDefault ? 1 : 0)][2];
		int i = 0;
		if (includeDefault) {
			engineChoices[i][1] = null;
			engineChoices[i][0] = Messages.ClonkPreferencePage_DefaultEngine;
			++i;
		}
		for (String s : engines) {
			engineChoices[i][1] = s;
			engineChoices[i][0] = makeUserFriendlyEngineName(s);
			i++;
		}
		return engineChoices;
	}

	private static String makeUserFriendlyEngineName(String s) {
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
		for (FieldEditor e : enginePrefs) {
			e.setPreferenceStore(engineConfigPrefStore);
			e.load();
		}
	}

	public void init(IWorkbench workbench) {
	}
	
}