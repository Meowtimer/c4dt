package net.arctics.clonk.ast;

import net.arctics.clonk.ProblemException;
import net.arctics.clonk.c4script.ast.ArrayElementExpression;
import net.arctics.clonk.c4script.ast.ArrayExpression;
import net.arctics.clonk.c4script.ast.BinaryOp;
import net.arctics.clonk.c4script.ast.CallExpr;
import net.arctics.clonk.c4script.ast.CombinedMatchingPlaceholder;
import net.arctics.clonk.c4script.ast.Parenthesized;
import net.arctics.clonk.c4script.ast.SimpleStatement;

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
			if (expression instanceof MatchingPlaceholder)
				return expression;
			else if (expression.getClass() == Placeholder.class)
				try {
					return new MatchingPlaceholder(((Placeholder)expression));
				} catch (final ProblemException e) {
					e.printStackTrace();
					return null;
				}
			else if (expression instanceof Parenthesized && ((Parenthesized)expression).innerExpression() instanceof MatchingPlaceholder)
				return ((Parenthesized)expression).innerExpression();
			else if (expression instanceof SimpleStatement && ((SimpleStatement)expression).expression() instanceof MatchingPlaceholder)
				return ((SimpleStatement)expression).expression();
		return null;
	}
	private MatchingPlaceholder subExpressionToMatchingPlaceholder(final ASTNode expression) {
		final ASTNode node = toMatchingPlaceholder(expression);
		if (node instanceof MatchingPlaceholder)
			return (MatchingPlaceholder) node;
		if (expression instanceof BinaryOp) {
			final BinaryOp bop = (BinaryOp) expression;
			switch (bop.operator()) {
			case BitAnd: case BitOr:
				final MatchingPlaceholder mpl = subExpressionToMatchingPlaceholder(bop.leftSide());
				final MatchingPlaceholder mpr = subExpressionToMatchingPlaceholder(bop.rightSide());
				if (mpl != null && mpr != null)
					try {
						return new CombinedMatchingPlaceholder(mpl, mpr, bop.operator());
					} catch (final ProblemException e) {
						e.printStackTrace();
						return null;
					}
				else
					return null;
			default:
				return null;
			}
		} else
			return null; 
	}
	private MatchingPlaceholder unwrapSub(final ArrayElementExpression e) {
		if (e.argument() instanceof ArrayExpression && e.argument().subElements().length == 1) {
			final MatchingPlaceholder sub = subExpressionToMatchingPlaceholder(e.argument().subElements()[0]);
			return sub;
		} else
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
		} else if (expression instanceof ArrayElementExpression && prevT instanceof MatchingPlaceholder) {
			final MatchingPlaceholder sub = unwrapSub((ArrayElementExpression) expression);
			if (sub != null) {
				((MatchingPlaceholder)prevT).sub(sub);
				return REMOVE;
			} else
				return expression;
		} else if (
			expression instanceof Sequence &&
			expression.subElements().length == 1 && expression.subElements()[0] instanceof MatchingPlaceholder
		)
			return expression.subElements()[0];
		return expression;
	}
}