package net.arctics.clonk.ui.editors;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

import net.arctics.clonk.ast.DeclMask;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ast.SourceLocation;
import net.arctics.clonk.ast.Structure;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.index.IIndexEntity;
import net.arctics.clonk.ui.editors.actions.OpenDeclarationAction;
import net.arctics.clonk.util.Utilities;

import org.eclipse.jface.internal.text.html.HTMLTextPresenter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.URLHyperlinkDetector;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;

/**
 * Editing state on a specific {@link Structure}. Shared among all editors editing the file the {@link Structure} was read from.
 * @author madeen
 *
 * @param <EditorType> The type of {@link ClonkTextEditor} the state is shared among.
 * @param <StructureType> Type of {@link Structure}.
 */
@SuppressWarnings("restriction")
public abstract class StructureEditingState<EditorType extends ClonkTextEditor, StructureType extends Structure>
	extends TextSourceViewerConfiguration
	implements IDocumentListener, IPartListener {

	public class ClonkTextHover implements ITextHover, ITextHoverExtension {
		private IHyperlink hyperlink;
		public ClonkTextHover() { super(); }
		@Override
		public String getHoverInfo(ITextViewer viewer, IRegion region) {
			if (hyperlink instanceof ClonkHyperlink) {
				final ClonkHyperlink clonkHyperlink = (ClonkHyperlink) hyperlink;
				final IIndexEntity entity = clonkHyperlink.target();
				if (entity != null)
					return entity.infoText(structure());
			}
			return null;
		}
		@Override
		public IRegion getHoverRegion(ITextViewer viewer, int offset) {
			hyperlink = hyperlinkAtOffset(offset);
			if (hyperlink != null)
				return hyperlink.getHyperlinkRegion();
			return null;
		}
		@Override
		public IInformationControlCreator getHoverControlCreator() {
			return new IInformationControlCreator() {
				@Override
				public IInformationControl createInformationControl(Shell parent) {
					return new DefaultInformationControl(parent, new HTMLTextPresenter(true));
				}
			};
		}
	}

	protected List<EditorType> editors = new LinkedList<EditorType>();
	protected List<ISourceViewer> viewers = new LinkedList<ISourceViewer>();
	protected StructureType structure;
	protected IDocument document;
	protected List<? extends StructureEditingState<EditorType, StructureType>> list;
	protected ContentAssistant assistant;
	protected ISourceViewer assistantSite;

	protected ContentAssistant installAssistant(ISourceViewer sourceViewer) {
		if (assistantSite != sourceViewer) {
			if (assistantSite != null)
				assistant.uninstall();
			assistantSite = sourceViewer;
			assistant.install(sourceViewer);
		}
		return assistant;
	}

	protected ContentAssistant createAssistant() { return new ContentAssistant(); }
	public ContentAssistant assistant() { return assistant; }

	/**
	 * Called after the text change listener was added to a {@link IDocument} -> {@link StructureEditingState} map.
	 */
	protected void initialize() {
		assistant = createAssistant();
		document.addDocumentListener(this);
	}

	static Map<Class<? extends StructureEditingState<?, ?>>, List<? extends StructureEditingState<?, ?>>> lists = new HashMap<>();

	@SuppressWarnings("unchecked")
	public static <S extends Structure, T extends StructureEditingState<?, S>> T existing(Class<T> cls, S structure) {
		List<T> list;
		synchronized (lists) {
			list = (List<T>) lists.get(cls);
			if (list == null)
				return null;
		}
		for (final T s : list)
			if (s.structure() == structure)
				return s;
		return null;
	}

	/**
	 * Add a text change listener of some supplied listener class to a {@link IDocument} -> {@link StructureEditingState} map.
	 * If there is already a listener in the map matching the document, this listener will be returned instead.
	 * @param type The listener class
	 * @param document The document
	 * @param structure The {@link Structure} corresponding to the document
	 * @param editor One editor editing the document.
	 * @param list The listeners map
	 * @param <E> The type of {@link ClonkTextEditor} the listener needs to apply for.
	 * @param <S> The type of {@link Structure} the listener needs to apply for.
	 * @param <T> The type of {@link StructureEditingState} to add.
	 * @return The listener that has been either created and added to the map or was already found in the map.
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	@SuppressWarnings("unchecked")
	public static <E extends ClonkTextEditor, S extends Structure, T extends StructureEditingState<E, S>> T request(
		Class<T> type,
		IDocument document,
		S structure,
		E editor
	) {
		List<T> list;
		synchronized (lists) {
			list = (List<T>) lists.get(type);
			if (list == null)
				lists.put(type, list = new LinkedList<T>());
		}
		final StructureEditingState<? super E, ? super S> result = stateFromList(list, structure);
		T r;
		if (result == null) {
			try {
				r = type.getConstructor(IPreferenceStore.class).newInstance(editor.preferenceStore());
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException |
				InvocationTargetException | NoSuchMethodException | SecurityException e) {
				e.printStackTrace();
				return null;
			}
			r.set(list, structure, document);
			r.initialize();
			list.add(r);
		} else
			r = (T)result;
		r.addEditor(editor);
		return r;
	}

	void set(List<? extends StructureEditingState<EditorType, StructureType>> list, StructureType structure, IDocument document) {
		this.list = list;
		this.structure = structure;
		this.document = document;
	}

	protected static <E extends ClonkTextEditor, S extends Structure, T extends StructureEditingState<E, S>> T stateFromList(List<T> list, S structure) {
		T result = null;
		for (final T s : list)
			if (Utilities.eq(s.structure(), structure)) {
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

	protected void addEditor(EditorType editor) {
		editors.add(editor);
		editor.getSite().getPage().addPartListener(this);
	}

	/**
	 * Remove an editor
	 * @param editor The editor
	 */
	public void removeEditor(EditorType editor) {
		synchronized (editors) {
			if (editors.remove(editor)) {
				maybeRemovePartListener(editor);
				if (editors.isEmpty()) {
					cancelReparsingTimer();
					list.remove(this);
					document.removeDocumentListener(this);
					cleanupAfterRemoval();
				}
			}
		}
	}

	private void maybeRemovePartListener(EditorType removedEditor) {
		boolean removePartListener = true;
		for (final EditorType ed : editors)
			if (ed.getSite().getPage() == removedEditor.getSite().getPage()) {
				removePartListener = false;
				break;
			}
		if (removePartListener)
			removedEditor.getSite().getPage().removePartListener(this);
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
			for (final Declaration dec : structure.subDeclarations(structure.index(), net.arctics.clonk.ast.DeclMask.ALL))
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
			} catch (final IllegalStateException e) {}
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

	@Override
	public void partActivated(IWorkbenchPart part) {}
	@Override
	public void partBroughtToTop(IWorkbenchPart part) {}
	@SuppressWarnings("unchecked")
	@Override
	public void partClosed(IWorkbenchPart part) {
		try { removeEditor((EditorType) part); }
		catch (final ClassCastException cce) {}
	}
	@Override
	public void partDeactivated(IWorkbenchPart part) {}
	@Override
	public void partOpened(IWorkbenchPart part) {}
	public void completionProposalApplied(ClonkCompletionProposal proposal) {}

	@SuppressWarnings("unchecked")
	public EditorType activeEditor() {
		final IEditorPart activeEditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		return editors.contains(activeEditor) ? (EditorType)activeEditor : null;
	}

	protected ITextHover hover;
	public StructureEditingState(IPreferenceStore store) { super(store); }
	public ColorManager getColorManager() { return ColorManager.INSTANCE; }
	@Override
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return CStylePartitionScanner.PARTITIONS;
	}
	protected static final URLHyperlinkDetector urlDetector = new URLHyperlinkDetector();
	@Override
	public IHyperlinkDetector[] getHyperlinkDetectors(ISourceViewer sourceViewer) {
		return new IHyperlinkDetector[] {urlDetector};
	}
	@Override
	public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
		if (hover == null)
			hover = new ClonkTextHover();
		return hover;
	}
	@Override
	public IReconciler getReconciler(ISourceViewer sourceViewer) { return null; }
	/**
	 * Create a {@link IHyperlink} at the given offset in the text document using the same mechanism that is being used to create hyperlinks when ctrl-hovering.
	 * This hyperlink will be used for functionality like {@link OpenDeclarationAction} that will not directly operate on specific kinds of {@link Declaration}s and is thus dependent on the {@link ClonkTextEditor} class returning adequate hyperlinks.
	 * @param offset The offset
	 * @return
	 */
	public IHyperlink hyperlinkAtOffset(int offset) {
		final ISourceViewer sourceViewer = editors.get(0).getProtectedSourceViewer();
		final IHyperlinkDetector[] detectors = getHyperlinkDetectors(sourceViewer);
		// emulate
		getHyperlinkPresenter(sourceViewer).hideHyperlinks();
		final IRegion r = new Region(offset, 0);
		for (final IHyperlinkDetector d : detectors) {
			final IHyperlink[] hyperlinks = d.detectHyperlinks(sourceViewer, r, false);
			if (hyperlinks != null && hyperlinks.length > 0)
				return hyperlinks[0];
		}
		return null;
	}
	@Override
	public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) { return installAssistant(sourceViewer); }
}