package net.arctics.clonk.ast;

import static net.arctics.clonk.util.ArrayUtil.concat;
import static net.arctics.clonk.util.Utilities.defaulting;

/**
 * A delegate consulted when comparing AST trees. Its job is deciding whether differences should be ignored or not
 * and controlling how comparisons are conducted to some degree.
 * @author madeen
 *
 */
public class ASTComparisonDelegate {

	public final ASTNode rightTop;
	public ASTNode left, right;

	public ASTComparisonDelegate(final ASTNode rightTop) { this.rightTop = rightTop; }

	/**
	 * Called if some difference was found and an attempt is made to make a previous left node consume a right node
	 * @param consumer The left node to consume.
	 * @param preceding TODO
	 * @param consumee The right node to be consumed.
	 * @return True if consumption was successful, false if not.
	 */
	public boolean consume(final ASTNode consumer, ASTNode[] preceding, final ASTNode consumee) {
		return false;
	}

	/**
	 * Called if class of left and right node differs. Returns true if the difference is accepted by this delegate
	 * and comparison continues.
	 * @return True if the class difference is to be accepted, false if not.
	 */
	public boolean acceptClassDifference() { return false; }
	/**
	 * Return whether to accept attribute differences between the two current nodes.
	 * @return Whether to accept attribute differences or not.
	 */
	public boolean acceptAttributeDifference() { return false; }

	public boolean acceptLeftExtraElement(final ASTNode leftNode) { return false; }
	public boolean acceptRightExtraElement(final ASTNode rightNode) { return false; }
	public boolean acceptSubElementDifference(final ASTNode left, final ASTNode right) { return false; }
	public boolean considerDifferent() { return false; }

	public boolean applyLeftToRightMapping(final ASTNode[] leftSubElements, final ASTNode[][] leftToRightMapping) { return true; }

	protected boolean consistent() { return true; }

	public boolean equal(final ASTNode left, final ASTNode right) {
		if (
			(left == null && right == null) ||
			(left != null && left.compare(right, this)) ||
			acceptSubElementDifference(left, right)
		) {
			if (rightTop == right) {
				applyLeftToRightMapping(new ASTNode[] {left}, new ASTNode[][] {{right}});
				return consistent();
			} else
				return true;
		} else
			return false;
	}

	public boolean lookAhead(Object curLeft, final ASTNode nextLeft, final ASTNode right) { return equal(nextLeft, right); }

	private static final ASTNode[] NO_NODES = new ASTNode[0];

	public ASTNode[][] compareSubElements(final ASTNode[] mine, final ASTNode[] others) {
		final ASTNode[][] leftToRightMapping = new ASTNode[mine.length][];

		int l, r;
		for (l = 0, r = 0; l < mine.length && r < others.length; l++, r++) {
			final ASTNode left = mine[l];
			final ASTNode nextLeft = l + 1 < mine.length ? mine[l+1] : null;
			final ASTNode right = r < others.length ? others[r] : null;

			if (equal(left, right))
				leftToRightMapping[l] = new ASTNode[] { right };
			else if (consume(left, defaulting(leftToRightMapping[l], NO_NODES), right)) {
				if (nextLeft != null && lookAhead(left, nextLeft, right)) {
					// look ahead if the next left one is equal to the current right one
					leftToRightMapping[l+1] = new ASTNode[] { right };
					l++;
				} else {
					// consume
					leftToRightMapping[l] = concat(leftToRightMapping[l], right);
					l--;
				}
			}
			else if (acceptLeftExtraElement(left)) {
				if (leftToRightMapping[l] == null)
					leftToRightMapping[l] = NO_NODES;
				r--;
			}
			else if (acceptRightExtraElement(right))
				l--;
			else
				return null;
		}

		// not all nodes looked at - see whether the extra elements can be accepted or consumed
		if (l != mine.length || r != others.length) {
			for (; l < mine.length; l++)
				if (leftToRightMapping[l] != null)
					continue;
				else if (acceptLeftExtraElement(mine[l]))
					leftToRightMapping[l] = new ASTNode[0];
				else
					return null;
			final ASTNode lastLeft = mine.length > 0 ? mine[mine.length-1] : null;
			if (lastLeft != null)
				for (; r < others.length; r++)
					if (consume(lastLeft, defaulting(leftToRightMapping[mine.length-1], NO_NODES), others[r]))
						leftToRightMapping[mine.length-1] = concat(leftToRightMapping[mine.length-1], others[r]);
					else
						break;
			for (; r < others.length; r++)
				if (!acceptRightExtraElement(others[r]))
					return null;
		}

		return leftToRightMapping;
	}
}
