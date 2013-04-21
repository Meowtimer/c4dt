package net.arctics.clonk.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ASTNodePrinter;

public class EmptyStatement extends Statement {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	@Override
	public void doPrint(ASTNodePrinter builder, int depth) {
		builder.append(";"); //$NON-NLS-1$
	}
}