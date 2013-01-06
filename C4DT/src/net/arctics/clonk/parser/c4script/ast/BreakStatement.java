package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.Keywords;

public class BreakStatement extends KeywordStatement {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	@Override
	public String keyword() { return Keywords.Break; }
	@Override
	public ControlFlow controlFlow() { return ControlFlow.BreakLoop; }
	@Override
	public void reportProblems(C4ScriptParser parser) throws ParsingException {
		if (parentOfType(ILoop.class) == null)
			parser.error(ParserErrorCode.KeywordInWrongPlace, this, C4ScriptParser.NO_THROW, keyword());
		super.reportProblems(parser);
	}
}