package net.arctics.clonk.parser.c4script.ast.evaluate;

import net.arctics.clonk.parser.c4script.ast.AccessVar;

public interface IVariableValueProvider {
	Object valueForVariable(AccessVar access);
}