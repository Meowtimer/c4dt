package net.arctics.clonk.parser.c4script;

import net.arctics.clonk.index.C4Object;

public abstract class StoredTypeInformation implements IStoredTypeInformation, Cloneable {

	private ITypeSet type;
	private C4Object objectType;

	@Override
	public C4Object getObjectType() {
		return objectType;
	}

	@Override
	public ITypeSet getType() {
		return type;
	}

	@Override
	public void storeObjectType(C4Object objectType) {
		this.objectType = objectType;
	}

	@Override
	public void storeType(ITypeSet type) {
		// if value type has already been specialized don't despecialize it again -.-
		if (this.type == null || type != C4Type.ANY)
			this.type = type;
	}
	
	public void apply(boolean soft) {}
	
	public void merge(IStoredTypeInformation other) {
		if (getType() == C4Type.UNKNOWN)
			// unknown before so now it is assumed to be of this type
			storeType(type);
		else if (!getType().equals(other.getType()))
			// assignments of multiple types - construct type set
			storeType(C4TypeSet.create(getType(), other.getType()));
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
	@Override
	public String toString() {
		return "type: " + getType() + " objecttype: " + getObjectType(); //$NON-NLS-1$ //$NON-NLS-2$
	}

}
