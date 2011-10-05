package net.arctics.clonk.preferences;

import java.beans.Beans;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.Engine.EngineSettings;
import net.arctics.clonk.ui.navigator.ClonkFolderView;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.preference.StringButtonFieldEditor;
import org.eclipse.jface.util.Util;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Configuration page for configuring one engine. At the moment, two such pages are created, parameterized to let the user edit settings for ClonkRage and OpenClonk. respectively.
 * @author madeen
 *
 */
public class EngineConfigurationPrefPage extends FieldEditorPreferencePage implements IExecutableExtension, IWorkbenchPreferencePage {

	private String myEngine;
	private FileFieldEditor c4GroupEditor;
	private FileFieldEditor engineExecutableEditor;
	private DirectoryFieldEditor gamePathEditor;
	private List<FieldEditor> enginePrefs = new ArrayList<FieldEditor>(10);
	private EngineConfigPrefStore engineConfigPrefStore;

	private class GamePathEditor extends DirectoryFieldEditor {

		public GamePathEditor(Composite parent, String name, String labelText) {
			super(name, labelText, parent);
		}

		public String c4GroupAccordingToOS() {
			if (Util.isWindows())
				return "c4group.exe"; //$NON-NLS-1$
			else
				return "c4group"; //$NON-NLS-1$
		}

		public void setFile(IPath gamePath, String gamePathText, FileFieldEditor editor, String... values) {
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
			setFile(gamePath, gamePathText, getC4GroupEditor(), c4GroupAccordingToOS());
			setFile(gamePath, gamePathText, getEngineExecutableEditor(), Engine.possibleEngineNamesAccordingToOS());
			//previousGamePath = gamePathText.toLowerCase();
		}
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
			dialog.setFilterPath(getGamePathEditor().getStringValue());
			if (extensions != null)
				dialog.setFilterExtensions(extensions);
			return dialog.open();
		}
	}

	private class EngineExecutableEditor extends EngineRelatedFileFieldEditor {
		public EngineExecutableEditor(String pref, String title, Composite parent, String[] extensions) {
			super(pref, title, parent, extensions);
		}

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
		}
	}

	private class EngineConfigPrefStore extends PreferenceStore {

		private Engine.EngineSettings settings = (EngineSettings) ClonkCore.getDefault().loadEngine(myEngine).getCurrentSettings().clone();

		@Override
		public void setValue(String name, String value) {
			try {
				settings.getClass().getField(name).set(settings, value);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}

		private Object val(EngineSettings settings, String attrName) {
			try {
				return settings.getClass().getField(attrName).get(settings);
			} catch (Exception e) {
				return null;
			}
		}

		@Override
		public void setValue(String name, boolean value) {
			try {
				settings.getClass().getField(name).set(settings, value);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}

		@Override
		public String getString(String name) {
			return (String)val(settings, name);
		}

		@Override
		public boolean getBoolean(String name) {
			return (Boolean)val(settings, name);
		}

		@Override
		public String getDefaultString(String name) { 
			return (String)val(ClonkCore.getDefault().loadEngine(myEngine).getCurrentSettings(), name);
		}

		@Override
		public boolean getDefaultBoolean(String name) {
			return (Boolean)val(ClonkCore.getDefault().loadEngine(myEngine).getCurrentSettings(), name);
		}

		public void apply() {
			ClonkCore.getDefault().loadEngine(myEngine).setCurrentSettings(settings);
			if (ClonkFolderView.instance() != null)
				ClonkFolderView.instance().update();
		}

		public void reset() {
			settings = (EngineSettings) ClonkCore.getDefault().loadEngine(myEngine).getCurrentSettings().clone();
		}

	};

	private static String[] appExtensions(boolean engine) {
		if (Util.isWindows()) {
			if (engine)
				return new String[] { "*.exe", "*.c4x" }; //$NON-NLS-1$ //$NON-NLS-2$
			return new String[] { "*.exe" }; //$NON-NLS-1$
		}
		return null;
	}

	public EngineConfigurationPrefPage() {
		super(GRID);
	}

	@Override
	protected void createFieldEditors() {
		Composite engineConfigurationComposite = new Composite(getFieldEditorParent(), SWT.NO_SCROLL);
		engineConfigurationComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		if (!Beans.isDesignTime())
			setPreferenceStore(engineConfigPrefStore);

		addField(
			gamePathEditor = new GamePathEditor(engineConfigurationComposite, "gamePath", Messages.GamePath) //$NON-NLS-1$
			);
		addField(
			c4GroupEditor = new EngineRelatedFileFieldEditor(
				"c4GroupPath", Messages.C4GroupExecutable, //$NON-NLS-1$
				engineConfigurationComposite,
				appExtensions(false)
				)
			);
		addField(
			engineExecutableEditor = new EngineExecutableEditor("engineExecutablePath", Messages.EngineExecutable, //$NON-NLS-1$
				engineConfigurationComposite, appExtensions(true))
			);
		addField(
			new DirectoryFieldEditor(
				"repositoryPath", //$NON-NLS-1$
				Messages.OpenClonkRepo,
				engineConfigurationComposite
				)
			);
		addField(
			new StringButtonFieldEditor(
				"docURLTemplate", //$NON-NLS-1$
				Messages.DocumentURLTemplate,
				engineConfigurationComposite
				) {
				private Button checker;
				@Override
				protected Button getChangeControl(Composite parent) {
					if (checker == null) {
						checker = new Button(parent, SWT.CHECK);
						checker.setText(Messages.EngineConfigurationPrefPage_UseRepositoryDocsFolder0);
						checker.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent e) {
								setTextControlEnablement();
							}
						});
					}
					return checker;
				};
				@Override
				public void setEnabled(boolean enabled, Composite parent) {
					super.setEnabled(enabled, parent);
					if (checker != null)
						checker.setEnabled(enabled);
				};
				@Override
				protected String changePressed() {
					return ""; //$NON-NLS-1$
				};
				private void setTextControlEnablement() {
					getTextControl().setEnabled(!checker.getSelection());
				}
				@Override
				public void load() {
					super.load();
					checker.setSelection(getPreferenceStore().getBoolean("useDocsFromRepository")); //$NON-NLS-1$
					setTextControlEnablement();
				};
				@Override
				protected void doStore() {
					super.doStore();
					getPreferenceStore().setValue("useDocsFromRepository", checker.getSelection()); //$NON-NLS-1$
				};
			}
			);
		addField(
			new BooleanFieldEditor(
				"readDocumentationFromRepository", //$NON-NLS-1$
				Messages.EngineConfigurationPrefPage_ReadDocFromRepository,
				engineConfigurationComposite
				)
			);

		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 3;
		engineConfigurationComposite.setLayout(gridLayout);
	}

	@Override
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
		myEngine = data.toString();
		engineConfigPrefStore = new EngineConfigPrefStore();
	}

	@Override
	public String getTitle() {
		return String.format(Messages.EngineConfigurationPrefPage_SettingsForEngineTitle, myEngine);
	}

	@Override
	public void init(IWorkbench workbench) {

	}

	@Override
	protected void performDefaults() {
		engineConfigPrefStore.reset();
		super.performDefaults();
	}

	@Override
	public boolean performOk() {
		boolean result = super.performOk();
		if (result)
			engineConfigPrefStore.apply();
		return result;
	}

	@Override
	protected void addField(FieldEditor editor) {
		super.addField(editor);
	}

	@Override
	protected void initialize() {
		super.initialize();
		for (FieldEditor e : this.enginePrefs) {
			e.setPreferenceStore(engineConfigPrefStore);
			e.load();
		}
	}

	public FileFieldEditor getC4GroupEditor() {
		return c4GroupEditor;
	}

	public FileFieldEditor getEngineExecutableEditor() {
		return engineExecutableEditor;
	}

	public DirectoryFieldEditor getGamePathEditor() {
		return gamePathEditor;
	}

}
