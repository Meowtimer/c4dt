package net.arctics.clonk.parser.c4script;

public interface ITypeSet extends Iterable<C4Type> {
	public boolean canBeAssignedFrom(ITypeSet other);
	public String toString(boolean special);
}
