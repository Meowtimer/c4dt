package net.arctics.clonk.parser.c4script.ast.evaluate;

import net.arctics.clonk.parser.c4script.C4Function;

public interface IEvaluationContext extends IVariableValueProvider {
	Object[] getArguments();
	C4Function getFunction();
}