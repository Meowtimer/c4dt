package net.arctics.clonk.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodePrinter;
import net.arctics.clonk.c4script.Keywords;

public class IterateArrayStatement extends KeywordStatement implements ILoop {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private ASTNode elementExpr, arrayExpr, body;

	public IterateArrayStatement(final ASTNode elementExpr, final ASTNode arrayExpr, final ASTNode body) {
		super();
		this.elementExpr = elementExpr;
		this.arrayExpr   = arrayExpr;
		this.body        = body;
		assignParentToSubElements();
	}

	public ASTNode arrayExpr() {
		return arrayExpr;
	}

	public void setArrayExpr(final ASTNode arrayExpr) {
		this.arrayExpr = arrayExpr;
	}

	public ASTNode elementExpr() {
		return elementExpr;
	}

	public void setElementExpr(final ASTNode elementExpr) {
		this.elementExpr = elementExpr;
	}

	@Override
	public String keyword() {
		return Keywords.For;
	}

	@Override
	public void doPrint(final ASTNodePrinter writer, final int depth) {
		final StringBuilder builder = new StringBuilder(keyword().length()+2+1+1+Keywords.In.length()+1+2);
		builder.append(keyword() + " ("); //$NON-NLS-1$
		elementExpr.print(builder, depth+1);
		// remove ';' that elementExpr (a statement) prints
		if (builder.charAt(builder.length()-1) == ';')
			builder.deleteCharAt(builder.length()-1);
		builder.append(" " + Keywords.In + " "); //$NON-NLS-1$ //$NON-NLS-2$
		arrayExpr.print(builder, depth+1);
		builder.append(") "); //$NON-NLS-1$
		writer.append(builder.toString());
		printBody(body, writer, depth);
	}

	@Override
	public ASTNode body() {
		return body;
	}

	public void setBody(final ASTNode body) {
		this.body = body;
	}

	@Override
	public ASTNode[] subElements() {
		return new ASTNode[] {elementExpr, arrayExpr, body};
	}

	@Override
	public void setSubElements(final ASTNode[] elms) {
		elementExpr = elms[0];
		arrayExpr   = elms[1];
		body        = elms[2];
	}

}