package net.arctics.clonk.parser.c4script;

public abstract class StoredTypeInformation implements IStoredTypeInformation, Cloneable {

	private IType type;

	@Override
	public IType getType() {
		return type;
	}

	@Override
	public void storeType(IType type) {
		// if value type has already been specialized don't despecialize it again -.-
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
		return "type: " + getType(); //$NON-NLS-1$
	}

}
