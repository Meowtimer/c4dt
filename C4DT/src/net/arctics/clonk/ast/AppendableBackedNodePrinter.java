package net.arctics.clonk.ast;

import java.io.IOException;


public class AppendableBackedNodePrinter implements ASTNodePrinter {
	private final Appendable appendable;

	public AppendableBackedNodePrinter(final Appendable builder) {
		this.appendable = builder;
	}

	@Override
	public boolean doCustomPrinting(final ASTNode elm, final int depth) {
		return false;
	}

	@Override
	public Appendable append(final char c) {
		try {
			return appendable.append(c);
		} catch (final IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public void append(final String text) {
		try {
			appendable.append(text);
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Appendable append(final CharSequence sequence) throws IOException {
		return appendable.append(sequence);
	}

	@Override
	public Appendable append(final CharSequence sequence, final int start, final int end) throws IOException {
		return appendable.append(sequence, start, end);
	}
	
	@Override
	public String toString() {
		return appendable.toString();
	}

	private int flags;
	
	@Override
	public void enable(final int flag) {
		flags |= flag;
	}

	@Override
	public void disable(final int flag) {
		flags &= ~flag;
	}

	@Override
	public boolean flag(final int flag) {
		return (flags & flag) != 0;
	}
}