package net.arctics.clonk.index;

import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.Variable.Scope;

public interface IVariableFactory {
	Variable newVariable(String varName, Scope scope);
}
