package net.arctics.clonk.debug;

import net.arctics.clonk.parser.c4script.C4Variable;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

public class ClonkDebugVariable extends ClonkDebugElement implements IVariable {

	private ClonkDebugStackFrame stackFrame;
	private C4Variable variable;
	private ClonkDebugValue value;
	
	public ClonkDebugVariable(ClonkDebugStackFrame stackFrame, C4Variable variable) {
		super(stackFrame.getTarget());
		this.stackFrame = stackFrame;
		this.variable = variable;
		this.value = new ClonkDebugValue(this);
	}

	@Override
	public String getName() throws DebugException {
		return variable.getName();
	}

	@Override
	public String getReferenceTypeName() throws DebugException {
		return variable.getType().toString();
	}

	@Override
	public IValue getValue() throws DebugException {
		return value;
	}

	@Override
	public boolean hasValueChanged() throws DebugException {
		return false;
	}

	@Override
	public void setValue(String expression) throws DebugException {
		// not supported as of yet
	}

	@Override
	public void setValue(IValue value) throws DebugException {
		// not supported as of yet
	}

	@Override
	public boolean supportsValueModification() {
		return false;
	}

	@Override
	public boolean verifyValue(String expression) throws DebugException {
		return false;
	}

	@Override
	public boolean verifyValue(IValue value) throws DebugException {
		return false;
	}

}
