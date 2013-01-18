package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ExprElm;
import net.arctics.clonk.parser.c4script.Keywords;

public class DoWhileStatement extends WhileStatement {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	public DoWhileStatement(ExprElm condition, ExprElm body) {
		super(condition, body);
	}

	@Override
	public void doPrint(ExprWriter builder, int depth) {
		builder.append(Keywords.Do);
		printBody(builder, depth);
		builder.append(" "); //$NON-NLS-1$
		builder.append(Keywords.While);
		builder.append(" ("); //$NON-NLS-1$
		condition.print(builder, depth);
		builder.append(");"); //$NON-NLS-1$
	}
}