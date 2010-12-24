package net.arctics.clonk.parser.c4script.ast;

public interface IDifferenceListener {
	
	public enum Option {
		CheckForIdentity
	}
	
	public static final Object SUBELEMENTS_LENGTH = new Object();
	
	void differs(ExprElm a, ExprElm b, Object what);
	boolean optionEnabled(Option option);
}
