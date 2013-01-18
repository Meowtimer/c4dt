package net.arctics.clonk.parser;

import java.util.ArrayList;
import java.util.List;

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
		int start = this.offset;
		int a = this.read();
		int b = this.read();
		if (a == -1 || b == -1) {
			this.seek(start);
			return null;
		} else if (a == '/' && b == '/') { //$NON-NLS-1$
			boolean javadoc;
			if (read() != '/') {
				javadoc = false;
				unread();
			} else
				javadoc = true;
			String commentText = this.readStringUntil(BufferedScanner.NEWLINE_CHARS);
			//fReader.eat(BufferedScanner.NEWLINE_DELIMITERS);
			return new Comment(commentText, false, javadoc);
		} else if (a == '/' && b == '*') { //$NON-NLS-1$
			boolean javadoc;
			if (read() != '*') {
				javadoc = false;
				unread();
			} else
				javadoc = true;
			int startMultiline = this.offset;
			while (!this.reachedEOF())
				if (this.read() == '*')
					if (this.read() == '/') {
						String commentText = this.readStringAt(startMultiline, this.offset-2);
						return new Comment(commentText, true, javadoc); // genug gefressen
					} else
						this.unread();
			String commentText = this.readStringAt(startMultiline, this.offset);
			return new Comment(commentText, true, javadoc);
		} else {
			this.seek(start);
			return null;
		}
	}
	
	public final void setExprRegionRelativeToFuncBody(ASTNode expr, int start, int end) {
		int bodyOffset = bodyOffset();
		expr.setLocation(start-bodyOffset, end-bodyOffset);
	}
	
	protected int bodyOffset() {
		return 0;
	}
	
	protected boolean parseComment() {
		int offset = this.offset;
		Comment c = parseCommentObject();
		if (c != null) {
			if (lastComment != null && lastComment.precedesOffset(offset, buffer))
				c.previousComment = lastComment;
			setExprRegionRelativeToFuncBody(c, offset, this.offset);
			c.setAbsoluteOffset(offset);
			lastComment = c;
			return true;
		}
		return false;
	}
	
	@Override
	public int eatWhitespace() {
		int pos = offset;
		while (super.eatWhitespace() > 0 || parseComment());
		return offset-pos;
	}
	
	public List<Comment> collectComments() {
		List<Comment> result = null;
		while (true) {
			if (super.eatWhitespace() > 0)
				continue;
			Comment c = parseCommentObject();
			if (c != null) {
				c.setPrependix(true);
				if (result == null)
					result = new ArrayList<Comment>(3);
				result.add(c);
				continue;
			}
			break;
		}
		return result;
	}
}
