package net.arctics.clonk.parser.c4script;

import net.arctics.clonk.index.C4Object;

public abstract class StoredTypeInformation implements IStoredTypeInformation, Cloneable {

	private C4Type type;
	private C4Object objectType;

	public C4Object getObjectType() {
		return objectType;
	}

	public C4Type getType() {
		return type;
	}

	public void storeObjectType(C4Object objectType) {
		this.objectType = objectType;
	}

	public void storeType(C4Type type) {
		this.type = type;
	}
	
	public void apply(boolean soft) {}
	
	public void merge(IStoredTypeInformation other) {
		if (getType() == C4Type.UNKNOWN)
			// unknown before so now it is assumed to be of this type
			storeType(type);
		else if (getType() != other.getType())
			// assignments of multiple types - can be anything
			storeType(C4Type.ANY);
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
	@Override
	public String toString() {
		return "type: " + getType() + " objecttype: " + getObjectType();
	}

}
