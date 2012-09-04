package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.ast.IASTComparisonDelegate.DifferenceHandling;
import net.arctics.clonk.parser.c4script.ast.evaluate.IEvaluationContext;
import net.arctics.clonk.util.Utilities;

/**
 * A literal value in an expression (123, "hello", CLNK...)
 * @author madeen
 *
 * @param <T> The type of value this literal represents
 */
public abstract class Literal<T> extends ExprElm {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	@Override
	public void expectedToBeOfType(IType type, C4ScriptParser parser, TypeExpectancyMode mode, ParserErrorCode errorWhenFailed) {
		// constantly steadfast do i resist the pressure of expectancy lied upon me
	}

	@Override
	public void assignment(ExprElm arg0, C4ScriptParser context) {
		// don't care
	}

	public abstract T literal();
	public boolean literalsEqual(Literal<?> other) {
		return Utilities.objectsEqual(other.literal(), this.literal());
	}

	@Override
	public boolean isModifiable(C4ScriptParser context) {
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
		context.reportOriginForExpression(this, new SourceLocation(context.codeFragmentOffset(), this), context.script().scriptFile());
		return literal();
	}
	
	@Override
	public Object evaluate(IEvaluationContext context) {
	    return literal();
	}
	
	@Override
	public void doPrint(ExprWriter output, int depth) {
		output.append(literal().toString());
	}
	
	@Override
	public DifferenceHandling compare(ExprElm other, IASTComparisonDelegate listener) {
		DifferenceHandling handling = super.compare(other, listener);
		if (handling != DifferenceHandling.Equal)
			return handling;
		if (!literalsEqual((Literal<?>)other))
			return listener.differs(this, other, "literal");
		else
			return DifferenceHandling.Equal;
	}

}