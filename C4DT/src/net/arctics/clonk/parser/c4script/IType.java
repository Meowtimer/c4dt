package net.arctics.clonk.parser.c4script;

import java.io.Serializable;

import net.arctics.clonk.index.ClonkIndex;

/**
 * Classes conforming to this interface may be a specific type or a set of types
 */
public interface IType extends Iterable<IType>, Serializable {
	boolean canBeAssignedFrom(IType other);
	String typeName(boolean special);
	boolean intersects(IType typeSet);
	boolean containsType(IType type);
	boolean containsAnyTypeOf(IType... types);
	int specificness();
	IType staticType();
	IType serializableVersion(ClonkIndex indexToBeSerialized);
	
	public abstract class Default {
		public static boolean containsAnyTypeOf(IType instance, IType... types) {
			for (IType t : types)
				if (instance.containsType(t))
					return true;
			return false;
		}
	}
}
