package net.arctics.clonk.parser.c4script;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.util.Utilities;

public class ReferenceType implements IType {

	private static final long serialVersionUID = 1L;

	private IType type;
	
	private ReferenceType(IType type) {
		super();
		this.type = type;
	}

	public IType getType() {
		return type;
	}
	
	@Override
	public Iterator<IType> iterator() {
		return Utilities.arrayIterable(C4Type.REFERENCE, type).iterator();
	}

	@Override
	public boolean canBeAssignedFrom(IType other) {
		return type.canBeAssignedFrom(other);
	}

	@Override
	public String typeName(boolean special) {
		return String.format("%s %s", type.toString(), C4Type.REFERENCE.toString());
	}
	
	@Override
	public String toString() {
		return typeName(false);
	}

	@Override
	public boolean intersects(IType typeSet) {
		return type.intersects(typeSet);
	}

	@Override
	public boolean containsType(IType type) {
		return type.containsType(type) || C4Type.REFERENCE.containsType(type);
	}

	@Override
	public boolean containsAnyTypeOf(IType... types) {
		return IType.Default.containsAnyTypeOf(this, types);
	}

	@Override
	public int specificness() {
		return type.specificness();
	}

	@Override
	public IType staticType() {
		return C4Type.REFERENCE;
	}
	
	private static List<ReferenceType> internalizedReferenceTypes = new LinkedList<ReferenceType>();
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof ReferenceType && ((ReferenceType)obj).type.equals(type);
	}
	
	public static ReferenceType get(IType type) {
		if (type.staticType() == type) {
			for (ReferenceType r : internalizedReferenceTypes) {
				if (r.type.equals(type)) {
					return r;
				}
			}
			ReferenceType r = new ReferenceType(type);
			internalizedReferenceTypes.add(r);
			return r;
		} else {
			return new ReferenceType(type);
		}
	}
	
	@Override
	public IType serializableVersion(ClonkIndex indexToBeSerialized) {
		if (type.serializableVersion(indexToBeSerialized) == type)
			return type;
		else
			return get(type.serializableVersion(indexToBeSerialized));
	}

}
