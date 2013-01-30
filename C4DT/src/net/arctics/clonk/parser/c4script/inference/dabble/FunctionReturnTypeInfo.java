package net.arctics.clonk.parser.c4script.inference.dabble;

import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.ast.CallDeclaration;
import net.arctics.clonk.parser.c4script.inference.dabble.DabbleInference.ScriptProcessor;

final class FunctionReturnTypeInfo extends TypeInfo {
	private Function function;
	public Function function() { return function; }

	public FunctionReturnTypeInfo(Function function) {
		super();
		this.function = function;
		if (function != null)
			type = function.returnType();
	}

	@Override
	public boolean storesTypeInformationFor(ASTNode expr, ScriptProcessor processor) {
		if (expr instanceof CallDeclaration) {
			CallDeclaration callFunc = (CallDeclaration) expr;
			if (callFunc.declaration() == this.function)
				return true;
		}
		return false;
	}

	@Override
	public boolean refersToSameExpression(ITypeInfo other) {
		return other instanceof FunctionReturnTypeInfo && ((FunctionReturnTypeInfo)other).function == this.function;
	}

	@Override
	public String toString() {
		return String.format("[function %s: %s]", function, type().typeName(true));
	}

	@Override
	public void apply(boolean soft, ScriptProcessor processor) {
		if (function == null)
			return;
		function = (Function) function.latestVersion();
		if (!soft && !function.isEngineDeclaration())
			function.assignType(type(), false);
	}

}