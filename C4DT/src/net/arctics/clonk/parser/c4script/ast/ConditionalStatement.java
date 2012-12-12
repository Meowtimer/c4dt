package net.arctics.clonk.parser.c4script.ast;

import java.util.EnumSet;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.PrimitiveType;

public abstract class ConditionalStatement extends KeywordStatement {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	protected ExprElm condition;
	protected ExprElm body;

	public ExprElm condition() {
		return condition;
	}

	public void setCondition(ExprElm condition) {
		this.condition = condition;
	}

	public ConditionalStatement(ExprElm condition, ExprElm body) {
		super();
		this.condition = condition;
		this.body = body;
		assignParentToSubElements();
	}

	protected void printBody(ExprWriter builder, int depth) {
		printBody(body, builder, depth);
	}

	@Override
	public void doPrint(ExprWriter builder, int depth) {
		builder.append(keyword());
		builder.append(" ("); //$NON-NLS-1$
		condition.print(builder, depth+1);
		builder.append(")"); //$NON-NLS-1$
		printBody(builder, depth);
	}

	public ExprElm body() {
		return body;
	}

	public void setBody(ExprElm body) {
		this.body = body;
	}

	@Override
	public ExprElm[] subElements() {
		return new ExprElm[] {condition, body};
	}
	
	@Override
	public boolean skipReportingProblemsForSubElements() {return true;}
	
	@Override
	public void reportProblems(C4ScriptParser parser) throws ParsingException {
		parser.reportProblemsOf(condition, true);
		parser.newTypeEnvironment();
		parser.reportProblemsOf(body, true);
		parser.endTypeEnvironment(true, false);
		loopConditionWarnings(parser);
	}

	@Override
	public void setSubElements(ExprElm[] elms) {
		condition = elms[0];
		body      = elms[1];
	}
	
	/**
	 * Emit warnings about loop conditions that could result in loops never executing or never ending.
	 * @param body The loop body. If the condition looks like it will always be true, checks are performed whether the body contains loop control flow statements.
	 * @param condition The loop condition to check
	 */
	protected void loopConditionWarnings(C4ScriptParser parser) {
		if (body == null || condition == null || !(this instanceof ILoop))
			return;
		Object condEv = PrimitiveType.BOOL.convert(condition == null ? true : condition.evaluateAtParseTime(parser.currentFunction()));
		if (Boolean.FALSE.equals(condEv))
			parser.warning(ParserErrorCode.ConditionAlwaysFalse, condition, C4ScriptParser.NO_THROW, condition);
		else if (Boolean.TRUE.equals(condEv)) {
			EnumSet<ControlFlow> flows = body.possibleControlFlows();
			if (!(flows.contains(ControlFlow.BreakLoop) || flows.contains(ControlFlow.Return)))
				parser.warning(ParserErrorCode.InfiniteLoop, this, C4ScriptParser.NO_THROW);
		}
	}

}