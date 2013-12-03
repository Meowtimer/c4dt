package net.arctics.clonk.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodePrinter;

public class ArraySliceExpression extends ASTNode {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private ASTNode lo, hi;

	public ArraySliceExpression(final ASTNode lo, final ASTNode hi) {
		this.lo = lo;
		this.hi = hi;
		assignParentToSubElements();
	}

	public ASTNode lo() {
		return lo;
	}

	public ASTNode hi() {
		return hi;
	}

	@Override
	public void doPrint(final ASTNodePrinter output, final int depth) {
		output.append("["); //$NON-NLS-1$
		if (lo != null)
			lo.print(output, depth+1);
		output.append(":"); //$NON-NLS-1$
		if (hi != null)
			hi.print(output, depth+1);
		output.append("]"); //$NON-NLS-1$
	}

	@Override
	public ASTNode[] subElements() {
		return new ASTNode[] {lo, hi};
	}
	@Override
	public void setSubElements(final ASTNode[] subElements) {
		lo  = subElements[0];
		hi = subElements[1];
	}
	@Override
	public boolean isValidInSequence(final ASTNode predecessor) { return predecessor != null; }

}