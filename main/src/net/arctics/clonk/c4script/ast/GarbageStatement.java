package net.arctics.clonk.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNodePrinter;

public class GarbageStatement extends Statement {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	private String garbage;
	public String garbage() { return garbage; }

	public GarbageStatement(final String garbageString, final int start) {
		garbage = garbageString;
		setLocation(start, start+garbage.length());
	}

	public String getGarbage() {
		return garbage;
	}

	@Override
	public void doPrint(final ASTNodePrinter output, final int depth) {
		output.append(garbage);
	}

	public void setGarbage(final String garbage) {
		this.garbage = garbage;
	}

}
