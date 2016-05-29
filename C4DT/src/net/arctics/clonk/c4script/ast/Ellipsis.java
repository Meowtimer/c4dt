package net.arctics.clonk.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodePrinter;

public class Ellipsis extends ASTNode {

	public static final String ELLIPSIS = "..."; //$NON-NLS-1$

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	public Ellipsis() {
		super();
	}

	@Override
	public void doPrint(final ASTNodePrinter output, final int depth) {
		output.append(ELLIPSIS);
	}

	@Override
	public boolean isValidAtEndOfSequence() {
		return false;
	}

}