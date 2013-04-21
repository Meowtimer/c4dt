package net.arctics.clonk.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.c4script.Keywords;
import net.arctics.clonk.parser.ASTNodePrinter;

public class True extends BoolLiteral {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	@Override
	public boolean booleanValue() {
		return true;
	}
	@Override
	public void doPrint(ASTNodePrinter output, int depth) {
		output.append(Keywords.True);
	}
	@Override
	public Boolean literal() {
		return true;
	}
}
