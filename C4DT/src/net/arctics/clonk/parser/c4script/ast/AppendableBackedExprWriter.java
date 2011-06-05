package net.arctics.clonk.parser.c4script.ast;

import java.io.IOException;

public class AppendableBackedExprWriter implements ExprWriter {
	private final Appendable builder;

	public AppendableBackedExprWriter(Appendable builder) {
		this.builder = builder;
	}

	@Override
	public boolean doCustomPrinting(ExprElm elm, int depth) {
		return false;
	}

	@Override
	public Appendable append(char c) {
		try {
			return builder.append(c);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public void append(String text) {
		try {
			builder.append(text);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Appendable append(CharSequence sequence) throws IOException {
		return builder.append(sequence);
	}

	@Override
	public Appendable append(CharSequence sequence, int start, int end) throws IOException {
		return builder.append(sequence, start, end);
	}
}