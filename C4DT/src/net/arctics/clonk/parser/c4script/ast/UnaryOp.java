package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.Operator;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.ast.evaluate.IEvaluationContext;

public class UnaryOp extends OperatorExpression {


	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	public enum Placement {
		Prefix,
		Postfix
	}

	private final UnaryOp.Placement placement;
	private ExprElm argument;

	public UnaryOp(Operator operator, UnaryOp.Placement placement, ExprElm argument) {
		super(operator);
		this.placement = placement;
		this.argument = argument;
		this.argument.setParent(this);
	}

	@Override
	public ExprElm[] getSubElements() {
		return new ExprElm[] {argument};
	}

	@Override
	public void setSubElements(ExprElm[] elements) {
		argument = elements[0];
	}

	private boolean needsSpace(UnaryOp other) {
		return this.getOperator().spaceNeededBetweenMeAnd(other.getOperator());
	}

	public void doPrint(ExprWriter output, int depth) {
		UnaryOp unop = (argument instanceof UnaryOp) ? (UnaryOp)argument : null;
		if (unop != null && unop.placement != this.placement)
			unop = null;
		if (placement == Placement.Postfix) {
			argument.print(output, depth+1);
			if (unop != null && needsSpace(unop))
				output.append(" "); // - -5 -.- //$NON-NLS-1$
			output.append(getOperator().getOperatorName());
		} else {
			output.append(getOperator().getOperatorName());
			if (unop != null && needsSpace(unop))
				output.append(" "); // - -5 -.- //$NON-NLS-1$
			argument.print(output, depth+1);
		}
	}

	public ExprElm getArgument() {
		return argument;
	}

	@Override
	public void reportErrors(C4ScriptParser context) throws ParsingException {
		getArgument().reportErrors(context);
		if (getOperator().modifiesArgument() && !getArgument().modifiable(context)) {
			//				System.out.println(getArgument().toString() + " does not behave");
			context.errorWithCode(ParserErrorCode.ExpressionNotModifiable, getArgument(), C4ScriptParser.NO_THROW);
		}
		if (!getArgument().validForType(getOperator().getFirstArgType(), context)) {
			context.warningWithCode(ParserErrorCode.IncompatibleTypes, getArgument(), getOperator().getFirstArgType().toString(), getArgument().getType(context).toString());
		}
		getArgument().expectedToBeOfType(getOperator().getFirstArgType(), context);
	}

	@Override
	public ExprElm optimize(C4ScriptParser context) throws CloneNotSupportedException {
		// could happen when argument is transformed to binary operator
		ExprElm arg = getArgument().optimize(context);
		if (arg instanceof BinaryOp)
			return new UnaryOp(getOperator(), placement, new Parenthesized(arg));
		if (getOperator() == Operator.Not && arg instanceof Parenthesized) {
			Parenthesized brackets = (Parenthesized)arg;
			if (brackets.getInnerExpr() instanceof BinaryOp) {
				BinaryOp op = (BinaryOp) brackets.getInnerExpr();
				if (op.getOperator() == Operator.Equal) {
					return new BinaryOp(Operator.NotEqual, op.getLeftSide().optimize(context), op.getRightSide().optimize(context));
				}
				else if (op.getOperator() == Operator.NotEqual) {
					return new BinaryOp(Operator.Equal, op.getLeftSide().optimize(context), op.getRightSide().optimize(context));
				}
				else if (op.getOperator() == Operator.StringEqual) {
					return new BinaryOp(Operator.ne, op.getLeftSide().optimize(context), op.getRightSide().optimize(context));
				}
				else if (op.getOperator() == Operator.ne) {
					return new BinaryOp(Operator.StringEqual, op.getLeftSide().optimize(context), op.getRightSide().optimize(context));
				}
			}
		}
		return super.optimize(context);
	}

	@Override
	public boolean isConstant() {
		return argument.isConstant();
	}

	@Override
	public boolean modifiable(C4ScriptParser context) {
		return placement == Placement.Prefix && getOperator().returnsRef();
	}

	@Override
	public Object evaluateAtParseTime(IEvaluationContext context) {
		try {
			Object ev = argument.evaluateAtParseTime(context);
			Object conv = getOperator().getFirstArgType().convert(ev);
			switch (getOperator()) {
			case Not:
				return !(Boolean)conv;
			case Subtract:
				return -((Number)conv).longValue();
			case Add:
				return conv;
			}
		}
		catch (ClassCastException e) {}
		catch (NullPointerException e) {}
		return super.evaluateAtParseTime(context);
	}

}