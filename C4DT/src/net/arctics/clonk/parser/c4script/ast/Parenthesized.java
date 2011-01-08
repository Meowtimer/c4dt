package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.IType;

public class Parenthesized extends Value {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	private ExprElm innerExpr;

	@Override
	public ExprElm[] getSubElements() {
		return new ExprElm[] {innerExpr};
	}
	@Override
	public void setSubElements(ExprElm[] elements) {
		innerExpr = elements[0];
	}
	public Parenthesized(ExprElm innerExpr) {
		super();
		this.innerExpr = innerExpr;
		assignParentToSubElements();
	}
	@Override
	public void doPrint(ExprWriter output, int depth) {
		output.append("("); //$NON-NLS-1$
		innerExpr.print(output, depth+1);
		output.append(")"); //$NON-NLS-1$
	}
	@Override
	protected IType obtainType(C4ScriptParser context) {
		return innerExpr.getType(context);
	}
	@Override
	public boolean modifiable(C4ScriptParser context) {
		return innerExpr.modifiable(context);
	}
	@Override
	public boolean hasSideEffects() {
		return innerExpr.hasSideEffects();
	}

	public ExprElm getInnerExpr() {
		return innerExpr;
	}

	@Override
	public ExprElm optimize(C4ScriptParser parser)
	throws CloneNotSupportedException {
		if (!(getParent() instanceof Operator) && !(getParent() instanceof Sequence))
			return innerExpr.optimize(parser);
		return super.optimize(parser);
	}

	@Override
	public Object evaluateAtParseTime(C4ScriptBase context) {
		return innerExpr.evaluateAtParseTime(context);
	}

}