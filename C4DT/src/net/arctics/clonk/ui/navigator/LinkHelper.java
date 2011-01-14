package net.arctics.clonk.ui.navigator;

import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.ui.editors.ClonkTextEditor;
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

	public void activateEditor(IWorkbenchPage page, IStructuredSelection selection) {
		try {
			if (selection.getFirstElement() instanceof Declaration) {
				Declaration dec = (Declaration) selection.getFirstElement();
				IWorkbenchPage wpage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				IEditorInput input = dec.getTopLevelStructure() != null ? dec.getTopLevelStructure().getEditorInput() : null;
				if (input != null && wpage.findEditor(input) != null)
					ClonkTextEditor.openDeclaration(dec, false);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public IStructuredSelection findSelection(IEditorInput anInput) {
		ScriptWithStorageEditorInput input = (ScriptWithStorageEditorInput) anInput;
		StructuredSelection sel = new TreeSelection(getTreePath(input.getScript()));
		return sel;
	}
	
	public static TreePath getTreePath(ITreeNode node) {
		return new TreePath(new Object[0]);
	}

}
