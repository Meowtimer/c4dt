package net.arctics.clonk.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNodePrinter;

public class EmptyStatement extends Statement {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	@Override
	public void doPrint(final ASTNodePrinter builder, final int depth) {
		builder.append(";"); //$NON-NLS-1$
	}

}