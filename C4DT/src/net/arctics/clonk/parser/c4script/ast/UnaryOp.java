package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.Operator;
import net.arctics.clonk.parser.c4script.ast.evaluate.IEvaluationContext;

public class UnaryOp extends OperatorExpression {


	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

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
	public ExprElm[] subElements() {
		return new ExprElm[] {argument};
	}

	@Override
	public void setSubElements(ExprElm[] elements) {
		argument = elements[0];
	}

	private boolean needsSpace(UnaryOp other) {
		return this.operator().spaceNeededBetweenMeAnd(other.operator());
	}

	@Override
	public void doPrint(ExprWriter output, int depth) {
		UnaryOp unop = (argument instanceof UnaryOp) ? (UnaryOp)argument : null;
		if (unop != null && unop.placement != this.placement)
			unop = null;
		if (placement == Placement.Postfix) {
			argument.print(output, depth+1);
			if (unop != null && needsSpace(unop))
				output.append(" "); // - -5 -.- //$NON-NLS-1$
			output.append(operator().operatorName());
		} else {
			output.append(operator().operatorName());
			if (unop != null && needsSpace(unop))
				output.append(" "); // - -5 -.- //$NON-NLS-1$
			argument.print(output, depth+1);
		}
	}

	public ExprElm argument() {
		return argument;
	}

	@Override
	public void reportErrors(C4ScriptParser context) throws ParsingException {
		argument().reportErrors(context);
		if (operator().modifiesArgument() && !argument().isModifiable(context)) {
			//				System.out.println(getArgument().toString() + " does not behave");
			context.errorWithCode(ParserErrorCode.ExpressionNotModifiable, argument(), C4ScriptParser.NO_THROW);
		}
		if (!argument().validForType(operator().firstArgType(), context)) {
			context.warningWithCode(ParserErrorCode.IncompatibleTypes, argument(), operator().firstArgType().toString(), argument().typeInContext(context).toString());
		}
		argument().expectedToBeOfType(operator().firstArgType(), context, TypeExpectancyMode.Expect, ParserErrorCode.InternalError);
	}

	@Override
	public ExprElm optimize(C4ScriptParser context) throws CloneNotSupportedException {
		// could happen when argument is transformed to binary operator
		ExprElm arg = argument().optimize(context);
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
	public boolean isModifiable(C4ScriptParser context) {
		return placement == Placement.Prefix && operator().returnsRef();
	}

	@Override
	public Object evaluateAtParseTime(IEvaluationContext context) {
		try {
			Object ev = argument.evaluateAtParseTime(context);
			Object conv = operator().firstArgType().convert(ev);
			switch (operator()) {
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