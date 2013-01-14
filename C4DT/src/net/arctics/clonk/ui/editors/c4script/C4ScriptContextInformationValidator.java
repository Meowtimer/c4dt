package net.arctics.clonk.ui.editors.c4script;

import static net.arctics.clonk.util.Utilities.as;
import net.arctics.clonk.parser.SourceLocation;

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

public class C4ScriptContextInformationValidator implements IContextInformationPresenter, IContextInformationValidator {

	private IContextInformation fInformation;
	private ITextViewer fTextViewer;
	private int fOffset;
	
	@Override
	public void install(IContextInformation info, ITextViewer viewer, int offset) {
		fInformation = info;
		fTextViewer = viewer;
		fOffset = offset;
	}

	@Override
	public boolean updatePresentation(int offset, TextPresentation presentation) {
		offset = fTextViewer.getSelectedRange().x;
		C4ScriptContextInformation info = as(fInformation, C4ScriptContextInformation.class);
		if (info == null)
			return false;
		if (!info.valid(offset))
			return false;
		SourceLocation par = info.currentParameterDisplayStringRange();
		if (par == null)
			return false;
		StyleRange start = new StyleRange(0, par.start(), null, null, SWT.NORMAL);
		StyleRange highlightedParameter = new StyleRange(par.start(), par.end()-par.start(), null, null, SWT.BOLD);
		StyleRange end = new StyleRange(par.end(), info.getInformationDisplayString().length() - par.end(), null, null, SWT.NORMAL);
		presentation.clear();
		for (StyleRange r : new StyleRange[] {start, highlightedParameter, end})
			presentation.addStyleRange(r);
		return true;
	}

	@Override
	public boolean isContextInformationValid(int offset) {
		try {
			if (fInformation instanceof C4ScriptContextInformation && !((C4ScriptContextInformation) fInformation).valid(offset))
				return false;
			IDocument document = fTextViewer.getDocument();
			IRegion line = document.getLineInformationOfOffset(fOffset);
			if (offset < line.getOffset() || offset >= document.getLength())
				return false;
			return true;
		} catch (BadLocationException x) {
			return false;
		}
	}
	
}
