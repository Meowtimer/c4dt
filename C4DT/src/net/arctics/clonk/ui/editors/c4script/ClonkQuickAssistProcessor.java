package net.arctics.clonk.ui.editors.c4script;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.c4script.C4Function;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.c4script.C4ScriptOperator;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.C4ScriptParser.IMarkerListener;
import net.arctics.clonk.parser.c4script.C4Type;
import net.arctics.clonk.parser.c4script.C4Variable.C4VariableScope;
import net.arctics.clonk.parser.c4script.Keywords;
import net.arctics.clonk.parser.c4script.ast.AccessDeclaration;
import net.arctics.clonk.parser.c4script.ast.AccessVar;
import net.arctics.clonk.parser.c4script.ast.BinaryOp;
import net.arctics.clonk.parser.c4script.ast.BunchOfStatements;
import net.arctics.clonk.parser.c4script.ast.CallFunc;
import net.arctics.clonk.parser.c4script.ast.Comment;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.ReturnStatement;
import net.arctics.clonk.parser.c4script.ast.Sequence;
import net.arctics.clonk.parser.c4script.ast.SimpleStatement;
import net.arctics.clonk.parser.c4script.ast.Statement;
import net.arctics.clonk.parser.c4script.ast.StringLiteral;
import net.arctics.clonk.parser.c4script.ast.Tuple;
import net.arctics.clonk.parser.c4script.ast.VarDeclarationStatement;
import net.arctics.clonk.ui.editors.ClonkCompletionProposal;
import net.arctics.clonk.ui.editors.ClonkTextEditor;
import net.arctics.clonk.util.ArrayHelpers;
import net.arctics.clonk.util.UI;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.texteditor.MarkerAnnotation;

import java.util.regex.Pattern;

/**
 * Bis jetzt keine Funktion
 * Until now!
 * @author ZokRadonh
 *
 */
public class ClonkQuickAssistProcessor implements IQuickAssistProcessor, IMarkerListener  {
	
	/*private class SpecifiableCallFunc extends CallFunc {
		@Override
		public String getDeclarationName() {
			if (declarationName == null) {
				ClonkQuickAssistProcessor.this.
				declarationName = UI.input(shell, title, prompt, defaultValue)
			}
		}
	}*/
	
	private static final ICompletionProposal[] NO_SUGGESTIONS=  new ICompletionProposal[0];

	public boolean canAssist(IQuickAssistInvocationContext invocationContext) {
		return true;
	}

	public boolean canFix(Annotation annotation) {
		if (annotation instanceof MarkerAnnotation) {
			MarkerAnnotation ma = (MarkerAnnotation)annotation;
			String ty;
			try {
				ty = ma.getMarker().getType();
			} catch (CoreException e) {
				e.printStackTrace();
				return false;
			}
			if (ty.equals(ClonkCore.MARKER_C4SCRIPT_ERROR) || ty.equals(ClonkCore.MARKER_C4SCRIPT_ERROR_WHILE_TYPING)) {
				return true;
			}
		}
		return false;
	}

	private boolean isAtPosition(int offset, Position pos) {
		return (pos != null) && (offset >= pos.getOffset() && offset <= (pos.getOffset() +  pos.getLength()));
	}
	
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
				if (isAtPosition(offset, pos)) {
					collectProposals(context.getSourceViewer(), (MarkerAnnotation) annotation, pos, proposals);
				}
			}
		}
		return proposals.toArray(new ICompletionProposal[proposals.size()]);
	}

	private int semicolonAdd = 0;
	
	@Override
	public WhatToDo markerEncountered(C4ScriptParser parser, ParserErrorCode code, int markerStart, int markerEnd, boolean noThrow, int severity, Object... args) {
		if (code == ParserErrorCode.TokenExpected && args[0].equals(";")) { //$NON-NLS-1$
			semicolonAdd = 1; // replace one more char (semicolon is there, just not in the substring denoted by the expressionRegion)
			return WhatToDo.DropCharges;
		}
		return WhatToDo.PassThrough;
	}
	
	private static class Replacement {
		private String title;
		private ExprElm replacementExpression;
		private ExprElm[] specifiable;
		public Replacement(String title, ExprElm replacementExpression, ExprElm... specifiable) {
			super();
			this.title = title;
			this.replacementExpression = replacementExpression;
			this.specifiable = specifiable;
		}
		public String getTitle() {
			return title;
		}
		public ExprElm getReplacementExpression() {
			return replacementExpression;
		}
		public ExprElm[] getSpecifiable() {
			return specifiable;
		}
		@Override
		public boolean equals(Object other) {
			if (other instanceof Replacement) {
				Replacement otherR = (Replacement) other;
				return otherR.title.equals(title) && otherR.replacementExpression.equals(replacementExpression);
			} else {
				return false;
			}
		}
	}
	
	private static final class ReplacementsList extends LinkedList<Replacement> {
		private static final long serialVersionUID = 1L;
		private ExprElm offending;
		public ReplacementsList(ExprElm offending) {
			super();
			this.offending = offending;
		}
		public void add(String replacement, ExprElm elm, boolean alwaysStatement, ExprElm... specifiable) {
			if (alwaysStatement && !(elm instanceof Statement)) {
				elm = new SimpleStatement(elm);
			}
			if (elm.getExprEnd() == elm.getExprStart() && offending != null) {
				elm.setExprRegion(offending.getExprStart(), offending.getExprEnd());
			}
			Replacement newOne = new Replacement(replacement, elm, specifiable);
			// don't add duplicates
			for (Replacement existing : this) {
				if (existing.equals(newOne))
					return;
			}
			this.add(newOne);
		}
		public void add(String replacement, ExprElm elm, ExprElm... specifiable) {
			add(replacement, elm, true, specifiable);
		}
	}
	
	private static ExprElm identifierReplacement(AccessDeclaration original, String newName) {
		AccessVar result = new AccessVar(newName);
		result.setExprRegion(original.getExprStart(), original.getExprStart()+original.getIdentifierLength());
		return result;
	}
	
	private void collectProposals(final ISourceViewer sourceViewer, MarkerAnnotation annotation, Position position, List<ICompletionProposal> proposals) {

		IMarker marker = annotation.getMarker();
		ParserErrorCode errorCode = ParserErrorCode.getErrorCode(marker);
		IRegion expressionRegion = ParserErrorCode.getExpressionLocation(marker);
		if (expressionRegion.getOffset() == -1)
			return;
		C4ScriptEditor editor = ClonkTextEditor.getEditorForSourceViewer(sourceViewer, C4ScriptEditor.class);
		if (editor == null)
			return;
		C4ScriptBase script = editor.scriptBeingEdited();
		if (script == null)
			return;
		C4Function func = script.funcAt(position.getOffset());
		final int tabIndentation = BufferedScanner.getTabIndentation(sourceViewer.getDocument().get(), expressionRegion.getOffset());
		ExpressionLocator locator = new ExpressionLocator(position.getOffset()-expressionRegion.getOffset());
		semicolonAdd = 0;
		final C4ScriptParser parser = C4ScriptParser.reportExpressionsAndStatements(sourceViewer.getDocument(), expressionRegion, script, func, locator, this);
		ExprElm offendingExpression = locator.getExprAtRegion();
		Statement topLevel = offendingExpression != null ? offendingExpression.containingStatementOrThis() : null;
		if (offendingExpression == topLevel)
			semicolonAdd = 0;
		
		if (offendingExpression != null && topLevel != null) {
			ReplacementsList replacements = new ReplacementsList(offendingExpression);
			switch (errorCode) {
			case VariableCalled:
				assert(offendingExpression instanceof CallFunc);
				replacements.add(
					Messages.ClonkQuickAssistProcessor_RemoveBrackets,
					topLevel.replaceSubElement(offendingExpression, new AccessVar(((CallFunc)offendingExpression).getDeclarationName()))
				);
				break;
			case NeverReached: {
				String s = topLevel.toString();
				replacements.add(
					Messages.ClonkQuickAssistProcessor_CommentOutStatement,
					new Comment(topLevel.toString(), s.contains("\n")) //$NON-NLS-1$
				);
				break;
			}
			case NotFinished:
				replacements.add(
					Messages.ClonkQuickAssistProcessor_AddMissingSemicolon,
					topLevel // will be added by converting topLevel to string
				);
				semicolonAdd = 0; // it's really missing!
				break;
			case UndeclaredIdentifier:
				if (offendingExpression instanceof AccessVar && offendingExpression.getParent() instanceof BinaryOp) {
					AccessVar var = (AccessVar) offendingExpression;
					BinaryOp op = (BinaryOp) offendingExpression.getParent();
					if (topLevel == op.getParent() && op.getOperator() == C4ScriptOperator.Assign && op.getLeftSide() == offendingExpression) {
						replacements.add(
							Messages.ClonkQuickAssistProcessor_ConvertToVarDeclaration,
							new VarDeclarationStatement(var.getDeclarationName(), op.getRightSide(), C4VariableScope.VAR)
						);
					}
				}
				if (offendingExpression instanceof AccessDeclaration) {
					AccessDeclaration accessDec = (AccessDeclaration) offendingExpression;
					ExprElm expr;
					if (offendingExpression.getParent() instanceof Sequence) {
						Sequence sequence = (Sequence) offendingExpression.getParent();
						expr = sequence.sequenceWithElementsRemovedFrom(offendingExpression); 
					} else {
						expr = null;
					}
					List<ICompletionProposal> possible = C4ScriptCompletionProcessor.compuateProposalsForExpression(
							expr, func, parser, sourceViewer.getDocument()
					);
					for (ICompletionProposal p : possible) {
						if (p instanceof ClonkCompletionProposal) {
							ClonkCompletionProposal clonkProposal = (ClonkCompletionProposal) p;
							C4Declaration dec = clonkProposal.getDeclaration();
							if (dec == null || !accessDec.declarationClass().isAssignableFrom(dec.getClass()))
								continue;
							int similarity = Utilities.getSimilarity(dec.getName(), accessDec.getDeclarationName());
							if (similarity > 0) {
								// always create AccessVar and set its region such that only the identifier part of the AccessDeclaration object
								// will be replaced -> no unnecessary tidy-up of CallFunc parameters
								ExprElm repl = identifierReplacement(accessDec, dec.getName());
								replacements.add(String.format(Messages.ClonkQuickAssistProcessor_ReplaceWith, dec.getName()), repl, false);
							}
						}
					}
				}
				break;
			case IncompatibleTypes:
				if (C4Type.makeType(ParserErrorCode.getArg(marker, 0), true) == C4Type.STRING) {
					replacements.add(
						Messages.ClonkQuickAssistProcessor_QuoteExpression,
						new StringLiteral(offendingExpression.toString()),
						false
					);
				}
				break;
			case NoSideEffects:
				if (topLevel instanceof SimpleStatement) {
					SimpleStatement statement = (SimpleStatement) topLevel;
					replacements.add(
						Messages.ClonkQuickAssistProcessor_ConvertToReturn,
						new ReturnStatement((statement.getExpression()))
					);
					replacements.add(
						Messages.ClonkQuickAssistProcessor_Remove,
						Statement.NULL_STATEMENT
					);
					CallFunc callFunc = new CallFunc(Messages.ClonkQuickAssistProcessor_FunctionToBeCalled, statement.getExpression());
					replacements.add(Messages.ClonkQuickAssistProcessor_WrapWithFunctionCall, callFunc, callFunc);
				}
				break;
			case NoAssignment:
				if (topLevel instanceof SimpleStatement) {
					SimpleStatement statement = (SimpleStatement) topLevel;
					
					if (statement.getExpression() instanceof BinaryOp) {
						BinaryOp binaryOp = (BinaryOp) statement.getExpression();
						if (binaryOp.getOperator() == C4ScriptOperator.Equal && binaryOp.getLeftSide().modifiable(parser)) {
							replacements.add(
								Messages.ClonkQuickAssistProcessor_ConvertComparisonToAssignment,
								// FIXME: reusing original operands so be careful when continuing to use original expression
								new BinaryOp(C4ScriptOperator.Assign, binaryOp.getLeftSide(), binaryOp.getRightSide())
							);
						}
					}
				}
				break;
			case NoInheritedFunction:
				if (offendingExpression instanceof CallFunc && ((CallFunc)offendingExpression).getDeclarationName().equals(Keywords.Inherited)) {
					replacements.add(
							String.format(Messages.ClonkQuickAssistProcessor_UseInsteadOf, Keywords.SafeInherited, Keywords.Inherited),
							identifierReplacement((AccessDeclaration) offendingExpression, Keywords.SafeInherited),
							false
					);
				}
				break;
			case ReturnAsFunction:
				if (offendingExpression instanceof Tuple) {
					Tuple tuple = (Tuple) offendingExpression;
					ExprElm[] elms = tuple.getElements();
					if (elms.length >= 2) {
						ExprElm returnExpr = elms[0];
						ExprElm[] rest = Utilities.arrayRange(elms, 1, elms.length-1, ExprElm.class);
						replacements.add(
							Messages.ClonkQuickAssistProcessor_RearrangeReturnStatement,
							new BunchOfStatements(
								ArrayHelpers.concat(SimpleStatement.wrapExpressions(rest), new ReturnStatement(returnExpr))
							)
						);
					}
				}
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
				String replacementAsString;
				try {
					replacementAsString = "later";replacement.getReplacementExpression().optimize(parser).toString(tabIndentation+1); //$NON-NLS-1$
				} catch (CloneNotSupportedException e) {
					break;
				}
				int offset = expressionRegion.getOffset();
				int length;
				if (!(replacement.getReplacementExpression() instanceof Statement)) {
					offset += replacement.getReplacementExpression().getExprStart();
					length = replacement.getReplacementExpression().getLength();
				} else {
					length = expressionRegion.getLength()+semicolonAdd;
				}
				proposals.add(new ClonkCompletionProposal(
					null,
					replacementAsString, offset, length,
					replacementAsString.length(),
					null, replacement.getTitle(), null, null, null, editor
				) {
					@Override
					public void apply(IDocument document) {
						for (ExprElm spec : replacement.getSpecifiable()) {
							if (spec instanceof AccessDeclaration) {
								AccessDeclaration accessDec = (AccessDeclaration) spec;
								String s = UI.input(
									sourceViewer.getTextWidget().getShell(),
									Messages.ClonkQuickAssistProcessor_SpecifyValue,
									String.format(Messages.ClonkQuickAssistProcessor_SpecifyFormat, accessDec.getDeclarationName()), accessDec.getDeclarationName(),
									new IInputValidator() {
										private final Pattern validIdentifierPattern = Pattern.compile("[a-zA-Z_]\\w*"); //$NON-NLS-1$
										@Override
										public String isValid(String newText) {
											if (!validIdentifierPattern.matcher(newText).matches()) {
												return String.format(Messages.ClonkQuickAssistProcessor_NotAValidFunctionName, newText);
											} else {
												return null;
											}
										}
									}
								);
								if (s != null) {
									accessDec.setDeclarationName(s);
								}
							}
						}
						try {
							this.replacementString = replacement.getReplacementExpression().optimize(parser).toString(tabIndentation+1);
						} catch (CloneNotSupportedException e) {
							e.printStackTrace();
						}
						super.apply(document);
					}
				});
			}
		}

	}

	public String getErrorMessage() {
		return Messages.ClonkQuickAssistProcessor_FailedToFix;
	}

}
