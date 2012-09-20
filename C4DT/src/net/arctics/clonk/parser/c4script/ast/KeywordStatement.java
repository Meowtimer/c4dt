package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.Conf;

/**
 * Baseclass for statements which begin with a keyword
 *
 */
public abstract class KeywordStatement extends Statement {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	public abstract String keyword();
	@Override
	public void doPrint(ExprWriter builder, int depth) {
		builder.append(keyword());
		builder.append(";"); //$NON-NLS-1$
	}

	protected void printBody(ExprElm body, ExprWriter builder, int depth) {
		int depthAdd = 0;
		if (!(body instanceof EmptyStatement)) {
			if (Conf.braceStyle == BraceStyleType.NewLine)
				builder.append("\n"); //$NON-NLS-1$
			boolean isBlock = body instanceof Block;
			switch (Conf.braceStyle) {
			case NewLine:
				Conf.printIndent(builder, depth - (isBlock ? 1 : 0));
				break;
			case SameLine:
				builder.append(' ');
				break;
			}
			depthAdd = isBlock ? 0 : 1;
		}
		body.print(builder, depth + depthAdd);
	}
	
	@Override
	public void reportProblems(C4ScriptParser parser) throws ParsingException {
		super.reportProblems(parser);
		if (flagsEnabled(MISPLACED))
			parser.error(ParserErrorCode.KeywordInWrongPlace, this, C4ScriptParser.NO_THROW, this.toString());
	}
}