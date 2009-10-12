package net.arctics.clonk.parser.c4script;

import net.arctics.clonk.index.C4Object;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.ExprElm;


public interface ITypedDeclaration {
	public void inferTypeFromAssignment(ExprElm val, C4ScriptParser context);
	public void expectedToBeOfType(C4Type t);
	public C4Type getType();
	public void forceType(C4Type type);
	public C4Object getObjectType();
	public void setObjectType(C4Object object);
	
	// interfaces should allow default implementations -.-
	public abstract static class Default {
		public static void expectedToBeOfType(ITypedDeclaration instance, C4Type type) {
			if (instance.getType() == C4Type.UNKNOWN)
				// unknown before so now it is assumed to be of this type
				instance.forceType(type);
			else if (instance.getType() != type)
				// assignments of multiple types - can be anything
				instance.forceType(C4Type.ANY);
		}
		public static void inferTypeFromAssignment(ITypedDeclaration instance, ExprElm val, C4ScriptParser context) {
			instance.setObjectType(val.guessObjectType(context));
			instance.expectedToBeOfType(val.getType(context));
		}
	}
}
