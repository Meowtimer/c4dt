package net.arctics.clonk.ui.editors.c4script;

import java.util.ArrayList;
import java.util.List;

import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.MutableRegion;
import net.arctics.clonk.parser.c4script.ast.Conf;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.ui.editors.ClonkCompletionProposal;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultIndentLineAutoEditStrategy;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

/**
 * Planned edit strategies:
 * <ul>
 * <li>smart indent</li>
 * <li><tt>}</tt> insertion after <tt>{</tt>+<tt>enter</tt></li>
 * <li>?automatic closing of <tt>)</tt>? this needs some really good intelligence</li>
 * <li>instead of <tt>\t</tt> always insert two spaces</li>
 * <li>complete parameter insertion when defining an object callback func(e.g. Contained* funcs)</li>
 * </ul>
 * @author ZokRadonh
 *
 */
public class C4ScriptAutoEditStrategy extends DefaultIndentLineAutoEditStrategy {
	
	private static class Autopair {
		public static final int FOLLOWSIDENT = 1;
		public static final int PRECEDESWHITESPACE = 2;
		
		public String start, end;
		public int flags;

		public Autopair(String start, String end, int flags) {
			super();
			this.start = start;
			this.end = end;
			this.flags = flags;
		}
		
		public boolean applies(String str, int situation) {
			return (flags & situation) == flags && str.endsWith(start);
		}
	}
	
	private static final Autopair[] AUTOPAIRS = {
		new Autopair("(", ")", Autopair.FOLLOWSIDENT|Autopair.PRECEDESWHITESPACE),
		new Autopair("\"", "\"", 0),
		new Autopair("[", "]", Autopair.FOLLOWSIDENT|Autopair.PRECEDESWHITESPACE)
	};
	
	private C4ScriptSourceViewerConfiguration configuration;
	private List<MutableRegion> overrideRegions = new ArrayList<MutableRegion>(3);
	
	public C4ScriptSourceViewerConfiguration getConfiguration() {
		return configuration;
	}

	public C4ScriptAutoEditStrategy(C4ScriptSourceViewerConfiguration configuration) {
		this.configuration = configuration;
	}
	
	public C4ScriptAutoEditStrategy(ClonkProjectNature project, String partitioning) {
	}
	
	private static boolean looksLikeIdent(IDocument d, int position) throws BadLocationException {
		final int END = 0, DIGITS = 1;
		int state = END;
		while (position >= 0) {
			char c = d.getChar(position);
			switch (state) {
			case DIGITS:
				if (Character.isLetter(c))
					return true;
				else if (!Character.isDigit(c))
					return false;
				break;
			case END:
				if (Character.isDigit(c))
					state = DIGITS;
				else if (Character.isLetter(c))
					return true;
			}
			--position;
		}
		return false;
	}
	
	@Override
	public void customizeDocumentCommand(IDocument d, DocumentCommand c) {

		// tabbing over override regions
		if (c.text.equals("\t")) { //$NON-NLS-1$
			for (int i = overrideRegions.size()-1; i >= 0; i--) {
				MutableRegion r = overrideRegions.get(i);
				if (r.getOffset() == c.offset) {
					overrideRegions.remove(i);
					c.text = ""; //$NON-NLS-1$
					c.shiftsCaret = false;
					c.caretOffset = r.getOffset()+r.getLength();
					return;
				}
			}
		}
		
		try {
			// auto-block
			if (c.text.endsWith("\n") && c.offset > 0 && d.getChar(c.offset-1) == '{') { //$NON-NLS-1$
				Function f = ((C4ScriptEditor)getConfiguration().getEditor()).getFuncAtCursor();
				if (f != null && unbalanced(d, f.getBody())) {
					IRegion r = d.getLineInformationOfOffset(c.offset);
					int start = r.getOffset();
					int end = findEndOfWhiteSpace(d, start, c.offset);
					if (end > start) {
						c.text += d.get(start, end-start) + Conf.indentString;
					}
					c.caretOffset = c.offset + c.text.length();
					c.shiftsCaret = false;
					c.text += "\n" + d.get(start, end-start) + "}"; //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		} catch (BadLocationException e1) {
			e1.printStackTrace();
		}
		
		if (c.text.length() == 0 && c.length > 0) {
			regionDeleted(c.offset, c.length, null);
		}
		else if (c.text.length() > 0 && c.length > 0) {
			// too complex; give up o_o
			overrideRegions.clear();
		}
		else {
			// user writes override region text himself - noop
			boolean overrideRegionTrespassed = false;
			for (int i = overrideRegions.size()-1; i >= 0; i--) {
				MutableRegion r = overrideRegions.get(i);
				try {
					if (r.getOffset() == c.offset && c.text.length() == 1 && d.getChar(r.getOffset()) == c.text.charAt(0)) {
						c.text = ""; //$NON-NLS-1$
						c.shiftsCaret = false;
						c.caretOffset = c.offset+1;
						r.incOffset(1);
						r.incLength(-1);
						if (r.getLength() == 0) {
							overrideRegions.remove(i);
						}
						overrideRegionTrespassed = true;
						break;
					}
				} catch (BadLocationException e) {
					e.printStackTrace();
					break;
				}
			}

			// look out for creation of new override region
			MutableRegion newOne = null;
			int situation = 0;
			try {
				if (looksLikeIdent(d, c.offset-1))
					situation |= Autopair.FOLLOWSIDENT;
			} catch (BadLocationException e) {}
			try {
				if (Character.isWhitespace(d.getChar(c.offset)))
					situation |= Autopair.PRECEDESWHITESPACE;
			} catch (BadLocationException e) {
				
			}
			if (!overrideRegionTrespassed) {
				for (Autopair autopair : AUTOPAIRS) {
					if (autopair.applies(c.text, situation)) {
						overrideRegions.add(0, newOne = new MutableRegion(c.offset+c.text.length(), autopair.end.length()));
						c.text += autopair.end;
						c.shiftsCaret = false;
						c.caretOffset = c.offset+autopair.end.length();
						break;
					}
				}
			}
			
			// inc offset of existing regions
			for (int i = newOne != null ? 1 : 0; i < overrideRegions.size(); i++) {
				MutableRegion r = overrideRegions.get(i);
				if (r.getOffset() >= c.offset) {
					r.incOffset(c.text.length());
				}
			}
		}

		super.customizeDocumentCommand(d, c);
	}

	private void regionDeleted(int offset, int length, MutableRegion exclude) {
		for (int i = overrideRegions.size()-1; i >= 0; i--) {
			MutableRegion r = overrideRegions.get(i);
			if (r == exclude)
				continue;
			if (r.getOffset() >= offset+length)
				r.incOffset(-length);
			else if (r.getOffset() >= offset && r.getOffset() < offset+length)
				overrideRegions.remove(i);
			else if (r.getOffset() < offset && r.getOffset()+r.getLength() > offset)
				r.incLength(offset-r.getOffset()-r.getLength());
		}
	}
	
	public void handleCursorPositionChanged(int cursorPos, IDocument d) {
		if (!overrideRegions.isEmpty()) {
			try {
				IRegion r = d.getLineInformationOfOffset(cursorPos);
				for (int i = overrideRegions.size()-1; i >= 0; i--) {
					MutableRegion or = overrideRegions.get(i);
					if (or.getOffset() < r.getOffset() || or.getOffset() > r.getOffset()+r.getLength())
						overrideRegions.remove(i);
				}
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
		}
	}

	public void completionProposalApplied(ClonkCompletionProposal proposal) {
		MutableRegion newOne = null;
		if (proposal.getReplacementString().endsWith(")")) { //$NON-NLS-1$
			overrideRegions.add(newOne = new MutableRegion(proposal.getReplacementOffset()+proposal.getReplacementString().length()-1, 1));
		}
		if (proposal.getReplacementLength() > 0)
			regionDeleted(proposal.getReplacementOffset(), proposal.getReplacementLength(), newOne);
		for (MutableRegion r : overrideRegions) {
			if (r == newOne)
				continue;
			if (r.getOffset() >= proposal.getReplacementOffset())
				r.incOffset(proposal.getReplacementString().length());
		}
	}

	private boolean unbalanced(IDocument d, IRegion body) throws BadLocationException {
		int open, close;
		open = close = 0;
		for (int x = 0; x < body.getLength()-1 && body.getOffset()+x < d.getLength(); x++) {
			char c = d.getChar(body.getOffset()+x);
			switch (c) {
			case '{':
				open++;
				break;
			case '}':
				close++;
				break;
			}
		}
		return open > close;
	}
	
}
