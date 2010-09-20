package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.c4script.Keywords;

public class ForStatement extends ConditionalStatement implements ILoop {
	/**
	 * 
	 */
	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	private ExprElm initializer, increment;
	public ForStatement(ExprElm initializer, ExprElm condition, ExprElm increment, ExprElm body) {
		super(condition, body);
		this.initializer = initializer;
		this.increment = increment;
		assignParentToSubElements();
	}
	@Override
	public String getKeyword() {
		return Keywords.For;
	}
	@Override
	public void doPrint(ExprWriter builder, int depth) {
		builder.append(getKeyword() + " ("); //$NON-NLS-1$
		if (initializer != null)
			initializer.print(builder, depth+1);
		else
			builder.append(";"); //$NON-NLS-1$
		builder.append(" "); // no ';' since initializer is already a statement //$NON-NLS-1$
		if (condition != null)
			condition.print(builder, depth+1);
		builder.append("; "); //$NON-NLS-1$
		if (increment != null)
			increment.print(builder, depth);
		builder.append(")"); //$NON-NLS-1$
		printBody(builder, depth);
	}
	@Override
	public ExprElm[] getSubElements() {
		return new ExprElm[] {initializer, condition, increment, body};
	}

	@Override
	public void setSubElements(ExprElm[] elms) {
		initializer = elms[0];
		condition   = elms[1];
		increment   = elms[2];
		body        = elms[3];
	}
}