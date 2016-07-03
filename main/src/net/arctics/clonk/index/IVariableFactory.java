package net.arctics.clonk.index;

import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.c4script.Variable.Scope;

@FunctionalInterface
public interface IVariableFactory {
	Variable newVariable(Scope scope, String varName);
}
