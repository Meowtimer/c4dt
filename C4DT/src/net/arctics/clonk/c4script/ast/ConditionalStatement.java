package net.arctics.clonk.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodePrinter;

public abstract class ConditionalStatement extends KeywordStatement {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	protected ASTNode condition;
	protected ASTNode body;

	public ASTNode condition() { return condition; }
	public void setCondition(final ASTNode condition) { this.condition = condition; }
	public ASTNode body() { return body; }
	public void setBody(final ASTNode body) { this.body = body; }

	@Override
	public ASTNode[] subElements() { return new ASTNode[] {condition, body}; }

	@Override
	public void setSubElements(final ASTNode[] elms) {
		condition = elms[0];
		body      = elms[1];
	}

	public ConditionalStatement(final ASTNode condition, final ASTNode body) {
		super();
		this.condition = condition;
		this.body = body;
		assignParentToSubElements();
	}

	protected void printBody(final ASTNodePrinter builder, final int depth) {
		printBody(body, builder, depth);
	}

	@Override
	public void doPrint(final ASTNodePrinter builder, final int depth) {
		builder.append(keyword());
		builder.append(" ("); //$NON-NLS-1$
		condition.print(builder, depth+1);
		builder.append(")"); //$NON-NLS-1$
		printBody(builder, depth);
	}

}