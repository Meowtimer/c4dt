package net.arctics.clonk.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodePrinter;
import net.arctics.clonk.ast.IEvaluationContext;
import net.arctics.clonk.ast.Sequence;

public class Parenthesized extends ASTNode implements ITidyable {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private ASTNode innerExpr;

	@Override
	public ASTNode[] subElements() { return new ASTNode[] {innerExpr}; }
	@Override
	public void setSubElements(ASTNode[] elements) { innerExpr = elements[0]; }
	public Parenthesized(ASTNode innerExpr) {
		super();
		this.innerExpr = innerExpr;
		assignParentToSubElements();
	}
	@Override
	public void doPrint(ASTNodePrinter output, int depth) {
		output.append("("); //$NON-NLS-1$
		innerExpr.print(output, depth+1);
		output.append(")"); //$NON-NLS-1$
	}
	@Override
	public boolean hasSideEffects() { return innerExpr.hasSideEffects(); }
	public ASTNode innerExpression() { return innerExpr; }

	@Override
	public ASTNode tidy(final Tidy tidy)
	throws CloneNotSupportedException {
		if (!(parent() instanceof OperatorExpression) && !(parent() instanceof Sequence))
			return tidy.tidy(innerExpr);
		return this;
	}

	@Override
	public Object evaluateStatic(IEvaluationContext context) {
		return innerExpr.evaluateStatic(context);
	}

}