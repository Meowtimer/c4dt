package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.EntityRegion;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.util.ArrayUtil;

/**
 * A single var declaration/initialization. Includes name of variable being declared, optionally the initialization expression and
 * a reference to the actual {@link Variable} object representing the variable being declared. 
 * @author madeen
 *
 */
public final class VarInitialization extends ExprElm {

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
	public ExprElm expression;
	/**
	 * Object representing variable. Does not need to be set.
	 */
	public Variable variable;
	/**
	 * Create a new {@link VarInitialization}.
	 * @param name Name of variable
	 * @param expression Expression. Can be null.
	 * @param namePos Position of name. Used for setting the region of this expression ({@link #setExprRegion(int, int)}})
	 */
	public VarInitialization(String name, ExprElm expression, int namePos) {
		super();
		this.name = name;
		this.expression = expression;
		setExprRegion(namePos, expression != null ? expression.end() : namePos + name.length());
		assignParentToSubElements();
	}
	/**
	 * Creat a new {@link VarInitialization}. Calls {@link VarInitialization#VarInitialization(String, ExprElm, int)}, additionally assigning {@link #variable} the passed var.
	 * @param name Name of variable
	 * @param expression Expression. Can be null.
	 * @param namePos Position of name. Used for setting the region of this expression ({@link #setExprRegion(int, int)}})
	 * @param var Variable to assign to {@link #variable}
	 */
	public VarInitialization(String name, ExprElm expression, int namePos, Variable var) {
		this(name, expression, namePos);
		this.variable = var;
	}
	@Override
	public ExprElm[] subElements() {
		return new ExprElm[] {expression};
	}
	@Override
	public void setSubElements(ExprElm[] elms) {
		expression = elms[0];
	}
	@Override
	public void doPrint(ExprWriter output, int depth) {
		if (type != null) {
			output.append(type.typeName(false));
			output.append(" ");
		}
		output.append(name);
		if (expression != null) {
			output.append(" = ");
			expression.print(output, depth+1);
		}
	}
	/**
	 * Return the {@link VarInitialization} preceding this one in the {@link VarDeclarationStatement}
	 * @return
	 */
	public VarInitialization getPreviousInitialization() {
		VarInitialization[] brothers = parentOfType(VarDeclarationStatement.class).varInitializations;
		return ArrayUtil.boundChecked(brothers, ArrayUtil.indexOf(this, brothers)-1);
	}
	/**
	 * Return the subsequent {@link VarInitialization} in the {@link VarDeclarationStatement}
	 * @return
	 */
	public VarInitialization getNextInitialization() {
		VarInitialization[] sisters = parentOfType(VarDeclarationStatement.class).varInitializations;
		return ArrayUtil.boundChecked(sisters, ArrayUtil.indexOf(this, sisters)+1);
	}
	@Override
	public EntityRegion declarationAt(int offset, C4ScriptParser parser) {
		return new EntityRegion(variable, this);
	}
}