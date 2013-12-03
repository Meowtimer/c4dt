package net.arctics.clonk.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodePrinter;

public class Unfinished extends SimpleStatement {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	public Unfinished(final ASTNode expression) { super(expression); }
	@Override
	public void doPrint(final ASTNodePrinter output, final int depth) { expression().print(output, depth); }
}
