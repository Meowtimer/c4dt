package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.ASTNodePrinter;
import net.arctics.clonk.parser.IPlaceholderPatternMatchTarget;
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
public abstract class Literal<T> extends ASTNode implements IPlaceholderPatternMatchTarget {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	@Override
	public boolean typingJudgement(IType type, C4ScriptParser parser, TypingJudgementMode mode) {
		// constantly steadfast do i resist the pressure of expectancy lied upon me
		return true;
	}

	@Override
	public void assignment(ASTNode arg0, C4ScriptParser context) {
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
	public void doPrint(ASTNodePrinter output, int depth) {
		output.append(literal().toString());
	}
	
	@Override
	public DifferenceHandling compare(ASTNode other, IASTComparisonDelegate listener) {
		DifferenceHandling handling = super.compare(other, listener);
		if (handling != DifferenceHandling.Equal)
			return handling;
		if (!literalsEqual((Literal<?>)other))
			return listener.differs(this, other, "literal");
		else
			return DifferenceHandling.Equal;
	}
	
	@Override
	public String patternMatchingText() {
		return literal() != null ? literal().toString() : null;
	}
	
	@Override
	public boolean allowsSequenceSuccessor(C4ScriptParser context, ASTNode successor) { return false; }
	@Override
	public boolean isValidInSequence(ASTNode predecessor, C4ScriptParser context) { return predecessor == null; }
	@Override
	public ITypeInfo createTypeInfo(C4ScriptParser parser) { return null; /* nope */ }
}