package net.arctics.clonk.parser.c4script;

import java.io.Serializable;

/**
 * Classes conforming to this interface may be a specific type or a set of types
 */
public interface IType extends Iterable<IType>, Serializable {
	public boolean canBeAssignedFrom(IType other);
	public String typeName(boolean special);
	public boolean subsetOfType(IType typeSet);
	public boolean containsType(IType type);
	public int specificness();
	public IType staticType();
}
