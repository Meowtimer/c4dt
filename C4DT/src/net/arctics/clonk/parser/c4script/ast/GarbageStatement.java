package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser;

public class GarbageStatement extends Statement {

	private static final long serialVersionUID = 1L;
	
	private String garbage;
	
	public GarbageStatement(String script, int start, int end) {
		garbage = script.substring(start, end);
		setExprRegion(start, end);
	}
	
	public String getGarbage() {
		return garbage;
	}
	
	@Override
	public void doPrint(ExprWriter output, int depth) {
		output.append(garbage);
	}
	
	@Override
	public void reportErrors(C4ScriptParser parser) throws ParsingException {
		parser.errorWithCode(ParserErrorCode.Garbage, this, true, garbage);
	}

}
