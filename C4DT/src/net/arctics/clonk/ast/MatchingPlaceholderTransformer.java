package net.arctics.clonk.ast;

import static net.arctics.clonk.util.Utilities.as;
import net.arctics.clonk.ProblemException;
import net.arctics.clonk.c4script.ast.BinaryOp;
import net.arctics.clonk.c4script.ast.CallExpr;
import net.arctics.clonk.c4script.ast.CombinedMatchingPlaceholder;
import net.arctics.clonk.c4script.ast.Parenthesized;

/**
 * {@link ITransformer} to transform a regularly parsed expression tree
 * containing {@link Placeholder} nodes into a tree where those placeholers
 * are replaced with {@link MatchingPlaceholder} objects whose presence and configuration
 * guide the {@link ASTNode#match(ASTNode)} invocation.
 * @author madeen
 *
 */
enum MatchingPlaceholderTransformer implements ITransformer {
	/** Singleton */
	INSTANCE;
	private ASTNode toMatchingPlaceholder(final ASTNode expression) {
		if (expression != null)
			if (expression.getClass() == Placeholder.class)
				try {
					return new MatchingPlaceholder(((Placeholder)expression));
				} catch (final ProblemException e) {
					e.printStackTrace();
					return null;
				}
			else if (expression instanceof BinaryOp) {
				final BinaryOp bop = (BinaryOp) expression;
				switch (bop.operator()) {
				case And: case Or: case BitAnd: case BitOr:
					final MatchingPlaceholder mpl = as(bop.leftSide(), MatchingPlaceholder.class);
					final MatchingPlaceholder mpr = as(bop.rightSide(), MatchingPlaceholder.class);
					if (mpl != null && mpr != null)
						try {
							return new CombinedMatchingPlaceholder(mpl, mpr, bop.operator());
						} catch (final ProblemException e) {
							e.printStackTrace();
							return null;
						}
					break;
				default:
					break;
				}
			} else if (expression instanceof Parenthesized && ((Parenthesized)expression).innerExpression() instanceof MatchingPlaceholder)
				return ((Parenthesized)expression).innerExpression();
		return null;
	}
	@Override
	public Object transform(final ASTNode prev, final Object prevT, final ASTNode expression) {
		final ASTNode matchingPlaceholder = toMatchingPlaceholder(expression);
		if (matchingPlaceholder != null)
			return matchingPlaceholder;
		else if (expression instanceof CallExpr && prevT instanceof MatchingPlaceholder) {
			((MatchingPlaceholder)prevT).setSubElements(expression.transformRecursively(this).subElements());
			return REMOVE;
		} else if (
			expression instanceof Sequence &&
			expression.subElements().length == 1 && expression.subElements()[0] instanceof MatchingPlaceholder
		)
			return expression.subElements()[0];
		return expression;
	}
}