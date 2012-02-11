/**
 * 
 */
package net.arctics.clonk.ui.navigator;

import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.util.UI;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.swt.graphics.Image;

public class ClonkOutlineProvider extends LabelProvider implements ITreeContentProvider, IStyledLabelProvider {

	protected static final Object[] NO_CHILDREN = new Object[0];
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	@Override
	public Object[] getChildren(Object obj) {
		if (obj instanceof Declaration)
			return ((Declaration)obj).subDeclarationsForOutline();
		return NO_CHILDREN;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	@Override
	public Object getParent(Object obj) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	@Override
	public boolean hasChildren(Object dec) {
		return dec instanceof Declaration && ((Declaration)dec).hasSubDeclarationsInOutline();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
	 */
	@Override
	public Object[] getElements(Object root) {
		return getChildren(root);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void inputChanged(Viewer arg0, Object arg1, Object arg2) {
		// TODO Auto-generated method stub
	}

	@Override
	public Image getImage(Object element) {
		return UI.iconFor(element);
	}

	@Override
	public String getText(Object element) {
		return getStyledText(element).toString();
	}

	@Override
	public StyledString getStyledText(Object element) {
		return getStyledTextForEveryone(element);
	}
	
	public static StyledString getStyledTextForEveryone(Object element) {
		if (element instanceof Function) {
			Function func = ((Function)element);
			StyledString string = new StyledString(func.getLongParameterString(true, false));
			if (func.returnType() != null && func.returnType() != PrimitiveType.UNKNOWN && func.returnType() != PrimitiveType.ANY) {
				string.append(" : "); //$NON-NLS-1$
				string.append(func.returnType().typeName(true), StyledString.DECORATIONS_STYLER);
			}
			return string;
		}
		if (element instanceof Variable) {
			Variable var = (Variable)element;
			StyledString string = new StyledString(var.name());
			if (var.type() != null && var.type() != PrimitiveType.UNKNOWN && var.type() != PrimitiveType.ANY) {
				string.append(" : ");
				string.append(var.type().typeName(true));
			}
			return string;
		}
		if (element != null)
			return new StyledString(element.toString());
		return new StyledString(""); //$NON-NLS-1$
	}

}
