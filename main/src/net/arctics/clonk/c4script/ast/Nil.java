package net.arctics.clonk.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNodePrinter;
import net.arctics.clonk.c4script.Keywords;

public class Nil extends Literal<Object> {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	@Override
	public Object literal() {
		return null;
	}

	@Override
	public void doPrint(final ASTNodePrinter output, final int depth) {
		output.append(Keywords.Nil);
	}

}
