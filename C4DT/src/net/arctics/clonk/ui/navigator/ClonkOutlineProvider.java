/**
 * 
 */
package net.arctics.clonk.ui.navigator;

import java.lang.ref.WeakReference;

import net.arctics.clonk.index.Definition;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.util.UI;

import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;

public class ClonkOutlineProvider extends LabelProvider implements ITreeContentProvider, IStyledLabelProvider {

	protected static final Object[] NO_CHILDREN = new Object[0];
	private WeakReference<Object> root;
	
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
		if (dec instanceof Declaration) {
			Object[] subDeclarations = ((Declaration)dec).subDeclarationsForOutline();
			return subDeclarations != null && subDeclarations.length > 0;
		}
		else
			return false;
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
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		root = new WeakReference<Object>(newInput);
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
		boolean foreign =
			element instanceof Declaration &&
			root != null && root.get() instanceof Declaration &&
			!((Declaration)element).containedIn((Declaration)root.get());
		return getStyledTextForEveryone(element, foreign);
	}
	
	public static StyledString getStyledTextForEveryone(Object element, boolean foreign) {
		StyledString result = new StyledString();
		if (foreign && element instanceof Declaration) {
			Declaration topDec = ((Declaration)element).topLevelStructure();
			if (topDec != null) {
				result.append(topDec instanceof Definition ? ((Definition)topDec).id().stringValue() : topDec.name(), StyledString.QUALIFIER_STYLER);
				result.append(" - ");
			}
		}
		if (element instanceof Function) {
			Function func = ((Function)element);
			result.append(func.longParameterString(true, false));
			if (func.returnType() != null && func.returnType() != PrimitiveType.UNKNOWN && func.returnType() != PrimitiveType.ANY) {
				result.append(" : "); //$NON-NLS-1$
				result.append(func.returnType().typeName(true), StyledString.DECORATIONS_STYLER);
			}
		}
		else if (element instanceof Variable) {
			Variable var = (Variable)element;
			result.append(var.name());
			if (var.type() != null && var.type() != PrimitiveType.UNKNOWN && var.type() != PrimitiveType.ANY) {
				result.append(" : ");
				result.append(var.type().typeName(true));
			}
		}
		else if (element != null)
			result.append(element.toString());
		return result;
	}

}
