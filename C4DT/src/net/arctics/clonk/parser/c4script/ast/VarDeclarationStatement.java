package net.arctics.clonk.parser.c4script.ast;

import java.util.Arrays;
import java.util.List;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.EntityRegion;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.Variable.Scope;
import net.arctics.clonk.util.ArrayUtil;
import org.eclipse.jface.text.Region;

/**
 * Variable declaration statement, containing multiple variable declarations which optionally include an initial assignment.
 * @author madeen
 *
 */
public class VarDeclarationStatement extends KeywordStatement {
	
	/**
	 * A single var declaration/initialization. Includes name of variable being declared, optionally the initialization expression and
	 * a reference to the actual {@link Variable} object representing the variable being declared. 
	 * @author madeen
	 *
	 */
	public static final class VarInitialization extends ExprElm {

		private static final long serialVersionUID = 1L;

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
		public Variable variableBeingInitialized;
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
			setExprRegion(namePos, expression != null ? expression.getExprEnd() : namePos + name.length());
			assignParentToSubElements();
		}
		/**
		 * Creat a new {@link VarInitialization}. Calls {@link VarInitialization#VarInitialization(String, ExprElm, int)}, additionally assigning {@link #variableBeingInitialized} the passed var.
		 * @param name Name of variable
		 * @param expression Expression. Can be null.
		 * @param namePos Position of name. Used for setting the region of this expression ({@link #setExprRegion(int, int)}})
		 * @param var Variable to assign to {@link #variableBeingInitialized}
		 */
		public VarInitialization(String name, ExprElm expression, int namePos, Variable var) {
			this(name, expression, namePos);
			this.variableBeingInitialized = var;
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
			VarInitialization[] brothers = getParent(VarDeclarationStatement.class).varInitializations;
			return ArrayUtil.boundChecked(brothers, ArrayUtil.indexOf(this, brothers)-1);
		}
		/**
		 * Return the subsequent {@link VarInitialization} in the {@link VarDeclarationStatement}
		 * @return
		 */
		public VarInitialization getNextInitialization() {
			VarInitialization[] brothers = getParent(VarDeclarationStatement.class).varInitializations;
			return ArrayUtil.boundChecked(brothers, ArrayUtil.indexOf(this, brothers)+1);
		}
		@Override
		public EntityRegion declarationAt(int offset, C4ScriptParser parser) {
			return new EntityRegion(variableBeingInitialized, this);
		}
	}
	
	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	private VarInitialization[] varInitializations;
	private Scope scope;

	public VarDeclarationStatement(List<VarInitialization> varInitializations, Scope scope) {
		super();
		this.varInitializations = varInitializations.toArray(new VarInitialization[varInitializations.size()]);
		this.scope = scope;
		assignParentToSubElements();
	}
	public VarDeclarationStatement(Scope scope, VarInitialization... varInitializations) {
		this(Arrays.asList(varInitializations), scope);
	}
	public VarDeclarationStatement(String varName, ExprElm initialization, int namePos, Scope scope) {
		this(ArrayUtil.list(new VarInitialization(varName, initialization, namePos)), scope);
	}
	@Override
	public String keyword() {
		return scope.toKeyword();
	}
	@Override
	public VarInitialization[] subElements() {
		return varInitializations;
	}
	@Override
	public void setSubElements(ExprElm[] elms) {
		VarInitialization[] newElms = new VarInitialization[elms.length];
		System.arraycopy(elms, 0, newElms, 0, elms.length);
		varInitializations = newElms;
	}
	public final VarInitialization[] variableInitializations() {
		return varInitializations;
	}
	@Override
	public void doPrint(ExprWriter builder, int depth) {
		builder.append(keyword());
		builder.append(" "); //$NON-NLS-1$
		int counter = 0;
		for (VarInitialization var : varInitializations) {
			var.print(builder, depth);
			if (++counter < varInitializations.length)
				builder.append(", "); //$NON-NLS-1$
			else
				builder.append(";"); //$NON-NLS-1$
		}
	}
	@Override
	public EntityRegion declarationAt(int offset, C4ScriptParser parser) {
		Function activeFunc = parser.currentFunction();
		if (activeFunc != null) {				
			int addToMakeAbsolute = activeFunc.body().getStart() + this.getExprStart();
			offset += addToMakeAbsolute;
			for (VarInitialization pair : varInitializations) {
				String varName = pair.name;
				Variable var = activeFunc.findVariable(varName);
				if (var != null && var.isAt(offset))
					return new EntityRegion(var, new Region(var.location().getStart()-activeFunc.body().getStart(), var.location().getLength()));
			}
		}
		return super.declarationAt(offset, parser);
	}
	@Override
	public void reportErrors(C4ScriptParser parser) throws ParsingException {
		super.reportErrors(parser);
		for (VarInitialization initialization : varInitializations) {
			if (initialization.variableBeingInitialized != null && initialization.expression != null) {
				new AccessVar(initialization.variableBeingInitialized).expectedToBeOfType(initialization.expression.typeInContext(parser), parser, TypeExpectancyMode.Force);
			}
		}
	}
}