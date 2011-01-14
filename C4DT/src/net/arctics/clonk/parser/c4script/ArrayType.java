package net.arctics.clonk.parser.c4script;

import java.util.Iterator;

import net.arctics.clonk.ClonkCore;

public class ArrayType implements IType {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	private IType elementType;
	
	public ArrayType(IType elmType) {
		this.elementType = elmType;
	}

	public IType getElementType() {
		return elementType;
	}
	
	@Override
	public Iterator<IType> iterator() {
		return PrimitiveType.ARRAY.iterator();
	}

	@Override
	public boolean canBeAssignedFrom(IType other) {
		return other == PrimitiveType.ARRAY;
	}

	@Override
	public String typeName(boolean special) {
		return elementType == null ? PrimitiveType.ARRAY.typeName(special) : String.format("<%s of %s>", PrimitiveType.ARRAY.typeName(special), elementType.typeName(special));
	}

	@Override
	public boolean intersects(IType typeSet) {
		return PrimitiveType.ARRAY.intersects(typeSet);
	}

	@Override
	public boolean containsType(IType type) {
		return type == PrimitiveType.ARRAY;
	}

	@Override
	public int specificness() {
		return PrimitiveType.ARRAY.specificness()+1;
	}

	@Override
	public IType staticType() {
		return PrimitiveType.ARRAY;
	}

	@Override
	public boolean containsAnyTypeOf(IType... types) {
		return IType.Default.containsAnyTypeOf(this, types);
	}

}
