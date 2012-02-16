package net.arctics.clonk.parser.c4script;

import java.util.Iterator;

import net.arctics.clonk.Core;

public class SameTypeAsSomeTypeable implements IType {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private ITypeable typeable;
	
	public SameTypeAsSomeTypeable(ITypeable typeable) {
		this.typeable = typeable;
	}

	protected IType actualType() {
		return typeable != null ? typeable.type() : PrimitiveType.ANY;
	}
	
	@Override
	public Iterator<IType> iterator() {
		return actualType().iterator();
	}

	@Override
	public boolean canBeAssignedFrom(IType other) {
		return actualType().canBeAssignedFrom(other);
	}

	@Override
	public String typeName(boolean special) {
		return actualType().typeName(special);
	}

	@Override
	public boolean intersects(IType typeSet) {
		return actualType().intersects(typeSet);
	}

	@Override
	public boolean containsType(IType type) {
		return actualType().containsType(type);
	}

	@Override
	public boolean containsAnyTypeOf(IType... types) {
		return actualType().containsAnyTypeOf(types);
	}

	@Override
	public int specificness() {
		return actualType().specificness();
	}

	@Override
	public IType staticType() {
		return actualType().staticType();
	}

	@Override
	public void setTypeDescription(String description) {
		actualType().setTypeDescription(description);
	}
	
	@Override
	public String toString() {
		return typeName(false);
	}

}
