package net.arctics.clonk.parser.c4script;

import java.util.Iterator;

import net.arctics.clonk.Core;
import net.arctics.clonk.util.ArrayUtil;

public class CallTargetType implements IType {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	@Override
	public Iterator<IType> iterator() { return ArrayUtil.<IType>iterable(PrimitiveType.OBJECT, PrimitiveType.ID, PrimitiveType.PROPLIST).iterator(); }
	@Override
	public String typeName(boolean special) { return PrimitiveType.OBJECT.typeName(special); }
	@Override
	public IType simpleType() { return PrimitiveType.OBJECT; }
}
