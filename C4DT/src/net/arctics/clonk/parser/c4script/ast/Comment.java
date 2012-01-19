package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.DeclarationRegion;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.ui.editors.c4script.ExpressionLocator;

/**
 * A comment in the code. Instances of this class can be used as regular {@link ExprElm} objects or
 * be attached to {@link Statement}s as {@link Attachment}.
 * @author madeen
 *
 */
public class Comment extends Statement implements Statement.Attachment {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	private String comment;
	private boolean multiLine;
	private boolean prependix;
	private int absoluteOffset;

	/**
	 * Create new {@link Comment}, specifying content and whether it's a multi-line comment.
	 * @param comment Content
	 * @param multiLine Whether multiline
	 */
	public Comment(String comment, boolean multiLine) {
		super();
		this.comment = comment;
		this.multiLine = multiLine;
	}

	/**
	 * Return the comment string.
	 */
	public String text() {
		return comment;
	}

	/**
	 * Set the comment string.
	 * @param comment The comment string
	 */
	public void setComment(String comment) {
		this.comment = comment;
	}
	
	@Override
	public void doPrint(ExprWriter builder, int depth) {
		String c = comment;
		if (multiLine = multiLine || c.contains("\n")) {
			builder.append("/*"); //$NON-NLS-1$
			builder.append(c);
			builder.append("*/"); //$NON-NLS-1$
		}
		else {
			builder.append("//"); //$NON-NLS-1$
			builder.append(c);
		}
	}

	/**
	 * Return whether this comment is multiline.
	 * @return True if the source code representation of this node is \/* ... *\/
	 */
	public boolean isMultiLine() {
		return multiLine;
	}

	/**
	 * Set multiline-ness of this {@link Comment}.
	 * @param multiLine Whether multiline
	 */
	public void setMultiLine(boolean multiLine) {
		this.multiLine = multiLine;
	}

	/**
	 * Return whether this {@link Comment} is the last text element preceding the specified offset.
	 * This is true only if at most one line-break is between the {@link Comment} text and the offset.
	 * @param offset The offset
	 * @param script Script text queried for content to determine the return value.
	 * @return Whether this comment precedes the offset as described.
	 */
	public boolean precedesOffset(int offset, CharSequence script) {
		int count = 0;
		if (offset > absoluteOffset+getLength()) {
			for (int i = absoluteOffset+getLength(); i < offset; i++) {
				if (!BufferedScanner.isLineDelimiterChar(script.charAt(i)))
					return false;
				if (script.charAt(i) == '\n' && ++count > 1)
					return false;
			}
			return true;
		}
		return false;
	}

	@Override
	public DeclarationRegion declarationAt(int offset, C4ScriptParser parser) {
		// parse comment as expression and see what goes
		ExpressionLocator locator = new ExpressionLocator(offset-2-parser.bodyOffset()); // make up for '//' or /*'
		try {
			C4ScriptParser commentParser= new C4ScriptParser(comment, parser.container(), parser.container().getScriptFile()) {
				@Override
				protected void initialize() {
					super.initialize();
					allErrorsDisabled = true;
				}
				@Override
				public int bodyOffset() {
					return 0;
				}
			};
			commentParser.parseStandaloneStatement(comment, parser.getCurrentFunc(), locator);
		} catch (ParsingException e) {}
		if (locator.getExprAtRegion() != null) {
			DeclarationRegion reg = locator.getExprAtRegion().declarationAt(offset, parser);
			if (reg != null)
				return reg.addOffsetInplace(getExprStart()+2);
			else
				return null;
		}
		else
			return super.declarationAt(offset, parser);
	}

	@Override
	public void applyAttachment(Position position, ExprWriter builder, int depth) {
		switch (position) {
		case Pre:
			if (prependix) {
				this.print(builder, depth);
				builder.append("\n");
				Conf.printIndent(builder, depth-1);
			}
			break;
		case Post:
			if (!prependix) {
				builder.append(" ");
				this.print(builder, depth);
			}
			break;
		}
	}
	
	@Override
	public void reportErrors(C4ScriptParser parser) throws ParsingException {
		// uh oh.. no
	}

	/**
	 * Set the absolute offset of this comment. This value is only is used in {@link #precedesOffset(int, CharSequence)}
	 * @param offset The offset to set as absolute one.
	 */
	public void setAbsoluteOffset(int offset) {
		absoluteOffset = offset;
	}
	
	/**
	 * Return whether the comment - as an {@link Attachment} to a {@link Statement} - is to be printed before or after the statement
	 * @return What he said
	 */
	public boolean isPrependix() {
		return prependix;
	}
	
	/**
	 * Sets whether the comment - as an {@link Attachment} to a {@link Statement} - is to be printed before the {@link Statement} or following it
	 * @param Whether to or not
	 */
	public void setPrependix(boolean prependix) {
		this.prependix = prependix;
	}

}