package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.c4script.C4ScriptOperator;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.IType;

public class Operator extends Value {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	private final C4ScriptOperator operator;

	@Override
	public IType getType(C4ScriptParser context) {
		return operator.getResultType();
	}

	public Operator(C4ScriptOperator operator) {
		super();
		this.operator = operator;
	}

	public C4ScriptOperator getOperator() {
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
		if (operator != ((Operator)other).operator) {
			listener.differs(this, other, field("operator"));
			return false;
		} else {
			return true;
		}
	}

}