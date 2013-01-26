package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ASTNodePrinter;

public class GarbageStatement extends Statement {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	private String garbage;
	public String garbage() { return garbage; }

	public GarbageStatement(String garbageString, int start) {
		garbage = garbageString;
		setLocation(start, start+garbage.length());
	}

	public String getGarbage() {
		return garbage;
	}

	@Override
	public void doPrint(ASTNodePrinter output, int depth) {
		output.append(garbage);
	}

	public void setGarbage(String garbage) {
		this.garbage = garbage;
	}

}
