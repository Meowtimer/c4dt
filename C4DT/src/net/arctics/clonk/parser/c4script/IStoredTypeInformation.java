package net.arctics.clonk.parser.c4script;

import net.arctics.clonk.parser.c4script.C4ScriptExprTree.ExprElm;

public interface IStoredTypeInformation {
	IType getType();
	void storeType(IType type);
	boolean generalTypeHint(IType type);
	
	boolean expressionRelevant(ExprElm expr, C4ScriptParser parser);
	boolean sameExpression(IStoredTypeInformation other);
	void apply(boolean soft);
	void merge(IStoredTypeInformation other);
	
	Object clone() throws CloneNotSupportedException; // Cloneable does not declare the method :c
}
