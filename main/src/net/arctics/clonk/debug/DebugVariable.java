package net.arctics.clonk.debug;

import net.arctics.clonk.c4script.Variable;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

public class DebugVariable extends DebugElement implements IVariable {

	private final StackFrame stackFrame;
	private final Variable variable;
	private final DebugVariableValue value;
	
	public DebugVariable(final StackFrame stackFrame, final Variable variable) {
		super(stackFrame.getTarget());
		this.stackFrame = stackFrame;
		this.variable = variable;
		this.value = new DebugVariableValue(this);
	}
	
	public Variable getVariable() {
		return variable;
	}

	public StackFrame getStackFrame() {
    	return stackFrame;
    }

	@Override
	public String getName() throws DebugException {
		return variable.name();
	}

	@Override
	public String getReferenceTypeName() throws DebugException {
		return variable.type().toString();
	}

	@Override
	public DebugVariableValue getValue() throws DebugException {
		return value;
	}

	@Override
	public boolean hasValueChanged() throws DebugException {
		return false;
	}

	@Override
	public void setValue(final String expression) throws DebugException {
		// not supported as of yet
	}

	@Override
	public void setValue(final IValue value) throws DebugException {
		// not supported as of yet
	}

	@Override
	public boolean supportsValueModification() {
		return false;
	}

	@Override
	public boolean verifyValue(final String expression) throws DebugException {
		return true;
	}

	@Override
	public boolean verifyValue(final IValue value) throws DebugException {
		return true;
	}

}
