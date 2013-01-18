package net.arctics.clonk.parser.c4script.ast;

import static net.arctics.clonk.util.Utilities.token;
import net.arctics.clonk.parser.ExprElm;

/**
 * A delegate consulted when comparing AST trees. Its job is deciding whether differences should be ignored or not
 * and controlling how comparisons are conducted to some degree.
 * @author madeen
 *
 */
public interface IASTComparisonDelegate {
	
	/**
	 * Options the delegate can specify.
	 * @author madeen
	 *
	 */
	public enum Option {
		/** When comparing AccessDeclaration elements, check for identity rather than name equality */
		CheckForIdentity
	}
	
	/**
	 * Enum informing the caller about how the comparison should be continued or not.
	 * @author madeen
	 *
	 */
	public enum DifferenceHandling {
		/** Delegate agrees that there is a difference so stop comparing. */
		Differs,
		/** Delegate tells caller to ignore the left side of the comparison. */
		IgnoreLeftSide,
		/** Delegate tells caller to ignore the right side of the comparison. */
		IgnoreRightSide,
		/** Value to be returned by ExprElm.compare when no differences were found. Should not be returned by the delegate. */
		Equal,
		/** Like {@link #Equal}, but signaling the caller to cease further comparisons. Used for compare overrides (see {@link Literal}) */
		EqualShortCircuited;
		
		/**
		 * Return true if this difference handling is {@link #Equal} or {@link #EqualShortCircuited}.
		 * @return See above.
		 */
		public boolean isEqual() {
			return this == Equal || this == EqualShortCircuited;
		}
	}
	
	/**
	 * Passed as the 'what' parameter to {@link #differs(ExprElm, ExprElm, Object)} if length of respective sub elements differs.
	 */
	public static final Object SUBELEMENTS_LENGTH = token("SUBELEMENTS_LENGTH");
	
	/**
	 * Passed as the 'what' parameter to {@link #differs(ExprElm, ExprElm, Object)} if the {@link ExprElm} elements being compared have differen types.
	 */
	public static final Object CLASS = token("CLASS");
	
	/**
	 * Called by the caller conducting the comparison when a difference has been detected.
	 * @param a The element on the left side of the comparison (the one compare is being called on)
	 * @param b The other element that is being compared to a
	 * @param what Object specifying the specific difference. Can be a string denoting the field differing or some special object (see {@link #SUBELEMENTS_LENGTH})
	 * @return Return a {@link DifferenceHandling} value specifying what should be done.
	 */
	DifferenceHandling differs(ExprElm a, ExprElm b, Object what);
	
	/**
	 * Inform caller about the options the delegate wants to have enabled.
	 * @param option The option to test
	 * @return Whether the option is activated or not
	 */
	boolean optionEnabled(Option option);
	
	/**
	 * Called when some wildcard was matched.
	 * @param wildcard The matched wildcard
	 * @param expression The expression matching the wildcard
	 */
	void wildcardMatched(Wildcard wildcard, ExprElm expression);
}
