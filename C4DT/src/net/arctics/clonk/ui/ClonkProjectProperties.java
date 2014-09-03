package net.arctics.clonk.ui;

import java.util.HashMap;
import java.util.Map;

import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.builder.ProjectSettings;
import net.arctics.clonk.preferences.ClonkPreferencePage;
import net.arctics.clonk.preferences.Messages;
import net.arctics.clonk.util.UI;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.ui.IWorkbenchPropertyPage;

public class ClonkProjectProperties extends FieldEditorPreferencePage implements IWorkbenchPropertyPage {

	private static final String ENGINENAME_PROPERTY = "engineName"; //$NON-NLS-1$

	public ClonkProjectProperties() {
		super(GRID);
	}

	private final class AdapterStore extends PreferenceStore {
		private final Map<String, String> values = new HashMap<String, String>();

		@Override
		public String getDefaultString(final String name) {
			return ""; //$NON-NLS-1$
		}

		@Override
		public void setValue(final String name, final String value) {
			values.put(name, value);
			commit(name, value);
		}

		@Override
		public void setValue(final String name, final boolean value) {
			setValue(name, String.valueOf(value));
		}

		@Override
		public boolean getBoolean(final String name) {
			return Boolean.parseBoolean(values.get(name));
		}

		@Override
		public String getString(final String name) {
			final String v = values.get(name);
			return v != null ? v : getDefaultString(name);
		}

		public void commit(final String n, final String v) {
			final ProjectSettings settings = getSettings();
			if (n.equals(ENGINENAME_PROPERTY)) {
				settings.setEngineName(v);
				UI.refreshAllProjectExplorers(getProject());
			}
		}

		private ProjectSettings getSettings() {
			return ClonkProjectNature.get(getProject()).settings();
		}

		public AdapterStore() throws CoreException {
			values.put(ENGINENAME_PROPERTY, getSettings().engineName());
		}
	}

	private IAdaptable element;
	private AdapterStore adapterStore;

	private IProject getProject() {
		return (IProject) element.getAdapter(IProject.class);
	}

	@Override
	public IPreferenceStore getPreferenceStore() {
		return adapterStore;
	}

	@Override
	protected void createFieldEditors() {
		addField(new ComboFieldEditor(ENGINENAME_PROPERTY, Messages.ClonkProjectProperties_SpecifiedEngine, ClonkPreferencePage.engineComboValues(true), getFieldEditorParent()));
	}

	@Override
	public IAdaptable getElement() {
		return element;
	}

	@Override
	public void setElement(final IAdaptable element) {
		this.element = element;
		try {
			this.adapterStore = new AdapterStore();
		} catch (final CoreException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean performOk() {
		if (super.performOk()) {
			ClonkProjectNature.get(getProject()).saveSettings();
			return true;
		} else
			return false;
	}

}