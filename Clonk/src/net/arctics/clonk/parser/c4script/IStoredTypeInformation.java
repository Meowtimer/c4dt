package net.arctics.clonk.parser.c4script;

import net.arctics.clonk.index.C4Object;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.ExprElm;

public interface IStoredTypeInformation {
	C4Object getObjectType();
	void storeObjectType(C4Object objectType);
	C4Type getType();
	void storeType(C4Type type);
	
	boolean expressionRelevant(ExprElm expr);
	boolean sameExpression(IStoredTypeInformation other);
	void apply(boolean soft);
	void merge(IStoredTypeInformation other);
	
	Object clone() throws CloneNotSupportedException; // Cloneable does not declare the method :c
}
