package net.arctics.clonk.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodePrinter;
import net.arctics.clonk.ast.ControlFlowException;
import net.arctics.clonk.ast.IEvaluationContext;
import net.arctics.clonk.c4script.Operator;
import net.arctics.clonk.c4script.ast.evaluate.IVariable;

public class UnaryOp extends OperatorExpression implements ITidyable {


	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	public enum Placement {
		Prefix,
		Postfix
	}

	private final UnaryOp.Placement placement;
	private ASTNode argument;

	public UnaryOp(final Operator operator, final UnaryOp.Placement placement, final ASTNode argument) {
		super(operator);
		this.placement = placement;
		this.argument = argument;
		this.argument.setParent(this);
	}

	@Override
	public ASTNode[] subElements() { return new ASTNode[] {argument}; }
	@Override
	public void setSubElements(final ASTNode[] elements) { argument = elements[0]; }
	public UnaryOp.Placement placement() { return placement; }

	@Override
	public void doPrint(final ASTNodePrinter output, final int depth) {
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
	public ASTNode tidy(final Tidy tidy) throws CloneNotSupportedException {
		// could happen when argument is transformed to binary operator
		final ASTNode arg = tidy.tidy(argument());
		if (arg instanceof BinaryOp)
			return new UnaryOp(operator(), placement, new Parenthesized(arg));
		if (operator() == Operator.Not && arg instanceof Parenthesized) {
			final Parenthesized brackets = (Parenthesized)arg;
			if (brackets.innerExpression() instanceof BinaryOp) {
				final BinaryOp op = (BinaryOp) brackets.innerExpression();
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
					return new BinaryOp(oppo, tidy.tidy(op.leftSide()), tidy.tidy(op.rightSide()));
			}
		}
		return this;
	}

	@Override
	public boolean isConstant() {
		return argument.isConstant();
	}

	@Override
	public Object evaluateStatic(final IEvaluationContext context) {
		try {
			final Object ev = argument.evaluateStatic(context);
			final Object conv = operator().firstArgType().convert(ev);
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
		catch (final ClassCastException e) {}
		catch (final NullPointerException e) {}
		return super.evaluateStatic(context);
	}
	
	@Override
	public Object evaluate(IEvaluationContext context) throws ControlFlowException {
		final Object ev = argument.evaluate(context);
		final Object conv = operator().firstArgType().convert(ev);
		switch (operator()) {
		case Not:
			return !(Boolean)conv;
		case Subtract:
			return -((Number)conv).longValue();
		case Add:
			return conv;
		case Increment: {
			final IVariable v = (IVariable)ev;
			switch (placement) {
			case Postfix:
				v.set(((Number)v.get()).longValue() + 1);
				return v.get();
			case Prefix:
				final Object res = v.get();
				v.set(((Number)v.get()).longValue() + 1);
				return res;
			default:
				return null;
			}
		}
		case Decrement: {
			final IVariable v = (IVariable)ev;
			switch (placement) {
			case Postfix:
				v.set(((Number)v.get()).longValue() - 1);
				return v.get();
			case Prefix:
				final Object res = v.get();
				v.set(((Number)v.get()).longValue() - 1);
				return res;
			default:
				return null;
			}
		}
		default:
			return null;
		}
	}

}