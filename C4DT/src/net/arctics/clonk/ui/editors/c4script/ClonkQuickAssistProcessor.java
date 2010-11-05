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
import net.arctics.clonk.parser.c4script.ast.SimpleStatement;
import net.arctics.clonk.parser.c4script.ast.StringLiteral;
import net.arctics.clonk.parser.c4script.ast.VarDeclarationStatement;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.ui.editors.ClonkCompletionProposal;
import net.arctics.clonk.ui.editors.ClonkTextEditor;

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
			if (ty.equals(ClonkCore.MARKER_C4SCRIPT_ERROR)) {
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
	public WhatToDo markerEncountered(ParserErrorCode code, int markerStart, int markerEnd, boolean noThrow, int severity, Object... args) {
		if (code == ParserErrorCode.TokenExpected && args[0].equals(";")) { //$NON-NLS-1$
			semicolonAdd = 1; // replace one more char (semicolon is there, just not in the substring denoted by the expressionRegion)
			return WhatToDo.DropCharges;
		}
		return WhatToDo.PassThrough;
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
		ExprElm topLevel = offendingExpression != null ? offendingExpression.containingStatement() : null;
		if (offendingExpression != null && topLevel != null) {
			String msg = null;
			boolean success = false;
			switch (errorCode) {
			case VariableCalled:
				assert(offendingExpression instanceof CallFunc);
				topLevel.replaceSubElement(offendingExpression, new AccessVar(((CallFunc)offendingExpression).getDeclarationName()));
				msg = Messages.ClonkQuickAssistProcessor_RemoveBrackets;
				break;
			case NeverReached: {
				String s = offendingExpression.toString();
				topLevel = new SimpleStatement(new Comment(offendingExpression.toString(), s.contains("\n"))); //$NON-NLS-1$
				success = true;
				msg = Messages.ClonkQuickAssistProcessor_CommentOutStatement;
				break;
			}
			case StatementNotProperlyFinished:
				msg = Messages.ClonkQuickAssistProcessor_AddMissingSemicolon; // will be added by converting topLevel to string
				success = true;
				semicolonAdd = 0; // it's really missing!
				break;
			case UndeclaredIdentifier:
				if (offendingExpression instanceof AccessVar && offendingExpression.getParent() instanceof BinaryOp) {
					AccessVar var = (AccessVar) offendingExpression;
					BinaryOp op = (BinaryOp) offendingExpression.getParent();
					if (topLevel == op.getParent() && op.getOperator() == C4ScriptOperator.Assign && op.getLeftSide() == offendingExpression) {
						topLevel = new VarDeclarationStatement(var.getDeclarationName(), op.getRightSide(), C4VariableScope.VAR);
						success = true;
						msg = Messages.ClonkQuickAssistProcessor_ConvertToVarDeclaration;
					}
				}
				break;
			case IncompatibleTypes:
				if (C4Type.makeType(ParserErrorCode.getArg(marker, 0), true) == C4Type.STRING) {
					msg = Messages.ClonkQuickAssistProcessor_QuoteExpression;
					success = true;
					offendingExpression.getParent().replaceSubElement(offendingExpression, new StringLiteral(offendingExpression.toString()));
				}
				break;
			}
			if (success) {
				String replacementAsString;
				try {
					replacementAsString = topLevel.optimize(parser).toString(tabIndentation+1);
				} catch (CloneNotSupportedException e) {
					return;
				}
				proposals.add(new ClonkCompletionProposal(
					null,
					replacementAsString, expressionRegion.getOffset(), expressionRegion.getLength()+semicolonAdd,
					replacementAsString.length(),
					null, msg, null, null, null, editor
				));
			}
		}

	}

	public String getErrorMessage() {
		return Messages.ClonkQuickAssistProcessor_FailedToFix;
	}

}
