package net.arctics.clonk.preferences;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.C4Engine;
import net.arctics.clonk.index.C4Engine.EngineSettings;
import net.arctics.clonk.ui.navigator.ClonkFolderView;

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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Clonk preferences
 */
public class ClonkPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	//private String previousGamePath;
	private FileFieldEditor c4GroupEditor;
	private FileFieldEditor engineExecutableEditor;
	private DirectoryFieldEditor gamePathEditor;
	private List<FieldEditor> enginePrefs = new ArrayList<FieldEditor>(10);
	
	public ClonkPreferencePage() {
		super(GRID);
		setPreferenceStore(ClonkCore.getDefault().getPreferenceStore());
		setDescription(Messages.ClonkPreferences);
	}
	
	private String currentEngine;
	
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
			return getString(name, getSettings());
		}

		public String getString(String name, C4Engine.EngineSettings e) {
			try {
				String result = (String) e.getClass().getField(name).get(e);
				if (result == null)
					result = ""; //$NON-NLS-1$
				return result;
			} catch (Exception e1) {
				e1.printStackTrace();
				return null;
			}
		}
		
		@Override
		public String getDefaultString(String name) {
			return getString(name, ClonkCore.getDefault().loadEngine(currentEngine).getCurrentSettings());
		}

		public C4Engine.EngineSettings getSettings() {
			C4Engine.EngineSettings e = settings.get(currentEngine);
			if (e == null) {
				C4Engine engine = ClonkCore.getDefault().loadEngine(currentEngine);
				e = (EngineSettings) engine.getCurrentSettings().clone();
				settings.put(currentEngine, e);
			}
			return e;
		}
		
		public void apply() {
			for (Map.Entry<String, C4Engine.EngineSettings> entry : settings.entrySet()) {
				ClonkCore.getDefault().loadEngine(entry.getKey()).setCurrentSettings(entry.getValue());
			}
			if (ClonkFolderView.instance() != null)
				ClonkFolderView.instance().update();
		}

		public void reset() {
			for (C4Engine engine : ClonkCore.getDefault().loadedEngines()) {
				if (settings.get(engine.getName()) != null)
					settings.put(engine.getName(), (EngineSettings) engine.getCurrentSettings().clone());
			}
		}
		
	};
	
	@Override
	public boolean performOk() {
		boolean result = super.performOk();
		if (result)
			engineConfigPrefStore.apply();
		return result;
	}
	
	@Override
	protected void performDefaults() {
		currentEngine = getPreferenceStore().getDefaultString(ClonkPreferences.ACTIVE_ENGINE);
		engineConfigPrefStore.reset();
		super.performDefaults();
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(FieldEditor.VALUE) && ((FieldEditor)event.getSource()).getPreferenceName().equals(ClonkPreferences.ACTIVE_ENGINE)) {
			for (FieldEditor ed : enginePrefs) {
				ed.store();
			}
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
	
	private class EngineRelatedFileFieldEditor extends FileFieldEditor {
		// redeclare since field from super class is not accessible -.-
		private String[] extensions;
		
		public EngineRelatedFileFieldEditor(String pref, String title, Composite parent, String[] extensions) {
			super(pref, title, parent);
			this.extensions = extensions;
		}

		@Override
		protected String changePressed() {
			FileDialog dialog = new FileDialog(getShell(), SWT.OPEN | SWT.SHEET);
			dialog.setFilterPath(gamePathEditor.getStringValue());
			if (extensions != null)
				dialog.setFilterExtensions(extensions);
			return dialog.open();
		}
	}
	
	private static String[] appExtensions(boolean engine) {
		if (Util.isWindows()) {
			if (engine)
				return new String[] { "*.exe", "*.c4x" }; //$NON-NLS-1$ //$NON-NLS-2$
			return new String[] { "*.exe" }; //$NON-NLS-1$
		}
		return null;
	}
	
	public void createFieldEditors() {
		
		// FIXME: not the best place to set that
		getPreferenceStore().setDefault(ClonkPreferences.DOC_URL_TEMPLATE, ClonkPreferences.DOC_URL_TEMPLATE_DEFAULT);
		getPreferenceStore().setDefault(ClonkPreferences.ACTIVE_ENGINE, ClonkPreferences.ACTIVE_ENGINE_DEFAULT);
		
		currentEngine = ClonkCore.getDefault().getPreferenceStore().getString(ClonkPreferences.ACTIVE_ENGINE);
		
		addField(
			new ComboFieldEditor(
				ClonkPreferences.ACTIVE_ENGINE,
				Messages.EngineVersion,
				engineComboValues(false),
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
						gamePathEditor = new DirectoryFieldEditor(
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
								if (val.equals("") || !new File(val).exists()) { //$NON-NLS-1$
									for (String s : values) {
										File f;
										if ((f = gamePath.append(s).toFile()).exists()) {
											editor.setStringValue(f.getAbsolutePath());
											break;
										}
									}
								}
								/* potential to annoy
								 else if (val.toLowerCase().startsWith(previousGamePath)) {
									editor.setStringValue(gamePathText + val.substring(previousGamePath.length()));
								}*/
							}

							@Override
							protected void valueChanged() {
								super.valueChanged();
								String gamePathText = getTextControl().getText();
								IPath gamePath = new Path(gamePathText);
								setFile(gamePath, gamePathText, c4GroupEditor, c4GroupAccordingToOS());
								setFile(gamePath, gamePathText, engineExecutableEditor, C4Engine.possibleEngineNamesAccordingToOS());
								//previousGamePath = gamePathText.toLowerCase();
							}
						}
				);
				addField(
						c4GroupEditor = new EngineRelatedFileFieldEditor(
								"c4GroupPath", //$NON-NLS-1$
								Messages.C4GroupExecutable,
								engineConfigurationComposite,
								appExtensions(false)
						)
				);
				addField(
						engineExecutableEditor = new EngineRelatedFileFieldEditor(
								"engineExecutablePath", //$NON-NLS-1$
								Messages.EngineExecutable,
								engineConfigurationComposite,
								appExtensions(true)
						) {
							
							@Override
							protected String changePressed() {
								String selection = super.changePressed();
								if (selection != null) {
									File d = new File(selection);
									if (Util.isMac() && d.isDirectory() && d.getName().endsWith(".app")) { //$NON-NLS-1$
							        	d = new File(d.getAbsolutePath()+"/Contents/MacOS/"+d.getName().substring(0, d.getName().length()-".app".length())); //$NON-NLS-1$ //$NON-NLS-2$
							        }
									return d.getAbsolutePath();
								}
								else
									return null;
							};
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
		addField(
			new BooleanFieldEditor(
				ClonkPreferences.OPEN_EXTERNAL_BROWSER,
				Messages.ClonkPreferencePage_OpenExternalBrowser,
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
		//previousGamePath = ClonkPreferences.getPreferenceOrDefault(ClonkPreferences.GAME_PATH).toLowerCase();
		for (FieldEditor e : enginePrefs) {
			e.setPreferenceStore(engineConfigPrefStore);
			e.load();
		}
	}

	public void init(IWorkbench workbench) {
	}
	
}