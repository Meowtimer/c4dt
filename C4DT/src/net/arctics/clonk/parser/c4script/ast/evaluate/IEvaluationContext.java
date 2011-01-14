package net.arctics.clonk.parser.c4script.ast.evaluate;

import net.arctics.clonk.parser.c4script.Function;

public interface IEvaluationContext extends IVariableValueProvider {
	Object[] getArguments();
	Function getFunction();
}