package net.arctics.clonk.ui.editors.c4script;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.function.Function.identity;
import static net.arctics.clonk.util.StreamUtil.ofType;
import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.attempt;
import static net.arctics.clonk.util.Utilities.eq;
import static net.arctics.clonk.util.Utilities.isAnyOf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.arctics.clonk.Core;
import net.arctics.clonk.Problem;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodePrinter;
import net.arctics.clonk.ast.AppendableBackedNodePrinter;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ast.ExpressionLocator;
import net.arctics.clonk.ast.Sequence;
import net.arctics.clonk.ast.SourceLocation;
import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.Function.FunctionScope;
import net.arctics.clonk.c4script.FunctionFragmentParser;
import net.arctics.clonk.c4script.Keywords;
import net.arctics.clonk.c4script.MutableRegion;
import net.arctics.clonk.c4script.Operator;
import net.arctics.clonk.c4script.ProblemReportingStrategy;
import net.arctics.clonk.c4script.ProblemReportingStrategy.Capabilities;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.ScriptParser;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.c4script.Variable.Scope;
import net.arctics.clonk.c4script.ast.AccessDeclaration;
import net.arctics.clonk.c4script.ast.AccessVar;
import net.arctics.clonk.c4script.ast.ArrayExpression;
import net.arctics.clonk.c4script.ast.BinaryOp;
import net.arctics.clonk.c4script.ast.Block;
import net.arctics.clonk.c4script.ast.BunchOfStatements;
import net.arctics.clonk.c4script.ast.CallDeclaration;
import net.arctics.clonk.c4script.ast.ConditionalStatement;
import net.arctics.clonk.c4script.ast.FunctionBody;
import net.arctics.clonk.c4script.ast.IntegerLiteral;
import net.arctics.clonk.c4script.ast.MemberOperator;
import net.arctics.clonk.c4script.ast.ReturnStatement;
import net.arctics.clonk.c4script.ast.SimpleStatement;
import net.arctics.clonk.c4script.ast.Statement;
import net.arctics.clonk.c4script.ast.StringLiteral;
import net.arctics.clonk.c4script.ast.Tidy;
import net.arctics.clonk.c4script.ast.Tuple;
import net.arctics.clonk.c4script.ast.Unfinished;
import net.arctics.clonk.c4script.ast.VarDeclarationStatement;
import net.arctics.clonk.c4script.ast.VarInitialization;
import net.arctics.clonk.c4script.typing.PrimitiveType;
import net.arctics.clonk.index.ID;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.Markers;
import net.arctics.clonk.ui.editors.DeclarationProposal;
import net.arctics.clonk.ui.editors.ProposalsSite;
import net.arctics.clonk.ui.editors.StructureEditingState;
import net.arctics.clonk.ui.editors.StructureTextEditor;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.Pair;
import net.arctics.clonk.util.StringUtil;
import net.arctics.clonk.util.UI;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.ui.views.markers.WorkbenchMarkerResolution;

/**
 * Offers various quick assists/fixes for C4Script source files
 * @author ZokRadonh
 */
public class ScriptQuickAssistProcessor implements IQuickAssistProcessor {

	private static final ICompletionProposal[] NO_SUGGESTIONS = new ICompletionProposal[0];
	private static ScriptQuickAssistProcessor singleton;

	public static ScriptQuickAssistProcessor singleton() { return singleton; }

	public ScriptQuickAssistProcessor() {
		super();
		assert(singleton == null);
		singleton = this;
	}

	@Override
	public boolean canAssist(final IQuickAssistInvocationContext invocationContext) { return true; }

	@Override
	public boolean canFix(final Annotation annotation) {
		return
			annotation instanceof MarkerAnnotation &&
			Core.MARKER_C4SCRIPT_ERROR.equals(attempt(
				((MarkerAnnotation)annotation).getMarker()::getType,
				CoreException.class, e -> {}
			)) &&
			fixes.keySet().contains(Markers.problem(((MarkerAnnotation)annotation).getMarker()));
	}

	private static boolean isAtPosition(final int offset, final Position pos) {
		return (pos != null) && (offset >= pos.getOffset() && offset <= (pos.getOffset() +  pos.getLength()));
	}

	@Override
	public ICompletionProposal[] computeQuickAssistProposals(final IQuickAssistInvocationContext context) {
		final int offset = context.getOffset();
		final List<ICompletionProposal> proposals = new LinkedList<ICompletionProposal>();
		final IAnnotationModel model = context.getSourceViewer().getAnnotationModel();
		if (model == null)
			return NO_SUGGESTIONS;
		@SuppressWarnings("rawtypes")
		final Iterator iter = model.getAnnotationIterator();
		while (iter.hasNext()) {
			final Annotation annotation = (Annotation)iter.next();
			if (canFix(annotation)) {
				final Position pos = model.getPosition(annotation);
				if (isAtPosition(offset, pos))
					collectProposals(((MarkerAnnotation) annotation).getMarker(), pos, proposals, null, StructureTextEditor.getEditorForSourceViewer(context.getSourceViewer(), C4ScriptEditor.class));
			}
		}
		return proposals.toArray(new ICompletionProposal[proposals.size()]);
	}

	public static final class ParameterizedProposalMarkerResolution extends WorkbenchMarkerResolution {
		private final ParameterizedProposal proposal;
		private final IMarker originalMarker;
		public ParameterizedProposalMarkerResolution(final ParameterizedProposal proposal, final IMarker originalMarker) {
			this.proposal = proposal;
			this.originalMarker = originalMarker;
		}
		@Override
		public String getDescription() { return proposal.getDisplayString(); }
		@Override
		public Image getImage() { return proposal.getImage(); }
		@Override
		public String getLabel() { return proposal.getDisplayString(); }
		@Override
		public void run(final IMarker marker) { proposal.runOnMarker(marker); }
		private boolean relevant(final IMarker marker) {
			return
				!marker.equals(this.originalMarker) &&
				Markers.problem(marker) == Markers.problem(originalMarker);
		}
		@Override
		public IMarker[] findOtherMarkers(final IMarker[] markers) {
			final List<IMarker> result = new ArrayList<IMarker>(markers.length);
			for (final IMarker m : markers)
				if (relevant(m))
					result.add(m);
			return result.toArray(new IMarker[result.size()]);
		}
	}

	public static final class ParameterizedProposal extends DeclarationProposal {
		private final Replacement replacement;
		private final int tabIndentation;
		private final ScriptParser parser;
		private final Function func;

		private ParameterizedProposal(final Declaration declaration,
			final String replacementString, final int replacementOffset,
			final int replacementLength, final int cursorPosition, final Image image,
			final String displayString, final IContextInformation contextInformation,
			final String additionalProposalInfo, final String postInfo,
			final ProposalsSite site,
			final Replacement replacement, final int tabIndentation,
			final ScriptParser parser, final Function func
		) {
			super(declaration, declaration, replacementString, replacementOffset,
				replacementLength, cursorPosition, image, displayString,
				contextInformation, additionalProposalInfo, postInfo,
				site);
			this.replacement = replacement;
			this.tabIndentation = tabIndentation;
			this.parser = parser;
			this.func = func;
		}

		public boolean createdFrom(final Replacement other) {
			return this.replacement.equals(other);
		}

		@Override
		public void apply(final ITextViewer viewer, final char trigger, final int stateMask, final int offset) {
			this.apply(viewer.getDocument());
		}

		@Override
		public void apply(final IDocument document) {
			replacement.performAdditionalActionsBeforeDoingReplacements();
			final ASTNode replacementExpr = replacement.replacementExpression();
			if (replacementExpr != ASTNode.NULL_EXPR) {
				for (final ASTNode spec : replacement.specifiable())
					if (spec instanceof AccessDeclaration) {
						final AccessDeclaration accessDec = (AccessDeclaration) spec;
						final String s = UI.input(
							PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
							Messages.ClonkQuickAssistProcessor_SpecifyValue,
							String.format(Messages.ClonkQuickAssistProcessor_SpecifyFormat, accessDec.name()), accessDec.name(),
							newText -> {
								if (!validIdentifierPattern.matcher(newText).matches())
									return String.format(Messages.ClonkQuickAssistProcessor_NotAValidFunctionName, newText);
								else
									return null;
							}
						);
						if (s != null)
							accessDec.setName(s);
					}
				try {
					final Tidy tidy = new Tidy(parser.script(), parser.script().strictLevel());
					this.replacementString = tidy.tidy(replacement.replacementExpression()).printed(tabIndentation+1);
				} catch (final CloneNotSupportedException e) {
					e.printStackTrace();
				}
			} else {
				// don't replace expression
				replacementString = ""; //$NON-NLS-1$
				replacementLength = 0;
			}
			cursorPosition = replacementString.length();
			super.apply(document);

			for (final Declaration dec : replacement.additionalDeclarations()) {
				final StringBuilder builder = new StringBuilder(50);
				dec.print(new AppendableBackedNodePrinter(builder), 0);
				builder.append("\n"); //$NON-NLS-1$
				builder.append("\n"); //$NON-NLS-1$
				try {
					document.replace(func.header().getOffset(), 0, builder.toString());
				} catch (final BadLocationException e) {
					e.printStackTrace();
				}
			}

			final ScriptEditingState listener = StructureEditingState.existing(ScriptEditingState.class, parser.script());
			if (listener != null)
				listener.scheduleReparsing(false);
		}

		public void runOnMarker(final IMarker marker) {
			Core.instance().performActionsOnFileDocument((IFile)marker.getResource(), document -> {
				replacementOffset = marker.getAttribute(IMarker.CHAR_START, replacementOffset);
				replacementLength = marker.getAttribute(IMarker.CHAR_END, replacementOffset+replacementLength)-replacementOffset;
				apply(document);
				return null;
			}, true);
		}

		public Replacement replacement() { return replacement; }
		@Override
		public boolean requiresDocumentReparse() { return replacement.additionalDeclarations().size() > 0; }
	}

	private static class Replacement {
		private String title;
		private final ASTNode replacementExpression;
		private final ASTNode[] specifiable;
		private final List<Declaration> additionalDeclarations = new LinkedList<Declaration>();
		private boolean regionToBeReplacedSpecifiedByReplacementExpression; // yes!
		public Replacement(final String title, final ASTNode replacementExpression, final ASTNode... specifiable) {
			super();
			this.title = title;
			this.replacementExpression = replacementExpression;
			this.specifiable = specifiable;
			this.regionToBeReplacedSpecifiedByReplacementExpression = !(replacementExpression instanceof Statement);
		}
		public String title() { return title; }
		public void setTitle(final String title) { this.title = title; }
		public ASTNode replacementExpression() { return replacementExpression; }
		public ASTNode[] specifiable() { return specifiable; }
		public List<Declaration> additionalDeclarations() { return additionalDeclarations; }
		@Override
		public boolean equals(final Object other) {
			if (other instanceof Replacement) {
				final Replacement otherR = (Replacement) other;
				return otherR.title.equals(title) && otherR.replacementExpression.equals(replacementExpression);
			} else
				return false;
		}
		@Override
		public String toString() {
			return String.format("%s: %s", title, replacementExpression.toString()); //$NON-NLS-1$
		}
		/**
		 * Method to override if performing additional actions apart from replacing code segments is desired.
		 */
		public void performAdditionalActionsBeforeDoingReplacements() {}
	}

	private static final class ReplacementsList extends LinkedList<Replacement> {
		private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
		private final ASTNode offending;
		private final List<ICompletionProposal> existingList;
		public ReplacementsList(final ASTNode offending, final List<ICompletionProposal> existingList) {
			super();
			this.offending = offending;
			this.existingList = existingList;
		}
		public Replacement add(final String replacement, ASTNode elm, final boolean alwaysStatement, final Boolean regionSpecified, final ASTNode... specifiable) {
			if (alwaysStatement && !(elm instanceof Statement))
				elm = new SimpleStatement(elm);
			if (elm.end() == elm.start() && offending != null)
				elm.setLocation(offending.start(), offending.end());
			final Replacement newOne = new Replacement(replacement, elm, specifiable);
			if (regionSpecified != null)
				newOne.regionToBeReplacedSpecifiedByReplacementExpression = regionSpecified;
			// don't add duplicates
			return this.stream()
				.filter(newOne::equals)
				.findFirst()
				.orElseGet(() -> ofType(existingList.stream(), ParameterizedProposal.class)
					.filter(prop -> prop.createdFrom(newOne))
					.map(ParameterizedProposal::replacement)
					.findFirst().orElseGet(() -> {
						this.add(newOne);
						return newOne;
					})
				);
		}
		public Replacement add(final String replacement, final ASTNode elm, final ASTNode... specifiable) {
			return add(replacement, elm, true, null, specifiable);
		}
	}

	private static ASTNode identifierReplacement(final AccessDeclaration original, final String newName) {
		final AccessVar result = new AccessVar(newName);
		result.setLocation(original.start(), original.start()+original.identifierLength());
		return result;
	}

	private static final Pattern validIdentifierPattern = Pattern.compile("[a-zA-Z_]\\w*"); //$NON-NLS-1$

	private static String parmNameFromExpression(final ASTNode expression, final int index) {
		final String exprString = expression.toString();
		final Matcher m = validIdentifierPattern.matcher(exprString);
		return m.matches() ? m.group() : "par"+index; //$NON-NLS-1$
	}

	public void collectProposals(final IMarker marker, final Position position, final List<ICompletionProposal> proposals, IDocument document, final C4ScriptEditor editor) {
		if (document == null)
			document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
		final ScriptEditingState state = editor.state();
		if (state != null)
			for (final ProblemReportingStrategy s : state.problemReportingStrategies())
				if ((s.capabilities() & Capabilities.TYPING) != 0)
					collectProposals(marker, position, proposals, document, editor.script());
	}

	public void collectProposals(
		final IMarker marker,
		final Position position,
		final List<ICompletionProposal> proposals,
		final IDocument document,
		final Script script
	) {
		if (document != null)
			internalCollectProposals(marker, position, proposals, document, script);
		else
			Core.instance().performActionsOnFileDocument(script.source(), connectedDocument -> {
				internalCollectProposals(marker, position, proposals, connectedDocument, script);
				return null;
			}, false);
	}

	static class Site {
		final IDocument document;
		final Script script;
		final IMarker marker;
		final Problem problem;
		final IRegion expressionRegion0;
		final Function func;
		final int tabIndentation;
		final FunctionFragmentParser parser;
		final ASTNode offendingExpression;
		final Statement topLevel;
		final IRegion expressionRegion;
		final ReplacementsList replacements;
		Replacement addRemoveReplacement() {
			final Replacement result = replacements.add(
				Messages.ClonkQuickAssistProcessor_Remove,
				new ReplacementStatement("", expressionRegion, document, expressionRegion.getOffset(), func.bodyLocation().getOffset()), //$NON-NLS-1$
				false, true
			);
			return result;
		}
		Site(final List<ICompletionProposal> proposals, final IDocument document, final Script script, final Position position, final IMarker marker) {
			this.document = document;
			this.script = script;
			this.marker = marker;
			this.problem = Markers.problem(marker);
			this.expressionRegion0 = new SourceLocation(marker.getAttribute(IMarker.CHAR_START, 0),  marker.getAttribute(IMarker.CHAR_END, 0));
			if (expressionRegion0.getOffset() == -1 || script == null || document == null)
				throw new IllegalStateException();
			this.func = script.funcAt(position.getOffset());
			this.tabIndentation = BufferedScanner.indentationOfStringAtPos(document.get(),
				func.bodyLocation().getOffset()+expressionRegion0.getOffset(), BufferedScanner.TABINDENTATIONMODE);
			final ExpressionLocator<Site> locator = new ExpressionLocator<Site>(position.getOffset()-func.bodyLocation().start());
			this.func.traverse(locator, this);
			this.parser = new FunctionFragmentParser(document, script, func, null);
			this.offendingExpression = locator.expressionAtRegion();
			this.topLevel = offendingExpression != null ? offendingExpression.parent(Statement.class) : null;
			this.expressionRegion = offendingExpression != null && topLevel != null ? offendingExpression.absolute() : expressionRegion0;
			if (offendingExpression == null || topLevel == null)
				throw new IllegalStateException();
			this.replacements = new ReplacementsList(offendingExpression, proposals);
		}
		void commitReplacements() {
			replacements.stream().map(replacement -> {
				final String replacementAsString = "later"; //$NON-NLS-1$
				final IRegion r = replacement.regionToBeReplacedSpecifiedByReplacementExpression
					? new Region(
						func.bodyLocation().getOffset() + replacement.replacementExpression().start(),
						replacement.replacementExpression().getLength()
					)
					: new Region(
						expressionRegion.getOffset(),
						replacement.replacementExpression() instanceof Statement
							? topLevel.getLength()
							: expressionRegion.getLength()
					);
				return new ParameterizedProposal(
					null, replacementAsString, r.getOffset(), r.getLength(),
					replacementAsString.length(), null, replacement.title(), null, null, null, null,
					replacement, tabIndentation, parser, func
				);
			}).forEach(replacements.existingList::add);
		}
	}

	@FunctionalInterface
	interface ProblemFixer {
		void contribute(Site site);
	}

	static Pair<Problem[], ProblemFixer> fix(ProblemFixer fixer, Problem... problems) {
		return Pair.pair(problems, fixer);
	}

	static class Problems {
		final Problem[] problems;
		Problems(Problem[] problems) {
			super();
			this.problems = problems;
		}
		Stream<Pair<Problem, ProblemFixer>> fixedBy(ProblemFixer fixer) {
			return stream(problems).map(p -> Pair.pair(p, fixer));
		}
	}

	static Problems problems(Problem... probs) {
		return new Problems(probs);
	}

	private final Map<Problem, ProblemFixer> fixes = Arrays.asList(

		problems(Problem.KeywordInWrongPlace).fixedBy(site -> {
			site.addRemoveReplacement();
		}),
		problems(Problem.NotFinished).fixedBy(site -> {
			if (site.topLevel instanceof Unfinished) {
				final boolean arrayElement = site.topLevel.parent() instanceof ArrayExpression;
				site.replacements.add(
					arrayElement ? Messages.C4ScriptQuickAssistProcessor_AddMissingComma : Messages.ClonkQuickAssistProcessor_AddMissingSemicolon,
					new ASTNode() {
						private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
						@Override
						public void doPrint(final ASTNodePrinter output, final int depth) {
							SimpleStatement.unwrap(site.topLevel).print(output, depth);
							output.append(arrayElement ? ',' : ';');
						}
					}, false, true
				);
			}
		}),
		problems(Problem.UndeclaredIdentifier, Problem.DeclarationNotFound).fixedBy(site -> {
			if (site.offendingExpression instanceof AccessVar && site.offendingExpression.parent() instanceof BinaryOp) {
				final AccessVar var = (AccessVar) site.offendingExpression;
				final BinaryOp op = (BinaryOp) site.offendingExpression.parent();
				if (site.topLevel == op.parent() && op.operator() == Operator.Assign && op.leftSide() == site.offendingExpression)
					site.replacements.add(
						Messages.ClonkQuickAssistProcessor_ConvertToVarDeclaration,
						new VarDeclarationStatement(var.name(), op.rightSide(), Keywords.VarNamed.length()+1, Scope.VAR)
					);
			}
			if (site.offendingExpression instanceof CallDeclaration)
				if (site.offendingExpression.predecessor() instanceof MemberOperator && !((MemberOperator)site.offendingExpression.predecessor()).hasTilde()) {
					final MemberOperator opWithTilde = new MemberOperator(false, true, ((MemberOperator)site.offendingExpression.predecessor()).id(), 3);
					opWithTilde.setLocation(site.offendingExpression.predecessor());
					site.replacements.add(Messages.ClonkQuickAssistProcessor_UseTildeWithNoSpace, opWithTilde, false, true);
				}
			if (site.offendingExpression instanceof AccessDeclaration && site.offendingExpression.predecessor() == null) {
				final AccessDeclaration accessDec = (AccessDeclaration) site.offendingExpression;

				// create new variable or function
				final Replacement createNewDeclarationReplacement = site.replacements.add(
					String.format(site.offendingExpression instanceof AccessVar ? Messages.ClonkQuickAssistProcessor_CreateLocalVar : Messages.ClonkQuickAssistProcessor_CreateLocalFunc, accessDec.name()),
					ASTNode.NULL_EXPR,
					false, false
				);
				final List<Declaration> decs = createNewDeclarationReplacement.additionalDeclarations();
				if (accessDec instanceof AccessVar)
					decs.add(new Variable(Scope.LOCAL, accessDec.name()));
				else {
					final CallDeclaration callFunc = (CallDeclaration) accessDec;
					final Function function = new Function(accessDec.name(), FunctionScope.PUBLIC);
					function.setParent(site.script);
					function.storeBody(new FunctionBody(function), "");
					decs.add(function);
					final List<Variable> parms = new ArrayList<Variable>(callFunc.params().length);
					int p = 0;
					for (final ASTNode parm : callFunc.params())
						parms.add(new Variable(parmNameFromExpression(parm, ++p), site.script.typings().get(parm)));
					function.setParameters(parms);
				}

				// gather proposals through ScriptCompletionProcessor and propose those with a similar name
				final ASTNode expr = site.offendingExpression.parent() instanceof Sequence
					? ((Sequence) site.offendingExpression.parent()).subSequenceUpTo(site.offendingExpression)
					: site.offendingExpression;
				final List<ICompletionProposal> possible = ScriptCompletionProcessor.computeProposalsForExpression
					(site.document, site.func, expr);
				ofType(possible.stream(), DeclarationProposal.class).forEach(clonkProposal -> {
					final Declaration dec = clonkProposal.declaration();
					if (dec == null || !accessDec.declarationClass().isAssignableFrom(dec.getClass()))
						return;
					final int similarity = StringUtil.similarityOf(dec.name(), accessDec.name());
					if (similarity > 0)
						site.replacements.add(String.format(Messages.ClonkQuickAssistProcessor_ReplaceWith, dec.name()),
							identifierReplacement(accessDec, dec.name()), false, true);
				});

				// propose adding projects to the referenced projects which contain a definition with a matching name
				if (accessDec.parent() instanceof CallDeclaration) {
					final Variable parm = ((CallDeclaration)accessDec.parent()).parmDefinitionForParmExpression(accessDec);
					final boolean isIDPar = parm != null && site.script.typing().compatible(parm.type(), PrimitiveType.ID);
					if (isIDPar) {
						final IProject p = site.script.resource().getProject();
						final List<IProject> referencedProjects = attempt(
							() -> asList(p.getReferencedProjects()), CoreException.class, Exception::printStackTrace
						);
						final ID defId = ID.get(accessDec.name());
						if (referencedProjects != null)
							stream(ClonkProjectNature.clonkProjectsInWorkspace())
								.filter(proj -> referencedProjects.indexOf(proj) == -1)
								.map(ClonkProjectNature::get)
								.filter(nat -> nat.index().definitionsWithID(defId) != null && eq(nat.index().engine(), site.script.engine()))
								.forEach(nat -> {
									site.replacements.add(new Replacement(String.format(Messages.ClonkQuickAssistProcessor_AddProjectToReferencedProjects, nat.getProject().getName()), accessDec) {
										@Override
										public void performAdditionalActionsBeforeDoingReplacements() {
											final IProjectDescription desc = attempt(p::getDescription, CoreException.class, Exception::printStackTrace);
											if (desc != null)
												try {
													desc.setReferencedProjects(ArrayUtil.concat(desc.getReferencedProjects(), nat.getProject()));
													p.setDescription(desc, null);
												} catch (final CoreException e) {
													e.printStackTrace();
												}
										}
									});
								});
					}
				}
			}
		}),
		problems(Problem.IncompatibleTypes).fixedBy(site -> {
			final PrimitiveType t = Markers.expectedType(site.marker);
			if (eq(t, PrimitiveType.STRING)) {
				final StringLiteral str = new StringLiteral(site.offendingExpression.toString());
				str.setLocation(site.offendingExpression);
				site.replacements.add(
					Messages.ClonkQuickAssistProcessor_QuoteExpression,
					str,
					false, true
				);
			}
			final IntegerLiteral ntgr = as(site.offendingExpression, IntegerLiteral.class);
			if (isAnyOf(t, PrimitiveType.NILLABLES) && ntgr != null && ntgr.longValue() == 0)
				site.replacements.add(
					Messages.C4ScriptQuickAssistProcessor_Replace0WithNil,
					new AccessVar(Keywords.Nil),
					false, false
				);
		}),
		problems(Problem.NoSideEffects).fixedBy(site -> {
			if (site.topLevel instanceof SimpleStatement) {
				final SimpleStatement statement = (SimpleStatement) site.topLevel;
				site.replacements.add(
					Messages.ClonkQuickAssistProcessor_ConvertToReturn,
					new ReturnStatement((statement.expression()))
				);
				site.addRemoveReplacement();
				final CallDeclaration callFunc = new CallDeclaration(Messages.ClonkQuickAssistProcessor_FunctionToBeCalled, statement.expression());
				site.replacements.add(Messages.ClonkQuickAssistProcessor_WrapWithFunctionCall, callFunc, callFunc);
			}
		}),
		problems(Problem.NoAssignment).fixedBy(site -> {
			if (site.topLevel instanceof SimpleStatement) {
				final SimpleStatement statement = (SimpleStatement) site.topLevel;
				if (statement.expression() instanceof BinaryOp) {
					final BinaryOp binaryOp = (BinaryOp) statement.expression();
					if (binaryOp.operator() == Operator.Equal)
						site.replacements.add(
							Messages.ClonkQuickAssistProcessor_ConvertComparisonToAssignment,
							new BinaryOp(Operator.Assign, binaryOp.leftSide(), binaryOp.rightSide())
						);
				}
			}
		}),
		problems(Problem.NoInheritedFunction).fixedBy(site -> {
			if (site.offendingExpression instanceof CallDeclaration && ((CallDeclaration)site.offendingExpression).name().equals(Keywords.Inherited))
				site.replacements.add(
					String.format(Messages.ClonkQuickAssistProcessor_UseInsteadOf, Keywords.SafeInherited, Keywords.Inherited),
					identifierReplacement((AccessDeclaration) site.offendingExpression, Keywords.SafeInherited),
					false, true
				);
		}),
		problems(Problem.ReturnAsFunction).fixedBy(site -> {
			if (site.offendingExpression instanceof Tuple) {
				final Tuple tuple = (Tuple) site.offendingExpression;
				final ASTNode[] elms = tuple.subElements();
				if (elms.length >= 2) {
					final ASTNode returnExpr = elms[0];
					final ASTNode[] rest = ArrayUtil.arrayRange(elms, 1, elms.length-1, ASTNode.class);
					final Statement[] statements = ArrayUtil.concat(SimpleStatement.wrapExpressions(rest), new ReturnStatement(returnExpr));
					final ConditionalStatement cond = as(tuple.parent().parent(), ConditionalStatement.class);
					final Block reordered = cond != null && cond.body() == tuple.parent()
						? new Block(statements) : new BunchOfStatements(statements);
					reordered.setLocation(tuple.parent()); // return statement
					site.replacements.add(
						Messages.ClonkQuickAssistProcessor_RearrangeReturnStatement,
						reordered,
						false,
						true
					);
				}
			}
		}),
		problems(Problem.Unused).fixedBy(site -> {
			if (site.offendingExpression instanceof VarInitialization) {
				final MutableRegion regionToDelete = new MutableRegion(0, site.expressionRegion.getLength());
				final VarInitialization cur = (VarInitialization) site.offendingExpression;
				final VarInitialization next = cur.succeedingInitialization();
				final VarInitialization previous = cur.precedingInitialization();
				final String replacementString = ""; //$NON-NLS-1$
				if (next == null) {
					if (previous != null) {
						// removing last initialization -> change ',' before it to ';'
						site.parser.seek(previous.end());
						site.parser.eatWhitespace();
						if (site.parser.peek() == ',')
							regionToDelete.setStartAndEnd(site.parser.tell(), cur.end());
					} else
						site.addRemoveReplacement().setTitle(Messages.ClonkQuickAssistProcessor_RemoveVariableDeclaration);
				} else
					regionToDelete.setStartAndEnd(cur.getOffset(), next.getOffset());
				site.replacements.add(
					Messages.ClonkQuickAssistProcessor_RemoveVariableDeclaration,
					new ReplacementStatement(replacementString, regionToDelete, site.document, site.expressionRegion.getOffset(), site.func.bodyLocation().getOffset()),
					false, true
				);
			}
		}),
		problems(Problem.Garbage).fixedBy(Site::addRemoveReplacement),
		problems(Problem.MemberOperatorWithTildeNoSpace).fixedBy(site -> {
			// just print out site.topLevel, space will be removed automatically
			site.replacements.add(Messages.ClonkQuickAssistProcessor_RemoveSpace, site.topLevel);
		})
	).stream().flatMap(identity()).collect(Collectors.toMap(Pair::first, Pair::second));

	private void internalCollectProposals(final IMarker marker, final Position position, final List<ICompletionProposal> proposals, final IDocument document, final Script script) {
		final Site site = new Site(proposals, document, script, position, marker);
		final ProblemFixer fixer = fixes.get(site.problem);
		if (fixer != null)
			fixer.contribute(site);
		try {
			site.replacements.add(
				Messages.ClonkQuickAssistProcessor_TidyUp,
				new Tidy(script, script.strictLevel()).tidyExhaustive(site.topLevel),
				false, true
			);
		} catch (final CloneNotSupportedException e) {}
		site.commitReplacements();

	}

	@Override
	public String getErrorMessage() {
		return Messages.ClonkQuickAssistProcessor_FailedToFix;
	}

}
