package net.arctics.clonk.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodePrinter;
import net.arctics.clonk.ast.IEvaluationContext;
import net.arctics.clonk.c4script.ProblemReporter;

/**
 * Simple statement wrapper for an expression.
 * 
 */
public class SimpleStatement extends Statement {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private ASTNode expression;
	
	public SimpleStatement(ASTNode expression) {
		super();
		this.expression = expression;
		assignParentToSubElements();
		setLocation(expression);
	}

	public ASTNode expression() {
		return expression;
	}

	public void setExpression(ASTNode expression) {
		this.expression = expression;
	}

	@Override
	public ASTNode[] subElements() {
		return new ASTNode[] {expression};
	}

	@Override
	public void setSubElements(ASTNode[] elms) {
		expression = elms[0];
	}

	@Override
	public void doPrint(ASTNodePrinter builder, int depth) {
		expression.print(builder, depth);
		builder.append(";"); //$NON-NLS-1$
	}

	@Override
	public ASTNode optimize(final ProblemReporter context) throws CloneNotSupportedException {
		final ASTNode exprReplacement = expression.optimize(context);
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
	public Object evaluate(IEvaluationContext context) throws ControlFlowException {
		return expression.evaluate(context);
	}
	
	public static Statement wrapExpression(ASTNode expr) {
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
	
	public static Statement[] wrapExpressions(ASTNode... expressions) {
		final Statement[] result = new Statement[expressions.length];
		for (int i = 0; i < expressions.length; i++)
			result[i] = wrapExpression(expressions[i]);
		return result;
	}

}