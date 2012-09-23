package net.arctics.clonk.debug;

import net.arctics.clonk.parser.c4script.Keywords;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.util.Utilities;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

public class ClonkDebugValue extends ClonkDebugElement implements IValue {
	
	private Object value;
	private PrimitiveType type;
	
	public ClonkDebugValue(ClonkDebugTarget target, Object value) {
		super(target);
		setValue(value, PrimitiveType.correspondingToInstance(value));
	}

	public Object getValue() {
		return value;
	}
	
	public PrimitiveType getType() {
		return type;
	}

	public void setValue(Object value, PrimitiveType type) {
		this.value = value;
		this.type = type;
	}

	@Override
	public String getReferenceTypeName() throws DebugException {
		return null;
	}

	@Override
	public String getValueString() throws DebugException {
		return value != null ? value.toString() : Keywords.Nil; //$NON-NLS-1$
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
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ClonkDebugValue) {
			ClonkDebugValue other = (ClonkDebugValue) obj;
			return Utilities.objectsEqual(other.value, value);
		}
		return false;
	}

}