package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser;

public class MissingStatement extends Statement {
	private static final long serialVersionUID = 1L;

	public MissingStatement(int start) {
		super();
		setExprRegion(start, start+1);
	}
	@Override
	public void reportProblems(C4ScriptParser parser) throws ParsingException {
		missing(parser);
	}
}
