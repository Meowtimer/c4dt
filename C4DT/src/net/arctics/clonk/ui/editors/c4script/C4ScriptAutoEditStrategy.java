package net.arctics.clonk.ui.editors.c4script;

import java.util.ArrayList;
import java.util.List;

import net.arctics.clonk.parser.c4script.MutableRegion;
import net.arctics.clonk.resource.ClonkProjectNature;

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
	
	private static final String[] AUTOPAIR_STRINGS = {"(", ")", "\"", "\""};
	
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

	/*
	 * Facts:
	 * 1. backspaces are only customizable if attached to viewers key-listener (see jdt)
	 * 2. search methods(scan* in jdt) should always respect partitioning to avoid strings and comments
	 * 3. every insertion has to check if the characters already exist before adding them to c.text
	 * 
	 * Unclear points:
	 * 1. sense/use of ReplaceEdit/DeleteEdit/...
	 * 2. correct calculation of c.caretOffset
	 */
	@Override
	public void customizeDocumentCommand(IDocument d, DocumentCommand c) {

		boolean overrideRegionTrespassed = false;
		for (int i = overrideRegions.size()-1; i >= 0; i--) {
			MutableRegion r = overrideRegions.get(i);
			try {
				if (r.getOffset() == c.offset && c.text.length() == 1 && d.getChar(r.getOffset()) == c.text.charAt(0)) {
					c.text = "";
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
		MutableRegion newOne = null;
		if (!overrideRegionTrespassed) {
			for (int i = 0; i < AUTOPAIR_STRINGS.length; i += 2) {
				if (c.text.endsWith(AUTOPAIR_STRINGS[i])){
					overrideRegions.add(0, newOne = new MutableRegion(c.offset+c.text.length(), AUTOPAIR_STRINGS[i+1].length()));
					c.text += AUTOPAIR_STRINGS[i+1];
					c.shiftsCaret = false;
					c.caretOffset = c.offset+AUTOPAIR_STRINGS[i+1].length();
					break;
				}
			}
		}
		for (int i = newOne != null ? 1 : 0; i < overrideRegions.size(); i++) {
			MutableRegion r = overrideRegions.get(i);
			if (r.getOffset() >= c.offset) {
				r.incOffset(c.text.length());
			}
		}


		super.customizeDocumentCommand(d, c);
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

	/*
	private boolean unbalanced(IDocument d, IRegion body, int i) throws BadLocationException {
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
	*/
	
}
