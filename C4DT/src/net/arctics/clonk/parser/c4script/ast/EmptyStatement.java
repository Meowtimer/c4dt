package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.Core;

public class EmptyStatement extends Statement {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	@Override
	public void doPrint(ASTNodePrinter builder, int depth) {
		builder.append(";"); //$NON-NLS-1$
	}
}