package net.arctics.clonk.ui.navigator;

import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ui.editors.StructureTextEditor;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPage;

public class OpenSpecialItemAction extends Action {
	private final ISelectionProvider provider;
	public OpenSpecialItemAction(final IWorkbenchPage page,
			final ISelectionProvider selectionProvider) {
		this.setText(Messages.OpenSpecialItemAction_Open);
		this.provider = selectionProvider;
	}
	@Override
	public boolean isEnabled() {
		final ISelection selection = provider.getSelection();
		if(!selection.isEmpty()) {
			final IStructuredSelection sSelection = (IStructuredSelection) selection;
			for (final Object o : sSelection.toArray())
				if (!(o instanceof Declaration))
					return false;
			return true;
		}
		return false;
	}
	@Override
	public void run() {
		for (final Object o : ((IStructuredSelection)provider.getSelection()).toArray())
			try {
				StructureTextEditor.openDeclaration((Declaration)o);
			} catch (final Exception e) {
				e.printStackTrace();
			}
	}
}