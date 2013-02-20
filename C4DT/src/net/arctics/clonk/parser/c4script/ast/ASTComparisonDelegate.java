package net.arctics.clonk.parser.c4script.ast;

import static net.arctics.clonk.util.ArrayUtil.concat;
import static net.arctics.clonk.util.Utilities.token;
import net.arctics.clonk.parser.ASTNode;

/**
 * A delegate consulted when comparing AST trees. Its job is deciding whether differences should be ignored or not
 * and controlling how comparisons are conducted to some degree.
 * @author madeen
 *
 */
public class ASTComparisonDelegate {

	/**
	 * Passed as the 'what' parameter to {@link #differs(ASTNode, ASTNode, Object)} if length of respective sub elements differs.
	 */
	public static final Object SUBELEMENTS_LENGTH = token("SUBELEMENTS_LENGTH");

	/**
	 * Passed as the 'what' parameter to {@link #differs(ASTNode, ASTNode, Object)} if the {@link ASTNode} elements being compared have differen types.
	 */
	public static final Object CLASS = token("CLASS");

	/**
	 * Special 'what' token informing delegate that previous cautious {@link boolean#IgnoreLeftSideOrTryNext} answer
	 * was honored.
	 */
	public static final Object IGNORELEFTSIDEHONORED = token("IGNORELEFTSIDECHOSEN");

	/**
	 * Special node passed for {@link Difference#left} or {@link Difference#right} if the respective side has run out of nodes while the other one has not.
	 */
	public static final ASTNode OUTOFSTOCK = new ASTNode();

	public final ASTNode top;
	public ASTNode left, right;

	public ASTComparisonDelegate(ASTNode top) { this.top = top; }

	/**
	 * Called if some difference was found and an attempt is made to make a previous left node consume a right node
	 * @param consumer The left node to consume.
	 * @param consumee The right node to be consumed.
	 * @return True if consumption was successful, false if not.
	 */
	public boolean consume(ASTNode consumer, ASTNode consumee) {
		return false;
	}

	/**
	 * Called if class of left and right node differs. Can order the comparer to ignore this difference.
	 * @return True if the class difference is to be ignored, false if not.
	 */
	public boolean ignoreClassDifference() { return false; }
	public boolean ignoreAttributeDifference() { return false; }
	public boolean ignoreLeftSubElement(ASTNode leftNode) { return false; }
	public boolean ignoreRightSubElement(ASTNode rightNode) { return false; }
	public boolean ignoreSubElementDifference(ASTNode left, ASTNode right) { return false; }
	public boolean considerDifferent() { return false; }

	public void applyLeftToRightMapping(ASTNode[] leftSubElements, ASTNode[][] leftToRightMapping) {}

	public boolean equal(ASTNode left, ASTNode right) {
		if (
			(left == null && right == null) ||
			(left != null && left.compare(right, this)) ||
			ignoreSubElementDifference(left, right)
		) {
			if (top == right)
				applyLeftToRightMapping(new ASTNode[] {left}, new ASTNode[][] {{right}});
			return true;
		} else
			return false;
	}

	public ASTNode[][] compareSubElements(ASTNode[] mine, ASTNode[] others) {
		ASTNode[][] leftToRightMapping = new ASTNode[mine.length][];

		int l, r;
		for (l = 0, r = 0; l < mine.length && r < others.length; l++, r++) {
			//ASTNode prevLeft = l > 0 ? mine[l-1] : null;
			ASTNode left = mine[l];
			ASTNode nextLeft = l + 1 < mine.length ? mine[l+1] : null;

			ASTNode right = r < others.length ? others[r] : null;
			//ASTNode nextRight = r + 1 < others.length ? others[r+1] : null;

			if (equal(left, right))
				leftToRightMapping[l] = new ASTNode[] { right };
			else if  (consume(left, right)) {
				if (nextLeft == null || !equal(nextLeft, right)) {
					leftToRightMapping[l] = concat(leftToRightMapping[l], right);
					l--;
				} else {
					leftToRightMapping[l+1] = new ASTNode[] { right };
					l++;
				}
			}
			else if (ignoreLeftSubElement(left)) {
				leftToRightMapping[l] = new ASTNode[0];
				r--;
			}
			else if (ignoreRightSubElement(right))
				l--;
			else
				return null;
		}

		if (l != mine.length || r != others.length) {
			for (; l < mine.length; l++)
				if (leftToRightMapping[l] != null)
					continue;
				else if (ignoreLeftSubElement(mine[l]))
					leftToRightMapping[l] = new ASTNode[0];
				else
					return null;
			ASTNode lastLeft = mine.length > 0 ? mine[mine.length-1] : null;
			if (lastLeft != null)
				for (; r < others.length; r++)
					if (consume(lastLeft, others[r]))
						leftToRightMapping[mine.length-1] = concat(leftToRightMapping[mine.length-1], others[r]);
					else
						break;
			for (; r < others.length; r++)
				if (!ignoreRightSubElement(others[r]))
					return null;
		}
		return leftToRightMapping;
	}
}
