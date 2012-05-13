package net.arctics.clonk.parser.c4script;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.arctics.clonk.Core;
import net.arctics.clonk.util.ArrayUtil;

public class ReferenceType implements IType {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

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
		return ArrayUtil.iterable(PrimitiveType.REFERENCE, type).iterator();
	}

	@Override
	public boolean canBeAssignedFrom(IType other) {
		return type.canBeAssignedFrom(other);
	}

	@Override
	public String typeName(boolean special) {
		return String.format("%s %s", type.toString(), PrimitiveType.REFERENCE.toString());
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
	public boolean subsetOf(IType type) {
		return type.equals(this) || this.type.subsetOf(type) || type == PrimitiveType.REFERENCE;
	}
	
	@Override
	public IType eat(IType other) {return this;}

	@Override
	public boolean subsetOfAny(IType... types) {
		return IType.Default.containsAnyTypeOf(this, types);
	}

	@Override
	public int specificness() {
		return type.specificness();
	}

	@Override
	public IType staticType() {
		return PrimitiveType.REFERENCE;
	}
	
	private static Map<IType, ReferenceType> internalizedReferenceTypes = new HashMap<IType, ReferenceType>();
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof ReferenceType && ((ReferenceType)obj).type.equals(type);
	}
	
	public static ReferenceType get(IType type) {
		if (type.staticType() == type) {
			ReferenceType r = internalizedReferenceTypes.get(type);
			if (r == null) {
				r = new ReferenceType(type);
				internalizedReferenceTypes.put(type, r);
			}
			return r;
		} else {
			return new ReferenceType(type);
		}
	}
	
	@Override
	public void setTypeDescription(String description) {}

}
