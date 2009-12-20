package net.arctics.clonk.ui;

import java.util.HashMap;
import java.util.Map;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.util.Utilities;
import net.arctics.clonk.index.ExternalLibsLoader;
import net.arctics.clonk.index.ProjectIndex;
import net.arctics.clonk.preferences.ExternalLibsEditor;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.ui.IWorkbenchPropertyPage;

public class ClonkProjectProperties extends FieldEditorPreferencePage implements IWorkbenchPropertyPage {

	private static final String DEPENDENCIES_PROPERTY = "dependencies"; //$NON-NLS-1$
	private static final String SHOWSDEPENDENCIES_PROPERTY = "showsDependencies"; //$NON-NLS-1$
	
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
			if (n.equals(DEPENDENCIES_PROPERTY)) {
				ProjectIndex projIndex = ClonkProjectNature.get(getProject()).getIndex();
				try {
					ExternalLibsLoader.readExternalLibs(projIndex, new NullProgressMonitor(), !v.equals("") ? v.split("<>") : new String[0]);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (n.equals(SHOWSDEPENDENCIES_PROPERTY)) {
				try {
					Utilities.setShowsDependencies(getProject(), Boolean.parseBoolean(v));
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
			try {
				getProject().refreshLocal(IResource.DEPTH_ONE, null);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		
		public AdapterStore() throws CoreException {
			values.put(DEPENDENCIES_PROPERTY, ClonkProjectNature.get(getProject()).getIndex().libsEncodedAsString());
			values.put(SHOWSDEPENDENCIES_PROPERTY, String.valueOf(getProject().hasNature(ClonkCore.CLONK_DEPS_NATURE_ID)));
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
		addField(new ExternalLibsEditor(DEPENDENCIES_PROPERTY, net.arctics.clonk.preferences.Messages.ExternalObjectsAndScripts, getFieldEditorParent()));
		addField(new BooleanFieldEditor(SHOWSDEPENDENCIES_PROPERTY, Messages.Project_ShowDependencies, getFieldEditorParent()));
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