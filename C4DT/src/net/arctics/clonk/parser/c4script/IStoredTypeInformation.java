package net.arctics.clonk.parser.c4script;

import net.arctics.clonk.index.C4Object;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.ExprElm;

public interface IStoredTypeInformation {
	C4Object getObjectType();
	void storeObjectType(C4Object objectType);
	ITypeSet getType();
	void storeType(ITypeSet type);
	
	boolean expressionRelevant(ExprElm expr, C4ScriptParser parser);
	boolean sameExpression(IStoredTypeInformation other);
	void apply(boolean soft);
	void merge(IStoredTypeInformation other);
	
	Object clone() throws CloneNotSupportedException; // Cloneable does not declare the method :c
}
