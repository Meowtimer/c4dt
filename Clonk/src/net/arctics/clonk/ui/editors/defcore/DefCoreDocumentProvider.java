package net.arctics.clonk.ui.editors.defcore;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.ui.editors.text.FileDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

public class DefCoreDocumentProvider extends FileDocumentProvider {
	public DefCoreDocumentProvider(ITextEditor editor) {
	}

	@Override
	protected IDocument createDocument(Object element) throws CoreException {
		IDocument document =  super.createDocument(element);
		if (document != null) {
			IDocumentPartitioner partitioner =
				new FastPartitioner(
					new DefCorePartitionScanner(),
					DefCorePartitionScanner.C4INI_PARTITIONS);
			partitioner.connect(document);
			document.setDocumentPartitioner(partitioner);
		}
		return document;
	}
	
}
