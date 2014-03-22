package net.arctics.clonk.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.IPlaceholderPatternMatchTarget;
import net.arctics.clonk.c4script.Operator;

public class OperatorExpression extends ASTNode implements IPlaceholderPatternMatchTarget {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private Operator operator;

	public OperatorExpression(final Operator operator) {
		super();
		this.operator = operator;
	}

	public Operator operator() { return operator; }
	public void operator(Operator op) { operator = op; }

	@Override
	public boolean hasSideEffects() {
		return operator().modifiesArgument() || super.hasSideEffects();
	}

	@Override
	public boolean equalAttributes(final ASTNode other) {
		if (!super.equalAttributes(other))
			return false;
		if (operator != ((OperatorExpression)other).operator)
			return false;
		return true;
	}

	@Override
	public String patternMatchingText() {
		return operator.operatorName();
	}

}