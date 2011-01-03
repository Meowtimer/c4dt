package net.arctics.clonk.ui;

import java.util.HashMap;
import java.util.Map;

import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.resource.ClonkProjectNature.Settings;
import net.arctics.clonk.util.Utilities;
import net.arctics.clonk.preferences.ClonkPreferencePage;
import net.arctics.clonk.preferences.Messages;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbenchPropertyPage;

public class ClonkProjectProperties extends FieldEditorPreferencePage implements IWorkbenchPropertyPage {

	private static final String ENGINENAME_PROPERTY = "engineName"; //$NON-NLS-1$
	private static final String DISABLED_ERRORS_PROPERTY = "disabledErrors"; //$NON-NLS-1$
	
	public ClonkProjectProperties() {
		super(GRID);
	}
	
	private final class AdapterStore extends PreferenceStore {
		private Map<String, String> values = new HashMap<String, String>();

		public String getDefaultString(String name) {
			return ""; //$NON-NLS-1$
		}

		public void setValue(String name, String value) {
			values.put(name, value);
			commit(name, value);
		}
		
		@Override
		public void setValue(String name, boolean value) {
			setValue(name, String.valueOf(value));
		}
		
		@Override
		public boolean getBoolean(String name) {
			return Boolean.parseBoolean(values.get(name));
		}

		public String getString(String name) {
			String v = values.get(name);
			return v != null ? v : getDefaultString(name);
		}

		public void commit(String n, String v) {
			Settings settings = getSettings();
			if (n.equals(ENGINENAME_PROPERTY)) {
				settings.setEngineName(v);
			} else if (n.equals(DISABLED_ERRORS_PROPERTY)) {
				settings.setDisabledErrors(v);
			}
			Utilities.getProjectExplorer().getCommonViewer().refresh(getProject());
		}

		private Settings getSettings() {
			return ClonkProjectNature.get(getProject()).getSettings();
		}
		
		public AdapterStore() throws CoreException {
			values.put(ENGINENAME_PROPERTY, getSettings().getEngineName());
			values.put(DISABLED_ERRORS_PROPERTY, getSettings().getDisabledErrors());
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
	/*	deprecate that - linking c4group should be enough for everybody!1
		addField(new ExternalLibsEditor(DEPENDENCIES_PROPERTY, net.arctics.clonk.preferences.Messages.ExternalObjectsAndScripts, getFieldEditorParent()));
		addField(new BooleanFieldEditor(SHOWSDEPENDENCIES_PROPERTY, Messages.Project_ShowDependencies, getFieldEditorParent())); */
		addField(new ComboFieldEditor(ENGINENAME_PROPERTY, Messages.EngineVersion, ClonkPreferencePage.engineComboValues(true), getFieldEditorParent()));
		addField(new StringFieldEditor(DISABLED_ERRORS_PROPERTY, Messages.DisabledErrors, getFieldEditorParent()));
	}

	public IAdaptable getElement() {
		return element;
	}

	public void setElement(IAdaptable element) {
		this.element = element;
		try {
			this.adapterStore = new AdapterStore();
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

}