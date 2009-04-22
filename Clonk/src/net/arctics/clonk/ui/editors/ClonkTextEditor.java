package net.arctics.clonk.ui.editors;

import java.io.IOException;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.C4Field;
import net.arctics.clonk.parser.C4ObjectIntern;
import net.arctics.clonk.parser.C4ScriptBase;
import net.arctics.clonk.parser.C4ScriptIntern;
import net.arctics.clonk.parser.SourceLocation;
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

public class ClonkTextEditor extends TextEditor {
	
	private ClonkContentOutlinePage outlinePage;
	
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
	
	public static IEditorPart openDeclaration(C4Field target, boolean activate) throws PartInitException, IOException, ParsingException {
		IWorkbench workbench = PlatformUI.getWorkbench();
		IWorkbenchPage workbenchPage = workbench.getActiveWorkbenchWindow().getActivePage();
		C4ScriptBase script = target instanceof C4ScriptBase ? (C4ScriptBase)target : target.getScript();
		if (script != null) {
			if (script instanceof C4ObjectIntern || script instanceof C4ScriptIntern) {
				IFile scriptFile = (IFile) script.getScriptFile();
				if (scriptFile != null) {
					IEditorPart editor = IDE.openEditor(workbenchPage, scriptFile, activate);
					C4ScriptEditor scriptEditor = (C4ScriptEditor)editor;						
					if (target != script) {
						scriptEditor.reparseWithDocumentContents(null, false);
						target = target.latestVersion();
						if (target != null)
							scriptEditor.selectAndReveal(target.getLocation());
					}
					return scriptEditor;
				} else {
					IFile defCore = ((C4ObjectIntern)script).getDefCoreFile();
					if (defCore != null)
						return IDE.openEditor(workbenchPage, defCore, activate);
				}
			}
			else if (script.getScriptFile() instanceof IStorage) {
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
				ClonkTextEditor ed = (ClonkTextEditor)editor;						
				if (target != script) {
					target = target.latestVersion();
					if (target != null)
						ed.selectAndReveal(target.getLocation());
				}
				return ed;
			}
		}
		return null;
	}
	
}
