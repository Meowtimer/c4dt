package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.c4script.DeclarationObtainmentContext;
import net.arctics.clonk.parser.c4script.Operator;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.ast.IASTComparisonDelegate.DifferenceHandling;

public class OperatorExpression extends Value {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	private final Operator operator;

	@Override
	protected IType obtainType(DeclarationObtainmentContext context) {
		return operator.getResultType();
	}

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
	public boolean isModifiable(C4ScriptParser context) {
		return operator().returnsRef();
	}
	
	@Override
	public DifferenceHandling compare(ExprElm other, IASTComparisonDelegate listener) {
		DifferenceHandling handling = super.compare(other, listener);
		if (handling != DifferenceHandling.Equal)
			return handling;
		if (operator != ((OperatorExpression)other).operator)
			return listener.differs(this, other, "operator");
		else
			return DifferenceHandling.Equal;
	}

}