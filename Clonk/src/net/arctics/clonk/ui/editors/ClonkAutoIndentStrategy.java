package net.arctics.clonk.ui.editors;

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
 * <li>complete parameter insertion when defining a object callback func(e.g. Contained* funcs)</li>
 * </ul>
 * @author ZokRadonh
 *
 */
public class ClonkAutoIndentStrategy extends DefaultIndentLineAutoEditStrategy {
	private ClonkProjectNature project;
	private String partitioning; // i think that is some kinda constant; ClonkPartitionScanner.C4S_CODEBODY?
	
	public ClonkAutoIndentStrategy() {
	}
	
	public ClonkAutoIndentStrategy(ClonkProjectNature project, String partitioning) {
		this.partitioning = partitioning;
		this.project = project;
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
		if (c.text.contains("\n") || c.text.contains("\r")) {
			try {
				String originalText = c.text;
				IRegion reg = d.getLineInformationOfOffset(c.offset);
				String line = d.get(reg.getOffset(),reg.getLength());
				int count = countIndentOfLine(line);
				line = line.trim();
				if (line.endsWith("{")) {
					count++;
				}
				for(int i = 0; i < count; i++) c.text += "  ";
				if (line.endsWith("{")) {
					c.text += "\r\n";
					for(int i = 0; i < count - 1; i++) c.text += "  ";
					c.text += "}";
//					c.caretOffset = c.offset + count * 2 + originalText.length();
				}
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
		}
//		super.customizeDocumentCommand(d, c);
	}
	
	private int countIndentOfLine(String line) {
		int i = 0, indent = 0;
		do {
		 if (line.charAt(i) == '\t') {
			indent++;
			i++;
		 }
		 else if (line.charAt(i) == ' ' && line.charAt(i+1) == ' ') {
			 indent++;
			 i += 2;
		 }
		 else
			 break;
		} while(i < line.length());
		return indent;
	}
	
	
}
