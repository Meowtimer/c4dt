package net.arctics.clonk.parser.c4script;

import static net.arctics.clonk.util.ArrayUtil.iterable;

import java.util.Iterator;

import net.arctics.clonk.Core;

public class ParameterType implements IType{
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private final Variable parameter;
	public ParameterType(Variable variable) {
		parameter = variable;
	}

	public Variable parameter() { return parameter; }
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof ParameterType && ((ParameterType)obj).parameter() == this.parameter();
	}
	
	@Override
	public Iterator<IType> iterator() {
		return iterable(PrimitiveType.UNKNOWN, this).iterator();
	}

	@Override
	public boolean canBeAssignedFrom(IType other) {
		return true;
	}

	@Override
	public String typeName(boolean special) {
		if (special && parameter() != null)
			return String.format("Type of parameter '%s'", parameter.name());
		else
			return PrimitiveType.UNKNOWN.typeName(false);
	}
	
	@Override
	public String toString() { return typeName(true); }

	@Override
	public IType simpleType() { return PrimitiveType.UNKNOWN; }
	@Override
	public void setTypeDescription(String description) {}
}