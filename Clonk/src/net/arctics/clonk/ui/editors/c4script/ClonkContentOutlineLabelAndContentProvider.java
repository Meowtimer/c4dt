/**
 * 
 */
package net.arctics.clonk.ui.editors.c4script;

import net.arctics.clonk.parser.C4Field;
import net.arctics.clonk.parser.C4Function;
import net.arctics.clonk.parser.C4ScriptBase;
import net.arctics.clonk.parser.C4Type;
import net.arctics.clonk.parser.C4Variable;
import net.arctics.clonk.util.Utilities;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.swt.graphics.Image;

/**
 * @author madeen
 *
 */
public class ClonkContentOutlineLabelAndContentProvider extends LabelProvider implements ITreeContentProvider, IStyledLabelProvider {
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#addListener(org.eclipse.jface.viewers.ILabelProviderListener)
	 */
//	public void addListener(ILabelProviderListener arg0) {
//		// TODO Auto-generated method stub
//
//	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	public Object[] getChildren(Object obj) {
		return ((C4Field)obj).getChildFieldsForOutline();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	public Object getParent(Object arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	public boolean hasChildren(Object field) {
		return ((C4Field)field).hasChildFields();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
	 */
	public Object[] getElements(Object root) {
		if (root instanceof C4ScriptBase) {
			C4ScriptBase script = (C4ScriptBase)root;
			Object[] result = new Object[script.numFunctions()+script.numVariables()];
			int i = 0;
			for (C4Function f : script.functions())
				result[i++] = f;
			for (C4Variable v : script.variables())
				result[i++] = v;
			return result;
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	public void inputChanged(Viewer arg0, Object arg1, Object arg2) {
		// TODO Auto-generated method stub
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
	 */
	public void dispose() {
		// TODO Auto-generated method stub
		super.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(java.lang.Object, java.lang.String)
	 */
//	public boolean isLabelProperty(Object arg0, String arg1) {
//		// TODO Auto-generated method stub
//		return false;
//	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#removeListener(org.eclipse.jface.viewers.ILabelProviderListener)
	 */
//	public void removeListener(ILabelProviderListener arg0) {
//		// TODO Auto-generated method stub
//
//	}

	public Image getImage(Object element) {
		return Utilities.getIconForObject(element);
	}

	public String getText(Object element) {
		return getStyledText(element).toString();
//		if (element instanceof C4Function) {
//			return ((C4Function)element).getLongParameterString(true);
//		}
//		if (element instanceof C4Variable) {
//			return ((C4Variable)element).getName();
//		}
//		return element.toString();
	}

	public StyledString getStyledText(Object element) {
		if (element instanceof C4Function) {
			C4Function func = ((C4Function)element);
			StyledString string = new StyledString(func.getLongParameterString(true));
			if (func.getReturnType() != null && func.getReturnType() != C4Type.UNKNOWN && func.getReturnType() != C4Type.ANY) {
				string.append(" : ");
				string.append(func.getReturnType().name(), StyledString.DECORATIONS_STYLER);
			}
			return string;
		}
		if (element instanceof C4Variable) {
			return new StyledString(((C4Variable)element).getName());
		}
		return new StyledString(element.toString());
	}

}
