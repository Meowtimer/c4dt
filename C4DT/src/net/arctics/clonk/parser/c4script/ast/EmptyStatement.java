package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.ClonkCore;

public class EmptyStatement extends Statement {
	/**
	 * 
	 */
	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	@Override
	public void doPrint(ExprWriter builder, int depth) {
		builder.append(";"); //$NON-NLS-1$
	}
}