package net.arctics.clonk.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ControlFlow;
import net.arctics.clonk.c4script.Keywords;

public class ContinueStatement extends KeywordStatement {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	@Override
	public String keyword() { return Keywords.Continue; }

	@Override
	public ControlFlow controlFlow() { return ControlFlow.NextIteration; }

}