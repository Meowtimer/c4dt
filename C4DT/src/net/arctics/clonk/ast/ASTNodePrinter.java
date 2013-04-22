package net.arctics.clonk.ast;

/**
 * Interface to be passed to {@link ASTNode} objects to let them print themselves.
 * Inherent printing of an {@link ASTNode} class is implemented by overriding {@link ASTNode#doPrint(ASTNodePrinter, int)}. 
 * @author madeen
 *
 */
public interface ASTNodePrinter extends Appendable {
	
	/**
	 * Flag causing the output to be put on a single line. This flag needs to be honored by the actual printing methods.
	 */
	static final int SINGLE_LINE = 1;
	
	/**
	 * Interception method to customize printing of a node.
	 * @param elm The node to print
	 * @param depth Current indentation depth
	 * @return Return true if the printer did its custom printing thing, cutting short the node's inherent printing.
	 */
	boolean doCustomPrinting(ASTNode elm, int depth);
	/**
	 * Append text.
	 * @param text The text string
	 */
	void append(String text);
	/**
	 * Enable one of the flags defined in this interface.
	 * @param flag
	 */
	void enable(int flag);
	/**
	 * Disable one of the flags defined in this interface.
	 * @param flag
	 */
	void disable(int flag);
	/**
	 * Return whether some flag is set.
	 * @param flag The flag to test
	 * @return True if set, false if not.
	 */
	boolean flag(int flag);
	/** Append a single character. **/
	@Override
	Appendable append(char c);
}