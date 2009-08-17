package net.arctics.clonk.ui;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.ListEditor;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.PlatformUI;

public class ClonkProjectProperties extends FieldEditorPreferencePage implements IWorkbenchPropertyPage {

	private static final String DEPENDENCIES_PROPERTY = "dependencies";
	
	private final class AdapterStore extends PreferenceStore {
		private Map<String, String> values = new HashMap<String, String>();

		public String getDefaultString(String name) {
			return "";
		}

		public void setValue(String name, String value) {
			values.put(name, value);
			commit(name, value);
		}

		public String getString(String name) {
			String v = values.get(name);
			return v != null ? v : getDefaultString(name);
		}

		@SuppressWarnings("unchecked")
		public void commit(String n, String v) {
			if (n.equals(DEPENDENCIES_PROPERTY))
				Utilities.getClonkNature(getProject()).getIndex().setDependencies(Utilities.collectionFromArray(LinkedList.class, v.split("<>")));
		}
		
		public AdapterStore() {
			values.put(DEPENDENCIES_PROPERTY, stringFromIterable(Utilities.getClonkNature(getProject()).getIndex().getDependencyNames()));
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
			return "";
		StringBuilder builder = new StringBuilder();
		for (String s : iterable) {
			builder.append(s);
			builder.append("<>");
		}
		return builder.toString();
	}
	
	@Override
	protected void createFieldEditors() {
		addField(new ListEditor(DEPENDENCIES_PROPERTY, "Dependencies", getFieldEditorParent()) {
			
			@Override
			protected String[] parseString(String stringList) {
				return stringList.split("<>");
			}
			
			@Override
			protected String getNewInputObject() {
				InputDialog input = new InputDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "New dependency", "Supply the name of a dependency", "", null);
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
	}

	public IAdaptable getElement() {
		return element;
	}

	public void setElement(IAdaptable element) {
		this.element = element;
		this.adapterStore = new AdapterStore();
	}

}