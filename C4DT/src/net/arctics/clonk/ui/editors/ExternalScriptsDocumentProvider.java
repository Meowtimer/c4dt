package net.arctics.clonk.ui.editors;


import org.eclipse.ui.editors.text.FileDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

public class ExternalScriptsDocumentProvider extends FileDocumentProvider {
	
	public ExternalScriptsDocumentProvider(ITextEditor textEditor) {
		super ();
	}
	
	@Override
	public String getEncoding(Object element) {
		return super.getEncoding(element);
	}

}