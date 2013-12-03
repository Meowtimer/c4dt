package net.arctics.clonk.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodePrinter;
import net.arctics.clonk.c4script.Keywords;

public class DoWhileStatement extends WhileStatement {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	public DoWhileStatement(final ASTNode condition, final ASTNode body) {
		super(condition, body);
	}

	@Override
	public void doPrint(final ASTNodePrinter builder, final int depth) {
		builder.append(Keywords.Do);
		printBody(builder, depth);
		builder.append(" "); //$NON-NLS-1$
		builder.append(Keywords.While);
		builder.append(" ("); //$NON-NLS-1$
		condition.print(builder, depth);
		builder.append(");"); //$NON-NLS-1$
	}
}