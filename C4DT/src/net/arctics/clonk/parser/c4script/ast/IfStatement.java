package net.arctics.clonk.parser.c4script.ast;

import java.util.EnumSet;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.c4script.Keywords;
import net.arctics.clonk.parser.c4script.ast.evaluate.IEvaluationContext;

public class IfStatement extends ConditionalStatement {


	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	private ExprElm elseExpr;

	public IfStatement(ExprElm condition, ExprElm body, ExprElm elseExpr) {
		super(condition, body);
		this.elseExpr = elseExpr;
		assignParentToSubElements();
	}

	@Override
	public String getKeyword() {
		return Keywords.If;
	}
	@Override
	public void doPrint(ExprWriter builder, int depth) {
		builder.append(getKeyword());
		builder.append(" ("); //$NON-NLS-1$
		condition.print(builder, depth);
		builder.append(")"); //$NON-NLS-1$
		printBody(builder, depth);
		if (elseExpr != null) {
			builder.append("\n"); //$NON-NLS-1$
			Conf.printIndent(builder, depth-1);
			builder.append(Keywords.Else);
			if (!(elseExpr instanceof IfStatement))
				printBody(elseExpr, builder, depth);
			else {
				builder.append(" ");
				elseExpr.print(builder, depth);
			}
		}
	}

	@Override
	public ExprElm[] getSubElements() {
		return new ExprElm[] {condition, body, elseExpr};
	}

	@Override
	public void setSubElements(ExprElm[] elms) {
		condition = elms[0];
		body      = elms[1];
		elseExpr  = elms[2];
	}
	
	@Override
	public EnumSet<ControlFlow> getPossibleControlFlows() {
		EnumSet<ControlFlow> result = EnumSet.of(ControlFlow.Continue);
		result.addAll(body.getPossibleControlFlows());
		if (elseExpr != null)
			result.addAll(elseExpr.getPossibleControlFlows());
		return result;
	}
	
	public ExprElm getElse() {
		return elseExpr;
	}
	
	@Override
	public ControlFlow getControlFlow() {
		// return most optimistic flow (the smaller ordinal() the more "continuy" the flow is)
		ControlFlow ifCase = body.getControlFlow();
		ControlFlow elseCase = elseExpr != null ? elseExpr.getControlFlow() : ControlFlow.Continue;
		return ifCase.ordinal() < elseCase.ordinal() ? ifCase : elseCase;
	}
	
	@Override
	public Object evaluate(IEvaluationContext context) throws ControlFlowException {
		if (!condition.evaluate(context).equals(false))
			return body.evaluate(context);
		else if (elseExpr != null)
			return elseExpr.evaluate(context);
		else
			return null;
	}

}