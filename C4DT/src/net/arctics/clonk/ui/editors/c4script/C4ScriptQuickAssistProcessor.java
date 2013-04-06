package net.arctics.clonk.ui.editors.c4script;

import static net.arctics.clonk.util.Utilities.isAnyOf;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.arctics.clonk.Core;
import net.arctics.clonk.Core.IDocumentAction;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.ASTNodePrinter;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.ID;
import net.arctics.clonk.parser.Markers;
import net.arctics.clonk.parser.Problem;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.Function.FunctionScope;
import net.arctics.clonk.parser.c4script.FunctionFragmentParser;
import net.arctics.clonk.parser.c4script.Keywords;
import net.arctics.clonk.parser.c4script.MutableRegion;
import net.arctics.clonk.parser.c4script.Operator;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.ProblemReportingContext;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.parser.c4script.TypeUtil;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.Variable.Scope;
import net.arctics.clonk.parser.c4script.ast.AccessDeclaration;
import net.arctics.clonk.parser.c4script.ast.AccessVar;
import net.arctics.clonk.parser.c4script.ast.ArrayExpression;
import net.arctics.clonk.parser.c4script.ast.BinaryOp;
import net.arctics.clonk.parser.c4script.ast.Block;
import net.arctics.clonk.parser.c4script.ast.BunchOfStatements;
import net.arctics.clonk.parser.c4script.ast.CallDeclaration;
import net.arctics.clonk.parser.c4script.ast.Comment;
import net.arctics.clonk.parser.c4script.ast.ConditionalStatement;
import net.arctics.clonk.parser.c4script.ast.IntegerLiteral;
import net.arctics.clonk.parser.c4script.ast.MemberOperator;
import net.arctics.clonk.parser.c4script.ast.ReturnStatement;
import net.arctics.clonk.parser.c4script.ast.Sequence;
import net.arctics.clonk.parser.c4script.ast.SimpleStatement;
import net.arctics.clonk.parser.c4script.ast.Statement;
import net.arctics.clonk.parser.c4script.ast.StringLiteral;
import net.arctics.clonk.parser.c4script.ast.Tuple;
import net.arctics.clonk.parser.c4script.ast.TypeUnification;
import net.arctics.clonk.parser.c4script.ast.Unfinished;
import net.arctics.clonk.parser.c4script.ast.VarDeclarationStatement;
import net.arctics.clonk.parser.c4script.ast.VarInitialization;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.ui.editors.ClonkCompletionProposal;
import net.arctics.clonk.ui.editors.ClonkTextEditor;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.StringUtil;
import net.arctics.clonk.util.UI;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
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
 *
 */
public class C4ScriptQuickAssistProcessor implements IQuickAssistProcessor {

	private static final ICompletionProposal[] NO_SUGGESTIONS = new ICompletionProposal[0];
	private static C4ScriptQuickAssistProcessor singleton;

	public static C4ScriptQuickAssistProcessor singleton() { return singleton; }

	public C4ScriptQuickAssistProcessor() {
		super();
		assert(singleton == null);
		singleton = this;
	}

	@Override
	public boolean canAssist(IQuickAssistInvocationContext invocationContext) { return true; }

	private static final Set<Problem> fixableParserErrorCodes = ArrayUtil.set(
		Problem.VariableCalled,
		Problem.NeverReached,
		Problem.NotFinished,
		Problem.UndeclaredIdentifier,
		Problem.IncompatibleTypes,
		Problem.NoSideEffects,
		Problem.NoAssignment,
		Problem.NoInheritedFunction,
		Problem.ReturnAsFunction,
		Problem.Unused,
		Problem.Garbage,
		Problem.MemberOperatorWithTildeNoSpace,
		Problem.KeywordInWrongPlace
	);

	@Override
	public boolean canFix(Annotation annotation) {
		if (annotation instanceof MarkerAnnotation) {
			final MarkerAnnotation ma = (MarkerAnnotation)annotation;
			String ty;
			try {
				ty = ma.getMarker().getType();
			} catch (final CoreException e) {
				return false;
			}
			if (ty.equals(Core.MARKER_C4SCRIPT_ERROR) || ty.equals(Core.MARKER_C4SCRIPT_ERROR_WHILE_TYPING))
				return fixableParserErrorCodes.contains(Markers.problem(ma.getMarker()));
		}
		return false;
	}

	private static boolean isAtPosition(int offset, Position pos) {
		return (pos != null) && (offset >= pos.getOffset() && offset <= (pos.getOffset() +  pos.getLength()));
	}

	@Override
	public ICompletionProposal[] computeQuickAssistProposals(IQuickAssistInvocationContext context) {
		final int offset = context.getOffset();
		final List<ICompletionProposal> proposals = new LinkedList<ICompletionProposal>();
		final IAnnotationModel model = context.getSourceViewer().getAnnotationModel();
		if (model == null)
			return NO_SUGGESTIONS;
		@SuppressWarnings("rawtypes")
		final
		Iterator iter = model.getAnnotationIterator();
		while (iter.hasNext()) {
			final Annotation annotation = (Annotation)iter.next();
			if (canFix(annotation)) {
				final Position pos = model.getPosition(annotation);
				if (isAtPosition(offset, pos))
					collectProposals(((MarkerAnnotation) annotation).getMarker(), pos, proposals, null, ClonkTextEditor.getEditorForSourceViewer(context.getSourceViewer(), C4ScriptEditor.class));
			}
		}
		return proposals.toArray(new ICompletionProposal[proposals.size()]);
	}

	public static final class ParameterizedProposalMarkerResolution extends WorkbenchMarkerResolution {

		private final ParameterizedProposal proposal;
		private final IMarker originalMarker;

		public ParameterizedProposalMarkerResolution(ParameterizedProposal proposal, IMarker originalMarker) {
			this.proposal = proposal;
			this.originalMarker = originalMarker;
		}

		@Override
		public String getDescription() {
			return proposal.getDisplayString();
		}

		@Override
		public Image getImage() {
			return proposal.getImage();
		}

		@Override
		public String getLabel() {
			return proposal.getDisplayString();
		}

		@Override
		public void run(IMarker marker) {
			proposal.runOnMarker(marker);
		}

		private boolean relevant(IMarker marker) {
			return
				!marker.equals(this.originalMarker) &&
				Markers.problem(marker) == Markers.problem(originalMarker);
		}

		@Override
		public IMarker[] findOtherMarkers(IMarker[] markers) {
			final List<IMarker> result = new ArrayList<IMarker>(markers.length);
			for (final IMarker m : markers)
				if (relevant(m))
					result.add(m);
			return result.toArray(new IMarker[result.size()]);
		}

	}

	public final class ParameterizedProposal extends ClonkCompletionProposal {
		private final Replacement replacement;
		private final int tabIndentation;
		private final C4ScriptParser parser;
		private final Function func;

		private ParameterizedProposal(Declaration declaration,
			String replacementString, int replacementOffset,
			int replacementLength, int cursorPosition, Image image,
			String displayString, IContextInformation contextInformation,
			String additionalProposalInfo, String postInfo,
			ClonkTextEditor editor,
			Replacement replacement, int tabIndentation,
			C4ScriptParser parser, Function func
		) {
			super(declaration, replacementString, replacementOffset,
				replacementLength, cursorPosition, image, displayString,
				contextInformation, additionalProposalInfo, postInfo,
				editor);
			this.replacement = replacement;
			this.tabIndentation = tabIndentation;
			this.parser = parser;
			this.func = func;
		}

		public boolean createdFrom(Replacement other) {
			return this.replacement.equals(other);
		}

		@Override
		public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
			this.apply(viewer.getDocument());
		}

		@Override
		public void apply(IDocument document) {
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
							new IInputValidator() {
								@Override
								public String isValid(String newText) {
									if (!validIdentifierPattern.matcher(newText).matches())
										return String.format(Messages.ClonkQuickAssistProcessor_NotAValidFunctionName, newText);
									else
										return null;
								}
							}
						);
						if (s != null)
							accessDec.setName(s);
					}
				try {
					this.replacementString = replacement.replacementExpression().exhaustiveOptimize
						(TypeUtil.problemReportingContext(parser.script())).printed(tabIndentation+1);
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

			for (final Replacement.AdditionalDeclaration dec : replacement.additionalDeclarations()) {
				final StringBuilder builder = new StringBuilder(50);
				dec.declaration.sourceCodeRepresentation(builder, dec.code);
				builder.append("\n"); //$NON-NLS-1$
				builder.append("\n"); //$NON-NLS-1$
				try {
					document.replace(func.header().getOffset(), 0, builder.toString());
				} catch (final BadLocationException e) {
					e.printStackTrace();
				}
			}

			final ScriptEditingState listener = ScriptEditingState.state(parser.script());
			if (listener != null)
				listener.scheduleReparsing(false);
		}

		public void runOnMarker(final IMarker marker) {
			Core.instance().performActionsOnFileDocument((IFile)marker.getResource(), new IDocumentAction<Object>() {
				@Override
				public Object run(IDocument document) {
					replacementOffset = marker.getAttribute(IMarker.CHAR_START, replacementOffset);
					replacementLength = marker.getAttribute(IMarker.CHAR_END, replacementOffset+replacementLength)-replacementOffset;
					apply(document);
					return null;
				}
			}, true);
		}

		public Replacement replacement() { return replacement; }
		@Override
		public boolean requiresDocumentReparse() { return replacement.additionalDeclarations().size() > 0; }
	}

	private static class Replacement {
		public static class AdditionalDeclaration {
			public Declaration declaration;
			public ASTNode code;
			public AdditionalDeclaration(Declaration declaration, ASTNode code) {
				super();
				this.declaration = declaration;
				this.code = code;
			}
		}
		private String title;
		private final ASTNode replacementExpression;
		private final ASTNode[] specifiable;
		private final List<AdditionalDeclaration> additionalDeclarations = new LinkedList<AdditionalDeclaration>();
		private boolean regionToBeReplacedSpecifiedByReplacementExpression; // yes!
		public Replacement(String title, ASTNode replacementExpression, ASTNode... specifiable) {
			super();
			this.title = title;
			this.replacementExpression = replacementExpression;
			this.specifiable = specifiable;
			this.regionToBeReplacedSpecifiedByReplacementExpression = !(replacementExpression instanceof Statement);
		}
		public String title() {
			return title;
		}
		public void setTitle(String title) {
			this.title = title;
		}
		public ASTNode replacementExpression() {
			return replacementExpression;
		}
		public ASTNode[] specifiable() {
			return specifiable;
		}
		public List<AdditionalDeclaration> additionalDeclarations() {
			return additionalDeclarations;
		}
		@Override
		public boolean equals(Object other) {
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
		public ReplacementsList(ASTNode offending, List<ICompletionProposal> existingList) {
			super();
			this.offending = offending;
			this.existingList = existingList;
		}
		public Replacement add(String replacement, ASTNode elm, boolean alwaysStatement, Boolean regionSpecified, ASTNode... specifiable) {
			if (alwaysStatement && !(elm instanceof Statement))
				elm = new SimpleStatement(elm);
			if (elm.end() == elm.start() && offending != null)
				elm.setLocation(offending.start(), offending.end());
			final Replacement newOne = new Replacement(replacement, elm, specifiable);
			if (regionSpecified != null)
				newOne.regionToBeReplacedSpecifiedByReplacementExpression = regionSpecified;
			// don't add duplicates
			for (final Replacement existing : this)
				if (existing.equals(newOne))
					return existing;
			for (final ICompletionProposal prop : existingList)
				if (prop instanceof ParameterizedProposal && ((ParameterizedProposal)prop).createdFrom(newOne))
					return ((ParameterizedProposal)prop).replacement();
			this.add(newOne);
			return newOne;
		}
		public Replacement add(String replacement, ASTNode elm, ASTNode... specifiable) {
			return add(replacement, elm, true, null, specifiable);
		}
	}

	private static ASTNode identifierReplacement(AccessDeclaration original, String newName) {
		final AccessVar result = new AccessVar(newName);
		result.setLocation(original.start(), original.start()+original.identifierLength());
		return result;
	}

	private static final Pattern validIdentifierPattern = Pattern.compile("[a-zA-Z_]\\w*"); //$NON-NLS-1$

	private static String parmNameFromExpression(ASTNode expression, int index) {
		final String exprString = expression.toString();
		final Matcher m = validIdentifierPattern.matcher(exprString);
		if (m.matches())
			return m.group();
		else
			return "par"+index; //$NON-NLS-1$
	}

	public void collectProposals(IMarker marker, Position position, List<ICompletionProposal> proposals, IDocument document, C4ScriptEditor editor) {
		if (document == null)
			document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
		if (document == null)
			document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
		collectProposals(marker, position, proposals, document, editor.script(), editor.typingStrategy().localTypingContext(editor.script(), 0, null));
	}

	public void collectProposals(
		final IMarker marker,
		final Position position,
		final List<ICompletionProposal> proposals,
		IDocument document,
		final Script script,
		final ProblemReportingContext problemReporting
	) {
		if (document != null)
			internalCollectProposals(marker, position, proposals, document, script, problemReporting);
		else
			Core.instance().performActionsOnFileDocument(script.source(), new IDocumentAction<Void>() {
				@Override
				public Void run(IDocument connectedDocument) {
					internalCollectProposals(marker, position, proposals, connectedDocument, script, problemReporting);
					return null;
				}
			}, false);
	}

	private void internalCollectProposals(IMarker marker, Position position, List<ICompletionProposal> proposals, IDocument document, Script script, ProblemReportingContext problemReporting) {
		final Problem errorCode = Markers.problem(marker);
		IRegion expressionRegion = new SourceLocation(marker.getAttribute(IMarker.CHAR_START, 0),  marker.getAttribute(IMarker.CHAR_END, 0));
		if (expressionRegion.getOffset() == -1)
			return;
		if (script == null || document == null)
			return;
		final Function func = script.funcAt(position.getOffset());
		final int tabIndentation = BufferedScanner.indentationOfStringAtPos(document.get(), func.bodyLocation().getOffset()+expressionRegion.getOffset(), BufferedScanner.TABINDENTATIONMODE);
		final ExpressionLocator locator = new ExpressionLocator(position.getOffset()-func.bodyLocation().start());
		func.traverse(locator, this);
		final FunctionFragmentParser parser = new FunctionFragmentParser(document, script, func, null);
		final ASTNode offendingExpression = locator.expressionAtRegion();
		final Statement topLevel = offendingExpression != null ? offendingExpression.statement() : null;

		if (offendingExpression != null && topLevel != null) {
			expressionRegion = offendingExpression.absolute();
			final ReplacementsList replacements = new ReplacementsList(offendingExpression, proposals);
			switch (errorCode) {
			case VariableCalled:
				assert(offendingExpression instanceof CallDeclaration);
				replacements.add(
					Messages.ClonkQuickAssistProcessor_RemoveBrackets,
					topLevel.replaceSubElement(offendingExpression, new AccessVar(((CallDeclaration)offendingExpression).name()), 0)
				);
				break;
			case NeverReached:
				{
					final String s = topLevel.toString();
					try {
						replacements.add(
							Messages.ClonkQuickAssistProcessor_CommentOutStatement,
							new Comment(document.get(expressionRegion.getOffset()+func.bodyLocation().start(), expressionRegion.getLength()),
								s.contains("\n"), false) //$NON-NLS-1$
						);
					} catch (final BadLocationException e) {
						e.printStackTrace();
					}
					addRemoveReplacement(document, expressionRegion, replacements, func);
					break;
				}
			case KeywordInWrongPlace:
				addRemoveReplacement(document, expressionRegion, replacements, func);
				break;
			case NotFinished:
				if (topLevel instanceof Unfinished) {
					final boolean arrayElement = topLevel.parent() instanceof ArrayExpression;
					replacements.add(
						arrayElement ? Messages.C4ScriptQuickAssistProcessor_AddMissingComma : Messages.ClonkQuickAssistProcessor_AddMissingSemicolon,
						new ASTNode() {
							private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
							@Override
							public void doPrint(ASTNodePrinter output, int depth) {
								Unfinished.unwrap(topLevel).print(output, depth);
								output.append(arrayElement ? ',' : ';');
							}
						}, false, true
					);
				}
				break;
			case UndeclaredIdentifier:
				if (offendingExpression instanceof AccessVar && offendingExpression.parent() instanceof BinaryOp) {
					final AccessVar var = (AccessVar) offendingExpression;
					final BinaryOp op = (BinaryOp) offendingExpression.parent();
					if (topLevel == op.parent() && op.operator() == Operator.Assign && op.leftSide() == offendingExpression)
						replacements.add(
							Messages.ClonkQuickAssistProcessor_ConvertToVarDeclaration,
							new VarDeclarationStatement(var.name(), op.rightSide(), Keywords.VarNamed.length()+1, Scope.VAR)
						);
				}
				if (offendingExpression instanceof CallDeclaration)
					if (offendingExpression.predecessorInSequence() instanceof MemberOperator && !((MemberOperator)offendingExpression.predecessorInSequence()).hasTilde()) {
						final MemberOperator opWithTilde = new MemberOperator(false, true, ((MemberOperator)offendingExpression.predecessorInSequence()).id(), 3);
						opWithTilde.setLocation(offendingExpression.predecessorInSequence());
						replacements.add(Messages.ClonkQuickAssistProcessor_UseTildeWithNoSpace, opWithTilde, false, true);
					}
				if (offendingExpression instanceof AccessDeclaration && offendingExpression.predecessorInSequence() == null) {
					final AccessDeclaration accessDec = (AccessDeclaration) offendingExpression;

					// create new variable or function
					final Replacement createNewDeclarationReplacement = replacements.add(
						String.format(offendingExpression instanceof AccessVar ? Messages.ClonkQuickAssistProcessor_CreateLocalVar : Messages.ClonkQuickAssistProcessor_CreateLocalFunc, accessDec.name()),
						ASTNode.NULL_EXPR,
						false, false
					);
					final List<Replacement.AdditionalDeclaration> decs = createNewDeclarationReplacement.additionalDeclarations();
					if (accessDec instanceof AccessVar)
						decs.add(new Replacement.AdditionalDeclaration(
							new Variable(accessDec.name(), Scope.LOCAL),
							ASTNode.NULL_EXPR
						));
					else {
						final CallDeclaration callFunc = (CallDeclaration) accessDec;
						Function function;
						decs.add(new Replacement.AdditionalDeclaration(
							function = new Function(accessDec.name(), FunctionScope.PUBLIC),
							ASTNode.NULL_EXPR
						));
						final List<Variable> parms = new ArrayList<Variable>(callFunc.params().length);
						int p = 0;
						for (final ASTNode parm : callFunc.params())
							parms.add(new Variable(parmNameFromExpression(parm, ++p), problemReporting.typeOf(parm)));
						function.setParameters(parms);
					}

					// gather proposals through ClonkCompletionProcessor and propose those with a similar name
					ASTNode expr;
					if (offendingExpression.parent() instanceof Sequence) {
						final Sequence sequence = (Sequence) offendingExpression.parent();
						expr = sequence.subSequenceUpTo(offendingExpression);
					} else
						expr = null;
					final List<ICompletionProposal> possible = C4ScriptCompletionProcessor.computeProposalsForExpression
						(expr, func, parser, document);
					for (final ICompletionProposal p : possible)
						if (p instanceof ClonkCompletionProposal) {
							final ClonkCompletionProposal clonkProposal = (ClonkCompletionProposal) p;
							final Declaration dec = clonkProposal.declaration();
							if (dec == null || !accessDec.declarationClass().isAssignableFrom(dec.getClass()))
								continue;
							final int similarity = StringUtil.similarityOf(dec.name(), accessDec.name());
							if (similarity > 0) {
								// always create AccessVar and set its region such that only the identifier part of the AccessDeclaration object
								// will be replaced -> no unnecessary tidy-up of CallFunc parameters
								final ASTNode repl = identifierReplacement(accessDec, dec.name());
								replacements.add(String.format(Messages.ClonkQuickAssistProcessor_ReplaceWith, dec.name()), repl, false, true);
							}
						}

					// propose adding projects to the referenced projects which contain a definition with a matching name
					if (accessDec.parent() instanceof CallDeclaration) {
						final Variable parm = ((CallDeclaration)accessDec.parent()).parmDefinitionForParmExpression(accessDec);
						if (parm != null && TypeUnification.compatible(parm.type(), PrimitiveType.ID)) {
							final IProject p = marker.getResource().getProject();
							IProject[] referencedProjects;
							try {
								referencedProjects = p.getReferencedProjects();
							} catch (final CoreException e) {
								e.printStackTrace();
								break;
							}
							final ID defId = ID.get(accessDec.name());
							for (final IProject proj : ClonkProjectNature.clonkProjectsInWorkspace())
								if (ArrayUtil.indexOf(proj, referencedProjects) == -1) {
									final ClonkProjectNature nat = ClonkProjectNature.get(proj);
									if (nat.index().definitionsWithID(defId) != null)
										replacements.add(new Replacement(String.format(Messages.ClonkQuickAssistProcessor_AddProjectToReferencedProjects, nat.getProject().getName()), accessDec) {
											@Override
											public void performAdditionalActionsBeforeDoingReplacements() {
												IProjectDescription desc;
												try {
													desc = p.getDescription();
												} catch (final CoreException e) {
													e.printStackTrace();
													return;
												}
												desc.setReferencedProjects(ArrayUtil.concat(desc.getReferencedProjects(), proj));
												try {
													p.setDescription(desc, null);
												} catch (final CoreException e) {
													e.printStackTrace();
												}
											}
										});
								}
						}
					}
				}
				break;
			case IncompatibleTypes:
				final PrimitiveType t = Markers.expectedType(marker);
				if (t == PrimitiveType.STRING) {
					final StringLiteral str = new StringLiteral(offendingExpression.toString());
					str.setLocation(offendingExpression);
					replacements.add(
						Messages.ClonkQuickAssistProcessor_QuoteExpression,
						str,
						false, true
					);
				}
				if (
					isAnyOf(t, PrimitiveType.NILLABLES) &&
					(offendingExpression instanceof IntegerLiteral && ((IntegerLiteral)offendingExpression).longValue() == 0)
				)
					replacements.add(
						Messages.C4ScriptQuickAssistProcessor_Replace0WithNil,
						new AccessVar(Keywords.Nil),
						false, false
					);
				break;
			case NoSideEffects:
				if (topLevel instanceof SimpleStatement) {
					final SimpleStatement statement = (SimpleStatement) topLevel;
					replacements.add(
						Messages.ClonkQuickAssistProcessor_ConvertToReturn,
						new ReturnStatement((statement.expression()))
					);
					addRemoveReplacement(document, expressionRegion, replacements, func);
					final CallDeclaration callFunc = new CallDeclaration(Messages.ClonkQuickAssistProcessor_FunctionToBeCalled, statement.expression());
					replacements.add(Messages.ClonkQuickAssistProcessor_WrapWithFunctionCall, callFunc, callFunc);
				}
				break;
			case NoAssignment:
				if (topLevel instanceof SimpleStatement) {
					final SimpleStatement statement = (SimpleStatement) topLevel;

					if (statement.expression() instanceof BinaryOp) {
						final BinaryOp binaryOp = (BinaryOp) statement.expression();
						if (binaryOp.operator() == Operator.Equal && problemReporting.isModifiable(binaryOp.leftSide()))
							replacements.add(
									Messages.ClonkQuickAssistProcessor_ConvertComparisonToAssignment,
									new BinaryOp(Operator.Assign, binaryOp.leftSide(), binaryOp.rightSide())
							);
					}
				}
				break;
			case NoInheritedFunction:
				if (offendingExpression instanceof CallDeclaration && ((CallDeclaration)offendingExpression).name().equals(Keywords.Inherited))
					replacements.add(
						String.format(Messages.ClonkQuickAssistProcessor_UseInsteadOf, Keywords.SafeInherited, Keywords.Inherited),
						identifierReplacement((AccessDeclaration) offendingExpression, Keywords.SafeInherited),
						false, true
					);
				break;
			case ReturnAsFunction:
				if (offendingExpression instanceof Tuple) {
					final Tuple tuple = (Tuple) offendingExpression;
					final ASTNode[] elms = tuple.subElements();
					if (elms.length >= 2) {
						final ASTNode returnExpr = elms[0];
						final ASTNode[] rest = ArrayUtil.arrayRange(elms, 1, elms.length-1, ASTNode.class);
						final Statement[] statements = ArrayUtil.concat(SimpleStatement.wrapExpressions(rest), new ReturnStatement(returnExpr));
						Block reordered;
						if (tuple.parent().parent() instanceof ConditionalStatement && ((ConditionalStatement)tuple.parent().parent()).body() == tuple.parent())
							reordered = new Block(statements);
						else
							reordered = new BunchOfStatements(statements);
						reordered.setLocation(tuple.parent()); // return statement
						replacements.add(
							Messages.ClonkQuickAssistProcessor_RearrangeReturnStatement,
							reordered,
							false,
							true
						);
					}
				}
				break;
			case Unused:
				if (offendingExpression instanceof VarInitialization) {
					final MutableRegion regionToDelete = new MutableRegion(0, expressionRegion.getLength());
					final VarInitialization cur = (VarInitialization) offendingExpression;
					final VarInitialization next = cur.succeedingInitialization();
					final VarInitialization previous = cur.precedingInitialization();
					final String replacementString = ""; //$NON-NLS-1$
					if (next == null) {
						if (previous != null) {
							// removing last initialization -> change ',' before it to ';'
							parser.seek(previous.end());
							parser.eatWhitespace();
							if (parser.peek() == ',')
								regionToDelete.setStartAndEnd(parser.tell(), cur.end());
						} else {
							addRemoveReplacement(document, offendingExpression.parent(), replacements, func).setTitle(Messages.ClonkQuickAssistProcessor_RemoveVariableDeclaration);
							break;
						}
					} else
						regionToDelete.setStartAndEnd(cur.getOffset(), next.getOffset());
					replacements.add(
						Messages.ClonkQuickAssistProcessor_RemoveVariableDeclaration,
						new ReplacementStatement(replacementString, regionToDelete, document, expressionRegion.getOffset(), func.bodyLocation().getOffset()),
						false, true
					);
				}
				break;
			case Garbage:
				addRemoveReplacement(document, expressionRegion, replacements, func);
				break;
			case MemberOperatorWithTildeNoSpace:
				// just print out topLevel, space will be removed automatically
				replacements.add(Messages.ClonkQuickAssistProcessor_RemoveSpace, topLevel);
				break;
			default:
				break;
			}

			try {
				replacements.add(
					Messages.ClonkQuickAssistProcessor_TidyUp,
					topLevel.exhaustiveOptimize(problemReporting)
				);
			} catch (final CloneNotSupportedException e) {
				e.printStackTrace();
			}

			for (final Replacement replacement : replacements) {
				final String replacementAsString = "later"; //$NON-NLS-1$
				int offset = func.bodyLocation().getOffset();
				int length;
				if (replacement.regionToBeReplacedSpecifiedByReplacementExpression) {
					offset += replacement.replacementExpression().start();
					length = replacement.replacementExpression().getLength();
				} else {
					offset = expressionRegion.getOffset();
					if (replacement.replacementExpression() instanceof Statement)
						// if the replacement expression is a statement, replace the whole statement the erroneous expression resided in
						length = topLevel.getLength();
					else
						length = expressionRegion.getLength();
				}
				proposals.add(new ParameterizedProposal(null, replacementAsString, offset, length,
						replacementAsString.length(), null, replacement.title(), null, null, null, null,
						replacement, tabIndentation, parser, func));
			}
		}

	}

	private Replacement addRemoveReplacement(IDocument document, final IRegion expressionRegion, ReplacementsList replacements, Function func) {
		final Replacement result = replacements.add(
			Messages.ClonkQuickAssistProcessor_Remove,
			new ReplacementStatement("", expressionRegion, document, expressionRegion.getOffset(), func.bodyLocation().getOffset()), //$NON-NLS-1$
			false, true
		);
		return result;
	}

	@Override
	public String getErrorMessage() {
		return Messages.ClonkQuickAssistProcessor_FailedToFix;
	}

}
