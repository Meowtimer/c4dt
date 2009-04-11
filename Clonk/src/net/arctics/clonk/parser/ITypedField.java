package net.arctics.clonk.parser;

import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.ExprElm;

public interface ITypedField {
	public void inferTypeFromAssignment(ExprElm val, C4ScriptParser context);
	public void expectedToBeOfType(C4Type t);
	public C4Type getType();
	public void setType(C4Type type);
	public C4Object getExpectedContent();
	public void setExpectedContent(C4Object object);
	
	// interfaces should allow default implementations -.-
	public abstract static class Default {
		public static void expectedToBeOfType(ITypedField instance, C4Type type) {
			if (instance.getType() == C4Type.UNKNOWN)
				// unknown before so now it is assumed to be of this type
				instance.setType(type);
			else if (instance.getType() != type)
				// assignments of multiple types - can be anything
				instance.setType(C4Type.ANY);
		}
		public static void inferTypeFromAssignment(ITypedField instance, ExprElm val, C4ScriptParser context) {
			instance.setExpectedContent(val.guessObjectType(context));
			instance.expectedToBeOfType(val.getType());
		}
	}
}
