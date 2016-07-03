package net.arctics.clonk.c4script.typing;

import net.arctics.clonk.Core;
import net.arctics.clonk.util.Utilities;

public abstract class WrappedType implements IType {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	protected final IType wrappedType;
	public IType wrappedType() { return wrappedType; }
	protected WrappedType(final IType type) {
		this.wrappedType = type;
	}
	public static IType unwrap(final IType type) {
		return type instanceof WrappedType ? ((WrappedType)type).wrappedType : type;
	}
	@Override
	public IType simpleType() {
		return wrappedType.simpleType();
	}
	@Override
	public String toString() {
		return typeName(false);
	}
	@Override
	public boolean equals(final Object obj) {
		if (obj.getClass() == this.getClass()) {
			final WrappedType other = (WrappedType)obj;
			return Utilities.eq(other.wrappedType, this.wrappedType);
		} else
			return false;
	}
}
