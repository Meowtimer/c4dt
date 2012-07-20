package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser;

public class GarbageStatement extends Statement {

	private static final long serialVersionUID = 1L;
	
	private String garbage;
	
	public GarbageStatement(String garbageString, int start) {
		garbage = garbageString;
		setExprRegion(start, start+garbage.length());
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
		parser.error(ParserErrorCode.Garbage, this, C4ScriptParser.NO_THROW, garbage);
	}
	
	public void setGarbage(String garbage) {
		this.garbage = garbage;
	}

}
