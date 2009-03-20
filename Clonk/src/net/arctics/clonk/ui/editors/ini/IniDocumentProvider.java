package net.arctics.clonk.ui.editors.ini;


import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.ui.editors.text.FileDocumentProvider;

public class IniDocumentProvider extends FileDocumentProvider {
	public IniDocumentProvider() {
		
	}

	@Override
	protected IDocument createDocument(Object element) throws CoreException {
		IDocument document =  super.createDocument(element);
		if (document != null) {
			IDocumentPartitioner partitioner =
				new FastPartitioner(
					new IniPartitionScanner(),
					IniPartitionScanner.C4INI_PARTITIONS);
			partitioner.connect(document);
			document.setDocumentPartitioner(partitioner);
		}
		return document;
	}
	
}
