package net.arctics.clonk.c4script.typing;

import java.util.Iterator;

import net.arctics.clonk.Core;
import net.arctics.clonk.util.ArrayUtil;

public class CallTargetType implements IType {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	public static final CallTargetType INSTANCE = new CallTargetType();
	@Override
	public Iterator<IType> iterator() { return ArrayUtil.<IType>iterable(PrimitiveType.OBJECT, PrimitiveType.ID, PrimitiveType.PROPLIST).iterator(); }
	@Override
	public String typeName(boolean special) { return PrimitiveType.OBJECT.typeName(special); }
	@Override
	public IType simpleType() { return PrimitiveType.OBJECT; }
	@Override
	public boolean equals(Object obj) { return obj instanceof CallTargetType; }
	@Override
	public String toString() { return typeName(true); }
}
