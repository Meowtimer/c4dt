package net.arctics.clonk.ui.editors.c4script;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.c4script.C4Function;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.c4script.C4ScriptOperator;
import net.arctics.clonk.parser.c4script.C4Type;
import net.arctics.clonk.parser.c4script.C4ScriptParser.IMarkerListener;
import net.arctics.clonk.parser.c4script.C4Variable.C4VariableScope;
import net.arctics.clonk.parser.c4script.ast.AccessVar;
import net.arctics.clonk.parser.c4script.ast.BinaryOp;
import net.arctics.clonk.parser.c4script.ast.CallFunc;
import net.arctics.clonk.parser.c4script.ast.Comment;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.ReturnStatement;
import net.arctics.clonk.parser.c4script.ast.SimpleStatement;
import net.arctics.clonk.parser.c4script.ast.Statement;
import net.arctics.clonk.parser.c4script.ast.StringLiteral;
import net.arctics.clonk.parser.c4script.ast.VarDeclarationStatement;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.ui.editors.ClonkCompletionProposal;
import net.arctics.clonk.ui.editors.ClonkTextEditor;
import net.arctics.clonk.util.Pair;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.texteditor.MarkerAnnotation;

/**
 * Bis jetzt keine Funktion
 * Until now!
 * @author ZokRadonh
 *
 */
public class ClonkQuickAssistProcessor implements IQuickAssistProcessor, IMarkerListener  {
	
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
	
	private static class ReplacementsList extends LinkedList<Pair<String, ExprElm>> {
		private static final long serialVersionUID = 1L;
		public void add(String replacement, ExprElm elm) {
			if (!(elm instanceof Statement)) {
				elm = new SimpleStatement(elm);
			}
			this.add(new Pair<String, ExprElm>(replacement, elm));
		}
	}
	
	private void collectProposals(ISourceViewer sourceViewer, MarkerAnnotation annotation, Position position, List<ICompletionProposal> proposals) {

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
		int tabIndentation = BufferedScanner.getTabIndentation(sourceViewer.getDocument().get(), expressionRegion.getOffset());
		ExpressionLocator locator = new ExpressionLocator(position.getOffset()-expressionRegion.getOffset());
		semicolonAdd = 0;
		C4ScriptParser parser = C4ScriptParser.reportExpressionsAndStatements(sourceViewer.getDocument(), expressionRegion, script, func, locator, this);
		ExprElm offendingExpression = locator.getExprAtRegion();
		Statement topLevel = offendingExpression != null ? offendingExpression.containingStatementOrThis() : null;
		if (offendingExpression == topLevel)
			semicolonAdd = 0;
		
		
		if (offendingExpression != null && topLevel != null) {
			ReplacementsList replacements = new ReplacementsList();
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
				break;
			case IncompatibleTypes:
				if (C4Type.makeType(ParserErrorCode.getArg(marker, 0), true) == C4Type.STRING) {
					replacements.add(
						Messages.ClonkQuickAssistProcessor_QuoteExpression,
						offendingExpression.getParent().replaceSubElement(offendingExpression, new StringLiteral(offendingExpression.toString()))
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
			}
			for (Pair<String, ExprElm> replacement : replacements) {
				String replacementAsString;
				try {
					replacementAsString = replacement.getSecond().optimize(parser).toString(tabIndentation+1);
				} catch (CloneNotSupportedException e) {
					return;
				}
				proposals.add(new ClonkCompletionProposal(
					null,
					replacementAsString, expressionRegion.getOffset(), expressionRegion.getLength()+semicolonAdd,
					replacementAsString.length(),
					null, replacement.getFirst(), null, null, null, editor
				));
			}
		}

	}

	public String getErrorMessage() {
		return Messages.ClonkQuickAssistProcessor_FailedToFix;
	}

}
