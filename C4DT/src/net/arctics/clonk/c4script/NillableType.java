package net.arctics.clonk.c4script;

import static net.arctics.clonk.util.ArrayUtil.iterable;

import java.util.Iterator;

import net.arctics.clonk.Core;

public class NillableType extends WrappedType {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	
	public static IType make(IType baseType) {
		// gnah, nillable catastrophy
		if (baseType instanceof PrimitiveType)
			switch ((PrimitiveType)baseType) {
			case ANY: case UNKNOWN:
				return baseType;
			default:
				return new NillableType(baseType);
			}
		else if (!(baseType instanceof NillableType))
			return new NillableType(baseType);
		else
			return baseType;
	}
	
	private NillableType(IType baseType) {
		super(baseType);
	}
	
	@Override
	public Iterator<IType> iterator() {
		return iterable(wrappedType, PrimitiveType.ANY).iterator(); 
	}

	@Override
	public String typeName(boolean special) {
		final String tn = wrappedType.typeName(special);
		return tn.contains(" ") && !(tn.startsWith("{") || tn.startsWith("[")) ? "("+tn+")?" : tn + "?";
	}

	@Override
	public IType simpleType() {
		return wrappedType.simpleType();
	}
}
