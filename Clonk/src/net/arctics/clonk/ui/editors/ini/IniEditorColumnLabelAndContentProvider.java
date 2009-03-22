package net.arctics.clonk.ui.editors.ini;

import net.arctics.clonk.parser.inireader.IniData.IniConfiguration;
import net.arctics.clonk.util.IHasChildren;
import net.arctics.clonk.util.IHasKeyAndValue;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;

public class IniEditorColumnLabelAndContentProvider implements ITableLabelProvider, ITreeContentProvider {

	private IniConfiguration configuration;
	
	public IniEditorColumnLabelAndContentProvider(IniConfiguration configuration) {
		this.configuration = configuration;
	}

	public Image getColumnImage(Object element, int columnIndex) {
		// TODO Auto-generated method stub
		return null;
	}

	@SuppressWarnings("unchecked")
	public String getColumnText(Object element, int columnIndex) {
		IHasKeyAndValue<String, String> keyVal = (IHasKeyAndValue<String, String>) element;
		switch (columnIndex) {
		case 0:
			return keyVal.getKey();
		case 1:
			return keyVal.getValue();
		}
		return "Unknown";
	}


	public Object[] getChildren(Object element) {
		return element instanceof IHasChildren ? ((IHasChildren)element).getChildren() : null;
	}

	public Object getParent(Object element) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean hasChildren(Object element) {
		return element instanceof IHasChildren ? ((IHasChildren)element).hasChildren() : false;
	}

	public Object[] getElements(Object element) {
		return getChildren(element);
	}

	public void dispose() {
		// TODO Auto-generated method stub
		
	}

	public void inputChanged(Viewer viewer, Object oldInput,
			Object newInput) {
		// TODO Auto-generated method stub
		
	}

	public void addListener(ILabelProviderListener listener) {
		// TODO Auto-generated method stub
		
	}

	public boolean isLabelProperty(Object element, String property) {
		// TODO Auto-generated method stub
		return false;
	}

	public void removeListener(ILabelProviderListener listener) {
		// TODO Auto-generated method stub
		
	}


}
