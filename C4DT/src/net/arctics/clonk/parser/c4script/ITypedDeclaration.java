package net.arctics.clonk.parser.c4script;

import net.arctics.clonk.index.C4Object;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.ExprElm;

public interface ITypedDeclaration {
	public void inferTypeFromAssignment(ExprElm val, C4ScriptParser context);
	public void expectedToBeOfType(ITypeSet t);
	public ITypeSet getType();
	public void forceType(ITypeSet type);
	public C4Object getObjectType();
	public void setObjectType(C4Object object);
	
	// interfaces should allow default implementations -.-
	public abstract static class Default {
		public static void expectedToBeOfType(ITypedDeclaration instance, ITypeSet type) {
			if (instance.getType() == C4Type.UNKNOWN)
				// unknown before so now it is assumed to be of this type
				instance.forceType(type);
			else if (!instance.getType().equals(type))
				// assignments of multiple types - declaration now has multiple potential types
				instance.forceType(C4TypeSet.registerTypeSet(type, instance.getType()));
		}
		public static void inferTypeFromAssignment(ITypedDeclaration instance, ExprElm val, C4ScriptParser context) {
			instance.setObjectType(val.guessObjectType(context));
			instance.expectedToBeOfType(val.getType(context));
		}
	}
}
