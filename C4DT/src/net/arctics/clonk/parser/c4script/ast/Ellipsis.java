package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.ASTNodePrinter;

public class Ellipsis extends ASTNode {


	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	public Ellipsis() {
		super();
	}

	@Override
	public void doPrint(ASTNodePrinter output, int depth) {
		output.append("..."); //$NON-NLS-1$
	}

	@Override
	public boolean isValidAtEndOfSequence() {
		return false;
	}

}