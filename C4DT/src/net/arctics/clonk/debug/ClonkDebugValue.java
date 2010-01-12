package net.arctics.clonk.debug;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

public class ClonkDebugValue extends ClonkDebugElement implements IValue {
	
	private ClonkDebugVariable variable;
	
	public ClonkDebugValue(ClonkDebugVariable variable) {
		super(variable.getTarget());
		this.variable = variable;
	}

	public ClonkDebugVariable getVariable() {
    	return variable;
    }

	@Override
	public String getReferenceTypeName() throws DebugException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getValueString() throws DebugException {
		return "Not implemented";
	}

	@Override
	public IVariable[] getVariables() throws DebugException {
		return NO_VARIABLES;
	}

	@Override
	public boolean hasVariables() throws DebugException {
		return false;
	}

	@Override
	public boolean isAllocated() throws DebugException {
		return true;
	}

}
