package net.arctics.clonk.parser.c4script;

public abstract class StoredTypeInformation implements IStoredTypeInformation, Cloneable {

	protected IType type = C4Type.UNKNOWN;

	@Override
	public IType getType() {
		return type;
	}

	@Override
	public void storeType(IType type) {
		this.type = type;
	}
	
	@Override
	public boolean generalTypeHint(IType hint) {
		if (type == C4Type.UNKNOWN) {
			storeType(hint);
		} else if (type == C4Type.ANY) {
			type = C4TypeSet.create(type, hint);
		} else {
			// false -> wrong hint
			// true -> type is at least contained in the hint so it is somewhat correct   
			return type.subsetOfType(hint);
		}
		return true;
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
