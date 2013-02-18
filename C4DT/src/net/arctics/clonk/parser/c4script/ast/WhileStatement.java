package net.arctics.clonk.parser.c4script.ast;

import java.util.EnumSet;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.IEvaluationContext;
import net.arctics.clonk.parser.c4script.Keywords;

public class WhileStatement extends ConditionalStatement implements ILoop {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	public WhileStatement(ASTNode condition, ASTNode body) {
		super(condition, body);
	}

	@Override
	public String keyword() {
		return Keywords.While;
	}
	
	@Override
	public EnumSet<ControlFlow> possibleControlFlows() {
		EnumSet<ControlFlow> result = body.possibleControlFlows();
		result.removeAll(EnumSet.of(ControlFlow.BreakLoop, ControlFlow.NextIteration));
		return result;
	}
	
	@Override
	public Object evaluate(IEvaluationContext context) throws ControlFlowException {
		Object ev = null;
		while (true) {
			if (condition != null && !convertToBool(condition.evaluate(context)))
				break;
			ev = body.evaluate(context);
		}
		return ev;
	}
}