package net.arctics.clonk.c4script.typing;

import java.util.Iterator;

import net.arctics.clonk.Core;

public class ErroneousType implements IRefinedPrimitiveType {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private final String erroneousTypeString;
	public ErroneousType(String erroneousTypeString) {
		super();
		this.erroneousTypeString = erroneousTypeString;
	}
	@Override
	public String typeName(boolean special) { return String.format("Invalid type: '%s'", erroneousTypeString); }
	@Override
	public IType simpleType() { return PrimitiveType.ERRONEOUS; }
	@Override
	public Iterator<IType> iterator() { return PrimitiveType.ERRONEOUS.iterator(); }
	@Override
	public PrimitiveType primitiveType() { return PrimitiveType.ERRONEOUS; }
}
