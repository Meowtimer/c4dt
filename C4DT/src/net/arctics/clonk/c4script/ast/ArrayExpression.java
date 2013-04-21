package net.arctics.clonk.c4script.ast;

import java.util.ArrayList;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.ASTNodePrinter;
import net.arctics.clonk.parser.IEvaluationContext;

public class ArrayExpression extends ASTNodeWithSubElementsArray {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	public ArrayExpression(ASTNode... elms) {
		super(elms);
	}

	@Override
	public void doPrint(ASTNodePrinter output, int depth) {
		output.append("["); //$NON-NLS-1$
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] != null)
				elements[i].print(output, depth+1);
			if (i < elements.length-1)
				output.append(", "); //$NON-NLS-1$
		}
		output.append("]"); //$NON-NLS-1$
	}

	@Override
	public boolean isValidInSequence(ASTNode predecessor) {
		return predecessor == null;
	}

	@Override
	public boolean isConstant() {
		for (ASTNode e : subElements())
			if (e != null && !e.isConstant())
				return false;
		return true;
	}

	@Override
	public Object evaluate(IEvaluationContext context) throws ControlFlowException {
		ArrayList<Object> elm = new ArrayList<Object>(elements.length);
		for (ASTNode e : elements)
			elm.add(e != null ? e.evaluate(context) : null);
		return elm;
	}

}