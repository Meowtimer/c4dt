package net.arctics.clonk.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodePrinter;
import net.arctics.clonk.ast.EntityRegion;
import net.arctics.clonk.ast.IEntityLocator;
import net.arctics.clonk.ast.IPlaceholderPatternMatchTarget;
import net.arctics.clonk.c4script.IType;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.index.IIndexEntity;
import net.arctics.clonk.util.ArrayUtil;

import org.eclipse.jface.text.Region;

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
	public IType type;
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
	 */
	public VarInitialization(String name, ASTNode expression, int start, int end, Variable var) {
		super();
		this.name = name;
		this.expression = expression;
		setLocation(start, end);
		assignParentToSubElements();
		this.variable = var;
	}
	@Override
	public ASTNode[] subElements() {
		return new ASTNode[] {expression};
	}
	@Override
	public void setSubElements(ASTNode[] elms) {
		expression = elms[0];
	}
	@Override
	public void doPrint(ASTNodePrinter output, int depth) {
		if (type != null) {
			output.append(type.typeName(false));
			output.append(" ");
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
		VarInitialization[] brothers = parentOfType(VarDeclarationStatement.class).variableInitializations();
		return ArrayUtil.boundChecked(brothers, ArrayUtil.indexOf(this, brothers)-1);
	}
	/**
	 * Return the subsequent {@link VarInitialization} in the {@link VarDeclarationStatement}
	 * @return
	 */
	public VarInitialization succeedingInitialization() {
		VarInitialization[] sisters = parentOfType(VarDeclarationStatement.class).variableInitializations();
		return ArrayUtil.boundChecked(sisters, ArrayUtil.indexOf(this, sisters)+1);
	}

	@Override
	public EntityRegion entityAt(int offset, IEntityLocator locator) {
		if (type instanceof IIndexEntity && offset < type.typeName(false).length())
			return new EntityRegion((IIndexEntity) type, new Region(start(), type.typeName(false).length()));
		else
			return new EntityRegion(variable, this);
	}

	@Override
	public String patternMatchingText() {
		return name;
	}

}