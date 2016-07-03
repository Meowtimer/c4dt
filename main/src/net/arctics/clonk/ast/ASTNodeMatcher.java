package net.arctics.clonk.ast;

import static net.arctics.clonk.util.ArrayUtil.concat;
import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.eq;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import net.arctics.clonk.ProblemException;
import net.arctics.clonk.ast.MatchingPlaceholder.Multiplicity;
import net.arctics.clonk.c4script.Standalone;
import net.arctics.clonk.c4script.ast.AccessVar;
import net.arctics.clonk.c4script.ast.Block;
import net.arctics.clonk.c4script.ast.SimpleStatement;
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
	public ASTNodeMatcher(final ASTNode top) { super(top); }
	public Map<MatchingPlaceholder, Object> result;
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
	public boolean acceptAttributeDifference() { return left instanceof MatchingPlaceholder; }
	@Override
	public boolean consume(final ASTNode consumer, ASTNode[] preceding, final ASTNode extra) {
		final MatchingPlaceholder mp = as(consumer, MatchingPlaceholder.class);
		if (mp != null && mp.multiplicity().acceptable(preceding.length+1))
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
	public boolean lookAhead(Object curLeft, ASTNode nextLeft, ASTNode right) {
		final MatchingPlaceholder mc = as(curLeft, MatchingPlaceholder.class);
		final MatchingPlaceholder mn = as(nextLeft, MatchingPlaceholder.class);
		final boolean _do =
			(mc == null || (mc.multiplicity().absolute() == null && mc.simple())) &&
			(mn == null || !mn.simpleAndMultiplicityOne());
		return _do ? super.lookAhead(curLeft, nextLeft, right) : false;
	}
	@Override
	protected boolean consistent() {
		return result == null || result.entrySet().stream()
			// elements without proto are the ones whose match results count while...
			.filter(e -> e.getKey().proto() == null)
			.allMatch(e ->
				result.entrySet().stream()
					// elements with a proto reference to those elements...
					.filter(s -> s.getKey().proto() == e.getKey())
					// are required to have matched the same stuff
					.allMatch(s -> same(s.getValue(), e.getValue())) &&
				// also, matching results for a placeholder must be consistent with the multiplicity and such
				e.getKey().consistent(e.getValue())
		);
	}
	private static boolean same(Object a, Object b) {
		return a.getClass() == b.getClass() &&
			a instanceof Object[] ? ArrayUtil.same((Object[])a, (Object[])b, ASTNodeMatcher::same) :
			eq(a, b);
	}
	@Override
	public boolean applyLeftToRightMapping(final ASTNode[] leftSubElements, final ASTNode[][] leftToRightMapping) {
		for (int i = 0; i < leftSubElements.length; i++) {
			final ASTNode left = leftSubElements[i];
			final ASTNode[] mapping = leftToRightMapping[i];
			final MatchingPlaceholder mp = as(left, MatchingPlaceholder.class);
			if (mp != null) {
				if (mp.multiplicity() == Multiplicity.AtLeastOne && (mapping == null || mapping.length == 0))
					return false;
				if (mapping != null)
					addToResult(mp, mapping);
			}
		}
		return true;
	}
	private void addToResult(final MatchingPlaceholder mp, final ASTNode extra[]) {
		if (result == null)
			result = new HashMap<MatchingPlaceholder, Object>();
		final Object existing = result.get(mp);
		final Object mut =
			existing instanceof ASTNode ? concat((ASTNode)existing, extra) :
			existing instanceof ASTNode[] ? ArrayUtil.concat((ASTNode[])existing, extra) :
			extra;
		result.put(mp, mut);
	}
	@Override
	public boolean acceptLeftExtraElement(final ASTNode leftNode) {
		if (leftNode instanceof MatchingPlaceholder) {
			final Multiplicity m = ((MatchingPlaceholder)leftNode).multiplicity();
			return m != Multiplicity.One;
		}
		else
			return false;
	}
	@Override
	public boolean acceptSubElementDifference(final ASTNode left, final ASTNode right) {
		final MatchingPlaceholder mp = as(left, MatchingPlaceholder.class);
		return mp != null && mp.multiplicity() == Multiplicity.One && mp.subElements().length == 0 && mp.satisfiedBy(right);
	}
	/**
	 * Replace {@link Placeholder} objects with {@link MatchingPlaceholder}, making the result suitable for
	 * being the target of a {@link ASTNode#match(ASTNode)} call.
	 * @return A version of this expression with {@link MatchingPlaceholder} inserted for {@link Placeholder}
	 */
	public static ASTNode parsePlaceholders(ASTNode node) {
		if (node instanceof Unfinished) {
			final ASTNode orig = node;
			node = SimpleStatement.unwrap(node);
			node.setParent(orig.parent());
		}
		return node.transformRecursively(MatchingPlaceholderTransformer.INSTANCE);
	}
	/**
	 * Prepare a parsed node for being a matching template by replacing regular {@link Placeholder}s with {@link MatchingPlaceholder}s.
	 * @param node The node to prepare
	 * @return The prepared node.
	 */
	public static ASTNode prepareForMatching(final ASTNode node) {
		final ASTNode transformed = parsePlaceholders(node);
		final Map<String, MatchingPlaceholder> mps = new HashMap<>();
		transformed.traverse((n, c) -> {
			final MatchingPlaceholder mp = as(n, MatchingPlaceholder.class);
			if (mp != null && !mp.entryName().equals("")) {
				final MatchingPlaceholder x = mps.get(mp.entryName());
				if (x != null) {
					if (!mp.simpleAndMultiplicityOne())
						throw new IllegalArgumentException(String.format("%s is a repeat occurence - needs to be simple", mp.printed()));
					mp.proto(x);
				} else
					mps.put(mp.entryName(), mp);
			}
			return TraversalContinuation.Continue;
		}, null);
		return transformed;
	}
	/**
	 * Parse a node from a C4Script source string and return the result of calling {@link #prepareForMatching(ASTNode)} with that node.
	 * @param statementText C4Script string to parse the node from
	 * @param engine Engine to use for parsing.
	 * @return Prepared-for-matching version of the node parsed from the string or null if something went wrong.
	 */
	public static ASTNode prepareForMatching(final String statementText, final Engine engine) {
		try {
			return prepareForMatching(new Standalone(engine).parse(statementText));
		} catch (final ProblemException e) {
			e.printStackTrace();
			return null;
		}
	}
	/**
	 * Return a map containing the matching results.
	 * @return The matching result as a map with keys being the names of the placeholders and the values being either arrays or single instances of {@link ASTNode}.
	 */
	public Map<String, Object> result() {
		try {
		return result != null ? result.entrySet().stream()
			.filter(e -> e.getKey().proto() == null && !e.getKey().entryName().equals(""))
			.collect(Collectors.toMap(e -> e.getKey().entryName(), e -> e.getValue()))
			: Collections.emptyMap();
		} catch (final Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}