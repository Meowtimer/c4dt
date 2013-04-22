package net.arctics.clonk.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodePrinter;
import net.arctics.clonk.ast.IEvaluationContext;
import net.arctics.clonk.c4script.Operator;
import net.arctics.clonk.c4script.ProblemReportingContext;

public class UnaryOp extends OperatorExpression {


	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	public enum Placement {
		Prefix,
		Postfix
	}

	private final UnaryOp.Placement placement;
	private ASTNode argument;

	public UnaryOp(Operator operator, UnaryOp.Placement placement, ASTNode argument) {
		super(operator);
		this.placement = placement;
		this.argument = argument;
		this.argument.setParent(this);
	}

	@Override
	public ASTNode[] subElements() { return new ASTNode[] {argument}; }
	@Override
	public void setSubElements(ASTNode[] elements) { argument = elements[0]; }
	public UnaryOp.Placement placement() { return placement; }

	@Override
	public void doPrint(ASTNodePrinter output, int depth) {
		UnaryOp unop = (argument instanceof UnaryOp) ? (UnaryOp)argument : null;
		if (unop != null && unop.placement != this.placement)
			unop = null;
		if (placement == Placement.Postfix) {
			argument.print(output, depth+1);
			if (unop != null && this.operator().spaceNeededBetweenMeAnd(unop.operator()))
				output.append(" "); // - -5 -.- //$NON-NLS-1$
			output.append(operator().operatorName());
		} else {
			output.append(operator().operatorName());
			if (unop != null && this.operator().spaceNeededBetweenMeAnd(unop.operator()))
				output.append(" "); // - -5 -.- //$NON-NLS-1$
			argument.print(output, depth+1);
		}
	}

	public ASTNode argument() {
		return argument;
	}

	@Override
	public ASTNode optimize(final ProblemReportingContext context) throws CloneNotSupportedException {
		// could happen when argument is transformed to binary operator
		ASTNode arg = argument().optimize(context);
		if (arg instanceof BinaryOp)
			return new UnaryOp(operator(), placement, new Parenthesized(arg));
		if (operator() == Operator.Not && arg instanceof Parenthesized) {
			Parenthesized brackets = (Parenthesized)arg;
			if (brackets.innerExpression() instanceof BinaryOp) {
				BinaryOp op = (BinaryOp) brackets.innerExpression();
				Operator oppo = null;
				switch (op.operator()) {
				case Equal:
					oppo = Operator.NotEqual;
					break;
				case NotEqual:
					oppo = Operator.Equal;
					break;
				case StringEqual:
					oppo = Operator.ne;
					break;
				case ne:
					oppo = Operator.StringEqual;
					break;
				default:
					break;
				}
				if (oppo != null)
					return new BinaryOp(oppo, op.leftSide().optimize(context), op.rightSide().optimize(context));
			}
		}
		return super.optimize(context);
	}

	@Override
	public boolean isConstant() {
		return argument.isConstant();
	}

	@Override
	public Object evaluateStatic(IEvaluationContext context) {
		try {
			Object ev = argument.evaluateStatic(context);
			Object conv = operator().firstArgType().convert(ev);
			switch (operator()) {
			case Not:
				return !(Boolean)conv;
			case Subtract:
				return -((Number)conv).longValue();
			case Add:
				return conv;
			default:
				break;
			}
		}
		catch (ClassCastException e) {}
		catch (NullPointerException e) {}
		return super.evaluateStatic(context);
	}

}