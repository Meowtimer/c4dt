package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.ClonkCore;

public abstract class ConditionalStatement extends KeywordStatement {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
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
	public void setSubElements(ExprElm[] elms) {
		condition = elms[0];
		body      = elms[1];
	}

}