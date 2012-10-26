package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser;

/**
 * Base class for statements which begin with a keyword.
 *
 */
public abstract class KeywordStatement extends Statement {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	/**
	 * The keyword of this statement.
	 * @return The keyword that this statement starts with.
	 */
	public abstract String keyword();
	
	@Override
	public void doPrint(ExprWriter builder, int depth) {
		builder.append(keyword());
		builder.append(";"); //$NON-NLS-1$
	}

	protected void printBody(ExprElm body, ExprWriter builder, int depth) {
		if (!(body instanceof Block))
			depth++;
		if (!(body instanceof EmptyStatement))
			Conf.blockPrelude(builder, depth);
		body.print(builder, depth);
	}
	
	@Override
	public void reportProblems(C4ScriptParser parser) throws ParsingException {
		super.reportProblems(parser);
		if (flagsEnabled(MISPLACED))
			parser.error(ParserErrorCode.KeywordInWrongPlace, this, C4ScriptParser.NO_THROW, this.toString());
	}
}