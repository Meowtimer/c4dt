package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.ast.evaluate.IEvaluationContext;

/**
 * Simple statement wrapper for an expression.
 * 
 */
public class SimpleStatement extends Statement {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private ExprElm expression;

	@Override
	public boolean isFinishedProperly() {
		return expression.isFinishedProperly();
	}
	
	@Override
	public void setFinishedProperly(boolean finishedProperly) {
		expression.setFinishedProperly(finishedProperly);
	}
	
	public SimpleStatement(ExprElm expression) {
		super();
		this.expression = expression;
		assignParentToSubElements();
	}

	public ExprElm expression() {
		return expression;
	}

	public void setExpression(ExprElm expression) {
		this.expression = expression;
	}

	@Override
	public ExprElm[] subElements() {
		return new ExprElm[] {expression};
	}

	@Override
	public void setSubElements(ExprElm[] elms) {
		expression = elms[0];
	}

	@Override
	public void doPrint(ExprWriter builder, int depth) {
		expression.print(builder, depth);
		builder.append(";"); //$NON-NLS-1$
	}

	@Override
	public ExprElm optimize(C4ScriptParser parser) throws CloneNotSupportedException {
		ExprElm exprReplacement = expression.optimize(parser);
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
	
	public static Statement wrapExpression(ExprElm expr) {
		if (expr instanceof Statement)
			return (Statement)expr;
		else if (expr != null)
			return new SimpleStatement(expr);
		else
			return null;
	}
	
	public static ExprElm unwrap(ExprElm expr) {
		if (expr instanceof SimpleStatement)
			return ((SimpleStatement)expr).expression();
		else
			return expr;
	}
	
	@Override
	public void reportProblems(C4ScriptParser parser) throws ParsingException {
		if (expression instanceof BinaryOp)
			((BinaryOp) expression).checkTopLevelAssignment(parser);
		super.reportProblems(parser);
	}
	
	public static Statement[] wrapExpressions(ExprElm... expressions) {
		Statement[] result = new Statement[expressions.length];
		for (int i = 0; i < expressions.length; i++)
			result[i] = wrapExpression(expressions[i]);
		return result;
	}

}