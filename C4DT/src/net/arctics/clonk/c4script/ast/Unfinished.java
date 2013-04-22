package net.arctics.clonk.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodePrinter;

public class Unfinished extends SimpleStatement {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	public Unfinished(ASTNode expression) { super(expression); }
	@Override
	public void doPrint(ASTNodePrinter output, int depth) { expression().print(output, depth); }
}
