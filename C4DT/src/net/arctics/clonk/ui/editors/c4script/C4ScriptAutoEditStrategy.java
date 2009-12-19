package net.arctics.clonk.ui.editors.c4script;

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
	
	private C4ScriptSourceViewerConfiguration configuration;
	
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
		if (c.text.endsWith("(")) {
			c.text += ")";
			c.shiftsCaret = false;
			c.caretOffset = c.offset+1;
			return;
		}
		/*if (c.text.contains("\n") || c.text.contains("\r")) { //$NON-NLS-1$ //$NON-NLS-2$
			try {
				//String originalText = c.text;
				IRegion reg = d.getLineInformationOfOffset(c.offset);
				String line = d.get(reg.getOffset(),reg.getLength());
				if (line.endsWith("{")) {
					C4Function f = configuration.getEditor().getFuncAt(c.offset);
					if (f != null && unbalanced(d, f.getBody(), reg.getOffset()+reg.getLength()-1)) {
						int whitespaceLen;
						for (whitespaceLen = 0; whitespaceLen < line.length() && BufferedScanner.isWhiteSpaceButNotLineDelimiterChar(line.charAt(whitespaceLen)); whitespaceLen++);
						String indentStr = line.substring(0, whitespaceLen);
						line = line.trim();
						int oldLen = c.text.length();
						String lineBreakStr = "\n";
						String blockCloseStr = "}";
						StringBuilder b = new StringBuilder(oldLen+indentStr.length()*2+C4ScriptExprTree.IndentString.length()+lineBreakStr.length()+blockCloseStr.length());
						c.text = b.append(c.text).append(indentStr).append(C4ScriptExprTree.IndentString).append(lineBreakStr).append(indentStr).append(blockCloseStr).toString();
						c.shiftsCaret = false;
						c.caretOffset = c.offset + 1 + indentStr.length() + C4ScriptExprTree.IndentString.length();
						return;
					}
				}
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
		}*/
		super.customizeDocumentCommand(d, c);
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
