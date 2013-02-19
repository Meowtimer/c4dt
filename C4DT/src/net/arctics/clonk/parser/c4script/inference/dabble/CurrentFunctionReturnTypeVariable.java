package net.arctics.clonk.parser.c4script.inference.dabble;

import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.ast.ReturnStatement;
import net.arctics.clonk.parser.c4script.inference.dabble.DabbleInference.ScriptProcessor;

public class CurrentFunctionReturnTypeVariable extends FunctionReturnTypeVariable {
	public CurrentFunctionReturnTypeVariable(Function function) { super(function); }
	@Override
	public boolean storesTypeInformationFor(ASTNode expr, ScriptProcessor processor) {
		if (expr instanceof ReturnStatement && expr.parentOfType(Function.class) == function)
			return true;
		return false;
	}
	@Override
	public boolean refersToSameExpression(ITypeVariable other) {
		return
			(other instanceof CurrentFunctionReturnTypeVariable && ((CurrentFunctionReturnTypeVariable)other).function == function) ||
			super.refersToSameExpression(other);
	}
	@Override
	public void apply(boolean soft, ScriptProcessor processor) { /* done by Dabble */ }
}
