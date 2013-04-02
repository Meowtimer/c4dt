package net.arctics.clonk.parser.c4script.typing.dabble;

import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.c4script.Function;

public class FunctionReturnTypeVariable extends TypeVariable {
	protected final Function baseFunction, function;
	public Function function() { return function; }
	public FunctionReturnTypeVariable(Function function) {
		super();
		this.function = function;
		this.baseFunction = function.baseFunction();
	}
	@Override
	public String toString() {
		return String.format("[function %s.%s: %s]", function.script().name(), function.name(), get().typeName(true));
	}
	@Override
	public void apply(boolean soft) {
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