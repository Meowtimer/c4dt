package net.arctics.clonk.ui.editors.c4script;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.c4script.ast.ExprWriter;
import net.arctics.clonk.parser.c4script.ast.Statement;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

public class ReplacementStatement extends Statement {
	private final String replacementString;
	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	public ReplacementStatement(String replacementString) {
		this.replacementString = replacementString;
	}
	
	public ReplacementStatement(String replacementString, IRegion regionToDelete, IDocument document, int absoluteOffset, boolean attemptToAdvanceRegion) {
		this(replacementString);
		if (attemptToAdvanceRegion) {
			possiblyAdvanceRegionToDeleteLine(document, regionToDelete, absoluteOffset);
		}
	}
	
	private void possiblyAdvanceRegionToDeleteLine(IDocument document, IRegion expressionRegion, int absoluteOffset) {
		int exprStart = expressionRegion.getOffset()-absoluteOffset;
		int additionalExprLength = 0;
		try {
			IRegion originalLineRegion = document.getLineInformationOfOffset(expressionRegion.getOffset());
			String originalLine = document.get(originalLineRegion.getOffset(), originalLineRegion.getLength());
			boolean deleteLine = true;
			for (int i = expressionRegion.getOffset()-originalLineRegion.getOffset()-1; i >= 0; i--) {
				if (!BufferedScanner.isWhiteSpace(originalLine.charAt(i))) {
					deleteLine = false;
					break;
				}
			}
			for (int i = expressionRegion.getOffset()+expressionRegion.getLength()+1-originalLineRegion.getOffset(); i < originalLine.length(); i++) {
				if (!BufferedScanner.isWhiteSpace(originalLine.charAt(i))) {
					deleteLine = false;
					break;
				}
			}
			if (deleteLine) {
				exprStart = originalLineRegion.getOffset()-expressionRegion.getOffset();
				for (int abs = originalLineRegion.getOffset()-1; abs >= 0 && BufferedScanner.isWhiteSpace(document.getChar(abs)); abs--) {
					exprStart--;
				}
				additionalExprLength = originalLineRegion.getOffset()+originalLine.length()-expressionRegion.getOffset()-expressionRegion.getLength();
			}
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		setExprRegion(exprStart, (exprStart < 0 ? 0 : exprStart)+expressionRegion.getLength()+additionalExprLength);
	}

	@Override
	public void doPrint(ExprWriter output, int depth) {
		output.append(replacementString);
	}
}