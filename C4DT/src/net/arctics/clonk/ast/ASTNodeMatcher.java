package net.arctics.clonk.ast;

import static net.arctics.clonk.util.ArrayUtil.concat;
import static net.arctics.clonk.util.Utilities.as;

import java.util.HashMap;
import java.util.Map;

import net.arctics.clonk.ProblemException;
import net.arctics.clonk.ast.MatchingPlaceholder.Multiplicity;
import net.arctics.clonk.c4script.ScriptsHelper;
import net.arctics.clonk.c4script.ast.AccessVar;
import net.arctics.clonk.c4script.ast.Block;
import net.arctics.clonk.c4script.ast.Unfinished;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.util.ArrayUtil;

/**
 * {@link ASTComparisonDelegate} used by {@link ASTNode#match(ASTNode)}.
 * {@link MatchingPlaceholder} objects present in the target of the {@link ASTNode#match(ASTNode)} call
 * will guide the comparison invocation.
 * A map mapping {@link MatchingPlaceholder} names to arrays of matched {@link ASTNode}s will be populated
 * and returned by {@link ASTNode#match(ASTNode)} on successful matching of all nodes.
 * To prepare a node for being the target of {@link ASTNode#match(ASTNode)} {@link #prepareForMatching(ASTNode)} needs
 * to be called with this node as argument.
 * @author madeen
 */
public class ASTNodeMatcher extends ASTComparisonDelegate {
	public ASTNodeMatcher(ASTNode top) { super(top); }
	public Map<String, Object> result;
	@Override
	public boolean acceptClassDifference() {
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
	public boolean applyLeftToRightMapping(ASTNode[] leftSubElements, ASTNode[][] leftToRightMapping) {
		for (int i = 0; i < leftSubElements.length; i++) {
			final ASTNode left = leftSubElements[i];
			final ASTNode[] mapping = leftToRightMapping[i];
			if (left instanceof MatchingPlaceholder) {
				final MatchingPlaceholder mp = (MatchingPlaceholder)left;
				switch (mp.multiplicity()) {
				case AtLeastOne:
					if (mapping == null || mapping.length < 1)
						return false;
					break;
				default:
					break;
				}
				if (mapping != null)
					addToResult(mp, mapping);
			}
		}
		return true;
	}
	private void addToResult(MatchingPlaceholder mp, ASTNode extra[]) {
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
	public boolean acceptLeftExtraElement(ASTNode leftNode) {
		if (leftNode instanceof MatchingPlaceholder)
			switch (((MatchingPlaceholder) leftNode).multiplicity()) {
			case AtLeastOne: case Multiple:
				return true;
			default:
				return false;
			}
		else
			return false;
	}
	@Override
	public boolean acceptSubElementDifference(ASTNode left, ASTNode right) {
		final MatchingPlaceholder mp = as(left, MatchingPlaceholder.class);
		return mp != null && mp.multiplicity() == Multiplicity.One && mp.subElements().length == 0 && mp.satisfiedBy(right);
	}
	/**
	 * Replace {@link Placeholder} objects with {@link MatchingPlaceholder}, making the result suitable for
	 * being the target of a {@link ASTNode#match(ASTNode)} call.
	 * @return A version of this expression with {@link MatchingPlaceholder} inserted for {@link Placeholder}
	 */
	public static ASTNode prepareForMatching(ASTNode node) {
		if (node instanceof Unfinished) {
			final ASTNode orig = node;
			node = Unfinished.unwrap(node);
			node.setParent(orig.parent());
		}
		return node.transformRecursively(MatchingPlaceholderTransformer.INSTANCE);
	}
	/**
	 * Parse a node from a C4Script source string and return the result of calling {@link #prepareForMatching(ASTNode)} with that node.
	 * @param statementText C4Script string to parse the node from
	 * @param engine Engine to use for parsing.
	 * @return Prepared-for-matching version of the node parsed from the string or null if something went wrong.
	 */
	public static ASTNode prepareForMatching(final String statementText, Engine engine) {
		try {
			return prepareForMatching(ScriptsHelper.parse(statementText, engine));
		} catch (final ProblemException e) {
			e.printStackTrace();
			return null;
		}
	}
}