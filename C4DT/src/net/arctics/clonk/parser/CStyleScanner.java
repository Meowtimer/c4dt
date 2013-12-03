package net.arctics.clonk.parser;

import java.util.ArrayList;
import java.util.List;

import net.arctics.clonk.c4script.ast.Comment;

/**
 * Scanner that ignores C-style comments
 * @author madeen
 *
 */
public class CStyleScanner extends BufferedScanner {

	protected Comment lastComment;

	public CStyleScanner(final Object source) { super(source); }

	protected Comment parseComment() {
		final int start = this.offset;
		final int a = this.read();
		final int b = this.read();
		if (a == -1 || b == -1) {
			this.seek(start);
			return null;
		} else if (a == '/' && b == '/') { 
			boolean javadoc;
			if (read() != '/') {
				javadoc = false;
				unread();
			} else
				javadoc = true;
			final String commentText = this.readStringUntil(BufferedScanner.NEWLINE_CHARS);
			//fReader.eat(BufferedScanner.NEWLINE_DELIMITERS);
			return new Comment(commentText, false, javadoc);
		} else if (a == '/' && b == '*') { 
			boolean javadoc;
			if (read() != '*') {
				javadoc = false;
				unread();
			} else
				javadoc = true;
			final int startMultiline = this.offset;
			while (!this.reachedEOF())
				if (this.read() == '*')
					if (this.read() == '/') {
						final String commentText = this.readStringAt(startMultiline, this.offset-2);
						return new Comment(commentText, true, javadoc); // genug gefressen
					} else
						this.unread();
			final String commentText = this.readStringAt(startMultiline, this.offset);
			return new Comment(commentText, true, javadoc);
		} else {
			this.seek(start);
			return null;
		}
	}

	@Override
	public int eatWhitespace() {
		final int pos = offset;
		while (super.eatWhitespace() > 0 || parseComment() != null);
		return offset-pos;
	}

	public List<Comment> collectComments() {
		List<Comment> result = null;
		while (true) {
			if (super.eatWhitespace() > 0)
				continue;
			final Comment c = parseComment();
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
