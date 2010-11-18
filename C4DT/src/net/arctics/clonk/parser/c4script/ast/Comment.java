package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.DeclarationRegion;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.ui.editors.c4script.ExpressionLocator;

public class Comment extends Statement implements Statement.Attachment {
	/**
	 * 
	 */
	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	private String comment;
	private boolean multiLine;

	public Comment(String comment, boolean multiLine) {
		super();
		this.comment = comment;
		this.multiLine = multiLine;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	/*private String commentAsPrintedStatement(C4Function function, int depth) {
		try {
			Statement s = C4ScriptParser.parseStandaloneStatement(comment, function, null);
			if (s != null) {
				String str = s.toString(depth);
				Matcher matcher = WHITE_SPACE_PATTERN.matcher(comment);
				if (matcher.find())
					str = matcher.group(1) + str;
				matcher = WHITE_SPACE_AT_END_PATTERN.matcher(comment);
				if (matcher.find())
					str = str + matcher.group(1);
				return str;
			} else {
				return comment;
			}
		} catch (ParsingException e) {
			return comment;
		}
	}*/
	
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

	public boolean isMultiLine() {
		return multiLine;
	}

	public void setMultiLine(boolean multiLine) {
		this.multiLine = multiLine;
	}

	public boolean precedesOffset(int offset, CharSequence script) {
		int count = 0;
		if (offset > getExprEnd()) {
			for (int i = getExprEnd()+1; i < offset; i++) {
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
		ExpressionLocator locator = new ExpressionLocator(offset-2); // make up for '//' or /*'
		try {
			C4ScriptParser.parseStandaloneStatement(comment, parser.getActiveFunc(), locator);
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
		case Post:
			builder.append(" ");
			this.print(builder, depth);
			break;
		}
	}
	
	@Override
	public void reportErrors(C4ScriptParser parser) throws ParsingException {
		// uh oh.. no
	}

}