package net.arctics.clonk.parser.c4script.ast;

import java.util.EnumSet;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.c4script.Keywords;

public class WhileStatement extends ConditionalStatement implements ILoop {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	public WhileStatement(ExprElm condition, ExprElm body) {
		super(condition, body);
	}

	@Override
	public String getKeyword() {
		return Keywords.While;
	}
	
	@Override
	public EnumSet<ControlFlow> getPossibleControlFlows() {
		EnumSet<ControlFlow> result = body.getPossibleControlFlows();
		result.removeAll(EnumSet.of(ControlFlow.BreakLoop, ControlFlow.NextIteration));
		return result;
	}
}