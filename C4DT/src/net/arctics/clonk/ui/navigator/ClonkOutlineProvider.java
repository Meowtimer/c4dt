/**
 *
 */
package net.arctics.clonk.ui.navigator;

import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.defaulting;

import java.lang.ref.WeakReference;

import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.Function.PrintParametersOptions;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.c4script.typing.IType;
import net.arctics.clonk.c4script.typing.PrimitiveType;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.util.UI;

import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;

public class ClonkOutlineProvider extends LabelProvider implements ITreeContentProvider, IStyledLabelProvider {

	protected static final Object[] NO_CHILDREN = new Object[0];
	private WeakReference<Declaration> root;

	public ClonkOutlineProvider() {}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	@Override
	public Object[] getChildren(final Object obj) {
		return obj instanceof Declaration ? ((Declaration)obj).subDeclarationsForOutline() : NO_CHILDREN;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	@Override
	public Object getParent(final Object obj) {
		return obj instanceof Declaration ? ((Declaration)obj).parentDeclaration() : null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	@Override
	public boolean hasChildren(final Object dec) {
		if (dec instanceof Declaration) {
			final Object[] subDeclarations = ((Declaration)dec).subDeclarationsForOutline();
			return subDeclarations != null && subDeclarations.length > 0;
		}
		else
			return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
	 */
	@Override
	public Object[] getElements(final Object root) {
		return getChildren(root);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
		root = new WeakReference<Declaration>((Declaration)newInput);
	}

	@Override
	public Image getImage(final Object element) {
		return UI.iconFor(element);
	}

	@Override
	public String getText(final Object element) {
		return getStyledText(element).toString();
	}

	@Override
	public StyledString getStyledText(final Object element) {
		final boolean foreign =
			element instanceof Declaration &&
			root != null && root.get() instanceof Declaration &&
			!((Declaration)element).containedIn(root.get());
		return styledTextFor(element, foreign, root != null ? root.get() : null);
	}

	public static StyledString styledTextFor(final Object element, final boolean foreign, final Declaration root) {
		try {
			final StyledString result = new StyledString();
			if (foreign && element instanceof Declaration) {
				final Declaration topDec = ((Declaration)element).topLevelStructure();
				if (topDec != null) {
					result.append(topDec instanceof Definition ? ((Definition)topDec).id().stringValue() : topDec.name(), StyledString.QUALIFIER_STYLER);
					result.append("::");
				}
			}

			if (element instanceof Declaration) {
				final Script script = defaulting(as(root, Script.class), ((Declaration) element).script());
				if (element instanceof Function) {
					final Function func = ((Function)element);
					result.append(func.parameterString(new PrintParametersOptions(script.typings().get(func), true, false, false)));
					final IType retType = func.returnType(script);
					if (retType != null && retType != PrimitiveType.UNKNOWN) {
						result.append(" : "); //$NON-NLS-1$
						result.append(retType.typeName(true), StyledString.DECORATIONS_STYLER);
					}
				}
				else if (element instanceof Variable) {
					final Variable var = (Variable)element;
					result.append(var.name());
					final IType type = var.type(script);
					if (type != null && type != PrimitiveType.UNKNOWN) {
						result.append(" : ");
						result.append(type.typeName(true));
					}
				}
				else if (element != null)
					result.append(element.toString());
			}
			return result;
		} catch (final Exception e) {
			System.out.println(String.format("Computing styled text for '%s' failed", element.toString()));
			e.printStackTrace();
			return new StyledString("<failed>");
		}
	}

}
