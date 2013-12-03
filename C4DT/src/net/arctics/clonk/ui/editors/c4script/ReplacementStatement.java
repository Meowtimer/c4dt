package net.arctics.clonk.ui.editors.c4script;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNodePrinter;
import net.arctics.clonk.c4script.ast.Statement;
import net.arctics.clonk.parser.BufferedScanner;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

/**
 * A statement to insert some arbitrary string into an AST.
 * @author madeen
 *
 */
public class ReplacementStatement extends Statement {
	private final String replacementString;
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	/**
	 * Create a new ReplacementStatement with a replacement string.
	 * @param replacementString The replacement string
	 */
	public ReplacementStatement(final String replacementString) {
		this.replacementString = replacementString;
	}
	
	public ReplacementStatement(final String replacementString, final IRegion relativeRegion, final IDocument document, final int relativeOffset, final int offsetToAbsolute) {
		this(replacementString);
		advanceRegionToDeleteLine(document, relativeRegion, relativeOffset, offsetToAbsolute);
	}
	
	private void advanceRegionToDeleteLine(final IDocument document, final IRegion relativeExpressionRegion, final int relativeOffset, final int offsetToAbsolute) {
		int exprStart = relativeExpressionRegion.getOffset();
		int additionalExprLength = 0;
		try {
			final IRegion originalLineRegion = document.getLineInformationOfOffset(offsetToAbsolute+relativeExpressionRegion.getOffset());
			final String originalLine = document.get(originalLineRegion.getOffset(), originalLineRegion.getLength());
			boolean deleteLine = true;
			for (int i = relativeExpressionRegion.getOffset()-originalLineRegion.getOffset()-1+offsetToAbsolute; i >= 0; i--)
				if (!BufferedScanner.isWhiteSpace(originalLine.charAt(i))) {
					deleteLine = false;
					break;
				}
			for (int i = relativeExpressionRegion.getOffset()+relativeExpressionRegion.getLength()+1-originalLineRegion.getOffset()+offsetToAbsolute; i < originalLine.length(); i++)
				if (!BufferedScanner.isWhiteSpace(originalLine.charAt(i))) {
					deleteLine = false;
					break;
				}
			if (deleteLine) {
				int newExprStart = originalLineRegion.getOffset()-offsetToAbsolute;
				for (int abs = originalLineRegion.getOffset()-1; abs >= 0 && BufferedScanner.isWhiteSpace(document.getChar(abs)); abs--)
					newExprStart--;
				additionalExprLength = exprStart - newExprStart;
				exprStart = newExprStart;
			}
		} catch (final BadLocationException e) {
			e.printStackTrace();
		}
		setLocation(exprStart, (exprStart < 0 ? 0 : exprStart)+relativeExpressionRegion.getLength()+additionalExprLength);
	}

	@Override
	public void doPrint(final ASTNodePrinter output, final int depth) {
		output.append(replacementString);
	}
}