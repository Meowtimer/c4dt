package net.arctics.clonk.ui.editors.landscapescript;

import static net.arctics.clonk.util.Utilities.fileEditedBy;
import net.arctics.clonk.landscapescript.LandscapeScript;
import net.arctics.clonk.ui.editors.StructureOutlinePage;
import net.arctics.clonk.ui.editors.StructureTextEditor;
import net.arctics.clonk.ui.editors.StructureEditingState;

public class LandscapeScriptEditor extends StructureTextEditor {
	private LandscapeScriptEditingState state;
	@Override
	protected LandscapeScriptEditingState state() {
		if (state == null) {
			state = StructureEditingState.request(LandscapeScriptEditingState.class, getDocumentProvider().getDocument(getEditorInput()), new LandscapeScript(fileEditedBy(this)), this);
			state.reparse();
			setSourceViewerConfiguration(state);
		}
		return state;
	}
	@Override
	public void refreshOutline() {
		if (outlinePage != null) {
			outlinePage.setInput(structure());
			super.refreshOutline();
		}
	}
	@Override
	public StructureOutlinePage outlinePage() {
		if (outlinePage == null) {
			outlinePage = new StructureOutlinePage();
			outlinePage.setEditor(this);
		}
		return super.outlinePage();
	}
}
