package net.arctics.clonk.c4script.ast;

import static net.arctics.clonk.c4script.Conf.printNodeList;

import java.util.ArrayList;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodePrinter;
import net.arctics.clonk.ast.ASTNodeWithSubElementsArray;
import net.arctics.clonk.ast.ControlFlowException;
import net.arctics.clonk.ast.IEvaluationContext;

public class ArrayExpression extends ASTNodeWithSubElementsArray {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	public ArrayExpression(ASTNode... elms) {
		super(elms);
	}

	@Override
	public void doPrint(ASTNodePrinter output, int depth) {
		printNodeList(output, elements, depth, "[", "]");
	}

	@Override
	public boolean isValidInSequence(ASTNode predecessor) {
		return predecessor == null;
	}

	@Override
	public boolean isConstant() {
		for (final ASTNode e : subElements())
			if (e != null && !e.isConstant())
				return false;
		return true;
	}

	@Override
	public Object evaluate(IEvaluationContext context) throws ControlFlowException {
		final ArrayList<Object> elm = new ArrayList<Object>(elements.length);
		for (final ASTNode e : elements)
			elm.add(e != null ? e.evaluate(context) : null);
		return elm;
	}

}