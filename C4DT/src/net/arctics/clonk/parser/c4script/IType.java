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
	public boolean containsAnyTypeOf(IType... types);
	public int specificness();
	public IType staticType();
	public boolean expandSubtypes();
	
	public abstract class Default {
		public static boolean containsAnyTypeOf(IType instance, IType... types) {
			for (IType t : types)
				if (instance.containsType(t))
					return true;
			return false;
		}
	}
}
