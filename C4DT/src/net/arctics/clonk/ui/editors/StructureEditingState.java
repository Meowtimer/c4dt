package net.arctics.clonk.ui.editors;

import java.util.LinkedList;
import java.util.List;
import java.util.TimerTask;

import net.arctics.clonk.parser.DeclMask;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.util.Utilities;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;

/**
 * Editing state on a specific {@link Structure}. Shared among all editors editing the file the {@link Structure} was read from.
 * @author madeen
 *
 * @param <EditorType> The type of {@link ClonkTextEditor} the state is shared among.
 * @param <StructureType> Type of {@link Structure}.
 */
public abstract class StructureEditingState<EditorType extends ClonkTextEditor, StructureType extends Structure> implements IDocumentListener {
	protected List<EditorType> editors = new LinkedList<EditorType>();
	protected StructureType structure;
	protected IDocument document;
	protected List<? extends StructureEditingState<EditorType, StructureType>> list;

	/**
	 * Called after the text change listener was added to a {@link IDocument} -> {@link StructureEditingState} map.
	 */
	protected void initialize() {}

	/**
	 * Add a text change listener of some supplied listener class to a {@link IDocument} -> {@link StructureEditingState} map.
	 * If there is already a listener in the map matching the document, this listener will be returned instead.
	 * @param <E> The type of {@link ClonkTextEditor} the listener needs to apply for.
	 * @param <S> The type of {@link Structure} the listener needs to apply for.
	 * @param <T> The type of {@link StructureEditingState} to add.
	 * @param list The listeners map
	 * @param type The listener class
	 * @param document The document
	 * @param structure The {@link Structure} corresponding to the document
	 * @param client One editor editing the document.
	 * @return The listener that has been either created and added to the map or was already found in the map.
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	@SuppressWarnings("unchecked")
	public static <E extends ClonkTextEditor, S extends Structure, T extends StructureEditingState<E, S>> T addTo(
		List<T> list,
		Class<T> type,
		IDocument document,
		S structure,
		E client
	) throws InstantiationException, IllegalAccessException {
		final StructureEditingState<? super E, ? super S> result = stateFromList(list, structure);
		T r;
		if (result == null) {
			r = type.newInstance();
			r.list = list;
			r.structure = structure;
			r.document = document;
			r.initialize();
			document.addDocumentListener(r);
			list.add(r);
		} else
			r = (T)result;
		r.editors.add(client);
		return r;
	}

	public static <E extends ClonkTextEditor, S extends Structure, T extends StructureEditingState<E, S>> T stateFromList(List<T> list, S structure) {
		T result = null;
		for (final T s : list)
			if (Utilities.objectsEqual(s.structure(), structure)) {
				result = s;
				break;
			}
		return result;
	}

	/*+
	 * Cancel a pending timed reparsing of the document.
	 */
	public void cancelReparsingTimer() {}

	/**
	 * Perform some cleanup after all corresponding editors have been closed.
	 */
	public void cleanupAfterRemoval() {}

	/**
	 * Remove an editor
	 * @param client
	 */
	public void removeEditor(EditorType client) {
		synchronized (editors) {
			if (editors.remove(client) && editors.isEmpty()) {
				cancelReparsingTimer();
				list.remove(document);
				document.removeDocumentListener(this);
				cleanupAfterRemoval();
			}
		}
	}

	@Override
	public void documentAboutToBeChanged(DocumentEvent event) {}

	@Override
	public void documentChanged(DocumentEvent event) { adjustDeclarationLocations(event); }

	/**
	 * Increment the components of some {@link SourceLocation} in-place that exceed a certain threshold.
	 * @param location The location to potentially modify
	 * @param threshold The threshold after which start and end offsets in the location will be incremented.
	 * @param add The value to add to the applicable offsets.
	 */
	protected void incrementLocationOffsetsExceedingThreshold(SourceLocation location, int threshold, int add) {
		if (location != null) {
			if (location.start() > threshold)
				location.setStart(location.start()+add);
			if (location.end() >= threshold)
				location.setEnd(location.end()+add);
		}
	}

	/**
	 * Adjust locations stored in a {@link Declaration} according to a call to {@link #incrementLocationOffsetsExceedingThreshold(SourceLocation, int, int)} for each of those locations.
	 * @param declaration The {@link Declaration}
	 * @param threshold The threshold to pass to {@link #incrementLocationOffsetsExceedingThreshold(SourceLocation, int, int)}
	 * @param add The increment value to pass to {@link #incrementLocationOffsetsExceedingThreshold(SourceLocation, int, int)}
	 */
	protected void adjustDec(Declaration declaration, int threshold, int add) {
		incrementLocationOffsetsExceedingThreshold(declaration, threshold, add);
	}

	/**
	 * Call {@link #adjustDec(Declaration, int, int)} for all applicable {@link Declaration}s stored in {@link #structure()}
	 * @param event Document event describing the document change that triggered this call.
	 */
	protected void adjustDeclarationLocations(DocumentEvent event) {
		if (event.getLength() == 0 && event.getText().length() > 0)
			// text was added
			for (final Declaration dec : structure.subDeclarations(structure.index(), DeclMask.ALL))
				adjustDec(dec, event.getOffset(), event.getText().length());
		else if (event.getLength() > 0 && event.getText().length() == 0)
			// text was removed
			for (final Declaration dec : structure.subDeclarations(structure.index(), DeclMask.ALL))
				adjustDec(dec, event.getOffset(), -event.getLength());
		else {
			final String newText = event.getText();
			final int replLength = event.getLength();
			final int offset = event.getOffset();
			final int diff = newText.length() - replLength;
			// mixed
			for (final Declaration dec : structure.subDeclarations(structure.index(), net.arctics.clonk.parser.DeclMask.ALL))
				if (dec.start() >= offset + replLength)
					adjustDec(dec, offset, diff);
				else if (dec instanceof Function) {
					// inside function: expand end location
					final Function func = (Function) dec;
					if (offset >= func.bodyLocation().start() && offset+replLength < func.bodyLocation().end())
						func.bodyLocation().setEnd(func.bodyLocation().end()+diff);
				}
		}
	}

	/**
	 * Cancel a {@link TimerTask} having been fired by this listener. May be null in which case nothing happens
	 * @param whichTask The task to cancel
	 * @return null so this method can be called and its return value be used as assignment right side with the TimerTask reference variable being on the left.
	 */
	public TimerTask cancelTimerTask(TimerTask whichTask) {
		if (whichTask != null)
			try {
				whichTask.cancel();
			} catch (final IllegalStateException e) {
				System.out.println("happens all the time, bitches");
			}
		return null;
	}

	/**
	 * The structure this listener corresponds to.
	 * @return The {@link Structure}
	 */
	public StructureType structure() { return structure; }
	
	/**
	 * Invalidate the {@link #structure()} reference and recompute.
	 */
	public void invalidate() {
		document.removeDocumentListener(this);
		document = editors.get(0).getDocumentProvider().getDocument(editors.get(0).getEditorInput());
		document.addDocumentListener(this);
	}
}