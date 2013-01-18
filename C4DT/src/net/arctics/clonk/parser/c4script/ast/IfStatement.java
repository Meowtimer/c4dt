package net.arctics.clonk.parser.c4script.ast;

import java.util.EnumSet;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.Conf;
import net.arctics.clonk.parser.c4script.Keywords;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.TypeEnvironment;
import net.arctics.clonk.parser.c4script.ast.evaluate.IEvaluationContext;

public class IfStatement extends ConditionalStatement {


	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private ASTNode elseExpr;

	public IfStatement(ASTNode condition, ASTNode body, ASTNode elseExpr) {
		super(condition, body);
		this.elseExpr = elseExpr;
		assignParentToSubElements();
	}

	@Override
	public String keyword() {
		return Keywords.If;
	}
	@Override
	public void doPrint(ExprWriter builder, int depth) {
		builder.append(keyword());
		builder.append(" ("); //$NON-NLS-1$
		condition.print(builder, depth);
		builder.append(")"); //$NON-NLS-1$
		printBody(body, builder, depth);
		if (elseExpr != null) {
			builder.append("\n"); //$NON-NLS-1$
			Conf.printIndent(builder, depth);
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
	public ASTNode[] subElements() {
		return new ASTNode[] {condition, body, elseExpr};
	}

	@Override
	public void setSubElements(ASTNode[] elms) {
		condition = elms[0];
		body      = elms[1];
		elseExpr  = elms[2];
	}
	
	@Override
	public EnumSet<ControlFlow> possibleControlFlows() {
		EnumSet<ControlFlow> result = EnumSet.of(ControlFlow.Continue);
		result.addAll(body.possibleControlFlows());
		if (elseExpr != null)
			result.addAll(elseExpr.possibleControlFlows());
		return result;
	}
	
	public ASTNode elseExpression() {
		return elseExpr;
	}
	
	@Override
	public ControlFlow controlFlow() {
		// return most optimistic flow (the smaller ordinal() the more "continuy" the flow is)
		ControlFlow ifCase = body.controlFlow();
		ControlFlow elseCase = elseExpr != null ? elseExpr.controlFlow() : ControlFlow.Continue;
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
	
	@Override
	public void reportProblems(C4ScriptParser parser) throws ParsingException {
		parser.reportProblemsOf(condition, true);
		// use two separate type environments for if and else statement, merging
		// gathered information afterwards
		TypeEnvironment ifEnvironment = parser.newTypeEnvironment();
		parser.reportProblemsOf(body, true);
		parser.endTypeEnvironment(false, false);
		if (elseExpr != null) {
			TypeEnvironment elseEnvironment = parser.newTypeEnvironment();
			parser.reportProblemsOf(elseExpr, true);
			parser.endTypeEnvironment(false, false);
			ifEnvironment.inject(elseEnvironment, false);
		}
		if (ifEnvironment.up != null)
			ifEnvironment.up.inject(ifEnvironment, false);

		if (!condition.containsConst()) {
			Object condEv = PrimitiveType.BOOL.convert(condition.evaluateAtParseTime(parser.currentFunction()));
			if (condEv != null && condEv != ASTNode.EVALUATION_COMPLEX)
				parser.warning(condEv.equals(true) ? ParserErrorCode.ConditionAlwaysTrue : ParserErrorCode.ConditionAlwaysFalse,
						condition, 0, condition);
		}
	}

}
