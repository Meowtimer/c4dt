package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.c4script.Conf;

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
	public void doPrint(ASTNodePrinter builder, int depth) {
		builder.append(keyword());
		builder.append(";"); //$NON-NLS-1$
	}

	protected void printBody(ASTNode body, ASTNodePrinter builder, int depth) {
		if (!(body instanceof Block))
			depth++;
		if (!(body instanceof EmptyStatement))
			Conf.blockPrelude(builder, depth);
		body.print(builder, depth);
	}
}