package net.arctics.clonk.ui.editors;


import net.arctics.clonk.preferences.PreferenceConstants;
import net.arctics.clonk.ui.editors.c4script.ScriptWithStorageEditorInput;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.ui.editors.text.FileDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

public class ClonkDocumentProvider extends FileDocumentProvider {
	
	public ClonkDocumentProvider(ITextEditor textEditor) {
		super ();
	}
	
	protected IDocument createDocument(Object element) throws CoreException {
		IDocument document = super.createDocument(element);
		if (document != null) {
			IDocumentPartitioner partitioner =
				new FastPartitioner(
					new ClonkPartitionScanner(),
					ClonkPartitionScanner.C4S_PARTITIONS
				);
			partitioner.connect(document);
			document.setDocumentPartitioner(partitioner);
		}
		return document;
	}
	
	@Override
	public String getEncoding(Object element) {
		if (element instanceof ScriptWithStorageEditorInput) {
			return Utilities.getPreference(PreferenceConstants.EXTERNAL_INDEX_ENCODING, PreferenceConstants.EXTERNAL_INDEX_ENCODING_DEFAULT, null);
		}
		return super.getEncoding(element);
	}

}