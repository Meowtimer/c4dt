package net.arctics.clonk.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodePrinter;
import net.arctics.clonk.ast.ControlFlow;
import net.arctics.clonk.ast.ControlFlowException;
import net.arctics.clonk.ast.IEvaluationContext;

/**
 * Simple statement wrapper for an expression.
 * 
 */
public class SimpleStatement extends Statement implements ITidyable {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private ASTNode expression;
	
	public SimpleStatement(final ASTNode expression) {
		super();
		this.expression = expression;
		assignParentToSubElements();
		setLocation(expression);
	}

	public ASTNode expression() {
		return expression;
	}

	public void setExpression(final ASTNode expression) {
		this.expression = expression;
	}

	@Override
	public ASTNode[] subElements() {
		return new ASTNode[] {expression};
	}

	@Override
	public void setSubElements(final ASTNode[] elms) {
		expression = elms[0];
	}

	@Override
	public void doPrint(final ASTNodePrinter builder, final int depth) {
		expression.print(builder, depth);
		builder.append(";"); //$NON-NLS-1$
	}

	@Override
	public ASTNode tidy(final Tidy tidy) throws CloneNotSupportedException {
		final ASTNode exprReplacement = tidy.tidy(expression);
		if (exprReplacement instanceof Statement)
			return exprReplacement;
		if (exprReplacement == expression)
			return this;
		return new SimpleStatement(exprReplacement);
	}

	@Override
	public ControlFlow controlFlow() {
		return expression.controlFlow();
	}

	@Override
	public boolean hasSideEffects() {
		return expression.hasSideEffects();
	}
	
	@Override
	public Object evaluate(final IEvaluationContext context) throws ControlFlowException {
		return expression.evaluate(context);
	}
	
	public static Statement wrapExpression(final ASTNode expr) {
		if (expr instanceof Statement)
			return (Statement)expr;
		else if (expr != null)
			return new SimpleStatement(expr);
		else
			return null;
	}
	
	public static ASTNode unwrap(ASTNode expr) {
		while (expr instanceof SimpleStatement)
			expr = ((SimpleStatement)expr).expression();
		return expr;
	}
	
	public static Statement[] wrapExpressions(final ASTNode... expressions) {
		final Statement[] result = new Statement[expressions.length];
		for (int i = 0; i < expressions.length; i++)
			result[i] = wrapExpression(expressions[i]);
		return result;
	}

}