package net.arctics.clonk.debug;

import org.eclipse.debug.core.model.IValue;

public class DebugVariableValue extends Value implements IValue {
	
	private final DebugVariable variable;
	
	public DebugVariableValue(final DebugVariable variable) {
		super(variable.getTarget(), null);
		this.variable = variable;
	}

	public DebugVariable variable() {
    	return variable;
    }
	
	@Override
	public boolean equals(final Object obj) {
		return super.equals(obj) && obj instanceof DebugVariableValue && ((DebugVariableValue)obj).variable == this.variable;
	}

}
