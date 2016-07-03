package net.arctics.clonk.c4script.ast.evaluate;

import net.arctics.clonk.ast.ControlFlowException;
import net.arctics.clonk.c4script.ast.AccessDeclaration;

public interface IVariableProvider {
	IVariable variable(AccessDeclaration access, Object obj) throws ControlFlowException;
}