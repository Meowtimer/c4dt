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

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.texteditor.MarkerAnnotation;

import net.arctics.clonk.Core;
import net.arctics.clonk.Problem;
import net.arctics.clonk.FileDocumentActions;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodePrinter;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ast.Sequence;
import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.Function.FunctionScope;
import net.arctics.clonk.c4script.Keywords;
import net.arctics.clonk.c4script.MutableRegion;
import net.arctics.clonk.c4script.Operator;
import net.arctics.clonk.c4script.ProblemReportingStrategy;
import net.arctics.clonk.c4script.ProblemReportingStrategy.Capabilities;
import net.arctics.clonk.c4script.Script;
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
import net.arctics.clonk.parser.Markers;
import net.arctics.clonk.ui.editors.DeclarationProposal;
import net.arctics.clonk.ui.editors.StructureTextEditor;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.Pair;
import net.arctics.clonk.util.StringUtil;

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
		final MarkerAnnotation markerAnnotation = as(annotation, MarkerAnnotation.class);
		return
			markerAnnotation != null &&
			Core.MARKER_C4SCRIPT_ERROR.equals(markerAnnotation.getType()) &&
			fixes.keySet().contains(Markers.problem(((MarkerAnnotation)annotation).getMarker()));
	}

	private static boolean isAtPosition(final int offset, final Position pos) {
		return (pos != null) && (offset >= pos.getOffset() && offset <= (pos.getOffset() + pos.getLength()));
	}

	@Override
	public ICompletionProposal[] computeQuickAssistProposals(final IQuickAssistInvocationContext context) {
		final int offset = context.getOffset();
		final List<ICompletionProposal> proposals = new LinkedList<ICompletionProposal>();
		final IAnnotationModel model = context.getSourceViewer().getAnnotationModel();
		if (model == null) {
			return NO_SUGGESTIONS;
		}
		@SuppressWarnings("unchecked")
		final Iterator<Annotation> iter = model.getAnnotationIterator();
		iter.forEachRemaining(annotation -> {
			if (canFix(annotation)) {
				final Position position = model.getPosition(annotation);
				if (isAtPosition(offset, position)) {
					collectProposals(((MarkerAnnotation) annotation).getMarker(),
						position, proposals, null,
						StructureTextEditor.getEditorForSourceViewer(context.getSourceViewer(), C4ScriptEditor.class)
					);
				}
			}
		});
		return proposals.toArray(new ICompletionProposal[proposals.size()]);
	}

	private static ASTNode identifierReplacement(final AccessDeclaration original, final String newName) {
		final AccessVar result = new AccessVar(newName);
		result.setLocation(original.start(), original.start()+original.identifierLength());
		return result;
	}

	public static final Pattern validIdentifierPattern = Pattern.compile("[a-zA-Z_]\\w*"); //$NON-NLS-1$

	private static String parameterNameFromExpression(final ASTNode expression, final int index) {
		final String exprString = expression.toString();
		final Matcher m = validIdentifierPattern.matcher(exprString);
		return m.matches() ? m.group() : "par"+index; //$NON-NLS-1$
	}

	public void collectProposals(final IMarker marker, final Position position, final List<ICompletionProposal> proposals, IDocument document, final C4ScriptEditor editor) {
		if (document == null) {
			document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
		}
		final ScriptEditingState state = editor.state();
		if (state != null) {
			for (final ProblemReportingStrategy s : state.problemReportingStrategies()) {
				if ((s.capabilities() & Capabilities.TYPING) != 0) {
					collectProposals(marker, position, proposals, document, editor.script());
				}
			}
		}
	}

	public void collectProposals(
		final IMarker marker,
		final Position position,
		final List<ICompletionProposal> proposals,
		final IDocument document,
		final Script script
	) {
		if (document != null) {
			internalCollectProposals(marker, position, proposals, document, script);
		} else {
			FileDocumentActions.performActionOnFileDocument(script.source(), connectedDocument -> {
				internalCollectProposals(marker, position, proposals, connectedDocument, script);
				return null;
			}, false);
		}
	}

	static Pair<Problem[], ProblemFixer> fix(ProblemFixer fixer, Problem... problems) {
		return Pair.pair(problems, fixer);
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
				if (site.topLevel == op.parent() && op.operator() == Operator.Assign && op.leftSide() == site.offendingExpression) {
					site.replacements.add(
						Messages.ClonkQuickAssistProcessor_ConvertToVarDeclaration,
						new VarDeclarationStatement(var.name(), op.rightSide(), Keywords.VarNamed.length()+1, Scope.VAR)
					);
				}
			}
			if (site.offendingExpression instanceof CallDeclaration) {
				if (site.offendingExpression.predecessor() instanceof MemberOperator && !((MemberOperator)site.offendingExpression.predecessor()).hasTilde()) {
					final MemberOperator opWithTilde = new MemberOperator(false, true, ((MemberOperator)site.offendingExpression.predecessor()).id(), 3);
					opWithTilde.setLocation(site.offendingExpression.predecessor());
					site.replacements.add(Messages.ClonkQuickAssistProcessor_UseTildeWithNoSpace, opWithTilde, false, true);
				}
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
				if (accessDec instanceof AccessVar) {
					decs.add(new Variable(Scope.LOCAL, accessDec.name()));
				} else {
					final CallDeclaration callFunc = (CallDeclaration) accessDec;
					final Function function = new Function(accessDec.name(), FunctionScope.PUBLIC);
					function.setParent(site.script);
					function.storeBody(new FunctionBody(function), "");
					decs.add(function);
					final List<Variable> parms = new ArrayList<Variable>(callFunc.params().length);
					int p = 0;
					for (final ASTNode parm : callFunc.params()) {
						parms.add(new Variable(parameterNameFromExpression(parm, ++p), site.script.typings().get(parm)));
					}
					function.setParameters(parms);
				}

				// gather proposals through ScriptCompletionProcessor and propose those with a similar name
				final ASTNode expr = site.offendingExpression.parent() instanceof Sequence
					? ((Sequence) site.offendingExpression.parent()).subSequenceUpTo(site.offendingExpression)
					: site.offendingExpression;
				final List<ICompletionProposal> possible = ScriptCompletionProcessor.computeProposalsForExpression
					(site.document, site.func, expr);
				
				ofType(possible.stream(), DeclarationProposal.class).forEach(proposal -> {
					final Declaration dec = proposal.declaration();
					if (dec == null || !accessDec.declarationClass().isAssignableFrom(dec.getClass())) {
						return;
					}
					final int similarity = StringUtil.similarityOf(dec.name(), accessDec.name());
					if (similarity > 0) {
						site.replacements.add(String.format(Messages.ClonkQuickAssistProcessor_ReplaceWith, dec.name()),
							identifierReplacement(accessDec, dec.name()), false, true);
					}
				});

				// propose adding projects to the referenced projects which contain a definition with a matching name
				if (accessDec.parent() instanceof CallDeclaration) {
					final Variable parm = ((CallDeclaration)accessDec.parent()).parmDefinitionForParmExpression(accessDec);
					final boolean isIDPar = parm != null && site.script.typing().compatible(parm.type(), PrimitiveType.ID);
					if (isIDPar) {
						final IProject p = site.script.resource().getProject();
						final List<IProject> referencedProjects = attempt(
							() -> asList(p.getReferencedProjects()), CoreException.class, exception -> exception.printStackTrace()
						);
						final ID defId = ID.get(accessDec.name());
						if (referencedProjects != null) {
							stream(ClonkProjectNature.clonkProjectsInWorkspace())
								.filter(proj -> referencedProjects.indexOf(proj) == -1)
								.map(ClonkProjectNature::get)
								.filter(nat -> nat.index().definitionsWithID(defId) != null && eq(nat.index().engine(), site.script.engine()))
								.forEach(nat -> {
									site.replacements.add(new Replacement(String.format(Messages.ClonkQuickAssistProcessor_AddProjectToReferencedProjects, nat.getProject().getName()), accessDec) {
										@Override
										public void performAdditionalActionsBeforeDoingReplacements() {
											final IProjectDescription desc = attempt(p::getDescription, CoreException.class, Exception::printStackTrace);
											if (desc != null) {
												try {
													desc.setReferencedProjects(ArrayUtil.concat(desc.getReferencedProjects(), nat.getProject()));
													p.setDescription(desc, null);
												} catch (final CoreException e) {
													e.printStackTrace();
												}
											}
										}
									});
								});
						}
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
			if (isAnyOf(t, PrimitiveType.NILLABLES) && ntgr != null && ntgr.longValue() == 0) {
				site.replacements.add(
					Messages.C4ScriptQuickAssistProcessor_Replace0WithNil,
					new AccessVar(Keywords.Nil),
					false, false
				);
			}
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
					if (binaryOp.operator() == Operator.Equal) {
						site.replacements.add(
							Messages.ClonkQuickAssistProcessor_ConvertComparisonToAssignment,
							new BinaryOp(Operator.Assign, binaryOp.leftSide(), binaryOp.rightSide())
						);
					}
				}
			}
		}),
		problems(Problem.NoInheritedFunction).fixedBy(site -> {
			if (site.offendingExpression instanceof CallDeclaration && ((CallDeclaration)site.offendingExpression).name().equals(Keywords.Inherited)) {
				site.replacements.add(
					String.format(Messages.ClonkQuickAssistProcessor_UseInsteadOf, Keywords.SafeInherited, Keywords.Inherited),
					identifierReplacement((AccessDeclaration) site.offendingExpression, Keywords.SafeInherited),
					false, true
				);
			}
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
				final VarInitialization varInitialization = (VarInitialization) site.offendingExpression;
				final VarInitialization next = varInitialization.succeedingInitialization();
				final VarInitialization previous = varInitialization.precedingInitialization();
				final String replacementString = ""; //$NON-NLS-1$
				if (next == null) {
					if (previous != null) {
						// removing last initialization -> change ',' before it to ';'
						site.parser.seek(previous.end());
						site.parser.eatWhitespace();
						if (site.parser.peek() == ',') {
							regionToDelete.setStartAndEnd(site.parser.tell(), varInitialization.end());
						}
					} else {
						site.addRemoveReplacement().setTitle(Messages.ClonkQuickAssistProcessor_RemoveVariableDeclaration);
					}
				} else {
					regionToDelete.setStartAndEnd(varInitialization.getOffset(), next.getOffset());
				}
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
		if (fixer != null) {
			fixer.contribute(site);
		}
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
