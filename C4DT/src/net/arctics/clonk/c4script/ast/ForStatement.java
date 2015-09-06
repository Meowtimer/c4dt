package net.arctics.clonk.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodePrinter;
import net.arctics.clonk.ast.ControlFlowException;
import net.arctics.clonk.ast.IEvaluationContext;
import net.arctics.clonk.c4script.Keywords;
import net.arctics.clonk.c4script.typing.TypeUtil;

public class ForStatement extends ConditionalStatement implements ILoop {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	
	private ASTNode initializer, increment;

	public ForStatement(final ASTNode initializer, final ASTNode condition, final ASTNode increment, final ASTNode body) {
		super(condition, body);
		this.initializer = initializer;
		this.increment = increment;
		assignParentToSubElements();
	}

	@Override
	public String keyword() { return Keywords.For; }
	public ASTNode initializer() { return initializer; }
	public ASTNode increment() { return increment; }

	@Override
	public void doPrint(final ASTNodePrinter builder, final int depth) {
		builder.append(keyword() + " ("); //$NON-NLS-1$
		if (initializer != null) {
			initializer.print(builder, depth+1);
		}
		else {
			builder.append(";"); //$NON-NLS-1$
		}
		builder.append(" "); // no ';' since initializer is already a statement //$NON-NLS-1$
		if (condition != null) {
			condition.print(builder, depth+1);
		}
		builder.append("; "); //$NON-NLS-1$
		if (increment != null) {
			increment.print(builder, depth);
		}
		builder.append(")"); //$NON-NLS-1$
		printBody(builder, depth);
	}

	@Override
	public ASTNode[] subElements() {
		return new ASTNode[] {initializer, condition, increment, body};
	}

	@Override
	public void setSubElements(final ASTNode[] elms) {
		initializer = elms[0];
		condition   = elms[1];
		increment   = elms[2];
		body        = elms[3];
	}

	@Override
	public Object evaluate(final IEvaluationContext context) throws ControlFlowException {
		if (initializer != null) {
			initializer.evaluate(context);
		}
		Object ev = null;
		while (true) {
			if (condition != null && !TypeUtil.convertToBool(condition.evaluate(context))) {
				break;
			}
			ev = body.evaluate(context);
			if (increment != null) {
				increment.evaluate(context);
			}
		}
		return ev;
	}
}