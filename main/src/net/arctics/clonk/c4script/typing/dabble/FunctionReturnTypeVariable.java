package net.arctics.clonk.c4script.typing.dabble;

import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.typing.TypeVariable;

public class FunctionReturnTypeVariable extends TypeVariable {
	protected final Function baseFunction, function;
	public Function function() { return function; }
	public FunctionReturnTypeVariable(final Function function) {
		super();
		this.function = function;
		this.baseFunction = function.baseFunction();
	}
	@Override
	public String toString() {
		return String.format("[function %s.%s: %s]", function.script().name(), function.name(), get().typeName(true));
	}
	@Override
	public void apply(final boolean soft) {
		if (function == null)
			return;
		if (!soft && !function.isEngineDeclaration())
			function.assignType(get(), false);
	}
	@Override
	public Declaration declaration() { return function; }
	@Override
	public Declaration key() { return function; }

}