package net.arctics.clonk.debug;

import org.eclipse.debug.core.model.IValue;

public class ClonkDebugVariableValue extends ClonkDebugValue implements IValue {
	
	private ClonkDebugVariable variable;
	
	public ClonkDebugVariableValue(ClonkDebugVariable variable) {
		super(variable.getTarget(), null);
		this.variable = variable;
	}

	public ClonkDebugVariable variable() {
    	return variable;
    }
	
	@Override
	public boolean equals(Object obj) {
		return super.equals(obj) && obj instanceof ClonkDebugVariableValue && ((ClonkDebugVariableValue)obj).variable == this.variable;
	}

}
