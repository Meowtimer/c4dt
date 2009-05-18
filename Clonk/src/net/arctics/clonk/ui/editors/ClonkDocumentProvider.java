package net.arctics.clonk.ui.editors;


import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.ui.editors.text.FileDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

public class ClonkDocumentProvider extends FileDocumentProvider {
	public ClonkDocumentProvider(ITextEditor textEditor) {
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
//		if (!getProject(editor).isIndexed()) getProject(editor).indexAll();
		return document;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.AbstractDocumentProvider#changed(java.lang.Object)
	 */
	@Override
	public void changed(Object element) {
		super.changed(element);
//		getProject(editor).index(getEditingFile(editor));
//		if (editor instanceof C4ScriptEditor) {
//			((C4ScriptEditor)editor).getOutlinePage().refresh();
//		}
	}

}