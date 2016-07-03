package net.arctics.clonk.c4script.ast;

import static net.arctics.clonk.util.ArrayUtil.filter;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.text.Region;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodePrinter;
import net.arctics.clonk.ast.ControlFlowException;
import net.arctics.clonk.ast.EntityRegion;
import net.arctics.clonk.ast.ExpressionLocator;
import net.arctics.clonk.ast.IEvaluationContext;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.c4script.Variable.Scope;
import net.arctics.clonk.c4script.ast.evaluate.IVariable;
import net.arctics.clonk.util.ArrayUtil;

/**
 * Variable declaration statement, containing multiple variable declarations which optionally include an initial assignment.
 * @author madeen
 *
 */
public class VarDeclarationStatement extends KeywordStatement {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private VarInitialization[] varInitializations;
	private final Scope scope;

	public Scope scope() { return scope; }

	public VarDeclarationStatement(final List<VarInitialization> varInitializations, final Scope scope) {
		super();
		this.varInitializations = varInitializations.toArray(new VarInitialization[varInitializations.size()]);
		this.scope = scope;
		assignParentToSubElements();
	}

	public VarDeclarationStatement(final Scope scope, final VarInitialization... varInitializations) {
		this(Arrays.asList(varInitializations), scope);
	}

	public VarDeclarationStatement(final String varName, final ASTNode initialization, final int namePos, final Scope scope) {
		this(ArrayUtil.list(new VarInitialization(varName, initialization, namePos, namePos+varName.length(), null, null)), scope);
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
	public void setSubElements(final ASTNode[] elms) {
		final List<VarInitialization> inits = Arrays.asList(filter(elms, VarInitialization.class));
		final VarInitialization[] newElms = new VarInitialization[inits.size()];
		System.arraycopy(inits.toArray(), 0, newElms, 0, inits.size());
		varInitializations = newElms;
	}

	public final VarInitialization[] variableInitializations() {
		return varInitializations;
	}

	@Override
	public void doPrint(final ASTNodePrinter builder, final int depth) {
		builder.append(keyword());
		builder.append(" "); //$NON-NLS-1$
		int counter = 0;
		for (final VarInitialization var : varInitializations) {
			var.print(builder, depth);
			if (++counter < varInitializations.length) {
				builder.append(", "); //$NON-NLS-1$
			}
			else {
				builder.append(";"); //$NON-NLS-1$
			}
		}
	}

	@Override
	public EntityRegion entityAt(int offset, final ExpressionLocator<?> locator) {
		final Function activeFunc = this.parent(Function.class);
		if (activeFunc != null) {
			final int addToMakeAbsolute = activeFunc.bodyLocation().start() + this.start();
			offset += addToMakeAbsolute;
			for (final VarInitialization pair : varInitializations) {
				final String varName = pair.name;
				final Variable var = activeFunc.findVariable(varName);
				if (var != null && var.isAt(offset)) {
					return new EntityRegion(var, new Region(var.start()-activeFunc.bodyLocation().start(), var.getLength()));
				}
			}
		}
		return super.entityAt(offset, locator);
	}

	@Override
	public Object evaluate(IEvaluationContext context) throws ControlFlowException {
		for (final VarInitialization in : varInitializations) {
			if (in.expression != null) {
				final IVariable var = (IVariable)new AccessVar(in.name).evaluate(context);
				var.set(in.expression.evaluate(context));
			}
		}
		return null;
	}

}