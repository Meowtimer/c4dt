package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.parser.ExprElm;


public interface ExprWriter extends Appendable {
	static final int SINGLE_LINE = 1;
	
	boolean doCustomPrinting(ExprElm elm, int depth);
	void append(String text);
	void enable(int flag);
	void disable(int flag);
	boolean flag(int flag);
	@Override
	Appendable append(char c);
}