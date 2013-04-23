package net.arctics.clonk.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodePrinter;

/**
 * Call the sequence
 * @author madeen
 *
 */
public class CallExpr extends Tuple implements IFunctionCall {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	public CallExpr(ASTNode[] params) { super(params); }
	@Override
	public ASTNode[] params() { return subElements(); }
	@Override
	public boolean isValidInSequence(ASTNode predecessor) { return predecessor != null; }
	@Override
	public void doPrint(ASTNodePrinter output, int depth) { CallDeclaration.printParmString(output, subElements(), depth); }
	@Override
	public boolean hasSideEffects() { return true; }
	@Override
	public int parmsStart() { return this.start()+1; }
	@Override
	public int parmsEnd() { return this.end()-1; }
	@Override
	public int indexOfParm(ASTNode parm) {
		for (int i = 0; i < elements.length; i++)
			if (elements[i] == parm)
				return i;
		return -1;
	}
}
