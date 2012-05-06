package net.arctics.clonk.ui.editors.actions;

import static net.arctics.clonk.util.Utilities.as;

import java.util.ResourceBundle;

import net.arctics.clonk.index.IIndexEntity;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.ui.editors.ClonkTextEditor;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Region;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextEditorAction;

/**
 * Base class for actions performed in a C4ScriptEditor. Provides facilities for locating entities/declarations at a certain text location.
 * @author madeen
 *
 */
public abstract class ClonkTextEditorAction extends TextEditorAction {

	public ClonkTextEditorAction(ResourceBundle bundle, String prefix, ITextEditor editor, String actionDefinitionId) {
		super(bundle, prefix, editor);
	}
	
	/**
	 * Return the entity denoted at the current selection.
	 * @param fallbackToCurrentFunction Whether to return the function the selection is currently in if locating an entity failed.
	 * @return The entity, the current function or null if ultimately unsuccessful.
	 */
	protected IIndexEntity entityAtSelection(boolean fallbackToCurrentFunction) {
		ITextSelection selection = (ITextSelection) getTextEditor().getSelectionProvider().getSelection();
		IRegion r = new Region(selection.getOffset(), selection.getLength());
		return ((ClonkTextEditor)getTextEditor()).entityAtRegion(fallbackToCurrentFunction, r);
	}

	/**
	 * Return the result of {@link #entityAtSelection(boolean)} cast to {@link Declaration}
	 * @param fallbackToCurrentFunction Passed on to {@link #entityAtSelection(boolean)}
	 * @return The entity at the selection cast to {@link Declaration} or null.
	 */
	protected Declaration declarationAtSelection(boolean fallbackToCurrentFunction) {
		return as(entityAtSelection(fallbackToCurrentFunction), Declaration.class);
	}

}