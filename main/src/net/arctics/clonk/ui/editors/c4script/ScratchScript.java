package net.arctics.clonk.ui.editors.c4script;

import net.arctics.clonk.Core;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.ui.editors.IHasEditorPart;
import net.arctics.clonk.util.SelfcontainedStorage;

import org.eclipse.core.resources.IStorage;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Temporary script that is created when no other script can be found for this editor
 * @author madeen
 */
final class ScratchScript extends Script implements IHasEditorPart {
	private transient final C4ScriptEditor editor;
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	public ScratchScript(final C4ScriptEditor editor) {
		super(new Index() {
			private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
		});
		this.editor = editor;
	}

	@Override
	public IStorage source() {
		final IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
		return new SelfcontainedStorage(editor.getEditorInput().toString(), document.get());
	}

	@Override
	public ITextEditor editorPart() { return editor; }
}