package net.arctics.clonk.parser.c4script.ast;


public interface ExprWriter {
	boolean doCustomPrinting(ExprElm elm, int depth);
	void append(String text);
	void append(char c);
}