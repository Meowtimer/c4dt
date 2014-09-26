package net.arctics.clonk.ini;

import java.util.Collection;

import net.arctics.clonk.c4script.Variable;

public interface IGlobalVariablesContributor {
	Collection<Variable> globalVariables();
}
