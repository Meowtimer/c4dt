package net.arctics.clonk.ui.editors.c4script;

import static net.arctics.clonk.util.Utilities.as;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import net.arctics.clonk.Core;
import net.arctics.clonk.Problem;
import net.arctics.clonk.ProblemException;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.DeclMask;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ast.IASTPositionProvider;
import net.arctics.clonk.ast.IASTVisitor;
import net.arctics.clonk.ast.SourceLocation;
import net.arctics.clonk.ast.TraversalContinuation;
import net.arctics.clonk.c4group.C4GroupItem;
import net.arctics.clonk.c4script.ScriptParser;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.FunctionFragmentParser;
import net.arctics.clonk.c4script.ProblemReporter;
import net.arctics.clonk.c4script.ProblemReportingStrategy;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.ProblemReportingStrategy.Capabilities;
import net.arctics.clonk.c4script.ast.AccessDeclaration;
import net.arctics.clonk.c4script.ast.CallDeclaration;
import net.arctics.clonk.c4script.ast.Comment;
import net.arctics.clonk.c4script.typing.IType;
import net.arctics.clonk.parser.IMarkerListener;
import net.arctics.clonk.parser.Markers;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.ui.editors.StructureEditingState;
import net.arctics.clonk.util.Pair;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPart;

/**
 * C4Script-specific specialization of {@link StructureEditingState} that tries to only trigger a full reparse of the script when necessary (i.e. not when editing inside of a function)
 * @author madeen
 *
 */
public final class ScriptEditingState extends StructureEditingState<C4ScriptEditor, Script> {

	private static final List<ScriptEditingState> list = new ArrayList<>();

	private final Timer reparseTimer = new Timer("ReparseTimer"); //$NON-NLS-1$
	private TimerTask reparseTask, reportFunctionProblemsTask;
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

	public static ScriptEditingState addTo(IDocument document, Script script, C4ScriptEditor client)  {
		try {
			return request(list, ScriptEditingState.class, document, script, client);
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
			scheduleProblemReport(f);
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

	private ScriptParser reparse(boolean onlyDeclarations) throws ProblemException {
		cancelReparsingTimer();
		return reparseWithDocumentContents(onlyDeclarations, document, refreshEditorsRunnable());
	}

	private Runnable refreshEditorsRunnable() {
		return new Runnable() {
			@Override
			public void run() {
				for (final C4ScriptEditor ed : editors) {
					ed.refreshOutline();
					try { ed.handleCursorPositionChanged(); } catch (final Exception e) {}
				}
			}
		};
	}

	synchronized ScriptParser reparseWithDocumentContents(
		boolean onlyDeclarations, Object scriptSource,
		Runnable uiRefreshRunnable
	) throws ProblemException {
		final Markers markers = new StructureMarkers(false);
		final ScriptParser parser = new ScriptParser(scriptSource, structure, null) {{
			setMarkers(markers);
			script().clearDeclarations();
			parseDeclarations();
			script().deriveInformation();
			validate();
		}};
		structure.traverse(Comment.TODO_EXTRACTOR, markers);
		if (!onlyDeclarations)
			reportProblems(markers);
		markers.deploy();
		if (uiRefreshRunnable != null)
			Display.getDefault().asyncExec(uiRefreshRunnable);
		return parser;
	}

	private void reportProblems() {
		final Markers markers = new StructureMarkers(true);
		reportProblems(markers);
		markers.deploy();
	}

	private void reportProblems(final Markers markers) {
		final ProblemReportingStrategy typing = this.typingStrategy();
		if (typing != null) {
			typing.initialize(markers, new NullProgressMonitor(), new Script[] {structure});
			typing.run();
		}
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
						reparseWithDocumentContents(onlyDeclarations, document, new Runnable() {
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

	private final class StructureMarkers extends Markers {
		StructureMarkers(boolean functionBodies){
			applyProjectSettings(structure.index());
			if (functionBodies)
				captureMarkersInFunctionBodies();
			else
				captureExistingMarkers(structure.scriptFile());
		}
		private void captureMarkersInFunctionBodies() {
			try {
				captured = new ArrayList<>(Arrays.asList(structure.scriptFile().findMarkers(Core.MARKER_C4SCRIPT_ERROR, true, IResource.DEPTH_ONE)));
				for (final Iterator<IMarker> it = captured.iterator(); it.hasNext();) {
					final IMarker c = it.next();
					if (structure.funcAt(c.getAttribute(IMarker.CHAR_START, -1)) == null)
						it.remove();
				}
			} catch (final CoreException e) {
				e.printStackTrace();
			}
		}
	}

	@SuppressWarnings("serial")
	static class MarkerConfines extends HashSet<ASTNode> implements IMarkerListener {
		public MarkerConfines(ASTNode... confines) { this.addAll(Arrays.asList(confines)); }
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

	private void scheduleProblemReport(final Function fn) {
		if (errorsWhileTypingDisabled())
			return;
		reportFunctionProblemsTask = cancelTimerTask(reportFunctionProblemsTask);
		reparseTimer.schedule(reportFunctionProblemsTask = new TimerTask() {
			@Override
			public void run() {
				try {
					if (structure.source() instanceof IResource && C4GroupItem.groupItemBackingResource((IResource) structure.source()) == null) {
						removeMarkers(fn, structure);
						final Function f = (Function) fn.latestVersion();
						final Markers markers = reportProblems(f);
						markers.deploy();
						Display.getDefault().asyncExec(refreshEditorsRunnable());
					}
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		}, 1000);
	}

	public synchronized Markers reportProblems(final Function function) {
		// ignore this request when errors while typing disabled
		if (errorsWhileTypingDisabled())
			return new Markers();

		final Markers markers = new Markers(new MarkerConfines(function));
		markers.applyProjectSettings(structure.index());

		// main visit - this will also branch out to called functions so their parameter types will be adjusted taking into account
		// concrete parameters passed from here
		structure.deriveInformation();
		for (final ProblemReportingStrategy strategy : problemReportingStrategies) {
			strategy.initialize(markers, new NullProgressMonitor(), Arrays.asList(Pair.pair(structure, function)));
			strategy.run();
		}
		final Function.Typing typing = structure.typings().get(function);
		if (typing != null) {
			final Set<Pair<Script, Function>> callees = new HashSet<Pair<Script, Function>>();
			function.traverse(new IASTVisitor<Void>() {
				@Override
				public TraversalContinuation visitNode(ASTNode node, Void context) {
					if (node instanceof CallDeclaration) {
						final CallDeclaration cd = (CallDeclaration) node;
						final Function f = as(cd.declaration(), Function.class);
						if (f != null && f.body() != null) {
							final IType pred = cd.predecessorInSequence() != null ? typing.nodeTypes[cd.predecessorInSequence().localIdentifier()] : structure;
							if (pred != null)
								for (final IType t : pred)
									if (t instanceof Script)
										callees.add(Pair.pair((Script)t, f));
						}
					}
					return TraversalContinuation.Continue;
				}
			}, null);
			if (callees.size() > 0)
				for (final ProblemReportingStrategy strategy : problemReportingStrategies) {
					strategy.initialize(markers, new NullProgressMonitor(), callees);
					strategy.run();
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
		reportFunctionProblemsTask = cancelTimerTask(reportFunctionProblemsTask);
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
					reparseWithDocumentContents(false, file, null);
			}
		} catch (final ProblemException e) {
			e.printStackTrace();
		}
		super.cleanupAfterRemoval();
	}

	public static ScriptEditingState state(Script script) {
		return stateFromList(list, script);
	}

	/**
	 *  Created if there is no suitable script to get from somewhere else
	 *  can be considered a hack to make viewing (svn) revisions of a file work
	 */
	private WeakReference<Script> cachedScript = new WeakReference<Script>(null);

	private WeakReference<ProblemReporter> cachedDeclarationObtainmentContext;

	@Override
	public Script structure() {
		Script result = cachedScript.get();
		if (result != null) {
			this.structure = result;
			return result;
		}

		if (editors.isEmpty())
			return super.structure();

		final IEditorInput input = editors.get(0).getEditorInput();
		if (input instanceof ScriptWithStorageEditorInput)
			result = ((ScriptWithStorageEditorInput)input).script();

		if (result == null) {
			final IFile f = Utilities.fileFromEditorInput(input);
			if (f != null) {
				final Script script = Script.get(f, true);
				if (script != null)
					result = script;
			}
		}

		boolean needsReparsing = false;
		if (result == null && cachedScript.get() == null) {
			result = new ScratchScript(editors.get(0));
			needsReparsing = true;
		}
		cachedScript = new WeakReference<Script>(result);
		if (needsReparsing)
			try {
				reparse(false);
			} catch (final Exception e) {
				e.printStackTrace();
			}
		if (result != null)
			result.traverse(new IASTVisitor<Script>() {
				@Override
				public TraversalContinuation visitNode(ASTNode node, Script parser) {
					final AccessDeclaration ad = as(node, AccessDeclaration.class);
					if (ad != null && ad.declaration() != null)
						ad.setDeclaration(ad.declaration().latestVersion());
					return TraversalContinuation.Continue;
				}
			}, result);
		this.structure = result;
		return result;
	}

	@Override
	public void invalidate() {
		cachedScript = new WeakReference<Script>(null);
		cachedDeclarationObtainmentContext = new WeakReference<ProblemReporter>(null);
		super.invalidate();
	}

	public ProblemReporter declarationObtainmentContext() {
		if (cachedDeclarationObtainmentContext != null) {
			final ProblemReporter ctx = cachedDeclarationObtainmentContext.get();
			if (ctx != null && ctx.script() == structure())
				return ctx;
		}
		ProblemReporter r = null;
		for (final ProblemReportingStrategy strategy : problemReportingStrategies())
			if ((strategy.capabilities() & Capabilities.TYPING) != 0) {
				cachedDeclarationObtainmentContext = new WeakReference<ProblemReporter>(
					r = strategy.localReporter(structure(), 0)
				);
				break;
			}
		return r;
	}

	public synchronized FunctionFragmentParser updateFunctionFragment(
		Function function,
		IASTVisitor<ProblemReporter> observer,
		boolean typingContextVisitInAnyCase
	) {
		final FunctionFragmentParser fparser = new FunctionFragmentParser(document, structure(), function, null);
		final boolean change = fparser.update();
		if (change || (observer != null && typingContextVisitInAnyCase)) {
			typingStrategy.initialize(null, new NullProgressMonitor(), Arrays.asList(Pair.pair(structure, function)));
			typingStrategy.setObserver(observer);
			typingStrategy.run();
		}
		return fparser;
	}

	@Override
	public void partBroughtToTop(IWorkbenchPart part) {
		if (editors.contains(part))
			reportProblems();
		super.partBroughtToTop(part);
	}

}