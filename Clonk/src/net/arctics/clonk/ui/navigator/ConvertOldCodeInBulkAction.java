package net.arctics.clonk.ui.navigator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.parser.C4Function;
import net.arctics.clonk.parser.C4ScriptBase;
import net.arctics.clonk.parser.C4ScriptParser;
import net.arctics.clonk.parser.C4ScriptExprTree.Statement;
import net.arctics.clonk.parser.C4ScriptParser.ParsingException;
import net.arctics.clonk.ui.editors.actions.c4script.ConvertOldCodeToNewCodeAction;
import net.arctics.clonk.util.Pair;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;

public class ConvertOldCodeInBulkAction extends Action {
	ConvertOldCodeInBulkAction(String text) {
		super(text);
	}

	@Override
	public void runWithEvent(Event event) {
		super.run();
		if (PlatformUI.getWorkbench() == null ||
				PlatformUI.getWorkbench().getActiveWorkbenchWindow() == null ||
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService() == null ||
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection() == null)
			return;
		ISelection selection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
		if (selection != null && selection instanceof TreeSelection) {
			TreeSelection tree = (TreeSelection) selection;
			Iterator<?> it = tree.iterator();
			List<IContainer> selectedContainers = new LinkedList<IContainer>();
			while (it.hasNext()) {
				Object obj = it.next();
				if (obj instanceof IProject) {
					try {
						IResource[] selectedResources = ((IProject)obj).members(IContainer.EXCLUDE_DERIVED);
						selectedContainers = new ArrayList<IContainer>();
						for(int i = 0; i < selectedResources.length;i++) {
							if (selectedResources[i] instanceof IContainer && !selectedResources[i].getName().startsWith("."))
								selectedContainers.add((IContainer) selectedResources[i]);
						}
					}
					catch (CoreException ex) {
						ex.printStackTrace();
					}
				}
				else if (obj instanceof IFolder) {
					selectedContainers.add((IContainer) obj);
				}
			}
			if (selectedContainers.size() > 0) {
				for (IContainer container : selectedContainers) {
					try {
						final TextFileDocumentProvider textFileDocProvider = new TextFileDocumentProvider();
						final List<IFile> failedSaves = new LinkedList<IFile>();
						container.accept(new IResourceVisitor() {
							public boolean visit(IResource resource)
									throws CoreException {
								if (resource instanceof IFile) {
									IFile file = (IFile) resource;
									C4ScriptBase script = Utilities.getScriptForFile(file);
									if (script != null) {
										C4ScriptParser parser = new C4ScriptParser(file, script);
										LinkedList<Pair<C4Function, LinkedList<Statement>>> statements = new LinkedList<Pair<C4Function, LinkedList<Statement>>>();
										parser.setExpressionListener(ConvertOldCodeToNewCodeAction.expressionCollector(null, statements, 0));
										try {
											parser.parse();
										} catch (ParsingException e1) {
											e1.printStackTrace();
										}
										textFileDocProvider.connect(file);
										IDocument document = textFileDocProvider.getDocument(file);
										
										if (document != null)
											ConvertOldCodeToNewCodeAction.runOnDocument(parser, new TextSelection(document, 0, 0), document, statements);

										try {
											textFileDocProvider.setEncoding(document, textFileDocProvider.getDefaultEncoding());
											textFileDocProvider.saveDocument(null, file, document, true);
										} catch (CoreException e) {
											e.printStackTrace();
											failedSaves.add(file);
										}
										textFileDocProvider.disconnect(file);
									}
								}
								return true;
							}
						});
						// TODO: do something with failedSaves
					} catch (CoreException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
}