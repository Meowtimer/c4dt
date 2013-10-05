package net.arctics.clonk.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodePrinter;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ast.EntityRegion;
import net.arctics.clonk.ast.ExpressionLocator;
import net.arctics.clonk.ast.IPlaceholderPatternMatchTarget;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.c4script.typing.IType;
import net.arctics.clonk.c4script.typing.TypeAnnotation;
import net.arctics.clonk.c4script.typing.Typing;
import net.arctics.clonk.util.ArrayUtil;

/**
 * A single var declaration/initialization. Includes name of variable being declared, optionally the initialization expression and
 * a reference to the actual {@link Variable} object representing the variable being declared.
 * @author madeen
 *
 */
public final class VarInitialization extends ASTNode implements IPlaceholderPatternMatchTarget {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	/**
	 * Explicit type annotation used for this initialization
	 */
	public TypeAnnotation typeAnnotation;
	/**
	 * Name of the variable being declared.
	 */
	public String name;
	/**
	 * Initialization expression. Can be null.
	 */
	public ASTNode expression;
	/**
	 * Object representing variable. Does not need to be set.
	 */
	public Variable variable;
	/**
	 * Create a new {@link VarInitialization}.
	 * @param name Name of variable
	 * @param expression Expression. Can be null.
	 * @param start Start location.
	 * @param end End location.
	 * @param var Variable. May be null.
	 * @param typeAnnotation TODO
	 */
	public VarInitialization(String name, ASTNode expression, int start, int end, Variable var, TypeAnnotation typeAnnotation) {
		super();
		this.name = name;
		this.expression = expression;
		this.typeAnnotation = typeAnnotation;
		this.variable = var;
		setLocation(start, end);
		assignParentToSubElements();
	}
	@Override
	public ASTNode[] subElements() { return new ASTNode[] {typeAnnotation, expression}; }
	@Override
	public void setSubElements(ASTNode[] elms) {
		typeAnnotation = (TypeAnnotation) elms[0];
		expression = elms[1];
	}
	public IType type() { return typeAnnotation != null ? typeAnnotation.type() : null; }
	@Override
	public void doPrint(ASTNodePrinter output, int depth) {
		final Declaration p = parent(Declaration.class);
		final Typing typing = p != null ? p.typing() : Typing.INFERRED;
		switch (typing) {
		case STATIC: case INFERRED:
			if (typeAnnotation != null) {
				output.append(typeAnnotation.type().typeName(typing == Typing.STATIC));
				output.append(" ");
			}
			break;
		case DYNAMIC:
			break;
		}
		output.append(name);
		if (expression != null) {
			output.append(" =");
			if (!(expression instanceof PropListExpression))
				output.append(' ');
			expression.print(output, depth);
		}
	}
	/**
	 * Return the {@link VarInitialization} preceding this one in the {@link VarDeclarationStatement}
	 * @return
	 */
	public VarInitialization precedingInitialization() {
		final VarInitialization[] brothers = parent(VarDeclarationStatement.class).variableInitializations();
		return ArrayUtil.boundChecked(brothers, ArrayUtil.indexOf(this, brothers)-1);
	}
	/**
	 * Return the subsequent {@link VarInitialization} in the {@link VarDeclarationStatement}
	 * @return
	 */
	public VarInitialization succeedingInitialization() {
		final VarInitialization[] sisters = parent(VarDeclarationStatement.class).variableInitializations();
		return ArrayUtil.boundChecked(sisters, ArrayUtil.indexOf(this, sisters)+1);
	}

	@Override
	public EntityRegion entityAt(int offset, ExpressionLocator<?> locator) { return new EntityRegion(variable, this); }
	@Override
	public String patternMatchingText() { return name; }

}