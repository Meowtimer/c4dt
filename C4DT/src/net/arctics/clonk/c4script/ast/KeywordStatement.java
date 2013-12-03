package net.arctics.clonk.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodePrinter;
import net.arctics.clonk.c4script.Conf;

/**
 * Base class for statements which begin with a keyword.
 *
 */
public abstract class KeywordStatement extends Statement {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	/**
	 * The keyword of this statement.
	 * @return The keyword that this statement starts with.
	 */
	public abstract String keyword();

	@Override
	public void doPrint(final ASTNodePrinter builder, final int depth) {
		builder.append(keyword());
		builder.append(";"); //$NON-NLS-1$
	}

	protected void printBody(final ASTNode body, final ASTNodePrinter builder, int depth) {
		if (!(body instanceof Block))
			depth++;
		if (!(body instanceof EmptyStatement)) {
			builder.append('\n');
			Conf.printIndent(builder, depth);
		}
		if (body != null)
			body.print(builder, depth);
	}
}