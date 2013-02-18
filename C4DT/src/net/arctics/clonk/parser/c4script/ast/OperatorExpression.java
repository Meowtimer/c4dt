package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.IPlaceholderPatternMatchTarget;
import net.arctics.clonk.parser.c4script.Operator;

public class OperatorExpression extends ASTNode implements IPlaceholderPatternMatchTarget {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private final Operator operator;

	public OperatorExpression(Operator operator) {
		super();
		this.operator = operator;
	}

	public Operator operator() {
		return operator;
	}

	@Override
	public boolean hasSideEffects() {
		return operator().modifiesArgument() || super.hasSideEffects();
	}

	@Override
	public boolean equalAttributes(ASTNode other) {
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