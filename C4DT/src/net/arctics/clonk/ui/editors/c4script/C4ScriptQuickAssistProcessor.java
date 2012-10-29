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
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.ID;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.C4ScriptParser.VisitCodeFlavour;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.Function.FunctionScope;
import net.arctics.clonk.parser.c4script.Keywords;
import net.arctics.clonk.parser.c4script.MutableRegion;
import net.arctics.clonk.parser.c4script.Operator;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.Variable.Scope;
import net.arctics.clonk.parser.c4script.ast.AccessDeclaration;
import net.arctics.clonk.parser.c4script.ast.AccessVar;
import net.arctics.clonk.parser.c4script.ast.BinaryOp;
import net.arctics.clonk.parser.c4script.ast.BunchOfStatements;
import net.arctics.clonk.parser.c4script.ast.CallDeclaration;
import net.arctics.clonk.parser.c4script.ast.Comment;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.LongLiteral;
import net.arctics.clonk.parser.c4script.ast.MemberOperator;
import net.arctics.clonk.parser.c4script.ast.ReturnStatement;
import net.arctics.clonk.parser.c4script.ast.Sequence;
import net.arctics.clonk.parser.c4script.ast.SimpleStatement;
import net.arctics.clonk.parser.c4script.ast.Statement;
import net.arctics.clonk.parser.c4script.ast.StringLiteral;
import net.arctics.clonk.parser.c4script.ast.Tuple;
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
	
	public static C4ScriptQuickAssistProcessor getSingleton() {
		return singleton;
	}
	
	public C4ScriptQuickAssistProcessor() {
		super();
		assert(singleton == null);
		singleton = this;
	}

	@Override
	public boolean canAssist(IQuickAssistInvocationContext invocationContext) {
		return true;
	}

	private static final Set<ParserErrorCode> fixableParserErrorCodes = ArrayUtil.set(
		ParserErrorCode.VariableCalled,
		ParserErrorCode.NeverReached,
		ParserErrorCode.NotFinished,
		ParserErrorCode.UndeclaredIdentifier,
		ParserErrorCode.IncompatibleTypes,
		ParserErrorCode.NoSideEffects,
		ParserErrorCode.NoAssignment,
		ParserErrorCode.NoInheritedFunction,
		ParserErrorCode.ReturnAsFunction,
		ParserErrorCode.Unused,
		ParserErrorCode.Garbage,
		ParserErrorCode.MemberOperatorWithTildeNoSpace,
		ParserErrorCode.KeywordInWrongPlace
	);
 	
	@Override
	public boolean canFix(Annotation annotation) {
		if (annotation instanceof MarkerAnnotation) {
			MarkerAnnotation ma = (MarkerAnnotation)annotation;
			String ty;
			try {
				ty = ma.getMarker().getType();
			} catch (CoreException e) {
				return false;
			}
			if (ty.equals(Core.MARKER_C4SCRIPT_ERROR) || ty.equals(Core.MARKER_C4SCRIPT_ERROR_WHILE_TYPING))
				return fixableParserErrorCodes.contains(ParserErrorCode.errorCode(ma.getMarker()));
		}
		return false;
	}

	private static boolean isAtPosition(int offset, Position pos) {
		return (pos != null) && (offset >= pos.getOffset() && offset <= (pos.getOffset() +  pos.getLength()));
	}
	
	@Override
	public ICompletionProposal[] computeQuickAssistProposals(IQuickAssistInvocationContext context) {
		int offset = context.getOffset();
		List<ICompletionProposal> proposals = new LinkedList<ICompletionProposal>();
		IAnnotationModel model = context.getSourceViewer().getAnnotationModel();
		if (model == null)
			return NO_SUGGESTIONS;
		@SuppressWarnings("rawtypes")
		Iterator iter = model.getAnnotationIterator();
		while (iter.hasNext()) {
			Annotation annotation = (Annotation)iter.next();
			if (canFix(annotation)) {
				Position pos = model.getPosition(annotation);
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
				ParserErrorCode.errorCode(marker) == ParserErrorCode.errorCode(originalMarker);
		}
		
		@Override
		public IMarker[] findOtherMarkers(IMarker[] markers) {
			List<IMarker> result = new ArrayList<IMarker>(markers.length);
			for (IMarker m : markers)
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
		public void apply(IDocument document) {
			replacement.performAdditionalActionsBeforeDoingReplacements();
			ExprElm replacementExpr = replacement.replacementExpression();
			if (replacementExpr != ExprElm.NULL_EXPR) {
				for (ExprElm spec : replacement.specifiable())
					if (spec instanceof AccessDeclaration) {
						AccessDeclaration accessDec = (AccessDeclaration) spec;
						String s = UI.input(
								PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
								Messages.ClonkQuickAssistProcessor_SpecifyValue,
								String.format(Messages.ClonkQuickAssistProcessor_SpecifyFormat, accessDec.declarationName()), accessDec.declarationName(),
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
							accessDec.setDeclarationName(s);
					}
				try {
					this.replacementString = replacement.replacementExpression().optimize(parser).toString(tabIndentation+1);
				} catch (CloneNotSupportedException e) {
					e.printStackTrace();
				}
			} else {
				// don't replace expression
				replacementString = ""; //$NON-NLS-1$
				replacementLength = 0;
			}
			cursorPosition = replacementString.length();
			super.apply(document);
			
			for (Replacement.AdditionalDeclaration dec : replacement.additionalDeclarations()) {
				StringBuilder builder = new StringBuilder(50);
				dec.declaration.sourceCodeRepresentation(builder, dec.code);
				builder.append("\n"); //$NON-NLS-1$
				builder.append("\n"); //$NON-NLS-1$
				try {
					document.replace(func.header().getOffset(), 0, builder.toString());
				} catch (BadLocationException e) {
					e.printStackTrace();
				}
			}
			
			C4ScriptEditor.TextChangeListener listener = C4ScriptEditor.TextChangeListener.listenerFor(document);
			if (listener != null)
				listener.scheduleReparsing(false);
		}

		public void runOnMarker(final IMarker marker) {
			try {
				Core.instance().performActionsOnFileDocument((IFile)marker.getResource(), new IDocumentAction<Object>() {
					@Override
					public Object run(IDocument document) {
						replacementOffset = marker.getAttribute(IMarker.CHAR_START, replacementOffset);
						replacementLength = marker.getAttribute(IMarker.CHAR_END, replacementOffset+replacementLength)-replacementOffset;
						apply(document);
						return null;
					}
				});
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}

		public Replacement getReplacement() {
			return replacement;
		}
	}

	private static class Replacement {
		public static class AdditionalDeclaration {
			public Declaration declaration;
			public ExprElm code;
			public AdditionalDeclaration(Declaration declaration, ExprElm code) {
				super();
				this.declaration = declaration;
				this.code = code;
			}
		}
		private String title;
		private final ExprElm replacementExpression;
		private final ExprElm[] specifiable;
		private final List<AdditionalDeclaration> additionalDeclarations = new LinkedList<AdditionalDeclaration>();
		private boolean regionToBeReplacedSpecifiedByReplacementExpression; // yes!
		public Replacement(String title, ExprElm replacementExpression, ExprElm... specifiable) {
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
		public ExprElm replacementExpression() {
			return replacementExpression;
		}
		public ExprElm[] specifiable() {
			return specifiable;
		}
		public List<AdditionalDeclaration> additionalDeclarations() {
			return additionalDeclarations;
		}
		@Override
		public boolean equals(Object other) {
			if (other instanceof Replacement) {
				Replacement otherR = (Replacement) other;
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
		private final ExprElm offending;
		private final List<ICompletionProposal> existingList;
		public ReplacementsList(ExprElm offending, List<ICompletionProposal> existingList) {
			super();
			this.offending = offending;
			this.existingList = existingList;
		}
		public Replacement add(String replacement, ExprElm elm, boolean alwaysStatement, ExprElm... specifiable) {
			if (alwaysStatement && !(elm instanceof Statement))
				elm = new SimpleStatement(elm);
			if (elm.end() == elm.start() && offending != null)
				elm.setExprRegion(offending.start(), offending.end());
			Replacement newOne = new Replacement(replacement, elm, specifiable);
			// don't add duplicates
			for (Replacement existing : this)
				if (existing.equals(newOne))
					return existing;
			for (ICompletionProposal prop : existingList)
				if (prop instanceof ParameterizedProposal && ((ParameterizedProposal)prop).createdFrom(newOne))
					return ((ParameterizedProposal)prop).getReplacement();
			this.add(newOne);
			return newOne;
		}
		public Replacement add(String replacement, ExprElm elm, ExprElm... specifiable) {
			return add(replacement, elm, true, specifiable);
		}
	}
	
	private static ExprElm identifierReplacement(AccessDeclaration original, String newName) {
		AccessVar result = new AccessVar(newName);
		result.setExprRegion(original.start(), original.start()+original.identifierLength());
		return result;
	}
	
	private static final Pattern validIdentifierPattern = Pattern.compile("[a-zA-Z_]\\w*"); //$NON-NLS-1$
	
	private static String parmNameFromExpression(ExprElm expression, int index) {
		String exprString = expression.toString();
		Matcher m = validIdentifierPattern.matcher(exprString);
		if (m.matches())
			return m.group();
		else
			return "par"+index; //$NON-NLS-1$
	}
	
	public void collectProposals(IMarker marker, Position position, List<ICompletionProposal> proposals, IDocument document, Object editorOrScript) {

		ParserErrorCode errorCode = ParserErrorCode.errorCode(marker);
		final IRegion expressionRegion = ParserErrorCode.expressionLocation(marker);
		if (expressionRegion.getOffset() == -1)
			return;
		C4ScriptEditor editor = editorOrScript instanceof C4ScriptEditor ? (C4ScriptEditor)editorOrScript : null;
		Script script = editorOrScript instanceof Script
			? (Script)editorOrScript
			: editor != null ? editor.script() : null;
		Object needToDisconnect = null;
		if (document == null)
			if (editor != null)
				document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
			else if (script != null && script.scriptStorage() instanceof IFile) {
				needToDisconnect = script.scriptStorage();
				try {
					Core.instance().textFileDocumentProvider().connect(needToDisconnect);
				} catch (CoreException e) {
					e.printStackTrace();
					return;
				}
				document = Core.instance().textFileDocumentProvider().getDocument(needToDisconnect);
			}
		try {
			if (script == null || document == null)
				return;
			Function func = script.funcAt(position.getOffset());
			final int tabIndentation = BufferedScanner.indentationOfStringAtPos(document.get(), func.bodyLocation().getOffset()+expressionRegion.getOffset(), BufferedScanner.TABINDENTATIONMODE);
			ExpressionLocator locator = new ExpressionLocator(position.getOffset()-func.bodyLocation().start());
			final C4ScriptParser parser = C4ScriptParser.visitCode(document, script, func, locator, null, VisitCodeFlavour.AlsoStatements, true);
			ExprElm offendingExpression = locator.expressionAtRegion();
			Statement topLevel = offendingExpression != null ? offendingExpression.containingStatementOrThis() : null;
			
			if (offendingExpression != null && topLevel != null) {
				ReplacementsList replacements = new ReplacementsList(offendingExpression, proposals);
				switch (errorCode) {
				case VariableCalled:
					assert(offendingExpression instanceof CallDeclaration);
					replacements.add(
						Messages.ClonkQuickAssistProcessor_RemoveBrackets,
						topLevel.replaceSubElement(offendingExpression, new AccessVar(((CallDeclaration)offendingExpression).declarationName()), 0)
					);
					break;
				case NeverReached: {
					String s = topLevel.toString();
					replacements.add(
						Messages.ClonkQuickAssistProcessor_CommentOutStatement,
						new Comment(topLevel.toString(), s.contains("\n"), false) //$NON-NLS-1$
					);
					addRemoveReplacement(document, expressionRegion, replacements, func);
					break;
				}
				case KeywordInWrongPlace:
					addRemoveReplacement(document, expressionRegion, replacements, func);
					break;
				case NotFinished:
					if (topLevel == offendingExpression || (topLevel instanceof SimpleStatement && offendingExpression == ((SimpleStatement)topLevel).expression()))
						replacements.add(
							Messages.ClonkQuickAssistProcessor_AddMissingSemicolon,
							topLevel // will be added by converting topLevel to string
						);
					break;
				case UndeclaredIdentifier:
					if (offendingExpression instanceof AccessVar && offendingExpression.parent() instanceof BinaryOp) {
						AccessVar var = (AccessVar) offendingExpression;
						BinaryOp op = (BinaryOp) offendingExpression.parent();
						if (topLevel == op.parent() && op.operator() == Operator.Assign && op.leftSide() == offendingExpression)
							replacements.add(
								Messages.ClonkQuickAssistProcessor_ConvertToVarDeclaration,
								new VarDeclarationStatement(var.declarationName(), op.rightSide(), Keywords.VarNamed.length()+1, Scope.VAR)
							);
					}
					if (offendingExpression instanceof CallDeclaration)
						if (offendingExpression.predecessorInSequence() instanceof MemberOperator && !((MemberOperator)offendingExpression.predecessorInSequence()).hasTilde()) {
							MemberOperator opWithTilde = new MemberOperator(false, true, ((MemberOperator)offendingExpression.predecessorInSequence()).getId(), 3);
							opWithTilde.setExprRegion(offendingExpression.predecessorInSequence());
							replacements.add(Messages.ClonkQuickAssistProcessor_UseTildeWithNoSpace, opWithTilde, false).regionToBeReplacedSpecifiedByReplacementExpression = true;
						}
					if (offendingExpression instanceof AccessDeclaration) {

						AccessDeclaration accessDec = (AccessDeclaration) offendingExpression;

						// create new variable or function
						Replacement createNewDeclarationReplacement = replacements.add(
							String.format(offendingExpression instanceof AccessVar ? Messages.ClonkQuickAssistProcessor_CreateLocalVar : Messages.ClonkQuickAssistProcessor_CreateLocalFunc, accessDec.declarationName()),
							ExprElm.NULL_EXPR,
							false
						);
						List<Replacement.AdditionalDeclaration> decs = createNewDeclarationReplacement.additionalDeclarations();
						if (accessDec instanceof AccessVar)
							decs.add(new Replacement.AdditionalDeclaration(
								new Variable(accessDec.declarationName(), Scope.LOCAL),
								ExprElm.NULL_EXPR
							));
						else {
							CallDeclaration callFunc = (CallDeclaration) accessDec;
							Function function;
							decs.add(new Replacement.AdditionalDeclaration(
								function = new Function(accessDec.declarationName(), FunctionScope.PUBLIC),
								ExprElm.NULL_EXPR
							));
							List<Variable> parms = new ArrayList<Variable>(callFunc.params().length);
							int p = 0;
							for (ExprElm parm : callFunc.params())
								parms.add(new Variable(parmNameFromExpression(parm, ++p), parm.type(parser)));
							function.setParameters(parms);
						}

						// gather proposals through ClonkCompletionProcessor and propose those with a similar name 
						ExprElm expr;
						if (offendingExpression.parent() instanceof Sequence) {
							Sequence sequence = (Sequence) offendingExpression.parent();
							expr = sequence.subSequenceUpTo(offendingExpression); 
						} else
							expr = null;
						List<ICompletionProposal> possible = C4ScriptCompletionProcessor.computeProposalsForExpression
							(expr, func, parser, document);
						for (ICompletionProposal p : possible)
							if (p instanceof ClonkCompletionProposal) {
								ClonkCompletionProposal clonkProposal = (ClonkCompletionProposal) p;
								Declaration dec = clonkProposal.getDeclaration();
								if (dec == null || !accessDec.declarationClass().isAssignableFrom(dec.getClass()))
									continue;
								int similarity = StringUtil.similarityOf(dec.name(), accessDec.declarationName());
								if (similarity > 0) {
									// always create AccessVar and set its region such that only the identifier part of the AccessDeclaration object
									// will be replaced -> no unnecessary tidy-up of CallFunc parameters
									ExprElm repl = identifierReplacement(accessDec, dec.name());
									replacements.add(String.format(Messages.ClonkQuickAssistProcessor_ReplaceWith, dec.name()), repl, false);
								}
							}
						
						// propose adding projects to the referenced projects which contain a definition with a matching name
						if (accessDec.parent() instanceof CallDeclaration) {
							Variable parm = ((CallDeclaration)accessDec.parent()).parmDefinitionForParmExpression(accessDec);
							if (parm != null && parm.type().canBeAssignedFrom(PrimitiveType.ID)) {
								final IProject p = marker.getResource().getProject();
								IProject[] referencedProjects;
								try {
									referencedProjects = p.getReferencedProjects();
								} catch (CoreException e) {
									e.printStackTrace();
									break;
								}
								ID defId = ID.get(accessDec.declarationName());
								for (final IProject proj : ClonkProjectNature.clonkProjectsInWorkspace())
									if (ArrayUtil.indexOf(proj, referencedProjects) == -1) {
										ClonkProjectNature nat = ClonkProjectNature.get(proj);
										if (nat.index().definitionsWithID(defId) != null)
											replacements.add(new Replacement(String.format(Messages.ClonkQuickAssistProcessor_AddProjectToReferencedProjects, nat.getProject().getName()), accessDec) {
												@Override
												public void performAdditionalActionsBeforeDoingReplacements() {
													IProjectDescription desc;
													try {
														desc = p.getDescription();
													} catch (CoreException e) {
														e.printStackTrace();
														return;
													}
													desc.setReferencedProjects(ArrayUtil.concat(desc.getReferencedProjects(), proj));
													try {
														p.setDescription(desc, null);
													} catch (CoreException e) {
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
					PrimitiveType t = PrimitiveType.fromString(ParserErrorCode.arg(marker, 0), true);
					if (t == PrimitiveType.STRING)
						replacements.add(
							Messages.ClonkQuickAssistProcessor_QuoteExpression,
							new StringLiteral(offendingExpression.toString()),
							false
						);
					if (
						isAnyOf(t, PrimitiveType.NILLABLES) &&
						(offendingExpression instanceof LongLiteral && ((LongLiteral)offendingExpression).longValue() == 0)
					)
						replacements.add(
							Messages.C4ScriptQuickAssistProcessor_Replace0WithNil,
							new AccessVar(Keywords.Nil),
							false
						);
					break;
				case NoSideEffects:
					if (topLevel instanceof SimpleStatement) {
						SimpleStatement statement = (SimpleStatement) topLevel;
						replacements.add(
							Messages.ClonkQuickAssistProcessor_ConvertToReturn,
							new ReturnStatement((statement.expression()))
						);
						addRemoveReplacement(document, expressionRegion, replacements, func);
						CallDeclaration callFunc = new CallDeclaration(Messages.ClonkQuickAssistProcessor_FunctionToBeCalled, statement.expression());
						replacements.add(Messages.ClonkQuickAssistProcessor_WrapWithFunctionCall, callFunc, callFunc);
					}
					break;
				case NoAssignment:
					if (topLevel instanceof SimpleStatement) {
						SimpleStatement statement = (SimpleStatement) topLevel;

						if (statement.expression() instanceof BinaryOp) {
							BinaryOp binaryOp = (BinaryOp) statement.expression();
							if (binaryOp.operator() == Operator.Equal && binaryOp.leftSide().isModifiable(parser))
								replacements.add(
										Messages.ClonkQuickAssistProcessor_ConvertComparisonToAssignment,
										new BinaryOp(Operator.Assign, binaryOp.leftSide(), binaryOp.rightSide())
								);
						}
					}
					break;
				case NoInheritedFunction:
					if (offendingExpression instanceof CallDeclaration && ((CallDeclaration)offendingExpression).declarationName().equals(Keywords.Inherited))
						replacements.add(
							String.format(Messages.ClonkQuickAssistProcessor_UseInsteadOf, Keywords.SafeInherited, Keywords.Inherited),
							identifierReplacement((AccessDeclaration) offendingExpression, Keywords.SafeInherited),
							false
						);
					break;
				case ReturnAsFunction:
					if (offendingExpression instanceof Tuple) {
						Tuple tuple = (Tuple) offendingExpression;
						ExprElm[] elms = tuple.subElements();
						if (elms.length >= 2) {
							ExprElm returnExpr = elms[0];
							ExprElm[] rest = ArrayUtil.arrayRange(elms, 1, elms.length-1, ExprElm.class);
							replacements.add(
								Messages.ClonkQuickAssistProcessor_RearrangeReturnStatement,
								new BunchOfStatements(
									ArrayUtil.concat(SimpleStatement.wrapExpressions(rest), new ReturnStatement(returnExpr))
								)
							);
						}
					}
					break;
				case Unused:
					if (offendingExpression instanceof VarInitialization) {
						final MutableRegion regionToDelete = new MutableRegion(0, expressionRegion.getLength());
						VarInitialization cur = (VarInitialization) offendingExpression;
						VarInitialization next = cur.succeedingInitialization();
						VarInitialization previous = cur.precedingInitialization();
						String replacementString = ""; //$NON-NLS-1$
						if (next == null) {
							if (previous != null) {
								// removing last initialization -> change ',' before it to ';'
								parser.seek(previous.end());
								parser.eatWhitespace();
								if (parser.peek() == ',')
									regionToDelete.setStartAndEnd(parser.tell(), cur.end());
							} else {
								addRemoveReplacement(document, expressionRegion, replacements, func).setTitle(Messages.ClonkQuickAssistProcessor_RemoveVariableDeclaration);
								break;
							}
						} else
							regionToDelete.setStartAndEnd(cur.getOffset(), next.getOffset());
						replacements.add(
							Messages.ClonkQuickAssistProcessor_RemoveVariableDeclaration,
							new ReplacementStatement(replacementString, regionToDelete, document, expressionRegion.getOffset(), func.bodyLocation().getOffset())
						).regionToBeReplacedSpecifiedByReplacementExpression = true;
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
						topLevel.exhaustiveOptimize(parser)
					);
				} catch (CloneNotSupportedException e) {
					e.printStackTrace();
				}

				for (final Replacement replacement : replacements) {
					String replacementAsString = "later"; //$NON-NLS-1$
					int offset = func.bodyLocation().getOffset();
					int length;
					if (replacement.regionToBeReplacedSpecifiedByReplacementExpression) {
						offset += replacement.replacementExpression().start();
						length = replacement.replacementExpression().getLength();
					} else {
						offset += expressionRegion.getOffset();
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
		} finally {
			if (needToDisconnect != null)
				Core.instance().textFileDocumentProvider().disconnect(needToDisconnect);
		}

	}

	private Replacement addRemoveReplacement(IDocument document, final IRegion expressionRegion, ReplacementsList replacements, Function func) {
		Replacement result = replacements.add(
			Messages.ClonkQuickAssistProcessor_Remove,
			new ReplacementStatement("", expressionRegion, document, expressionRegion.getOffset(), func.bodyLocation().getOffset()) //$NON-NLS-1$
		);
		result.regionToBeReplacedSpecifiedByReplacementExpression = true;
		return result;
	}

	@Override
	public String getErrorMessage() {
		return Messages.ClonkQuickAssistProcessor_FailedToFix;
	}

}
