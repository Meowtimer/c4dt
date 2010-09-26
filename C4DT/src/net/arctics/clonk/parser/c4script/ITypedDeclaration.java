package net.arctics.clonk.parser.c4script;

import net.arctics.clonk.parser.c4script.ast.ExprElm;

public interface ITypedDeclaration {
	public void inferTypeFromAssignment(ExprElm val, C4ScriptParser context);
	public void expectedToBeOfType(IType t);
	public IType getType();
	public void forceType(IType type);
	
	// interfaces should allow default implementations -.-
	public abstract static class Default {
		public static void expectedToBeOfType(ITypedDeclaration instance, IType type) {
			if (instance.getType() == C4Type.UNKNOWN)
				// unknown before so now it is assumed to be of this type
				instance.forceType(type);
			else if (!instance.getType().equals(type))
				// assignments of multiple types - declaration now has multiple potential types
				instance.forceType(C4TypeSet.create(type, instance.getType()));
		}
		public static void inferTypeFromAssignment(ITypedDeclaration instance, ExprElm val, C4ScriptParser context) {
			instance.expectedToBeOfType(val.getType(context));
		}
	}
}
