package net.arctics.clonk.c4script.typing;

import java.util.Iterator;

import net.arctics.clonk.Core;
import net.arctics.clonk.util.ArrayUtil;

public class ReferenceType extends WrappedType implements IRefinedPrimitiveType {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	public ReferenceType(final IType type) { super(type); }

	public IType type() {
		return wrappedType;
	}
	
	@Override
	public Iterator<IType> iterator() {
		return ArrayUtil.iterable(PrimitiveType.REFERENCE, wrappedType).iterator();
	}

	@Override
	public String typeName(final boolean special) {
		return String.format("%s %s", wrappedType.toString(), PrimitiveType.REFERENCE.toString());
	}
	
	@Override
	public String toString() {
		return typeName(false);
	}

	@Override
	public IType simpleType() {
		return PrimitiveType.REFERENCE;
	}
	
	@Override
	public boolean equals(final Object obj) {
		return obj instanceof ReferenceType && ((ReferenceType)obj).wrappedType.equals(wrappedType);
	}
	
	public static IType make(final IType type) {
		if (type instanceof PrimitiveType)
			return ((PrimitiveType)type).referenceType();
		else
			return type;
	}
	
	@Override
	public PrimitiveType primitiveType() {
		return PrimitiveType.REFERENCE;
	}

}
