package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.c4script.ScriptBase;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.ast.evaluate.IEvaluationContext;

public class Literal<T> extends Value {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	private final T literal;

	@Override
	public void expectedToBeOfType(IType type, C4ScriptParser parser, TypeExpectancyMode mode, ParserErrorCode errorWhenFailed) {
		// constantly steadfast do i resist the pressure of expectancy lied upon me
	}

	@Override
	public void inferTypeFromAssignment(ExprElm arg0, C4ScriptParser arg1) {
		// don't care
	}

	public Literal(T literal) {
		super();
		this.literal = literal;
	}

	public T getLiteral() {
		return literal;
	}

	@Override
	public boolean modifiable(C4ScriptParser context) {
		return false;
	}

	@Override
	public boolean isValidInSequence(ExprElm predecessor, C4ScriptParser context) {
		return predecessor == null;
	}

	@Override
	public boolean isConstant() {
		return true;
	}

	@Override
	public T evaluateAtParseTime(ScriptBase context) {
		return literal;
	}
	
	@Override
	public Object evaluate(IEvaluationContext context) {
	    return literal;
	}
	
	@Override
	public void doPrint(ExprWriter output, int depth) {
		output.append(getLiteral().toString());
	}
	
	@Override
	public boolean compare(ExprElm other, IDifferenceListener listener) {
		if (!super.compare(other, listener))
			return false;
		if (!literal.equals(((Literal<?>)other).literal)) {
			listener.differs(this, other, "literal");
			return false;
		} else {
			return true;
		}
	}

}