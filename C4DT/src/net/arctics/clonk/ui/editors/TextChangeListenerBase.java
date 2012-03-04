package net.arctics.clonk.ui.editors;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.IHasSubDeclarations;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;

/**
 * Helper listener that is shared between several editors having opened the same file. Will take care of adjusting {@link Declaration} locations and such according to document changes.
 * Keeps track of its 'clients' (the editors).
 * @author madeen
 *
 * @param <EditorType> The type of {@link ClonkTextEditor} the text change listener is attached to.
 * @param <StructureType> Type of {@link Structure} the file being edited is represented as.
 */
public abstract class TextChangeListenerBase<EditorType extends ClonkTextEditor, StructureType extends Structure> implements IDocumentListener {
	protected List<EditorType> clients = new LinkedList<EditorType>();
	protected StructureType structure;
	protected IDocument document;
	protected Map<IDocument, TextChangeListenerBase<EditorType, StructureType>> listeners;
	
	/**
	 * Called after the text change listener was added to a {@link IDocument} -> {@link TextChangeListenerBase} map.
	 */
	protected void added() {}
	
	/**
	 * Add a text change listener of some supplied listener class to a {@link IDocument} -> {@link TextChangeListenerBase} map.
	 * If there is already a listener in the map matching the document, this listener will be returned instead.
	 * @param <E> The type of {@link ClonkTextEditor} the listener needs to apply for.
	 * @param <S> The type of {@link Structure} the listener needs to apply for.
	 * @param <T> The type of {@link TextChangeListenerBase} to add.
	 * @param listeners The listeners map
	 * @param listenerClass The listener class
	 * @param document The document
	 * @param structure The {@link Structure} corresponding to the document 
	 * @param client One editor editing the document.
	 * @return The listener that has been either created and added to the map or was already found in the map.
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	@SuppressWarnings("unchecked")
	public static <E extends ClonkTextEditor, S extends Structure, T extends TextChangeListenerBase<E, S>> T addTo(
		Map<IDocument, TextChangeListenerBase<E, S>> listeners,
		Class<T> listenerClass, IDocument document, S structure, E client)
	throws InstantiationException, IllegalAccessException {
		TextChangeListenerBase<?, ?> result = listeners.get(document);
		T r;
		if (result == null) {
			r = listenerClass.newInstance();
			r.listeners = listeners;
			r.structure = structure;
			r.document = document;
			r.added();
			document.addDocumentListener(r);
			listeners.put(document, r);
		} else {
			r = (T)result;
		}
		r.clients.add(client);
		return r;
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
	public void removeClient(EditorType client) {
		clients.remove(client);
		if (clients.size() == 0) {
			cancelReparsingTimer();
			listeners.remove(document);
			document.removeDocumentListener(this);
			cleanupAfterRemoval();
		}
	}

	@Override
	public void documentAboutToBeChanged(DocumentEvent event) {
	}
	
	@Override
	public void documentChanged(DocumentEvent event) {
		structure.markAsDirty();
		adjustDeclarationLocations(event);
	}

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
		incrementLocationOffsetsExceedingThreshold(declaration.location(), threshold, add);
	}

	/**
	 * Call {@link #adjustDec(Declaration, int, int)} for all applicable {@link Declaration}s stored in {@link #getStructure()}
	 * @param event Document event describing the document change that triggered this call.
	 */
	protected void adjustDeclarationLocations(DocumentEvent event) {
		if (event.getLength() == 0 && event.getText().length() > 0) {
			// text was added
			for (Declaration dec : structure.accessibleDeclarations(IHasSubDeclarations.ALL)) {
				adjustDec(dec, event.getOffset(), event.getText().length());
			}
		}
		else if (event.getLength() > 0 && event.getText().length() == 0) {
			// text was removed
			for (Declaration dec : structure.accessibleDeclarations(IHasSubDeclarations.ALL)) {
				adjustDec(dec, event.getOffset(), -event.getLength());
			}
		}
		else {
			String newText = event.getText();
			int replLength = event.getLength();
			int offset = event.getOffset();
			int diff = newText.length() - replLength;
			// mixed
			for (Declaration dec : structure.accessibleDeclarations(IHasSubDeclarations.ALL)) {
				if (dec.location().start() >= offset + replLength)
					adjustDec(dec, offset, diff);
				else if (dec instanceof Function) {
					// inside function: expand end location
					Function func = (Function) dec;
					if (offset >= func.body().start() && offset+replLength < func.body().end()) {
						func.body().setEnd(func.body().end()+diff);
					}
				}
			}
		}
	}
	
	/**
	 * Cancel a {@link TimerTask} having been fired by this listener. May be null in which case nothing happens
	 * @param whichTask The task to cancel
	 * @return null so this method can be called and its return value be used as assignment right side with the TimerTask reference variable being on the left.
	 */
	public TimerTask cancelTimerTask(TimerTask whichTask) {
		if (whichTask != null) {
			try {
				whichTask.cancel();
			} catch (IllegalStateException e) {
				System.out.println("happens all the time, bitches");
			}
		}
		return null;
	}
	
	/**
	 * The structure this listener corresponds to.
	 * @return The {@link Structure}
	 */
	public StructureType getStructure() {
		return structure;
	}
	
	/**
	 * To be called when the old {@link Structure} has become stale and a new one has been created.
	 * @param structure The new {@link Structure} that represents the same file as the old one.
	 */
	@SuppressWarnings("unchecked")
	public void updateStructure(Structure structure) {
		this.structure = (StructureType) structure;
	}
}