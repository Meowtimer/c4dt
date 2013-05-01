package net.arctics.clonk.c4script.ast;

import net.arctics.clonk.ast.ASTNode;

public interface ITidyable {
	/**
	 * Returns an expression that is functionally equivalent to the original expression but modified to adhere to #strict/#strict 2 rules and be more readable.
	 * For example, And/Or function calls get replaced by operators, uses of the Call function get converted to direct function calls.
	 * This method tries to reuse existing objects and reassigns the parents of those objects so the original ExprElm tree might be invalid in subtle ways.
	 * @param tidy Object performing the tidying
	 * @return a #strict/#strict 2/readability enhanced version of the original expression
	 * @throws CloneNotSupportedException
	 */
	ASTNode tidy(Tidy tidy) throws CloneNotSupportedException;
}
