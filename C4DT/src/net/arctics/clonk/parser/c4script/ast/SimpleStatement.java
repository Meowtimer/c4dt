package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.ast.evaluate.IEvaluationContext;

/**
 * Simple statement wrapper for an expression.
 * 
 */
public class SimpleStatement extends Statement {
	/**
	 * 
	 */
	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
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

	public ExprElm getExpression() {
		return expression;
	}

	public void setExpression(ExprElm expression) {
		this.expression = expression;
	}

	@Override
	public ExprElm[] getSubElements() {
		return new ExprElm[] {expression};
	}

	@Override
	public void setSubElements(ExprElm[] elms) {
		expression = elms[0];
	}

	@Override
	public void doPrint(ExprWriter builder, int depth) {
		expression.print(builder, depth+1);
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
	public ControlFlow getControlFlow() {
		return expression.getControlFlow();
	}

	@Override
	public boolean hasSideEffects() {
		return expression.hasSideEffects();
	}
	
	@Override
	public Object evaluate(IEvaluationContext context) throws ControlFlowException {
		return expression.evaluate(context);
	}
	
	public static Statement statementFromExpression(ExprElm expr) {
		return expr instanceof Statement ? (Statement)expr : new SimpleStatement(expr);
	}
	
	@Override
	public void reportErrors(C4ScriptParser parser) throws ParsingException {
		if (!isFinishedProperly()) {
			parser.errorWithCode(ParserErrorCode.NotFinished, this, true);
		}
		if (expression instanceof BinaryOp) {
			((BinaryOp) expression).checkTopLevelAssignment(parser);
		}
		super.reportErrors(parser);
	}

}