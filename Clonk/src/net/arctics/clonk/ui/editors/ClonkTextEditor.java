package net.arctics.clonk.ui.editors;

import java.io.IOException;

import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.c4script.C4Structure;
import net.arctics.clonk.parser.c4script.C4ScriptParser.ParsingException;
import net.arctics.clonk.ui.editors.c4script.C4ScriptEditor;
import net.arctics.clonk.ui.editors.c4script.ClonkContentOutlinePage;

import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

public class ClonkTextEditor extends TextEditor {
	
	protected ClonkContentOutlinePage outlinePage;
	private ShowInAdapter showInAdapter;
	
	public void selectAndReveal(SourceLocation location) {
		this.selectAndReveal(location.getStart(), location.getEnd() - location.getStart());
	}
	
	public void refreshOutline() {
		if (outlinePage != null) // don't start lazy loading of outlinePage
			outlinePage.refresh();
	}
	
	public ClonkContentOutlinePage getOutlinePage() {
		if (outlinePage == null) {
			outlinePage = new ClonkContentOutlinePage();
			outlinePage.setEditor(this);
		}
		return outlinePage;
	}
	
	@SuppressWarnings("unchecked")
	public Object getAdapter(Class adapter) {
		if (IContentOutlinePage.class.equals(adapter)) {
			return getOutlinePage();
		}
		if (IShowInSource.class.equals(adapter) || IShowInTargetList.class.equals(adapter)) {
			if (showInAdapter == null)
				showInAdapter = new ShowInAdapter(this);
			return showInAdapter;
		}
		return super.getAdapter(adapter);
	}
	
	public static IEditorPart openDeclaration(C4Declaration target, boolean activate) throws PartInitException, IOException, ParsingException {
		IWorkbench workbench = PlatformUI.getWorkbench();
		IWorkbenchPage workbenchPage = workbench.getActiveWorkbenchWindow().getActivePage();
		C4Structure structure = target.getStructure();
		if (structure != null) {
			IEditorInput input = structure.getEditorInput();
			if (input != null) {
				IEditorDescriptor descriptor = input instanceof IFileEditorInput ? IDE.getEditorDescriptor(((IFileEditorInput)input).getFile()) : null;
				String editorId = descriptor != null ? descriptor.getId() : "clonk.editors.C4ScriptEditor"; 
				IEditorPart editor = IDE.openEditor(workbenchPage, input, editorId, activate);
				ClonkTextEditor clonkTextEditor = (ClonkTextEditor) editor;
				if (target != structure) {
					if (structure.isEditable() && clonkTextEditor instanceof C4ScriptEditor)
						((C4ScriptEditor) clonkTextEditor).reparseWithDocumentContents(null, false);
					target = target.latestVersion();
					if (target != null)
						clonkTextEditor.selectAndReveal(target.getLocation());
				}
			}
		}
		return null;
	}
	
	public static IEditorPart openDeclaration(C4Declaration target) throws PartInitException, IOException, ParsingException {
		return openDeclaration(target, true);
	}
	
	/**
	 * Return the declaration that represents the file being edited
	 * @return the declaration
	 */
	public C4Declaration getTopLevelDeclaration() {
		return null;
	}
	
}
