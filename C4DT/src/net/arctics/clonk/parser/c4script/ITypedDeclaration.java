package net.arctics.clonk.parser.c4script;

import net.arctics.clonk.parser.c4script.ast.TypeExpectancyMode;

public interface ITypedDeclaration extends IDeclaration {
	public void expectedToBeOfType(IType t, TypeExpectancyMode mode);
	public IType getType();
	public void forceType(IType type);
	boolean typeIsInvariant();
	
	// interfaces should allow default implementations -.-
	public abstract static class Default {
		public static void expectedToBeOfType(ITypedDeclaration instance, IType type) {
			if (instance.getType() == PrimitiveType.UNKNOWN)
				// unknown before so now it is assumed to be of this type
				instance.forceType(type);
			else if (!instance.getType().equals(type))
				// assignments of multiple types - declaration now has multiple potential types
				instance.forceType(TypeSet.create(type, instance.getType()));
		}
	}
}
