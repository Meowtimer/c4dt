package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.c4script.Keywords;

public class ContinueStatement extends KeywordStatement {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	@Override
	public String getKeyword() {
		return Keywords.Continue;
	}
	@Override
	public ControlFlow getControlFlow() {
		return ControlFlow.NextIteration;
	}
}