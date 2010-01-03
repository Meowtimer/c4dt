package net.arctics.clonk.ui.editors;


import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.ui.editors.c4script.ScriptWithStorageEditorInput;
import org.eclipse.ui.editors.text.FileDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

public class ExternalScriptsDocumentProvider extends FileDocumentProvider {
	
	public ExternalScriptsDocumentProvider(ITextEditor textEditor) {
		super ();
	}
	
	@Override
	public String getEncoding(Object element) {
		if (element instanceof ScriptWithStorageEditorInput) {
			return ClonkPreferences.getPreferenceOrDefault(ClonkPreferences.EXTERNAL_INDEX_ENCODING);
		}
		return super.getEncoding(element);
	}

}