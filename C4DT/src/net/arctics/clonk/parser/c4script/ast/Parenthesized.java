package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.c4script.DeclarationObtainmentContext;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.ast.evaluate.IEvaluationContext;

public class Parenthesized extends ASTNode {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private ASTNode innerExpr;

	@Override
	public ASTNode[] subElements() {
		return new ASTNode[] {innerExpr};
	}
	@Override
	public void setSubElements(ASTNode[] elements) {
		innerExpr = elements[0];
	}
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
	public IType unresolvedType(DeclarationObtainmentContext context) {
		return innerExpr.type(context);
	}
	@Override
	public boolean isModifiable(C4ScriptParser context) {
		return innerExpr.isModifiable(context);
	}
	@Override
	public boolean hasSideEffects() {
		return innerExpr.hasSideEffects();
	}

	public ASTNode innerExpression() {
		return innerExpr;
	}

	@Override
	public ASTNode optimize(C4ScriptParser parser)
	throws CloneNotSupportedException {
		if (!(parent() instanceof OperatorExpression) && !(parent() instanceof Sequence))
			return innerExpr.optimize(parser);
		return super.optimize(parser);
	}

	@Override
	public Object evaluateAtParseTime(IEvaluationContext context) {
		return innerExpr.evaluateAtParseTime(context);
	}

}