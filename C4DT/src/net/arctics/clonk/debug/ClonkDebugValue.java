package net.arctics.clonk.debug;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

public class ClonkDebugValue extends ClonkDebugElement implements IValue {
	
	private ClonkDebugVariable variable;
	private Object value;
	
	public ClonkDebugValue(ClonkDebugVariable variable) {
		super(variable.getTarget());
		this.variable = variable;
	}

	public ClonkDebugVariable getVariable() {
    	return variable;
    }

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	@Override
	public String getReferenceTypeName() throws DebugException {
		return null;
	}

	@Override
	public String getValueString() throws DebugException {
		return value != null ? value.toString() : "nil"; //$NON-NLS-1$
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
