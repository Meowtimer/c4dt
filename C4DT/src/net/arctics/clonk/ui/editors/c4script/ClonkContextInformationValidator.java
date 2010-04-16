package net.arctics.clonk.ui.editors.c4script;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationPresenter;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.widgets.Display;

public class ClonkContextInformationValidator implements
		IContextInformationPresenter, IContextInformationValidator {

	private IContextInformation fInformation;
	private ITextViewer fTextViewer;
	private int fCurrentParameter;
	private int fOffset;
	
	public void install(IContextInformation info, ITextViewer viewer, int offset) {
		fInformation = info;
		fTextViewer = viewer;
		fOffset = offset;
		fCurrentParameter = -1;
	}

	public boolean updatePresentation(int offset, TextPresentation presentation) {
		int currentParameter = 0;
		int restrictParmsIndex = Integer.MAX_VALUE;
		offset = fTextViewer.getSelectedRange().x;
		if (fInformation instanceof ClonkContextInformation) {
			ClonkContextInformation clonkInformation = (ClonkContextInformation) fInformation;
			if (!clonkInformation.valid(offset))
				return false;
			if (clonkInformation.getInformationDisplayString().endsWith("...")) //$NON-NLS-1$
				restrictParmsIndex = clonkInformation.getParmCount()-1;
		}
		try {
	        currentParameter = getCharCount(fTextViewer.getDocument(), fOffset, offset, ",", "", true); //$NON-NLS-1$//$NON-NLS-2$
	        currentParameter = Math.min(currentParameter, restrictParmsIndex);
	        //System.out.println(String.format("%d %d %d", fOffset, offset, currentParameter));
        } catch (BadLocationException e) {
	        e.printStackTrace();
        }

		if (fCurrentParameter != -1) {
			if (currentParameter == fCurrentParameter)
				return false;
		}

		presentation.clear();
		fCurrentParameter= currentParameter;

		if (fInformation == null)
			return false;
		String s= fInformation.getInformationDisplayString();
		int[] commas= computeCommaPositions(s);

		if (commas.length - 2 < fCurrentParameter) {
			presentation.addStyleRange(new StyleRange(0, s.length(), null, null, SWT.NORMAL));
			return true;
		}
		
		int start= commas[fCurrentParameter] + 1;
		int end= commas[fCurrentParameter + 1];
		if (start > 0)
			presentation.addStyleRange(new StyleRange(0, start, null, null, SWT.NORMAL));

		if (end > start)
			presentation.addStyleRange(new StyleRange(start, end - start, Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE), null, SWT.BOLD));

		if (end < s.length())
			presentation.addStyleRange(new StyleRange(end, s.length() - end, null, null, SWT.NORMAL));

		return true;
	}

	public boolean isContextInformationValid(int offset) {
		try {
			if (fInformation instanceof ClonkContextInformation && !((ClonkContextInformation) fInformation).valid(offset))
				return false;

			IDocument document= fTextViewer.getDocument();
			IRegion line= document.getLineInformationOfOffset(fOffset);

			if (offset < line.getOffset() || offset >= document.getLength())
				return false;

			return getCharCount(document, fOffset, offset, "(", ")", false) >= 0; //$NON-NLS-1$ //$NON-NLS-2$

		} catch (BadLocationException x) {
			return false;
		}
	}

	private int getCharCount(IDocument document, final int start, final int end, String increments, String decrements, boolean considerNesting) throws BadLocationException {

		Assert.isTrue((increments.length() != 0 || decrements.length() != 0) && !increments.equals(decrements));
		
		final int NONE= 0;
		final int BRACKET= 1;
		final int BRACE= 2;
		final int PAREN= 3;
		final int ANGLE= 4;

		int nestingMode= NONE;
		int nestingLevel= 0;

		int charCount= 0;
		int offset= start;
		while (offset < end) {
			char curr= document.getChar(offset++);
			switch (curr) {
				case '/':
					if (offset < end) {
						char next= document.getChar(offset);
						if (next == '*') {
							// a comment starts, advance to the comment end
							offset= getCommentEnd(document, offset + 1, end);
						} else if (next == '/') {
							// '//'-comment: nothing to do anymore on this line
							offset= end;
						}
					}
					break;
				case '*':
					if (offset < end) {
						char next= document.getChar(offset);
						if (next == '/') {
							// we have been in a comment: forget what we read before
							charCount= 0;
							++ offset;
						}
					}
					break;
				case '"':
				case '\'':
					offset= getStringEnd(document, offset, end, curr);
					break;
				case '[':
					if (considerNesting) {
						if (nestingMode == BRACKET || nestingMode == NONE) {
							nestingMode= BRACKET;
							nestingLevel++;
						}
						break;
					}
				case ']':
					if (considerNesting) {
						if (nestingMode == BRACKET)
							if (--nestingLevel == 0)
								nestingMode= NONE;
						break;
					}
				case '(':
					if (considerNesting) {
						if (nestingMode == ANGLE) {
							// generics heuristic failed
							nestingMode=PAREN;
							nestingLevel= 1;
						}
						if (nestingMode == PAREN || nestingMode == NONE) {
							nestingMode= PAREN;
							nestingLevel++;
						}
						break;
					}
				case ')':
					if (considerNesting) {
						if (nestingMode == PAREN)
							if (--nestingLevel == 0)
								nestingMode= NONE;
						break;
					}
				case '{':
					if (considerNesting) {
						if (nestingMode == ANGLE) {
							// generics heuristic failed
							nestingMode=BRACE;
							nestingLevel= 1;
						}
						if (nestingMode == BRACE || nestingMode == NONE) {
							nestingMode= BRACE;
							nestingLevel++;
						}
						break;
					}
				case '}':
					if (considerNesting) {
						if (nestingMode == BRACE)
							if (--nestingLevel == 0)
								nestingMode= NONE;
						break;
					}

				default:
					if (nestingLevel != 0)
						continue;

					if (increments.indexOf(curr) >= 0) {
						++ charCount;
					}

					if (decrements.indexOf(curr) >= 0) {
						-- charCount;
					}
			}
		}

		return charCount;
	}
	
	private int getCommentEnd(IDocument d, int pos, int end) throws BadLocationException {
		while (pos < end) {
			char curr= d.getChar(pos);
			pos++;
			if (curr == '*') {
				if (pos < end && d.getChar(pos) == '/') {
					return pos + 1;
				}
			}
		}
		return end;
	}

	private int getStringEnd(IDocument d, int pos, int end, char ch) throws BadLocationException {
		while (pos < end) {
			char curr= d.getChar(pos);
			pos++;
			if (curr == '\\') {
				// ignore escaped characters
				pos++;
			} else if (curr == ch) {
				return pos;
			}
		}
		return end;
	}
	
	private int[] computeCommaPositions(String code) {
		final int length= code.length();
	    int pos= 0;
		List<Integer> positions= new ArrayList<Integer>();
		positions.add(new Integer(-1));
		while (pos < length && pos != -1) {
			char ch= code.charAt(pos);
			switch (ch) {
	            case ',':
		            positions.add(Integer.valueOf(pos));
		            break;
	            case '<':
	            	pos= code.indexOf('>', pos);
	            	break;
	            case '[':
	            	pos= code.indexOf(']', pos);
	            	break;
	            default:
	            	break;
            }
			if (pos != -1)
				pos++;
		}
		positions.add(Integer.valueOf(length));
		
		int[] fields= new int[positions.size()];
		for (int i= 0; i < fields.length; i++)
	        fields[i]= ((Integer) positions.get(i)).intValue();
	    return fields;
    }
	
}
