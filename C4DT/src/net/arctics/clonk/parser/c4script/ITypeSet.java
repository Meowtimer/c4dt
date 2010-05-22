package net.arctics.clonk.parser.c4script;

import java.io.Serializable;

public interface ITypeSet extends Iterable<C4Type>, Serializable {
	public boolean canBeAssignedFrom(ITypeSet other);
	public String toString(boolean special);
	public boolean subsetOf(ITypeSet typeSet);
	public boolean contains(C4Type type);
	public int specificness();
}
