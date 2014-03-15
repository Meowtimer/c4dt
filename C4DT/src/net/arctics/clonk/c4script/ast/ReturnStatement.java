package net.arctics.clonk.c4script.ast;

import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodePrinter;
import net.arctics.clonk.ast.ControlFlow;
import net.arctics.clonk.ast.ControlFlowException;
import net.arctics.clonk.ast.IEvaluationContext;
import net.arctics.clonk.c4script.Keywords;

public class ReturnStatement extends KeywordStatement implements ITidyable {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private ASTNode returnExpr;

	public ASTNode returnExpr() { return returnExpr; }

	@Override
	public Object evaluate(final IEvaluationContext context) throws ControlFlowException {
		throw new ReturnException(evaluateVariable(returnExpr.evaluate(context)));
	}

	public ReturnStatement(final ASTNode returnExpr) {
		super();
		this.returnExpr = returnExpr;
		assignParentToSubElements();
	}

	@Override
	public String keyword() {
		return Keywords.Return;
	}

	@Override
	public void doPrint(final ASTNodePrinter builder, final int depth) {
		builder.append(keyword());
		if (returnExpr != null) {
			builder.append(" "); //$NON-NLS-1$
			// return(); -> return 0;
			if (returnExpr == ASTNode.NULL_EXPR)
				builder.append("0"); //$NON-NLS-1$
			else
				returnExpr.print(builder, depth);
		}
		builder.append(";"); //$NON-NLS-1$
	}

	public ASTNode returnExpression() {
		return returnExpr;
	}

	public void setReturnExpr(final ASTNode returnExpr) {
		this.returnExpr = returnExpr;
	}

	@Override
	public ASTNode[] subElements() {
		return new ASTNode[] {returnExpr};
	}

	@Override
	public void setSubElements(final ASTNode[] elms) {
		returnExpr = elms[0];
	}

	@Override
	public ControlFlow controlFlow() {
		return ControlFlow.Return;
	}

	@Override
	public ASTNode tidy(final Tidy tidy) throws CloneNotSupportedException {
		// return (0); -> return 0;
		if (returnExpr instanceof Parenthesized)
			return new ReturnStatement(tidy.tidy(((Parenthesized)returnExpr).innerExpression()));
		// return (0, Sound("Ugh")); -> { Sound("Ugh"); return 0; }
		// FIXME: should declare temporary variable so that order of expression execution isn't changed
		if (returnExpr instanceof Tuple) {
			final Tuple tuple = (Tuple) returnExpr;
			final ASTNode[] tupleElements = tuple.subElements();
			final List<ASTNode> statements = new LinkedList<>();
			for (int i = 1; i < tupleElements.length; i++)
				statements.add(new SimpleStatement(tupleElements[i]));
			statements.add(new ReturnStatement(tupleElements[0]));
			return parent() instanceof ConditionalStatement ? new Block(statements) : new BunchOfStatements(statements);
		}
		return this;
	}
}