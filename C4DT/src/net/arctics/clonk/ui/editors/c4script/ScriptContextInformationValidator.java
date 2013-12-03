package net.arctics.clonk.ui.editors.c4script;

import static net.arctics.clonk.util.Utilities.as;
import net.arctics.clonk.ast.SourceLocation;

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

public class ScriptContextInformationValidator implements IContextInformationPresenter, IContextInformationValidator {

	private IContextInformation fInformation;
	private ITextViewer fTextViewer;
	private int fOffset;

	@Override
	public void install(final IContextInformation info, final ITextViewer viewer, final int offset) {
		fInformation = info;
		fTextViewer = viewer;
		fOffset = offset;
	}

	@Override
	public boolean updatePresentation(int offset, final TextPresentation presentation) {
		offset = fTextViewer.getSelectedRange().x;
		final ScriptContextInformation info = as(fInformation, ScriptContextInformation.class);
		if (info == null)
			return false;
		if (!info.valid(offset))
			return false;
		final SourceLocation par = info.currentParameterDisplayStringRange();
		if (par == null)
			return false;
		final int fnNameLen = info.function().name().length();
		final StyleRange fn = new StyleRange(0, fnNameLen, null, null, SWT.ITALIC);
		final StyleRange highlightedParameter = new StyleRange(par.start(), par.end()-par.start(), null, null, SWT.BOLD);
		presentation.clear();
		for (final StyleRange r : new StyleRange[] {fn, highlightedParameter})
			presentation.addStyleRange(r);
		return true;
	}

	@Override
	public boolean isContextInformationValid(final int offset) {
		try {
			if (fInformation instanceof ScriptContextInformation && !((ScriptContextInformation) fInformation).valid(offset))
				return false;
			final IDocument document = fTextViewer.getDocument();
			final IRegion line = document.getLineInformationOfOffset(fOffset);
			if (offset < line.getOffset() || offset >= document.getLength())
				return false;
			return true;
		} catch (final BadLocationException x) {
			return false;
		}
	}

}
