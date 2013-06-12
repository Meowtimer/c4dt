package net.arctics.clonk.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodePrinter;
import net.arctics.clonk.c4script.Keywords;
import net.arctics.clonk.c4script.typing.IType;

public class CastExpression extends ASTNode {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private final IType targetType;
	private final ASTNode expression;
	public CastExpression(IType targetType, ASTNode expression) {
		super();
		this.targetType = targetType;
		this.expression = expression;
		assignParentToSubElements();
	}
	public ASTNode expression() { return expression; }
	public IType targetType() { return targetType; }
	@Override
	public ASTNode[] subElements() { return new ASTNode[] {expression}; }
	@Override
	public void doPrint(ASTNodePrinter output, int depth) {
		output.append(Keywords.Cast);
		output.append('[');
		output.append(targetType.typeName(false));
		output.append(']');
		output.append(' ');
		expression.print(output, depth);
	}
	@Override
	public boolean isValidAtEndOfSequence() { return true; }
	@Override
	public boolean isValidInSequence(ASTNode predecessor) { return predecessor == null; }
}
