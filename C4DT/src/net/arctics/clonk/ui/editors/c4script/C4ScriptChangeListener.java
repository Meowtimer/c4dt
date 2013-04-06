package net.arctics.clonk.ui.editors.c4script;

import static net.arctics.clonk.util.Utilities.as;

import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import net.arctics.clonk.Core;
import net.arctics.clonk.Core.IDocumentAction;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.DeclMask;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.IASTPositionProvider;
import net.arctics.clonk.parser.IMarkerListener;
import net.arctics.clonk.parser.Markers;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.Problem;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.FunctionFragmentParser;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.ProblemReportingContext;
import net.arctics.clonk.parser.c4script.ProblemReportingStrategy;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.ProblemReportingStrategy.Capabilities;
import net.arctics.clonk.parser.c4script.ast.CallDeclaration;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.resource.c4group.C4GroupItem;
import net.arctics.clonk.ui.editors.TextChangeListenerBase;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Display;

/**
 * C4Script-specific specialization of {@link TextChangeListenerBase} that tries to only trigger a full reparse of the script when necessary (i.e. not when editing inside of a function)
 * @author madeen
 *
 */
public final class C4ScriptChangeListener extends TextChangeListenerBase<C4ScriptEditor, Script> {

	private static final Map<IDocument, TextChangeListenerBase<C4ScriptEditor, Script>> listeners =
		new HashMap<>();

	private final Timer reparseTimer = new Timer("ReparseTimer"); //$NON-NLS-1$
	private TimerTask reparseTask, functionReparseTask;
	private List<ProblemReportingStrategy> problemReportingStrategies;
	private ProblemReportingStrategy typingStrategy;

	@Override
	protected void initialize() {
		super.initialize();
		try {
			problemReportingStrategies = structure.index().nature().instantiateProblemReportingStrategies(0);
			for (final ProblemReportingStrategy strategy : problemReportingStrategies)
				if ((strategy.capabilities() & Capabilities.TYPING) != 0) {
					typingStrategy = strategy;
					break;
				}
		} catch (final Exception e) {
			problemReportingStrategies = Arrays.asList();
		}
	}

	public List<ProblemReportingStrategy> problemReportingStrategies() { return problemReportingStrategies; }
	public ProblemReportingStrategy typingStrategy() { return typingStrategy; }

	public static C4ScriptChangeListener addTo(IDocument document, Script script, C4ScriptEditor client)  {
		try {
			return addTo(listeners, C4ScriptChangeListener.class, document, script, client);
		} catch (final Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public void documentAboutToBeChanged(DocumentEvent event) {}

	@Override
	public void documentChanged(DocumentEvent event) {
		super.documentChanged(event);
		final Function f = structure.funcAt(event.getOffset());
		if (f != null && !f.isOldStyle())
			// editing inside new-style function: adjust locations of declarations without complete reparse
			// only recheck the function and display problems after delay
			scheduleReparsingOfFunction(f);
		else
			// only schedule reparsing when editing outside of existing function
			scheduleReparsing(false);
	}

	@Override
	protected void adjustDec(Declaration declaration, int offset, int add) {
		super.adjustDec(declaration, offset, add);
		if (declaration instanceof Function)
			incrementLocationOffsetsExceedingThreshold(((Function)declaration).bodyLocation(), offset, add);
		for (final Declaration v : declaration.subDeclarations(declaration.index(), DeclMask.ALL))
			adjustDec(v, offset, add);
	}
	
	private static C4ScriptParser parserForDocument(Object document, final Script script) {
		C4ScriptParser parser = null;
		if (document instanceof IDocument)
			parser = new C4ScriptParser(((IDocument)document).get(), script, script.scriptFile());
		else if (document instanceof IFile)
			parser = Core.instance().performActionsOnFileDocument((IFile) document, new IDocumentAction<C4ScriptParser>() {
				@Override
				public C4ScriptParser run(IDocument document) {
					return new C4ScriptParser(document.get(), script, script.scriptFile());
				}
			}, false);
		if (parser == null)
			throw new InvalidParameterException("document");
		return parser;
	}
	
	C4ScriptParser reparseWithDocumentContents(
		boolean onlyDeclarations, Object document,
		final Script script,
		Runnable uiRefreshRunnable
	) throws ParsingException {
		final Markers markers = new Markers();
		final C4ScriptParser parser = parserForDocument(document, script);
		parser.setMarkers(markers);
		parser.clear(!onlyDeclarations, !onlyDeclarations);
		parser.parseDeclarations();
		parser.script().generateCaches();
		parser.validate();
		if (!onlyDeclarations) {
			if (this.typingStrategy() != null) {
				final ProblemReportingContext localTyping = this.typingStrategy().localTypingContext(parser.script(), parser.fragmentOffset(), null);
				localTyping.setMarkers(markers);
				localTyping.reportProblems();
			}
			markers.deploy();
		}
		// make sure it's executed on the ui thread
		if (uiRefreshRunnable != null)
			Display.getDefault().asyncExec(uiRefreshRunnable);
		return parser;
	}

	public void scheduleReparsing(final boolean onlyDeclarations) {
		reparseTask = cancelTimerTask(reparseTask);
		if (structure == null)
			return;
		reparseTimer.schedule(reparseTask = new TimerTask() {
			@Override
			public void run() {
				if (errorsWhileTypingDisabled())
					return;
				try {
					try {
						reparseWithDocumentContents(onlyDeclarations, document, structure, new Runnable() {
							@Override
							public void run() {
								for (final C4ScriptEditor ed : editors) {
									ed.refreshOutline();
									ed.handleCursorPositionChanged();
								}
							}
						});
					} finally {
						cancel();
					}
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		}, 1000);
	}

	public static void removeMarkers(Function func, Script script) {
		if (script != null && script.resource() != null)
			try {
				// delete regular markers that are in the region of interest
				final IMarker[] markers = script.resource().findMarkers(Core.MARKER_C4SCRIPT_ERROR, false, 3);
				final SourceLocation body = func != null ? func.bodyLocation() : null;
				for (final IMarker m : markers) {
					// delete marks inside the body region
					final int markerStart = m.getAttribute(IMarker.CHAR_START, 0);
					final int markerEnd   = m.getAttribute(IMarker.CHAR_END, 0);
					if (body == null || (markerStart >= body.start() && markerEnd < body.end())) {
						m.delete();
						continue;
					}
				}
			} catch (final CoreException e) {
				e.printStackTrace();
			}
	}

	@SuppressWarnings("serial")
	static class MarkerConfines extends HashSet<ASTNode> implements IMarkerListener {
		public MarkerConfines(ASTNode... confines) {
			this.addAll(Arrays.asList(confines));
		}
		@Override
		public Decision markerEncountered(
			Markers markers, IASTPositionProvider positionProvider,
			Problem code, ASTNode node,
			int markerStart, int markerEnd, int flags,
			int severity, Object... args
		) {
			if (node == null)
				return Decision.DropCharges;
			for (final ASTNode confine : this)
				if (node.containedIn(confine))
					return Decision.PassThrough;
			return Decision.DropCharges;
		}
	}

	private void scheduleReparsingOfFunction(final Function fn) {
		if (errorsWhileTypingDisabled())
			return;
		functionReparseTask = cancelTimerTask(functionReparseTask);
		reparseTimer.schedule(functionReparseTask = new TimerTask() {
			@Override
			public void run() {
				try {
					if (structure.source() instanceof IResource && C4GroupItem.groupItemBackingResource((IResource) structure.source()) == null) {
						removeMarkers(fn, structure);
						final Function f = (Function) fn.latestVersion();
						final Markers markers = reparseFunction(f, ReparseFunctionMode.FULL);
						for (final Variable localVar : f.locals()) {
							final SourceLocation l = localVar;
							l.setStart(f.bodyLocation().getOffset()+l.getOffset());
							l.setEnd(f.bodyLocation().getOffset()+l.end());
						}
						markers.deploy();
					}
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		}, 1000);
	}

	private void addCalls(Set<Function> to, Function from) {
		for (final Collection<CallDeclaration> cdc : from.script().callMap().values())
			for (final CallDeclaration cd : cdc)
				if (cd.containedIn(from)) {
					final Function called = as(cd.declaration(), Function.class);
					if (called != null)
						to.add(called);
				}
	}
	
	public enum ReparseFunctionMode {
		/** Revisit called functions so that their parameter types are
		 *  adjusted according to arguments passed here */
		REVISIT_CALLED_FUNCTIONS;
		
		public static final EnumSet<C4ScriptChangeListener.ReparseFunctionMode> FULL = EnumSet.allOf(C4ScriptChangeListener.ReparseFunctionMode.class);
		public static final EnumSet<C4ScriptChangeListener.ReparseFunctionMode> LIGHT = EnumSet.noneOf(C4ScriptChangeListener.ReparseFunctionMode.class);
	}

	public Markers reparseFunction(final Function function, EnumSet<C4ScriptChangeListener.ReparseFunctionMode> mode) {
		if (errorsWhileTypingDisabled())
			return new Markers();
		final Markers markers = new Markers(new MarkerConfines(function));
		markers.applyProjectSettings(structure.index());
		final Set<Function> revisitFunctions = mode.contains(ReparseFunctionMode.REVISIT_CALLED_FUNCTIONS) ? new HashSet<Function>() : null;
		if (revisitFunctions != null)
			addCalls(revisitFunctions, function); // add old calls so when the user removes a call the function called will still be revisited
		final C4ScriptParser parser = FunctionFragmentParser.update(document, structure, function, markers);
		structure.generateCaches();
		for (final ProblemReportingStrategy strategy : problemReportingStrategies) {
			final ProblemReportingContext mainTyping = strategy.localTypingContext(parser.script(), parser.fragmentOffset(), null);
			if (markers != null)
				mainTyping.setMarkers(markers);
			mainTyping.visitFunction(function);
			if (revisitFunctions != null) {
				addCalls(revisitFunctions, function); // add new calls
				// potentially revisit functions to adjust typing of parameters to the concrete parameter types supplied here
				for (final Function called : revisitFunctions)
					if (called != null && mainTyping.triggersRevisit(function, called))
						if (markers != null && markers.listener() instanceof C4ScriptChangeListener.MarkerConfines)
							if (((C4ScriptChangeListener.MarkerConfines)markers.listener()).add(called)) {
								removeMarkers(called, called.script());
								for (final Variable p : called.parameters())
									p.forceType(PrimitiveType.UNKNOWN, false);
								final ProblemReportingContext calledTyping = strategy.localTypingContext(called.parentOfType(Script.class), 0, mainTyping);
								calledTyping.setMarkers(markers);
								calledTyping.visitFunction(called);
							}
			}
		}
		return markers;
	}

	private boolean errorsWhileTypingDisabled() {
		return !ClonkPreferences.toggle(ClonkPreferences.SHOW_ERRORS_WHILE_TYPING, true);
	}

	@Override
	public void cancelReparsingTimer() {
		reparseTask = cancelTimerTask(reparseTask);
		functionReparseTask = cancelTimerTask(functionReparseTask);
		super.cancelReparsingTimer();
	}

	@Override
	public void cleanupAfterRemoval() {
		if (reparseTimer != null)
			reparseTimer.cancel();
		try {
			if (structure.source() instanceof IFile) {
				final IFile file = (IFile)structure.source();
				// might have been closed due to removal of the file - don't cause exception by trying to reparse that file now
				if (file.exists())
					reparseWithDocumentContents(false, file, structure, null);
			}
		} catch (final ParsingException e) {
			e.printStackTrace();
		}
		super.cleanupAfterRemoval();
	}

	public static C4ScriptChangeListener listenerFor(IDocument document) {
		return (C4ScriptChangeListener) listeners.get(document);
	}
}