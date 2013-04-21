package net.arctics.clonk.c4script.ast.evaluate;

import net.arctics.clonk.c4script.ast.AccessVar;

public interface IVariableValueProvider {
	Object valueForVariable(AccessVar access);
}