package net.arctics.clonk.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodePrinter;
import net.arctics.clonk.c4script.Conf;

/**
 * Call the sequence
 * @author madeen
 *
 */
public class CallExpr extends Tuple implements IFunctionCall {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	public CallExpr(final ASTNode... params) { super(params); }

	@Override
	public ASTNode[] params() { return subElements(); }

	@Override
	public boolean isValidInSequence(final ASTNode predecessor) { return predecessor != null; }

	@Override
	public void doPrint(final ASTNodePrinter output, final int depth) { Conf.printNodeList(output, subElements(), depth, "(", ")"); }

	@Override
	public boolean hasSideEffects() { return true; }

	@Override
	public int parmsStart() { return this.start()+1; }

	@Override
	public int parmsEnd() { return this.end()-1; }

	@Override
	public int indexOfParm(final ASTNode parm) {
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] == parm) {
				return i;
			}
		}
		return -1;
	}

}
