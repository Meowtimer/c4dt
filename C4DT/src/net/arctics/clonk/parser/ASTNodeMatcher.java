package net.arctics.clonk.parser;

import static net.arctics.clonk.util.ArrayUtil.concat;
import static net.arctics.clonk.util.Utilities.as;

import java.util.HashMap;
import java.util.Map;

import net.arctics.clonk.c4script.ScriptsHelper;
import net.arctics.clonk.c4script.ast.ASTComparisonDelegate;
import net.arctics.clonk.c4script.ast.AccessVar;
import net.arctics.clonk.c4script.ast.BinaryOp;
import net.arctics.clonk.c4script.ast.Block;
import net.arctics.clonk.c4script.ast.CallExpr;
import net.arctics.clonk.c4script.ast.CombinedMatchingPlaceholder;
import net.arctics.clonk.c4script.ast.MatchingPlaceholder;
import net.arctics.clonk.c4script.ast.Parenthesized;
import net.arctics.clonk.c4script.ast.Placeholder;
import net.arctics.clonk.c4script.ast.Sequence;
import net.arctics.clonk.c4script.ast.Unfinished;
import net.arctics.clonk.c4script.ast.MatchingPlaceholder.Multiplicity;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.parser.ASTNode.ITransformer;
import net.arctics.clonk.util.ArrayUtil;

public class ASTNodeMatcher extends ASTComparisonDelegate {
	public ASTNodeMatcher(ASTNode top) { super(top); }
	public Map<String, Object> result;
	@Override
	public boolean ignoreClassDifference() {
		return
			(left instanceof MatchingPlaceholder &&
			 ((MatchingPlaceholder)left).multiplicity() == Multiplicity.One &&
			 ((MatchingPlaceholder)left).satisfiedBy(right)) ||
			(left instanceof Block && right instanceof Block) ||
			(left instanceof AccessVar && right instanceof AccessVar);
	}
	@Override
	public boolean consume(ASTNode consumer, ASTNode extra) {
		final MatchingPlaceholder mp = as(consumer, MatchingPlaceholder.class);
		if (mp != null && mp.multiplicity() != Multiplicity.One)
			if (mp.satisfiedBy(extra)) {
				final ASTNode[] mpSubElements = mp.subElements();
				if (mpSubElements.length > 0) {
					final ASTNode oldLeft = left; left = mp;
					final ASTNode oldRight = right; right = extra;
					final boolean result = compareSubElements(mpSubElements, extra.subElements()) != null;
					left = oldLeft; right = oldRight;
					return result;
				} else
					return true;
			}
		return false;
	}
	@Override
	public void applyLeftToRightMapping(ASTNode[] leftSubElements, ASTNode[][] leftToRightMapping) {
		for (int i = 0; i < leftSubElements.length; i++) {
			final ASTNode left = leftSubElements[i];
			if (left instanceof MatchingPlaceholder)
				if (leftToRightMapping[i] != null)
					addToResult(leftToRightMapping[i], (MatchingPlaceholder)left);
		}
	}
	private void addToResult(ASTNode extra[], MatchingPlaceholder mp) {
		if (result == null)
			result = new HashMap<String, Object>();
		Object existing = result.get(mp.entryName());
		if (existing instanceof ASTNode)
			existing = concat((ASTNode)existing, extra);
		else if (existing instanceof ASTNode[])
			existing = ArrayUtil.concat((ASTNode[])existing, extra);
		else
			existing = extra;
		result.put(mp.entryName(), existing);
	}
	@Override
	public boolean ignoreLeftSubElement(ASTNode leftNode) {
		return leftNode instanceof MatchingPlaceholder && ((MatchingPlaceholder)leftNode).multiplicity() == Multiplicity.Multiple;
	}
	@Override
	public boolean ignoreSubElementDifference(ASTNode left, ASTNode right) {
		final MatchingPlaceholder mp = as(left, MatchingPlaceholder.class);
		return mp != null && mp.multiplicity() == Multiplicity.One && mp.subElements().length == 0 && mp.satisfiedBy(right);
	}
	/**
	 * Replace {@link Placeholder} objects with {@link MatchingPlaceholder} objects that bring
	 * improved matching capabilities with them.
	 * @return A version of this expression with {@link MatchingPlaceholder} inserted for {@link Placeholder}
	 */
	public static ASTNode matchingExpr(ASTNode node) {
		if (node instanceof Unfinished) {
			final ASTNode orig = node;
			node = Unfinished.unwrap(node);
			node.setParent(orig.parent());
		}
		return node.transformRecursively(new ITransformer() {
			private ASTNode toMatchingPlaceholder(ASTNode expression) {
				if (expression != null)
					if (expression.getClass() == Placeholder.class)
						try {
							return new MatchingPlaceholder(((Placeholder)expression));
						} catch (final ParsingException e) {
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
								} catch (final ParsingException e) {
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
			public Object transform(ASTNode prev, Object prevT, ASTNode expression) {
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
		});

	}
	public static ASTNode matchingExpr(final String statementText, Engine engine) {
		try {
			return matchingExpr(ScriptsHelper.parse(statementText, engine));
		} catch (final ParsingException e) {
			e.printStackTrace();
			return null;
		}
	}
}