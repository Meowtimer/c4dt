package net.arctics.clonk.ui.editors;

import java.io.IOException;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.C4ObjectIntern;
import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.c4script.C4ScriptParser.ParsingException;
import net.arctics.clonk.ui.editors.c4script.C4ScriptEditor;
import net.arctics.clonk.ui.editors.c4script.ClonkContentOutlinePage;
import net.arctics.clonk.ui.editors.c4script.ScriptWithStorageEditorInput;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.ui.IEditorPart;
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
		C4ScriptBase script = target instanceof C4ScriptBase ? (C4ScriptBase)target : target.getScript();
		if (script != null) {
			Object scriptStorage = script.getScriptFile();
			if (scriptStorage instanceof IFile) {
				IFile scriptFile = (IFile) scriptStorage;
				if (scriptFile != null) {
					IEditorPart editor = IDE.openEditor(workbenchPage, scriptFile, activate);
					if (editor instanceof C4ScriptEditor) {
						C4ScriptEditor scriptEditor = (C4ScriptEditor)editor;						
						if (target != script) {
							scriptEditor.reparseWithDocumentContents(null, false);
							target = target.latestVersion();
							if (target != null)
								scriptEditor.selectAndReveal(target.getLocation());
						}
					}
					return editor;
				} else {
					IFile defCore = ((C4ObjectIntern)script).getDefCoreFile();
					if (defCore != null)
						return IDE.openEditor(workbenchPage, defCore, activate);
				}
			}
			else if (scriptStorage instanceof IStorage) {
				if (script != ClonkCore.getDefault().ENGINE_OBJECT) {
					IEditorPart editor = workbenchPage.openEditor(new ScriptWithStorageEditorInput(script), "clonk.editors.C4ScriptEditor");
					C4ScriptEditor scriptEditor = (C4ScriptEditor)editor;
					if (target != script)
						scriptEditor.selectAndReveal(target.getLocation());
					return scriptEditor;
				}
			}
		}
		else {
			IResource res = target.getResource();
			if (res instanceof IFile) {
				IEditorPart editor = IDE.openEditor(workbenchPage, (IFile) res, activate);
				if (editor instanceof ClonkTextEditor) {
					ClonkTextEditor ed = (ClonkTextEditor)editor;						
					if (target != script) {
						target = target.latestVersion();
						if (target != null)
							ed.selectAndReveal(target.getLocation());
					}
				}
				return editor;
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
