package net.arctics.clonk.preferences;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.EngineSettings;
import net.arctics.clonk.ui.navigator.ClonkFolderView;

/**
 * Configuration page for configuring one {@link Engine}. At the moment, two such pages are created, parameterized to let the user edit settings for ClonkRage and OpenClonk. respectively.
 * @author madeen
 *
 */
public class EngineConfigurationPrefPage extends FieldEditorPreferencePage implements IExecutableExtension, IWorkbenchPreferencePage {

	private String myEngine;
	private FileFieldEditor c4GroupEditor;
	private FileFieldEditor engineExecutableEditor;
	private DirectoryFieldEditor repositoryPathEditor;
	private DirectoryFieldEditor gamePathEditor;
	private final List<FieldEditor> enginePrefs = new ArrayList<FieldEditor>(10);
	private EngineConfigPrefStore engineConfigPrefStore;
	private BooleanFieldEditor readDocumentationFromRepositoryEditor;

	private class GamePathEditor extends DirectoryFieldEditor {

		public GamePathEditor(final Composite parent, final String name, final String labelText) {
			super(name, labelText, parent);
		}

		public String c4GroupAccordingToOS() {
			if (Util.isWindows()) {
				return "c4group.exe"; //$NON-NLS-1$
			}
			else {
				return "c4group"; //$NON-NLS-1$
			}
		}

		public void setFile(final IPath gamePath, final String gamePathText, final FileFieldEditor editor, final String... values) {
			final String val = editor.getStringValue();
			if (val.equals("") || !new File(val).exists()) {
				for (final String s : values) {
					File f;
					if ((f = gamePath.append(s).toFile()).exists()) {
						editor.setStringValue(f.getAbsolutePath());
						break;
					}
				}
			}
		}

		@Override
		protected void valueChanged() {
			super.valueChanged();
			final String gamePathText = getTextControl().getText();
			final IPath gamePath = new Path(gamePathText);
			setFile(gamePath, gamePathText, c4groupEditor(), c4GroupAccordingToOS());
			setFile(gamePath, gamePathText, engineExecutableEditor(), Engine.possibleEngineNamesAccordingToOS());
			//previousGamePath = gamePathText.toLowerCase();
		}
	}

	private class EngineRelatedFileFieldEditor extends FileFieldEditor {
		// redeclare since field from super class is not accessible -.-
		private final String[] extensions;

		public EngineRelatedFileFieldEditor(final String pref, final String title, final Composite parent, final String[] extensions) {
			super(pref, title, parent);
			this.extensions = extensions;
		}

		@Override
		protected String changePressed() {
			final FileDialog dialog = new FileDialog(getShell(), SWT.OPEN | SWT.SHEET);
			dialog.setFilterPath(gamePathEditor().getStringValue());
			if (extensions != null) {
				dialog.setFilterExtensions(extensions);
			}
			return dialog.open();
		}
	}

	private class EngineExecutableEditor extends EngineRelatedFileFieldEditor {
		public EngineExecutableEditor(final String pref, final String title, final Composite parent, final String[] extensions) {
			super(pref, title, parent, extensions);
		}

		@Override
		protected String changePressed() {
			final String selection = super.changePressed();
			if (selection != null) {
				File d = new File(selection);
				if (Util.isMac() && d.isDirectory() && d.getName().endsWith(".app"))
				 {
					d = new File(d.getAbsolutePath()+"/Contents/MacOS/"+d.getName().substring(0, d.getName().length()-".app".length())); //$NON-NLS-1$ //$NON-NLS-2$
				}
				return d.getAbsolutePath();
			} else {
				return null;
			}
		}
	}

	private class EngineConfigPrefStore extends PreferenceStore {

		private EngineSettings settings = (EngineSettings) Core.instance().loadEngine(myEngine).settings().clone();

		@Override
		public void setValue(final String name, final String value) {
			try {
				settings.getClass().getField(name).set(settings, value);
			} catch (final Exception e1) {
				e1.printStackTrace();
			}
		}

		private Object val(final EngineSettings settings, final String attrName) {
			try {
				return settings.getClass().getField(attrName).get(settings);
			} catch (final Exception e) {
				return null;
			}
		}

		@Override
		public void setValue(final String name, final boolean value) {
			try {
				settings.getClass().getField(name).set(settings, value);
			} catch (final Exception e1) {
				e1.printStackTrace();
			}
		}

		@Override
		public String getString(final String name) {
			return (String)val(settings, name);
		}

		@Override
		public boolean getBoolean(final String name) {
			return (Boolean)val(settings, name);
		}

		@Override
		public String getDefaultString(final String name) {
			return (String)val(Core.instance().loadEngine(myEngine).settings(), name);
		}

		@Override
		public boolean getDefaultBoolean(final String name) {
			return (Boolean)val(Core.instance().loadEngine(myEngine).settings(), name);
		}

		public void apply() {
			Core.instance().loadEngine(myEngine).applySettings(settings);
			if (ClonkFolderView.instance() != null) {
				ClonkFolderView.instance().update();
			}
		}

		public void reset() {
			settings = (EngineSettings) Core.instance().loadEngine(myEngine).settings().clone();
		}

	};

	private static String[] appExtensions(final boolean engine) {
		if (Util.isWindows()) {
			if (engine)
			 {
				return new String[] { "*.exe", "*.c4x" }; //$NON-NLS-1$ //$NON-NLS-2$
			}
			return new String[] { "*.exe" }; //$NON-NLS-1$
		}
		return null;
	}

	public EngineConfigurationPrefPage() {
		super(GRID);
	}

	@Override
	protected void createFieldEditors() {
		final Composite engineConfigurationComposite = new Composite(getFieldEditorParent(), SWT.NO_SCROLL);
		engineConfigurationComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
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
			repositoryPathEditor = new DirectoryFieldEditor(
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
				protected Button getChangeControl(final Composite parent) {
					if (checker == null) {
						checker = new Button(parent, SWT.CHECK);
						checker.setText(Messages.EngineConfigurationPrefPage_UseRepositoryDocsFolder0);
						checker.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(final SelectionEvent e) {
								setTextControlEnablement();
							}
						});
					}
					return checker;
				};
				@Override
				public void setEnabled(final boolean enabled, final Composite parent) {
					super.setEnabled(enabled, parent);
					if (checker != null) {
						checker.setEnabled(enabled);
					}
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
			readDocumentationFromRepositoryEditor = new BooleanFieldEditor(
				"readDocumentationFromRepository", //$NON-NLS-1$
				Messages.EngineConfigurationPrefPage_ReadDocFromRepository,
				engineConfigurationComposite
			)
		);

		final GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 3;
		engineConfigurationComposite.setLayout(gridLayout);
	}

	@Override
	public void setInitializationData(final IConfigurationElement config, final String propertyName, final Object data) throws CoreException {
		myEngine = data.toString();
		engineConfigPrefStore = new EngineConfigPrefStore();
	}

	@Override
	public String getTitle() {
		return String.format(Messages.EngineConfigurationPrefPage_SettingsForEngineTitle, myEngine);
	}

	@Override
	public void init(final IWorkbench workbench) {

	}

	@Override
	protected void performDefaults() {
		engineConfigPrefStore.reset();
		super.performDefaults();
	}

	@Override
	public boolean performOk() {
		if (readDocumentationFromRepositoryEditor.getBooleanValue() && repositoryPathEditor.getStringValue().equals("")) {
			setErrorMessage(Messages.EngineConfigurationPrefPage_ReadingDocumentationRequiresRepositoryPath);
			return false;
		}
		setValid(true);
		final boolean result = super.performOk();
		if (result) {
			engineConfigPrefStore.apply();
		}
		return result;
	}

	@Override
	protected void addField(final FieldEditor editor) {
		super.addField(editor);
	}

	@Override
	protected void initialize() {
		super.initialize();
		for (final FieldEditor e : this.enginePrefs) {
			e.setPreferenceStore(engineConfigPrefStore);
			e.load();
		}
	}

	public FileFieldEditor c4groupEditor() {
		return c4GroupEditor;
	}

	public FileFieldEditor engineExecutableEditor() {
		return engineExecutableEditor;
	}

	public DirectoryFieldEditor gamePathEditor() {
		return gamePathEditor;
	}

}
