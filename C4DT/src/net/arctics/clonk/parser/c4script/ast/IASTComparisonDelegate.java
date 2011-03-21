package net.arctics.clonk.parser.c4script.ast;

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
		Equal
	}
	
	/**
	 * Passed as the 'what' parameter to {@link #differs(ExprElm, ExprElm, Object)} if length of respective sub elements differs.
	 */
	public static final Object SUBELEMENTS_LENGTH = new Object();
	
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
}
