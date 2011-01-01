package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.c4script.Keywords;

public class IterateArrayStatement extends KeywordStatement implements ILoop {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	private ExprElm elementExpr, arrayExpr, body;

	public IterateArrayStatement(ExprElm elementExpr, ExprElm arrayExpr, ExprElm body) {
		super();
		this.elementExpr = elementExpr;
		this.arrayExpr   = arrayExpr;
		this.body        = body;
		assignParentToSubElements();
	}

	public ExprElm getArrayExpr() {
		return arrayExpr;
	}

	public void setArrayExpr(ExprElm arrayExpr) {
		this.arrayExpr = arrayExpr;
	}

	public ExprElm getElementExpr() {
		return elementExpr;
	}

	public void setElementExpr(ExprElm elementExpr) {
		this.elementExpr = elementExpr;
	}

	@Override
	public String getKeyword() {
		return Keywords.For;
	}

	@Override
	public void doPrint(ExprWriter writer, int depth) {
		StringBuilder builder = new StringBuilder(getKeyword().length()+2+1+1+Keywords.In.length()+1+2);
		builder.append(getKeyword() + " ("); //$NON-NLS-1$
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

	public ExprElm getBody() {
		return body;
	}

	public void setBody(ExprElm body) {
		this.body = body;
	}

	@Override
	public ExprElm[] getSubElements() {
		return new ExprElm[] {elementExpr, arrayExpr, body};
	}

	@Override
	public void setSubElements(ExprElm[] elms) {
		elementExpr = elms[0];
		arrayExpr   = elms[1];
		body        = elms[2];
	}

}