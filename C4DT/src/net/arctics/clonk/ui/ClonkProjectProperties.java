package net.arctics.clonk.ui;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.ListEditor;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.PlatformUI;

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

		@SuppressWarnings("unchecked")
		public void commit(String n, String v) {
			if (n.equals(DEPENDENCIES_PROPERTY)) {
				ClonkProjectNature.get(getProject()).getIndex().setDependencyNames(Utilities.collectionFromArray(LinkedList.class, !v.equals("") ? v.split("<>") : new String[0])); //$NON-NLS-1$ //$NON-NLS-2$
			} else if (n.equals(SHOWSDEPENDENCIES_PROPERTY)) {
				try {
					Utilities.setShowsDependencies(getProject(), Boolean.parseBoolean(v));
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
		}
		
		public AdapterStore() throws CoreException {
			values.put(DEPENDENCIES_PROPERTY, stringFromIterable(ClonkProjectNature.get(getProject()).getIndex().getDependencyNames()));
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
	
	private String stringFromIterable(Iterable<String> iterable) {
		if (iterable == null)
			return ""; //$NON-NLS-1$
		StringBuilder builder = new StringBuilder();
		int i = 0;
		for (String s : iterable) {
			builder.append(s);
			builder.append("<>"); //$NON-NLS-1$
			i++;
		}
		return builder.toString();
	}
	
	@Override
	protected void createFieldEditors() {
		addField(new ListEditor(DEPENDENCIES_PROPERTY, Messages.Project_Dependencies, getFieldEditorParent()) {
			
			@Override
			protected String[] parseString(String stringList) {
				return stringList.split("<>"); //$NON-NLS-1$
			}
			
			@Override
			protected String getNewInputObject() {
				InputDialog input = new InputDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), Messages.Project_NewDependency, Messages.Project_NewDependencyDesc, "", null); //$NON-NLS-1$
				switch (input.open()) {
				case InputDialog.CANCEL:
					return null;
				default:
					return input.getValue();
				}
			}
			
			@Override
			protected String createList(String[] items) {
				return stringFromIterable(Utilities.arrayIterable(items));
			}
		});
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