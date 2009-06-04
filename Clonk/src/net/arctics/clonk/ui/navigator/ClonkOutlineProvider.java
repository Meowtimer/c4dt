/**
 * 
 */
package net.arctics.clonk.ui.navigator;

import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.c4script.C4Function;
import net.arctics.clonk.parser.c4script.C4Type;
import net.arctics.clonk.parser.c4script.C4Variable;
import net.arctics.clonk.util.Utilities;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.swt.graphics.Image;

public class ClonkOutlineProvider extends LabelProvider implements ITreeContentProvider, IStyledLabelProvider {

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	public Object[] getChildren(Object obj) {
		if (obj instanceof C4Declaration)
			return ((C4Declaration)obj).getSubDeclarationsForOutline();
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	public Object getParent(Object obj) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	public boolean hasChildren(Object dec) {
		return dec instanceof C4Declaration && ((C4Declaration)dec).hasSubDeclarationsInOutline();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
	 */
	public Object[] getElements(Object root) {
		return getChildren(root);
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

	public Image getImage(Object element) {
		return Utilities.getIconForObject(element);
	}

	public String getText(Object element) {
		return getStyledText(element).toString();
	}

	public StyledString getStyledText(Object element) {
		return getStyledTextForEveryone(element);
	}
	
	public static StyledString getStyledTextForEveryone(Object element) {
		if (element instanceof C4Function) {
			C4Function func = ((C4Function)element);
			StyledString string = new StyledString(func.getLongParameterString(true));
			if (func.getReturnType() != null && func.getReturnType() != C4Type.UNKNOWN && func.getReturnType() != C4Type.ANY) {
				string.append(" : ");
				string.append(func.getReturnType().toString(true), StyledString.DECORATIONS_STYLER);
			}
			return string;
		}
		if (element instanceof C4Variable) {
			return new StyledString(((C4Variable)element).getName());
		}
		return new StyledString(element.toString());
	}

}
