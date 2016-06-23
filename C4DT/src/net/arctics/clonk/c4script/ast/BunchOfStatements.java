package net.arctics.clonk.c4script.ast;

import java.util.List;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodePrinter;
import net.arctics.clonk.c4script.Conf;

public class BunchOfStatements extends Block {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	public BunchOfStatements(final List<ASTNode> statements) { super(statements); }
	public BunchOfStatements(final ASTNode... statements) { super(statements); }

	@Override
	public void doPrint(final ASTNodePrinter builder, final int depth) {
		boolean first = true;
		for (final ASTNode statement : statements()) {
			statement.printPrefix(builder, depth);
			if (first) {
				first = false;
			} else {
				builder.append("\n"); //$NON-NLS-1$
				Conf.printIndent(builder, depth-1);
			}
			statement.print(builder, depth);
			statement.printSuffix(builder, depth);
		}
	}

}