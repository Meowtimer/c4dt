package net.arctics.clonk.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodePrinter;
import net.arctics.clonk.c4script.Keywords;
import net.arctics.clonk.c4script.typing.IType;
import net.arctics.clonk.c4script.typing.PrimitiveType;
import net.arctics.clonk.c4script.typing.TypeAnnotation;

public class CastExpression extends ASTNode {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private TypeAnnotation targetTypeAnnotation;
	private ASTNode expression;
	public CastExpression(final TypeAnnotation targetTypeAnnotation, final ASTNode expression) {
		super();
		this.targetTypeAnnotation = targetTypeAnnotation;
		this.expression = expression;
		assignParentToSubElements();
	}
	public ASTNode expression() { return expression; }
	public TypeAnnotation targetTypeAnnotation() { return targetTypeAnnotation; }
	public IType targetType() { return targetTypeAnnotation != null ? targetTypeAnnotation.type() : PrimitiveType.UNKNOWN; }
	@Override
	public ASTNode[] subElements() { return new ASTNode[] {targetTypeAnnotation, expression}; }
	@Override
	public void setSubElements(final ASTNode[] elms) {
		targetTypeAnnotation = (TypeAnnotation) elms[0];
		expression = elms[1];
	}
	@Override
	public void doPrint(final ASTNodePrinter output, final int depth) {
		output.append(Keywords.Cast);
		output.append('[');
		output.append(targetTypeAnnotation.printed());
		output.append(']');
		output.append(' ');
		expression.print(output, depth);
	}
	@Override
	public boolean isValidAtEndOfSequence() { return true; }
	@Override
	public boolean isValidInSequence(final ASTNode predecessor) { return predecessor == null; }
}
