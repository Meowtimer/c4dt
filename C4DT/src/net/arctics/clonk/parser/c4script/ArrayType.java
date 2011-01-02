package net.arctics.clonk.parser.c4script;

import java.util.Iterator;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.ClonkIndex;

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
		return C4Type.ARRAY.iterator();
	}

	@Override
	public boolean canBeAssignedFrom(IType other) {
		return other == C4Type.ARRAY;
	}

	@Override
	public String typeName(boolean special) {
		return elementType == null ? C4Type.ARRAY.typeName(special) : String.format("<%s of %s>", C4Type.ARRAY.typeName(special), elementType.typeName(special));
	}

	@Override
	public boolean intersects(IType typeSet) {
		return C4Type.ARRAY.intersects(typeSet);
	}

	@Override
	public boolean containsType(IType type) {
		return type == C4Type.ARRAY;
	}

	@Override
	public int specificness() {
		return C4Type.ARRAY.specificness()+1;
	}

	@Override
	public IType staticType() {
		return C4Type.ARRAY;
	}

	@Override
	public boolean containsAnyTypeOf(IType... types) {
		return IType.Default.containsAnyTypeOf(this, types);
	}

	@Override
	public IType serializableVersion(ClonkIndex index) {
		if (elementType.serializableVersion(index) == elementType) {
			return this;
		} else {
			return new ArrayType(elementType.serializableVersion(index));
		}
	}

}
