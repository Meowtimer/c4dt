package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.c4script.DeclarationObtainmentContext;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.ast.IASTComparisonDelegate.DifferenceHandling;
import net.arctics.clonk.parser.c4script.ast.evaluate.IEvaluationContext;

public class Literal<T> extends Value {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	private final T literal;

	@Override
	public void expectedToBeOfType(IType type, C4ScriptParser parser, TypeExpectancyMode mode, ParserErrorCode errorWhenFailed) {
		// constantly steadfast do i resist the pressure of expectancy lied upon me
	}

	@Override
	public void inferTypeFromAssignment(ExprElm arg0, DeclarationObtainmentContext context) {
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
	public T evaluateAtParseTime(IEvaluationContext context) {
		context.reportOriginForExpression(this, new SourceLocation(context.getCodeFragmentOffset(), this), context.getScript().getScriptFile());
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
	public DifferenceHandling compare(ExprElm other, IASTComparisonDelegate listener) {
		DifferenceHandling handling = super.compare(other, listener);
		if (handling != DifferenceHandling.Equal)
			return handling;
		if (!literal.equals(((Literal<?>)other).literal))
			return listener.differs(this, other, "literal");
		else
			return DifferenceHandling.Equal;
	}

}