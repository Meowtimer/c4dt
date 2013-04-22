package net.arctics.clonk.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodePrinter;
import net.arctics.clonk.ast.IEvaluationContext;
import net.arctics.clonk.c4script.Keywords;
import net.arctics.clonk.c4script.ProblemReportingContext;

public class ReturnStatement extends KeywordStatement {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private ASTNode returnExpr;
	
	public ASTNode returnExpr() { return returnExpr; }

	@Override
	public Object evaluate(IEvaluationContext context) throws ControlFlowException {
		throw new ReturnException(returnExpr.evaluate(context));
	}
	
	public ReturnStatement(ASTNode returnExpr) {
		super();
		this.returnExpr = returnExpr;
		assignParentToSubElements();
	}

	@Override
	public String keyword() {
		return Keywords.Return;
	}

	@Override
	public void doPrint(ASTNodePrinter builder, int depth) {
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

	public void setReturnExpr(ASTNode returnExpr) {
		this.returnExpr = returnExpr;
	}

	@Override
	public ASTNode[] subElements() {
		return new ASTNode[] {returnExpr};
	}

	@Override
	public void setSubElements(ASTNode[] elms) {
		returnExpr = elms[0];
	}

	@Override
	public ControlFlow controlFlow() {
		return ControlFlow.Return;
	}

	@Override
	public ASTNode optimize(final ProblemReportingContext context) throws CloneNotSupportedException {
		// return (0); -> return 0;
		if (returnExpr instanceof Parenthesized)
			return new ReturnStatement(((Parenthesized)returnExpr).innerExpression().optimize(context));
		// return (0, Sound("Ugh")); -> { Sound("Ugh"); return 0; }
		// FIXME: should declare temporary variable so that order of expression execution isn't changed
		/*
		if (returnExpr instanceof Tuple) {
			Tuple tuple = (Tuple) returnExpr;
			ExprElm[] tupleElements = tuple.getElements();
			List<Statement> statements = new LinkedList<Statement>();
			for (int i = 1; i < tupleElements.length; i++) {
				statements.add(new SimpleStatement(tupleElements[i].newStyleReplacement(parser)));
			}
			statements.add(new ReturnStatement(tupleElements[0].newStyleReplacement(parser)));
			return getParent() instanceof ConditionalStatement ? new Block(statements) : new BunchOfStatements(statements);
		}
		 */
		return super.optimize(context);
	}
}