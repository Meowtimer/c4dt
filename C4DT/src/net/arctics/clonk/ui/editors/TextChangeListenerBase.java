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

public abstract class TextChangeListenerBase<EditorType extends ClonkTextEditor, StructureType extends Structure> implements IDocumentListener {
	protected List<EditorType> clients = new LinkedList<EditorType>();
	protected StructureType structure;
	protected IDocument document;
	protected Map<IDocument, TextChangeListenerBase<EditorType, StructureType>> listeners;
	
	protected void added() {}
	
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
	
	public void cancel() {}
	public void cleanupAfterRemoval() {}
	
	public void removeClient(EditorType client) {
		clients.remove(client);
		if (clients.size() == 0) {
			cancel();
			listeners.remove(document);
			document.removeDocumentListener(this);
			cleanupAfterRemoval();
		}
	}
	
	public void documentAboutToBeChanged(DocumentEvent event) {
	}
	
	@Override
	public void documentChanged(DocumentEvent event) {
		structure.setDirty(true);
		adjustDeclarationLocations(event);
	}

	protected void addToLocation(SourceLocation location, int offset, int add) {
		if (location != null) {
			if (location.getStart() > offset)
				location.setStart(location.getStart()+add);
			if (location.getEnd() >= offset)
				location.setEnd(location.getEnd()+add);
		}
	}

	protected void adjustDec(Declaration declaration, int offset, int add) {
		addToLocation(declaration.getLocation(), offset, add);
	}

	protected void adjustDeclarationLocations(DocumentEvent event) {
		if (event.getLength() == 0 && event.getText().length() > 0) {
			// text was added
			for (Declaration dec : structure.allSubDeclarations(IHasSubDeclarations.DIRECT_SUBDECLARATIONS)) {
				adjustDec(dec, event.getOffset(), event.getText().length());
			}
		}
		else if (event.getLength() > 0 && event.getText().length() == 0) {
			// text was removed
			for (Declaration dec : structure.allSubDeclarations(IHasSubDeclarations.DIRECT_SUBDECLARATIONS)) {
				adjustDec(dec, event.getOffset(), -event.getLength());
			}
		}
		else {
			String newText = event.getText();
			int replLength = event.getLength();
			int offset = event.getOffset();
			int diff = newText.length() - replLength;
			// mixed
			for (Declaration dec : structure.allSubDeclarations(IHasSubDeclarations.DIRECT_SUBDECLARATIONS)) {
				if (dec.getLocation().getStart() >= offset + replLength)
					adjustDec(dec, offset, diff);
				else if (dec instanceof Function) {
					// inside function: expand end location
					Function func = (Function) dec;
					if (offset >= func.getBody().getStart() && offset+replLength < func.getBody().getEnd()) {
						func.getBody().setEnd(func.getBody().getEnd()+diff);
					}
				}
			}
		}
	}
	
	public TimerTask cancel(TimerTask whichTask) {
		if (whichTask != null) {
			try {
				whichTask.cancel();
			} catch (IllegalStateException e) {
				System.out.println("happens all the time, bitches");
			}
		}
		return null;
	}
	
	public StructureType getStructure() {
		return structure;
	}
}