package net.arctics.clonk.index;

import static net.arctics.clonk.util.ArrayUtil.iterable;

import java.util.Iterator;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.PrimitiveType;

public class MetaDefinition implements IType {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	private final Definition definition;
	
	public MetaDefinition(Definition definition) {
		super();
		this.definition = definition;
	}
	
	@Override
	public Iterator<IType> iterator() {
		return iterable((IType)PrimitiveType.ID, definition).iterator();
	}

	@Override
	public boolean canBeAssignedFrom(IType other) {
		return other == PrimitiveType.ID || other instanceof MetaDefinition;
	}

	@Override
	public String typeName(boolean special) {
		return String.format("Definition '%s'", definition.name());
	}

	@Override
	public IType simpleType() {
		return PrimitiveType.ID;
	}

	@Override
	public void setTypeDescription(String description) {}
}