package net.arctics.clonk.ui.editors.c4script;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.c4script.C4Function;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.c4script.ast.AccessVar;
import net.arctics.clonk.parser.c4script.ast.CallFunc;
import net.arctics.clonk.parser.c4script.ast.Comment;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.SimpleStatement;
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
public class ClonkQuickAssistProcessor implements IQuickAssistProcessor  {
	
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
		
		ExpressionLocator locator = new ExpressionLocator(position.getOffset()-expressionRegion.getOffset());
		C4ScriptParser.reportExpressionsAndStatements(sourceViewer.getDocument(), expressionRegion, script, func, locator, null);
		ExprElm offendingExpression = locator.getExprAtRegion();
		ExprElm topLevel = offendingExpression != null && offendingExpression.getParent() != null ? offendingExpression.getParent() : locator.getTopLevelInRegion();
		if (offendingExpression != null && topLevel != null) {
			ExprElm replacement = null;
			String msg = null;
			switch (errorCode) {
			case VariableCalled:
				assert(offendingExpression instanceof CallFunc);
				topLevel.replaceSubElement(offendingExpression, replacement = new AccessVar(((CallFunc)offendingExpression).getDeclarationName()));
				msg = Messages.ClonkQuickAssistProcessor_RemoveBrackets;
				break;
			case NeverReached: {
				String s = offendingExpression.toString();
				replacement = new SimpleStatement(new Comment(offendingExpression.toString(), s.contains("\n"))); //$NON-NLS-1$
				msg = Messages.ClonkQuickAssistProcessor_CommentOutStatement;
				break;
			}
			}
			if (replacement != null) {
				String replacementAsString = replacement.toString();
				proposals.add(new ClonkCompletionProposal(null, replacementAsString, position.getOffset(), position.getLength(), replacementAsString.length(), null, msg, null, null, null, editor));
			}
		}

	}

	public String getErrorMessage() {
		return Messages.ClonkQuickAssistProcessor_FailedToFix;
	}

}
