package net.arctics.clonk.ui.editors.actions;

import static net.arctics.clonk.util.Utilities.as;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ResourceBundle;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.index.IIndexEntity;
import net.arctics.clonk.ui.editors.StructureTextEditor;

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

	@Retention(RetentionPolicy.RUNTIME)
	public @interface CommandId {
		String id();
	}
	
	public static CommandId id(Class<? extends ClonkTextEditorAction> c) {
		return c.getAnnotation(CommandId.class);
	}
	
	public static String idString(Class<? extends ClonkTextEditorAction> c) {
		CommandId commandId = id(c);
		if (commandId == null)
			return null;
		else
			return Core.id(commandId.id());
	}
	
	public static String resourceBundlePrefix(Class<? extends ClonkTextEditorAction> c) {
		String idString = idString(c);
		return idString.substring(idString.lastIndexOf('.')+1)+".";
	}
	
	private void assignId() {
		CommandId id = id(getClass());
		if (id != null) {
			String idString = Core.id(id.id());
			this.setId(idString);
			this.setActionDefinitionId(idString);
		} else
			System.out.println(String.format("Missing CommandId for %s", getClass().getSimpleName()));
	}
	
	public ClonkTextEditorAction(ResourceBundle bundle, String prefix, ITextEditor editor) {
		super(bundle, prefix, editor);
		assignId();
	}
	
	/**
	 * Return the entity denoted at the current selection.
	 * @param fallbackToCurrentFunction Whether to return the function the selection is currently in if locating an entity failed.
	 * @return The entity, the current function or null if ultimately unsuccessful.
	 */
	protected IIndexEntity entityAtSelection(boolean fallbackToCurrentFunction) {
		ITextSelection selection = (ITextSelection) getTextEditor().getSelectionProvider().getSelection();
		IRegion r = new Region(selection.getOffset(), selection.getLength());
		return ((StructureTextEditor)getTextEditor()).entityAtRegion(fallbackToCurrentFunction, r);
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