package net.arctics.clonk.ui.editors;

import net.arctics.clonk.resource.ClonkProjectNature;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.ui.editors.text.FileDocumentProvider;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

public class ClonkDocumentProvider extends FileDocumentProvider {
	private ITextEditor editor;
	// ich bin in einem geltungsbereich
	
	public ClonkDocumentProvider(ITextEditor textEditor) {
		editor = textEditor;
	}
	
	protected IDocument createDocument(Object element) throws CoreException {
		IDocument document = super.createDocument(element);
		if (document != null) {
			IDocumentPartitioner partitioner =
				new FastPartitioner(
					new ClonkPartitionScanner(),
					ClonkPartitionScanner.C4S_PARTITIONS);
			partitioner.connect(document);
			document.setDocumentPartitioner(partitioner);
		}
		if (!getProject(editor).isIndexed()) getProject(editor).indexAll();
		return document;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.AbstractDocumentProvider#changed(java.lang.Object)
	 */
	@Override
	public void changed(Object element) {
		// TODO Auto-generated method stub
		super.changed(element);
		getProject(editor).index(getEditingFile(editor));
	}
	
	protected ClonkProjectNature getProject(ITextEditor editor) {
		try {
			if (editor.getEditorInput() instanceof FileEditorInput) {
				IProjectNature clonkProj = ((FileEditorInput)editor.getEditorInput()).getFile().getProject().getNature("net.arctics.clonk.clonknature");
				if (clonkProj instanceof ClonkProjectNature) {
					return (ClonkProjectNature)clonkProj;
				}
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	protected IFile getEditingFile(ITextEditor editor) {
		if (editor.getEditorInput() instanceof FileEditorInput) {
			return ((FileEditorInput)editor.getEditorInput()).getFile();
		}
		else return null;
	}
}