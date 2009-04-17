package net.arctics.clonk.ui.navigator;

import net.arctics.clonk.parser.C4ScriptBase;
import net.arctics.clonk.ui.editors.c4script.C4ScriptEditor;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPage;

public class OpenSpecialItemAction extends Action {
	
	private ISelectionProvider provider;
	public OpenSpecialItemAction(IWorkbenchPage page,
			ISelectionProvider selectionProvider) {
		this.setText("Open"); //$NON-NLS-1$
		this.provider = selectionProvider;
	}

	@Override
	public boolean isEnabled() {
		ISelection selection = provider.getSelection();
		if(!selection.isEmpty()) {
			final IStructuredSelection sSelection = (IStructuredSelection) selection;
			for (Object o : sSelection.toArray()) {
				if (!(o instanceof C4ScriptBase))
					return false;
			}
			return true;
		}
		return false;
	}
	
	@Override
	public void run() {
		for (Object o : ((IStructuredSelection)provider.getSelection()).toArray()) {
			try {
				C4ScriptEditor.openDeclaration((C4ScriptBase)o);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
}
