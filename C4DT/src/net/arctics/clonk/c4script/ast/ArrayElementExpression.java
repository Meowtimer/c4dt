package net.arctics.clonk.c4script.ast;

import java.util.List;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodePrinter;
import net.arctics.clonk.ast.IEvaluationContext;

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

	@Override
	public Object evaluate(IEvaluationContext context) throws ControlFlowException {
		Object array = predecessorInSequence().evaluate(context);
		if (array instanceof List<?>)
			return ((List<?>)array).get(((Number)argument.evaluate(context)).intValue());
		else
			return null;
	}

}