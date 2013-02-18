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
import org.eclipse.swt.widgets.Display;

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
		int fnNameLen = info.function().name().length();
		StyleRange fn = new StyleRange(0, fnNameLen,
			Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY), null, SWT.ITALIC);
		StyleRange highlightedParameter = new StyleRange(par.start(), par.end()-par.start(),
			Display.getCurrent().getSystemColor(SWT.COLOR_BLUE), null, SWT.BOLD);
		presentation.clear();
		for (StyleRange r : new StyleRange[] {fn, highlightedParameter})
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
