package net.arctics.clonk.c4script.ast;

import java.io.IOException;

import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodePrinter;

public class AppendableBackedExprWriter implements ASTNodePrinter {
	private final Appendable appendable;

	public AppendableBackedExprWriter(Appendable builder) {
		this.appendable = builder;
	}

	@Override
	public boolean doCustomPrinting(ASTNode elm, int depth) {
		return false;
	}

	@Override
	public Appendable append(char c) {
		try {
			return appendable.append(c);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public void append(String text) {
		try {
			appendable.append(text);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Appendable append(CharSequence sequence) throws IOException {
		return appendable.append(sequence);
	}

	@Override
	public Appendable append(CharSequence sequence, int start, int end) throws IOException {
		return appendable.append(sequence, start, end);
	}
	
	@Override
	public String toString() {
		return appendable.toString();
	}

	private int flags;
	
	@Override
	public void enable(int flag) {
		flags |= flag;
	}

	@Override
	public void disable(int flag) {
		flags &= ~flag;
	}

	@Override
	public boolean flag(int flag) {
		return (flags & flag) != 0;
	}
}