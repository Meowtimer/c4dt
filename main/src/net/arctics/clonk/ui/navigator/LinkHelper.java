package net.arctics.clonk.ui.navigator;

import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ui.editors.StructureTextEditor;
import net.arctics.clonk.ui.editors.c4script.ScriptWithStorageEditorInput;
import net.arctics.clonk.util.ITreeNode;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.ILinkHelper;

public class LinkHelper implements ILinkHelper {

	@Override
	public void activateEditor(final IWorkbenchPage page, final IStructuredSelection selection) {
		try {
			if (selection.getFirstElement() instanceof Declaration) {
				final Declaration dec = (Declaration) selection.getFirstElement();
				final IWorkbenchPage wpage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				final IEditorInput input = dec.topLevelStructure() != null ? dec.topLevelStructure().makeEditorInput() : null;
				if (input != null && wpage.findEditor(input) != null)
					StructureTextEditor.openDeclaration(dec, false);
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public IStructuredSelection findSelection(final IEditorInput anInput) {
		final ScriptWithStorageEditorInput input = (ScriptWithStorageEditorInput) anInput;
		final StructuredSelection sel = new TreeSelection(getTreePath(input.script()));
		return sel;
	}
	
	public static TreePath getTreePath(final ITreeNode node) {
		return new TreePath(new Object[0]);
	}

}
