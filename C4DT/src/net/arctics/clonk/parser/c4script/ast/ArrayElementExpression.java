package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.ASTNodePrinter;

public class ArrayElementExpression extends ASTNode {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	protected ASTNode argument;

	public ArrayElementExpression(ASTNode argument) {
		super();
		this.argument = argument;
		assignParentToSubElements();
	}

	@Override
	public void doPrint(ASTNodePrinter output, int depth) {
		output.append("["); //$NON-NLS-1$
		argument().print(output, depth+1);
		output.append("]"); //$NON-NLS-1$
	}

	@Override
	public boolean isValidInSequence(ASTNode predecessor) {
		return predecessor != null;
	}

	@Override
	public ASTNode[] subElements() {
		return new ASTNode[] {argument};
	}

	@Override
	public void setSubElements(ASTNode[] subElements) {
		argument = subElements[0];
	}

	public ASTNode argument() {
		return argument;
	}

}