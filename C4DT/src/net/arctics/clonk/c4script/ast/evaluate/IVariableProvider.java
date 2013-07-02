package net.arctics.clonk.c4script.ast.evaluate;

import net.arctics.clonk.ast.ControlFlowException;
import net.arctics.clonk.c4script.ast.AccessVar;

public interface IVariableProvider {
	IVariable variable(AccessVar access, Object obj) throws ControlFlowException;
}