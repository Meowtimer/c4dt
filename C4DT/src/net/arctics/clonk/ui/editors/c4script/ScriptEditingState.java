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
import net.arctics.clonk.ast.ExpressionLocator;
import net.arctics.clonk.ast.IASTPositionProvider;
import net.arctics.clonk.ast.IASTVisitor;
import net.arctics.clonk.ast.SourceLocation;
import net.arctics.clonk.ast.TraversalContinuation;
import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.c4group.C4GroupItem;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.Function.FunctionScope;
import net.arctics.clonk.c4script.FunctionFragmentParser;
import net.arctics.clonk.c4script.ProblemReporter;
import net.arctics.clonk.c4script.ProblemReportingStrategy;
import net.arctics.clonk.c4script.ProblemReportingStrategy.Capabilities;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.ScriptParser;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.c4script.ast.AccessDeclaration;
import net.arctics.clonk.c4script.ast.Block;
import net.arctics.clonk.c4script.ast.CallDeclaration;
import net.arctics.clonk.c4script.ast.Comment;
import net.arctics.clonk.c4script.ast.EntityLocator;
import net.arctics.clonk.c4script.ast.EntityLocator.RegionDescription;
import net.arctics.clonk.c4script.ast.IFunctionCall;
import net.arctics.clonk.c4script.ast.KeywordStatement;
import net.arctics.clonk.c4script.ast.Literal;
import net.arctics.clonk.c4script.ast.PropListExpression;
import net.arctics.clonk.c4script.ast.StringLiteral;
import net.arctics.clonk.c4script.typing.FunctionType;
import net.arctics.clonk.c4script.typing.IType;
import net.arctics.clonk.c4script.typing.TypeUtil;
import net.arctics.clonk.index.IIndexEntity;
import net.arctics.clonk.parser.CStyleScanner;
import net.arctics.clonk.parser.IMarkerListener;
import net.arctics.clonk.parser.Markers;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.ui.editors.CStylePartitionScanner;
import net.arctics.clonk.ui.editors.DeclarationProposal;
import net.arctics.clonk.ui.editors.EntityHyperlink;
import net.arctics.clonk.ui.editors.ScriptCommentScanner;
import net.arctics.clonk.ui.editors.StructureEditingState;
import net.arctics.clonk.ui.editors.StructureTextScanner.ScannerPerEngine;
import net.arctics.clonk.util.Pair;
import net.arctics.clonk.util.StringUtil;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.DefaultTextDoubleClickStrategy;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.quickassist.IQuickAssistAssistant;
import org.eclipse.jface.text.quickassist.QuickAssistAssistant;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPart;

/**
 * C4Script-specific specialization of {@link StructureEditingState} that tries to only trigger a full reparse of the script when necessary (i.e. not when editing inside of a function)
 * @author madeen
 *
 */
public final class ScriptEditingState extends StructureEditingState<C4ScriptEditor, Script> {

	public final class Assistant extends ContentAssistant {
		private ScriptCompletionProcessor processor;
		public Assistant() {
			processor = new ScriptCompletionProcessor(ScriptEditingState.this);
			for (final String s : CStylePartitionScanner.PARTITIONS)
				setContentAssistProcessor(processor, s);
			setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
			setRepeatedInvocationMode(true);
			setStatusLineVisible(true);
			setStatusMessage(Messages.C4ScriptSourceViewerConfiguration_StandardProposals);
			enablePrefixCompletion(false);
			enableAutoInsert(true);
			enableAutoActivation(true);
			setAutoActivationDelay(0);
			enableColoredLabels(true);
			setInformationControlCreator(new IInformationControlCreator() {
				@Override
				public IInformationControl createInformationControl(Shell parent) {
					final DefaultInformationControl def = new DefaultInformationControl(parent,Messages.C4ScriptSourceViewerConfiguration_PressTabOrClick);
					return def;
				}
			});
			setSorter(processor);
			addCompletionListener(processor);
		}
		// make these public
		@Override
		public void hide() { super.hide(); }
		@Override
		public boolean isProposalPopupActive() { return super.isProposalPopupActive(); }
		public ScriptCompletionProcessor processor() { return processor; }
		public void showParameters(ITextOperationTarget target) {
			if (target.canDoOperation(ISourceViewer.CONTENTASSIST_CONTEXT_INFORMATION))
				target.doOperation(ISourceViewer.CONTENTASSIST_CONTEXT_INFORMATION);
		}
	}

	class ScriptHyperlinkDetector implements IHyperlinkDetector {
		@Override
		public IHyperlink[] detectHyperlinks(ITextViewer viewer, IRegion region, boolean canShowMultipleHyperlinks) {
			try {
				final EntityLocator locator = new EntityLocator(structure(), viewer.getDocument(),region);
				if (locator.entity() != null)
					return new IHyperlink[] {
						new EntityHyperlink(locator.expressionRegion(), locator.entity())
					};
				else if (locator.potentialEntities() != null)
					return new IHyperlink[] {
						new EntityHyperlink(locator.expressionRegion(), locator.potentialEntities())
					};
				return null;
			} catch (final Exception e) {
				e.printStackTrace();
				return null;
			}
		}
	}

	public class ScriptTextHover extends ClonkTextHover {
		private EntityLocator entityLocator;
		@Override
		public String getHoverInfo(ITextViewer viewer, IRegion region) {
			final IFile scriptFile = structure().file();
			final StringBuilder messageBuilder = new StringBuilder();
			appendEntityInfo(viewer, region, messageBuilder);
			appendMarkerInfo(region, scriptFile, messageBuilder);
			return messageBuilder.toString();
		}
		private void appendEntityInfo(ITextViewer viewer, IRegion region, final StringBuilder messageBuilder) {
			if (entityLocator != null && entityLocator.entity() != null) {
				final ASTNode pred = entityLocator.expressionAtRegion() != null ? entityLocator.expressionAtRegion().predecessorInSequence() : null;
				final Script context = pred == null ? structure() : as(TypeUtil.inferredType(pred), Script.class);
				messageBuilder.append(entityLocator.entity().infoText(context));
			}
			else {
				final String superInfo = super.getHoverInfo(viewer, region);
				if (superInfo != null)
					messageBuilder.append(superInfo);
			}
		}
		private void appendMarkerInfo(IRegion region, final IFile scriptFile, final StringBuilder messageBuilder) {
			try {
				final IMarker[] markers = scriptFile.findMarkers(Core.MARKER_C4SCRIPT_ERROR, true, IResource.DEPTH_ONE);
				boolean foundSomeMarkers = false;
				for (final IMarker m : markers) {
					int charStart;
					final IRegion markerRegion = new Region(
						charStart = m.getAttribute(IMarker.CHAR_START, -1),
						m.getAttribute(IMarker.CHAR_END, -1)-charStart
					);
					if (Utilities.regionContainsOtherRegion(markerRegion, region)) {
						if (!foundSomeMarkers) {
							if (messageBuilder.length() > 0)
								messageBuilder.append("<br/><br/><b>"+Messages.C4ScriptTextHover_Markers1+"</b><br/>"); //$NON-NLS-1$
							foundSomeMarkers = true;
						}
						String msg = m.getAttribute(IMarker.MESSAGE).toString();
						msg = StringUtil.htmlerize(msg);
						messageBuilder.append(msg);
						messageBuilder.append("<br/>"); //$NON-NLS-1$
					}
				}
			} catch (final Exception e) {
				// whatever
			}
		}
		@Override
		public IRegion getHoverRegion(ITextViewer viewer, int offset) {
			super.getHoverRegion(viewer, offset);
			final IRegion region = new Region(offset, 0);
			try {
				entityLocator = new EntityLocator(structure(), viewer.getDocument(), region);
			} catch (final Exception e) {
				e.printStackTrace();
				return null;
			}
			return region;
		}
	}

	class DoubleClickStrategy extends DefaultTextDoubleClickStrategy {
		@Override
		protected IRegion findExtendedDoubleClickSelection(IDocument document, int pos) {
			final Function func = structure().funcAt(pos);
			if (func != null) {
				final ExpressionLocator<Void> locator = new ExpressionLocator<Void>(pos-func.bodyLocation().start());
				func.traverse(locator, null);
				ASTNode expr = locator.expressionAtRegion();
				if (expr == null)
					return new Region(func.wholeBody().getOffset(), func.wholeBody().getLength());
				else for (; expr != null; expr = expr.parent())
					if (expr instanceof KeywordStatement || expr instanceof Comment || expr instanceof StringLiteral) {
						final IRegion word = findWord(document, pos);
						try {
							if (word != null && !document.get(word.getOffset(), word.getLength()).equals("\t"))
								return word;
							else
								continue;
						} catch (final BadLocationException e) {
							continue;
						}
					} else if (expr instanceof Literal)
						return new Region(func.bodyLocation().getOffset()+expr.start(), expr.getLength());
					else if (expr instanceof AccessDeclaration) {
						final AccessDeclaration accessDec = (AccessDeclaration) expr;
						return new Region(func.bodyLocation().getOffset()+accessDec.identifierStart(), accessDec.identifierLength());
					} else if (expr instanceof PropListExpression || expr instanceof Block)
						return new Region(expr.start()+func.bodyLocation().getOffset(), expr.getLength());
			}
			return null;
		}
	}

	@Override
	protected ContentAssistant createAssistant() { return new Assistant(); }
	@Override
	public Assistant assistant() { return (Assistant) assistant; }

	private final Timer timer = new Timer("ReparseTimer"); //$NON-NLS-1$
	private TimerTask reparseTask, reportFunctionProblemsTask;
	private final Object
		structureModificationLock = new Object(),
		obtainStructureLock = new Object();
	private final ScriptAutoEditStrategy autoEditStrategy = new ScriptAutoEditStrategy(this);

	public ScriptAutoEditStrategy autoEditStrategy() { return autoEditStrategy; }

	@Override
	public void documentAboutToBeChanged(DocumentEvent event) {}

	@Override
	public void documentChanged(DocumentEvent event) {
		super.documentChanged(event);
		final Function f = structure().funcAt(event.getOffset());
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

	private void reparse() throws ProblemException {
		cancelReparsingTimer();
		reparseWithDocumentContents(refreshEditorsRunnable());
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

	void reparseWithDocumentContents(Runnable uiRefreshRunnable) throws ProblemException {
		structure().requireLoaded();

		final Markers markers = new StructureMarkers(false);
		final String source = document.get();
		synchronized (structureModificationLock) {
			new ScriptParser(source, structure(), null) {{
				setMarkers(markers);
				script().clearDeclarations();
				parseDeclarations();
				script().deriveInformation();
				validate();
			}};
			structure().traverse(Comment.TODO_EXTRACTOR, markers);
			reportProblems(markers);
		}
		markers.deploy();
		if (uiRefreshRunnable != null)
			Display.getDefault().asyncExec(uiRefreshRunnable);
	}

	private void reportProblems() {
		final Markers markers = new StructureMarkers(true);
		synchronized (structureModificationLock) {
			reportProblems(markers);
		}
		markers.deploy();
	}

	private void reportProblems(final Markers markers) {
		for (final ProblemReportingStrategy s : problemReportingStrategies()) {
			s.initialize(markers, new NullProgressMonitor(), new Script[] {structure()});
			s.run();
		}
	}

	public List<ProblemReportingStrategy> problemReportingStrategies() {
		return ClonkProjectNature.get(structure().file()).problemReportingStrategies();
	}

	public void scheduleReparsing(final boolean onlyDeclarations) {
		reparseTask = cancelTimerTask(reparseTask);
		if (timer == null || structure() == null)
			return;
		timer.schedule(reparseTask = new TimerTask() {
			@Override
			public void run() {
				if (errorsWhileTypingDisabled())
					return;
				try {
					try {
						reparseWithDocumentContents(new Runnable() {
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
			applyProjectSettings(structure().index());
			if (functionBodies)
				captureMarkersInFunctionBodies();
			else
				captureExistingMarkers(structure().file());
		}
		private void captureMarkersInFunctionBodies() {
			try {
				captured = new ArrayList<>(Arrays.asList(structure().file().findMarkers(Core.MARKER_C4SCRIPT_ERROR, true, IResource.DEPTH_ONE)));
				for (final Iterator<IMarker> it = captured.iterator(); it.hasNext();) {
					final IMarker c = it.next();
					if (structure().funcAt(c.getAttribute(IMarker.CHAR_START, -1)) == null)
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
		if (timer == null || errorsWhileTypingDisabled())
			return;
		reportFunctionProblemsTask = cancelTimerTask(reportFunctionProblemsTask);
		timer.schedule(reportFunctionProblemsTask = new TimerTask() {
			@Override
			public void run() {
				try {
					if (structure().source() instanceof IResource && C4GroupItem.groupItemBackingResource((IResource) structure().source()) == null) {
						removeMarkers(fn, structure());
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

	public Markers reportProblems(final Function function) {
		// ignore this request when errors while typing disabled
		if (errorsWhileTypingDisabled())
			return new Markers();

		final Markers markers = new Markers(new MarkerConfines(function));
		markers.applyProjectSettings(structure().index());

		// visit the function
		structure().deriveInformation();
		for (final ProblemReportingStrategy strategy : problemReportingStrategies()) {
			strategy.initialize(markers, new NullProgressMonitor(), Arrays.asList(Pair.pair(structure(), function)));
			strategy.run();
		}

		// visit directly called functions
		final Function.Typing typing = structure().typings().get(function);
		if (typing != null) {
			final Set<Pair<Script, Function>> callees = new HashSet<Pair<Script, Function>>();
			function.traverse(new IASTVisitor<Void>() {
				@Override
				public TraversalContinuation visitNode(ASTNode node, Void context) {
					if (node instanceof CallDeclaration) {
						final CallDeclaration cd = (CallDeclaration) node;
						final Function f = as(cd.declaration(), Function.class);
						if (f != null && f.body() != null) {
							final IType pred = cd.predecessorInSequence() != null ? typing.nodeTypes[cd.predecessorInSequence().localIdentifier()] : structure();
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
				for (final ProblemReportingStrategy strategy : problemReportingStrategies()) {
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
		if (timer != null)
			timer.cancel();
		try {
			if (structure().source() instanceof IFile) {
				final IFile file = (IFile)structure().source();
				// might have been closed due to removal of the file - don't cause exception by trying to reparse that file now
				if (file.exists())
					reparseWithDocumentContents(null);
			}
		} catch (final ProblemException e) {
			e.printStackTrace();
		}
		super.cleanupAfterRemoval();
	}

	/**
	 *  Created if there is no suitable script to get from somewhere else
	 *  can be considered a hack to make viewing (svn) revisions of a file work
	 */
	private WeakReference<Script> cachedScript = new WeakReference<Script>(null);

	@Override
	public Script structure() {
		synchronized (obtainStructureLock) {
			Script result = cachedScript.get();
			Cases: if (result == null) {
				if (editors.isEmpty()) {
					result = structure;
					break Cases;
				}

				final IEditorInput input = editors.get(0).getEditorInput();
				if (input instanceof ScriptWithStorageEditorInput) {
					result = ((ScriptWithStorageEditorInput)input).script();
					break Cases;
				}

				final IFile f = Utilities.fileFromEditorInput(input);
				if (f != null) {
					final Script script = Script.get(f, true);
					if (script != null) {
						result = script;
						break Cases;
					}
				}

				result = new ScratchScript(editors.get(0));
				cachedScript = new WeakReference<Script>(result);
				try {
					reparse();
					result.traverse(new IASTVisitor<Script>() {
						@Override
						public TraversalContinuation visitNode(ASTNode node, Script parser) {
							final AccessDeclaration ad = as(node, AccessDeclaration.class);
							if (ad != null && ad.declaration() != null)
								ad.setDeclaration(ad.declaration().latestVersion());
							return TraversalContinuation.Continue;
						}
					}, result);
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}

			cachedScript = new WeakReference<Script>(result);
			return this.structure = result;
		}
	}

	@Override
	public void invalidate() {
		cachedScript = new WeakReference<Script>(null);
		structure();
		super.invalidate();
	}

	public FunctionFragmentParser updateFunctionFragment(
		Function function,
		IASTVisitor<ProblemReporter> observer,
		boolean typingContextVisitInAnyCase
	) {
		synchronized (structureModificationLock) {
			final FunctionFragmentParser fparser = new FunctionFragmentParser(document, structure(), function, null);
			final boolean change = fparser.update();
			if (change || (observer != null && typingContextVisitInAnyCase))
				for (final ProblemReportingStrategy s : problemReportingStrategies())
					if ((s.capabilities() & Capabilities.TYPING) != 0) {
						s.initialize(null, new NullProgressMonitor(), Arrays.asList(Pair.pair(structure(), function)));
						s.setObserver(observer);
						s.run();
					}
			return fparser;
		}
	}

	@Override
	public void partActivated(IWorkbenchPart part) {
		if (editors.contains(part) && structure() != null)
			new Job("Refreshing problem markers") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					structure().requireLoaded();
					reportProblems();
					Display.getDefault().asyncExec(refreshEditorsRunnable());
					return Status.OK_STATUS;
				}
			}.schedule();
		super.partBroughtToTop(part);
	}

	public Function functionAt(int offset) {
		final Script script = structure();
		if (script != null) {
			final Function f = script.funcAt(offset);
			return f;
		}
		return null;
	}

	public static class Call {
		public IFunctionCall callFunc;
		public int parmIndex;
		public int parmsStart, parmsEnd;
		public EntityLocator locator;
		public Call(Function func, IFunctionCall callFunc2, ASTNode parm, EntityLocator locator) {
			this.callFunc = callFunc2;
			this.parmIndex = parm != null ? callFunc2.indexOfParm(parm) : 0;
			this.parmsStart = func.bodyLocation().start()+callFunc2.parmsStart();
			this.parmsEnd = func.bodyLocation().start()+callFunc2.parmsEnd();
			this.locator = locator;
		}
		public ASTNode callPredecessor() {
			return callFunc instanceof ASTNode ? ((ASTNode)callFunc).predecessorInSequence() : null;
		}
	}

	public Call innermostFunctionCallParmAtOffset(int offset) throws BadLocationException, ProblemException {
		final Function f = functionAt(offset);
		if (f == null)
			return null;
		updateFunctionFragment(f, null, false);
		final EntityLocator locator = new EntityLocator(structure(), document, new Region(offset, 0));
		ASTNode expr;

		// cursor somewhere between parm expressions... locate CallFunc and search
		final int bodyStart = f.bodyLocation().start();
		for (
			expr = locator.expressionAtRegion();
			expr != null;
			expr = expr.parent()
		)
			if (expr instanceof IFunctionCall && offset-bodyStart >= ((IFunctionCall)expr).parmsStart())
				 break;
		if (expr != null) {
			final IFunctionCall callFunc = (IFunctionCall) expr;
			ASTNode prev = null;
			for (final ASTNode parm : callFunc.params()) {
				if (bodyStart+parm.end() > offset) {
					if (prev == null)
						break;
					final String docText = document.get(bodyStart+prev.end(), parm.start()-prev.end());
					final CStyleScanner scanner = new CStyleScanner(docText);
					scanner.eatWhitespace();
					final boolean comma = scanner.read() == ',' && offset+1 > bodyStart+prev.end() + scanner.tell();
					return new Call(f, callFunc, comma ? parm : prev, locator);
				}
				prev = parm;
			}
			return new Call(f, callFunc, prev, locator);
		}
		return null;
	}

	@Override
	public void completionProposalApplied(DeclarationProposal proposal) {
		autoEditStrategy().completionProposalApplied(proposal);
		try {
			if (proposal.requiresDocumentReparse())
				reparse();
		} catch (final ProblemException e) {
			e.printStackTrace();
		}
		Display.getCurrent().asyncExec(new Runnable() {
			@Override
			public void run() {
				for (final C4ScriptEditor ed : editors)
					ed.showContentAssistance();
			}
		});
		super.completionProposalApplied(proposal);
	}

	protected Function functionFromEntity(IIndexEntity entity) {
		Function function = null;
		if (entity instanceof Function)
			function = (Function)entity;
		else if (entity instanceof Variable) {
			final IType type = ((Variable)entity).type();
			if (type instanceof FunctionType)
				function = ((FunctionType)type).prototype();
		}
		return function;
	}

	public IIndexEntity mergeFunctions(int offset, final ScriptEditingState.Call funcCallInfo, final RegionDescription d) {
		IIndexEntity entity = null;
		if (funcCallInfo.locator.initializeRegionDescription(d, structure(), new Region(offset, 1))) {
			funcCallInfo.locator.initializeProposedDeclarations(structure(), d, null, (ASTNode)funcCallInfo.callFunc);
			Function commono = null;
			final Set<? extends IIndexEntity> potentials = funcCallInfo.locator.potentialEntities();
			if (potentials != null)
				if (potentials.size() == 1)
					entity = potentials.iterator().next();
				else for (final IIndexEntity e : potentials) {
					if (commono == null)
						commono = new Function(Messages.C4ScriptCompletionProcessor_MultipleCandidates, FunctionScope.PRIVATE);
					entity = commono;
					final Function f = functionFromEntity(e);
					if (f != null)
						for (int i = 0; i < f.numParameters(); i++) {
							final Variable fpar = f.parameter(i);
							final Variable cpar = commono.numParameters() > i
								? commono.parameter(i)
									: commono.addParameter(new Variable(fpar.name(), fpar.type()));
								cpar.forceType(structure().typing().unify(cpar.type(), fpar.type()));
								if (!Arrays.asList(cpar.name().split("/")).contains(fpar.name())) //$NON-NLS-1$
									cpar.setName(cpar.name()+"/"+fpar.name()); //$NON-NLS-1$
						}
				}
		}
		return entity;
	}

	private static ScannerPerEngine<ScriptCodeScanner> SCANNERS = new ScannerPerEngine<ScriptCodeScanner>(ScriptCodeScanner.class);
	private final ITextDoubleClickStrategy doubleClickStrategy = new DoubleClickStrategy();
	public ScriptEditingState(IPreferenceStore store) { super(store); }
	@Override
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) { return CStylePartitionScanner.PARTITIONS; }
	@Override
	public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer, String contentType) { return doubleClickStrategy; }
	@Override
	public IQuickAssistAssistant getQuickAssistAssistant(ISourceViewer sourceViewer) {
		final IQuickAssistAssistant assistant = new QuickAssistAssistant();
		assistant.setQuickAssistProcessor(new ScriptQuickAssistProcessor());
		return assistant;
	}
	@Override
	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		final PresentationReconciler reconciler = new PresentationReconciler();
		final ScriptCommentScanner commentScanner = new ScriptCommentScanner(getColorManager(), "COMMENT"); //$NON-NLS-1$
		final ScriptCodeScanner scanner = SCANNERS.get(structure().engine());

		DefaultDamagerRepairer dr = new DefaultDamagerRepairer(scanner);
		reconciler.setDamager(dr, CStylePartitionScanner.CODEBODY);
		reconciler.setRepairer(dr, CStylePartitionScanner.CODEBODY);

		dr = new DefaultDamagerRepairer(scanner);
		reconciler.setDamager(dr, CStylePartitionScanner.STRING);
		reconciler.setRepairer(dr, CStylePartitionScanner.STRING);

		dr = new DefaultDamagerRepairer(scanner);
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		dr = new DefaultDamagerRepairer(new ScriptCommentScanner(getColorManager(), "JAVADOCCOMMENT"));
		reconciler.setDamager(dr, CStylePartitionScanner.JAVADOC_COMMENT);
		reconciler.setRepairer(dr, CStylePartitionScanner.JAVADOC_COMMENT);

		dr = new DefaultDamagerRepairer(commentScanner);
		reconciler.setDamager(dr, CStylePartitionScanner.COMMENT);
		reconciler.setRepairer(dr, CStylePartitionScanner.COMMENT);

		dr = new DefaultDamagerRepairer(commentScanner);
		reconciler.setDamager(dr, CStylePartitionScanner.MULTI_LINE_COMMENT);
		reconciler.setRepairer(dr, CStylePartitionScanner.MULTI_LINE_COMMENT);

		return reconciler;
	}
	@Override
	public IHyperlinkDetector[] getHyperlinkDetectors(ISourceViewer sourceViewer) {
		return new IHyperlinkDetector[] {
			new ScriptHyperlinkDetector(),
			urlDetector
		};
	}
	@Override
	public IAutoEditStrategy[] getAutoEditStrategies(ISourceViewer sourceViewer, String contentType) {
		return new IAutoEditStrategy[] {autoEditStrategy};
	}
	@Override
	public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
	    if (hover == null)
	    	hover = new ScriptTextHover();
	    return hover;
	}
}