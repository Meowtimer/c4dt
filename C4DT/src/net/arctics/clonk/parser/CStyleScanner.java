package net.arctics.clonk.parser;

import net.arctics.clonk.parser.c4script.ast.Comment;

/**
 * Scanner that ignores C-style comments
 * @author madeen
 *
 */
public class CStyleScanner extends BufferedScanner {
	
	protected Comment lastComment;
	
	public CStyleScanner(Object source) {
		super(source);
	}

	protected Comment parseCommentObject() {
		String sequence = this.readString(2);
		if (sequence == null) {
			return null;
		}
		else if (sequence.equals("//")) { //$NON-NLS-1$
			String commentText = this.readStringUntil(BufferedScanner.NEWLINE_CHARS);
			//fReader.eat(BufferedScanner.NEWLINE_DELIMITERS);
			return new Comment(commentText, false);
		}
		else if (sequence.equals("/*")) { //$NON-NLS-1$
			int startMultiline = this.getPosition();
			while (!this.reachedEOF()) {
				if (this.read() == '*') {
					if (this.read() == '/') {
						String commentText = this.readStringAt(startMultiline, this.getPosition()-2);
						return new Comment(commentText, true); // genug gefressen
					}
					else {
						this.unread();
					}
				}
			}
			String commentText = this.readStringAt(startMultiline, this.getPosition());
			return new Comment(commentText, true);
		}
		else {
			this.move(-2);
			return null;
		}
	}
	
	protected boolean parseComment() {
		int offset = this.getPosition();
		Comment c = parseCommentObject();
		if (c != null) {
			c.setExprRegion(offset, this.getPosition());
			lastComment = c;
			return true;
		}
		return false;
	}
	
	@Override
	public int eatWhitespace() {
		int pos = getPosition();
		while (super.eatWhitespace() > 0 || parseComment());
		return getPosition()-pos;
	}
}
