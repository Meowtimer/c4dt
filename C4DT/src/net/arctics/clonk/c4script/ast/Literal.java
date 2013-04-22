package net.arctics.clonk.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodePrinter;
import net.arctics.clonk.ast.IEvaluationContext;
import net.arctics.clonk.ast.IPlaceholderPatternMatchTarget;
import net.arctics.clonk.ast.SourceLocation;
import net.arctics.clonk.util.Utilities;

/**
 * A literal value in an expression (123, "hello", CLNK...)
 * @author madeen
 *
 * @param <T> The type of value this literal represents
 */
public abstract class Literal<T> extends ASTNode implements IPlaceholderPatternMatchTarget {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	public abstract T literal();
	public boolean literalsEqual(Literal<?> other) {
		return Utilities.eq(other.literal(), this.literal());
	}

	@Override
	public boolean isConstant() {
		return true;
	}

	@Override
	public T evaluateStatic(IEvaluationContext context) {
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
	public boolean equalAttributes(ASTNode other) {
		if (!super.equalAttributes(other))
			return false;
		if (!literalsEqual((Literal<?>)other))
			return false;
		return true;
	}

	@Override
	public String patternMatchingText() {
		return literal() != null ? literal().toString() : null;
	}

	@Override
	public boolean allowsSequenceSuccessor(ASTNode successor) { return false; }
	@Override
	public boolean isValidInSequence(ASTNode predecessor) { return predecessor == null; }
}