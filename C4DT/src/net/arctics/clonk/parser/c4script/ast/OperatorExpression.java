package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.c4script.Operator;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.IType;

public class OperatorExpression extends Value {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	private final Operator operator;

	@Override
	protected IType obtainType(C4ScriptParser context) {
		return operator.getResultType();
	}

	public OperatorExpression(Operator operator) {
		super();
		this.operator = operator;
	}

	public Operator getOperator() {
		return operator;
	}

	@Override
	public boolean hasSideEffects() {
		return getOperator().modifiesArgument() || super.hasSideEffects();
	}

	@Override
	public boolean modifiable(C4ScriptParser context) {
		return getOperator().returnsRef();
	}
	
	@Override
	public boolean compare(ExprElm other, IDifferenceListener listener) {
		if (!super.compare(other, listener))
			return false;
		if (operator != ((OperatorExpression)other).operator) {
			listener.differs(this, other, "operator");
			return false;
		} else {
			return true;
		}
	}

}