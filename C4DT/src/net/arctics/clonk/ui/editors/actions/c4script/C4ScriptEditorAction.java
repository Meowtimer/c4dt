package net.arctics.clonk.ui.editors.actions.c4script;

import static net.arctics.clonk.util.Utilities.as;

import java.util.ResourceBundle;

import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.IIndexEntity;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.ui.editors.c4script.C4ScriptEditor;
import net.arctics.clonk.ui.editors.c4script.EntityLocator;

import org.eclipse.jface.text.BadLocationException;
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
public abstract class C4ScriptEditorAction extends TextEditorAction {

	public C4ScriptEditorAction(ResourceBundle bundle, String prefix, ITextEditor editor, String actionDefinitionId) {
		super(bundle, prefix, editor);
	}
	
	/**
	 * Get the expression at the current selection.
	 * @return The expression or null if the current selection is outside any region containing expressions.
	 */
	protected ExprElm expressionAtSelection() {
		ITextSelection selection = (ITextSelection) getTextEditor().getSelectionProvider().getSelection();
		IRegion r = new Region(selection.getOffset(), selection.getLength());
		try {
			return new EntityLocator(
				getTextEditor(),
				getTextEditor().getDocumentProvider().getDocument(getTextEditor().getEditorInput()),
				r
			).getExprAtRegion();
		} catch (BadLocationException e) {
			e.printStackTrace();
			return null;
		} catch (ParsingException e) {
			return null;
		}
	}
	
	/**
	 * Return the entity denoted at the current selection.
	 * @param fallbackToCurrentFunction Whether to return the function the selection is currently in if locating an entity failed.
	 * @return The entity, the current function or null if ultimately unsuccessful.
	 */
	protected IIndexEntity entityAtSelection(boolean fallbackToCurrentFunction) {
		ITextSelection selection = (ITextSelection) getTextEditor().getSelectionProvider().getSelection();
		IRegion r = new Region(selection.getOffset(), selection.getLength());
		try {
			EntityLocator info = new EntityLocator(
				getTextEditor(),
				getTextEditor().getDocumentProvider().getDocument(getTextEditor().getEditorInput()),
				r
			);
			
			if (info.entity() != null)
				return info.entity();
			else if (fallbackToCurrentFunction && getTextEditor() instanceof C4ScriptEditor)
				return ((C4ScriptEditor)getTextEditor()).functionAtCursor();
			else
				return null;
		} catch (BadLocationException e) {
			e.printStackTrace();
			return null;
		} catch (ParsingException e) {
			return null;
		}
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