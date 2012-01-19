package net.arctics.clonk.parser.c4script.ast;


public interface ExprWriter extends Appendable {
	boolean doCustomPrinting(ExprElm elm, int depth);
	void append(String text);
	@Override
	Appendable append(char c);
}