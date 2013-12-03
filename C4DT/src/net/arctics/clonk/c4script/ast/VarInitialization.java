package net.arctics.clonk.c4script.ast;

import static net.arctics.clonk.util.Utilities.eq;
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
	
	public static class Name extends ASTNode {
		private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
		private final String name;
		public Name(final String name) {
			this.name = name;
		}
		@Override
		protected boolean equalAttributes(final ASTNode other) {
			return super.equalAttributes(other) && eq(this.name, ((Name)other).name);
		}
		public String name() { return name; }
	}

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
	 * @param typeAnnotation Explicit type annotation
	 */
	public VarInitialization(final String name, final ASTNode expression, final int start, final int end, final Variable var, final TypeAnnotation typeAnnotation) {
		super();
		this.name = name;
		this.expression = expression;
		this.typeAnnotation = typeAnnotation;
		this.variable = var;
		setLocation(start, end);
		assignParentToSubElements();
	}
	@Override
	public ASTNode[] subElements() { return new ASTNode[] {typeAnnotation, tempSubElement(new Name(name)), expression}; }
	@Override
	public void setSubElements(final ASTNode[] elms) {
		typeAnnotation = (TypeAnnotation) elms[0];
		name = ((Name)elms[1]).name();
		expression = elms[2];
	}
	public IType type() { return typeAnnotation != null ? typeAnnotation.type() : null; }
	@Override
	public void doPrint(final ASTNodePrinter output, final int depth) {
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
	public EntityRegion entityAt(final int offset, final ExpressionLocator<?> locator) { return new EntityRegion(variable, this); }
	@Override
	public String patternMatchingText() { return name; }
	
	@Override
	protected boolean equalAttributes(final ASTNode _other) {
		if (!super.equalAttributes(_other))
			return false;
		final VarInitialization other = (VarInitialization) _other;
		return eq(this.name, other.name);
	}

}