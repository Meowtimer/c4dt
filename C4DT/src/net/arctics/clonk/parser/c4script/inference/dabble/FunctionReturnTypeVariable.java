package net.arctics.clonk.parser.c4script.inference.dabble;

import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.ast.CallDeclaration;
import net.arctics.clonk.parser.c4script.inference.dabble.DabbleInference.Visitor;

public class FunctionReturnTypeVariable extends TypeVariable {
	protected final Function baseFunction, function;
	public Function function() { return function; }
	public FunctionReturnTypeVariable(Function function) {
		super();
		this.function = function;
		this.baseFunction = function.baseFunction();
	}
	@Override
	public boolean binds(ASTNode node, Visitor visitor) {
		if (node instanceof CallDeclaration) {
			CallDeclaration callFunc = (CallDeclaration) node;
			if (callFunc.declaration() instanceof Function && ((Function)callFunc.declaration()).baseFunction() == baseFunction)
				return true;
		}
		return false;
	}
	@Override
	public boolean same(ITypeVariable other) {
		return other instanceof FunctionReturnTypeVariable && ((FunctionReturnTypeVariable)other).baseFunction == this.baseFunction;
	}
	@Override
	public String toString() {
		return String.format("[function %s.%s: %s]", function.script().name(), function.name(), get().typeName(true));
	}
	@Override
	public void apply(boolean soft, Visitor visitor) {
		if (function == null)
			return;
		if (!soft && !function.isEngineDeclaration())
			function.assignType(get(), false);
	}
	@Override
	public Declaration declaration(Visitor visitor) { return function; }

}