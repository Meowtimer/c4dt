package net.arctics.clonk.parser.c4script.ast;

import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.DeclarationObtainmentContext;
import net.arctics.clonk.parser.c4script.Operator;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.ast.evaluate.IEvaluationContext;

public class BinaryOp extends OperatorExpression {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	@Override
	protected IType obtainType(DeclarationObtainmentContext context) {
		switch (getOperator()) {
		// &&/|| special: they return either the left or right side of the operator so the return type is the lowest common denominator of the argument types
		case And: case Or:
			IType leftSideType = getLeftSide().getType(context);
			IType rightSideType = getRightSide().getType(context);
			if (leftSideType == rightSideType)
				return leftSideType;
			else
				return PrimitiveType.ANY;
		case Assign:
			return getRightSide().getType(context);
		default:
			return super.obtainType(context);
		}
	}

	@Override
	public ExprElm optimize(C4ScriptParser context) throws CloneNotSupportedException {
		// #strict 2: ne -> !=, S= -> ==
		if (context.getStrictLevel() >= 2) {
			Operator op = getOperator();
			if (op == Operator.StringEqual || op == Operator.eq)
				op = Operator.Equal;
			else if (op == Operator.ne)
				op = Operator.NotEqual;
			if (op != getOperator()) {
				return new BinaryOp(op, getLeftSide().optimize(context), getRightSide().optimize(context));
			}
		}

		// blub() && blab() && return(1); -> {blub(); blab(); return(1);}
		if ((getOperator() == Operator.And || getOperator() == Operator.Or) && (getParent() instanceof SimpleStatement)) {// && getRightSide().isReturn()) {
			ExprElm block = convertOperatorHackToBlock(context);
			if (block != null)
				return block;
		}

		return super.optimize(context);
	}

	private ExprElm convertOperatorHackToBlock(C4ScriptParser context) throws CloneNotSupportedException {
		LinkedList<ExprElm> leftSideArguments = new LinkedList<ExprElm>();
		ExprElm r;
		boolean works = true;
		Operator hackOp = this.getOperator();
		// gather left sides (must not be operators)
		for (r = getLeftSide(); r instanceof BinaryOp; r = ((BinaryOp)r).getLeftSide()) {
			BinaryOp op = (BinaryOp)r;
			if (op.getOperator() != hackOp) {
				works = false;
				break;
			}
			if (op.getRightSide() instanceof BinaryOp) {
				works = false;
				break;
			}
			leftSideArguments.addLast(op.getRightSide());
		}
		// return at the right end signals this should rather be a block
		if (works) {
			leftSideArguments.addFirst(r);
			List<Statement> statements = new LinkedList<Statement>();
			// wrap expressions in statements
			for (ExprElm ex : leftSideArguments) {
				statements.add(new SimpleStatement(ex.optimize(context)));
			}
			// convert func call to proper return statement
			if (getRightSide().getControlFlow() == ControlFlow.Return)
				statements.add(new ReturnStatement(((CallFunc)getRightSide()).getReturnArg().optimize(context)));
			else
				statements.add(new SimpleStatement(getRightSide().optimize(context)));
			return new Block(statements);
		}
		return null;
	}

	private ExprElm leftSide, rightSide;

	@Override
	public ExprElm[] getSubElements() {
		return new ExprElm[] {leftSide, rightSide};
	}

	@Override
	public void setSubElements(ExprElm[] elements) {
		leftSide  = elements[0];
		rightSide = elements[1];
	}

	public BinaryOp(Operator operator, ExprElm leftSide, ExprElm rightSide) {
		super(operator);
		setLeftSide(leftSide);
		setRightSide(rightSide);
	}

	public void checkTopLevelAssignment(C4ScriptParser parser) throws ParsingException {
		if (!getOperator().modifiesArgument())
			parser.warningWithCode(ParserErrorCode.NoAssignment, this);
	}

	public BinaryOp(Operator op) {
		super(op);
	}

	public ExprElm getLeftSide() {
		return leftSide;
	}

	public ExprElm getRightSide() {
		return rightSide;
	}

	public void setLeftSide(ExprElm leftSide) {
		this.leftSide = leftSide;
		leftSide.setParent(this);
	}

	public void setRightSide(ExprElm rightSide) {
		this.rightSide = rightSide;
		rightSide.setParent(this);
		setFinishedProperly(rightSide.isFinishedProperly());
	}

	public void doPrint(ExprWriter output, int depth) {

		// put brackets around operands in case some transformation messed up prioritization
		boolean needsBrackets = leftSide instanceof BinaryOp && getOperator().getPriority() > ((BinaryOp)leftSide).getOperator().getPriority();
		if (needsBrackets)
			output.append("("); //$NON-NLS-1$
		leftSide.print(output, depth+1);
		if (needsBrackets)
			output.append(")"); //$NON-NLS-1$

		output.append(" "); //$NON-NLS-1$
		output.append(getOperator().getOperatorName());
		output.append(" "); //$NON-NLS-1$

		needsBrackets = rightSide instanceof BinaryOp && getOperator().getPriority() > ((BinaryOp)rightSide).getOperator().getPriority();
		if (needsBrackets)
			output.append("("); //$NON-NLS-1$
		rightSide.print(output, depth+1);
		if (needsBrackets)
			output.append(")"); //$NON-NLS-1$
	}

	@Override
	public void reportErrors(C4ScriptParser context) throws ParsingException {
		getLeftSide().reportErrors(context);
		getRightSide().reportErrors(context);
		// sanity
		setExprRegion(getLeftSide().getExprStart(), getRightSide().getExprEnd());
		// i'm an assignment operator and i can't modify my left side :C
		if (getOperator().modifiesArgument() && !getLeftSide().modifiable(context)) {
			context.errorWithCode(ParserErrorCode.ExpressionNotModifiable, getLeftSide(), C4ScriptParser.NO_THROW);
		}
		// obsolete operators in #strict 2
		if ((getOperator() == Operator.StringEqual || getOperator() == Operator.ne) && (context.getStrictLevel() >= 2)) {
			context.warningWithCode(ParserErrorCode.ObsoleteOperator, this, getOperator().getOperatorName());
		}
		// wrong parameter types
		if (!getLeftSide().validForType(getOperator().getFirstArgType(), context))
			context.warningWithCode(ParserErrorCode.IncompatibleTypes, getLeftSide(), getOperator().getFirstArgType(), getLeftSide().getType(context));
		if (!getRightSide().validForType(getOperator().getSecondArgType(), context))
			context.warningWithCode(ParserErrorCode.IncompatibleTypes, getRightSide(), getOperator().getSecondArgType(), getRightSide().getType(context));

		IType expectedLeft, expectedRight;
		switch (getOperator()) {
		case Assign: case Equal:
			expectedLeft = expectedRight = null;
			break;
		default:
			expectedLeft = getOperator().getFirstArgType();
			expectedRight = getOperator().getSecondArgType();
		}
		
		if (expectedLeft != null)
			getLeftSide().expectedToBeOfType(expectedLeft, context);
		if (expectedRight != null)
			getRightSide().expectedToBeOfType(expectedRight, context);

		if (getOperator() == Operator.Assign) {
			getLeftSide().inferTypeFromAssignment(getRightSide(), context);
		}
	}
	
	@Override
	public void postLoad(ExprElm parent, DeclarationObtainmentContext root) {
		super.postLoad(parent, root);
		if (getOperator() == Operator.Assign) {
			getLeftSide().inferTypeFromAssignment(getRightSide(), root);
		}
	}

	@Override
	public Object evaluateAtParseTime(IEvaluationContext context) {
		try {
			Object leftSide  = getOperator().getFirstArgType().convert(this.getLeftSide().evaluateAtParseTime(context));
			Object rightSide = getOperator().getSecondArgType().convert(this.getRightSide().evaluateAtParseTime(context));
			if (leftSide != null && leftSide != ExprElm.EVALUATION_COMPLEX) {
				switch (getOperator()) {
				case And:
					// false && <anything> => false
					if (leftSide.equals(false))
						return false;
				case Or:
					// true || <anything> => true 
					if (leftSide.equals(true))
						return true;
				}
				if (rightSide != null && rightSide != ExprElm.EVALUATION_COMPLEX) {
					return evaluateOn(leftSide, rightSide);
				}
			}
		}
		catch (ClassCastException e) {}
		catch (NullPointerException e) {}
		return super.evaluateAtParseTime(context);
	}

	private Object evaluateOn(Object leftSide, Object rightSide) {
        switch (getOperator()) {
        case Add:
        	return ((Number)leftSide).longValue() + ((Number)rightSide).longValue();
        case Subtract:
        	return ((Number)leftSide).longValue() - ((Number)rightSide).longValue();
        case Multiply:
        	return ((Number)leftSide).longValue() * ((Number)rightSide).longValue();
        case Divide:
        	return ((Number)leftSide).longValue() / ((Number)rightSide).longValue();
        case Modulo:
        	return ((Number)leftSide).longValue() % ((Number)rightSide).longValue();
        case Larger:
        	return ((Number)leftSide).longValue() > ((Number)rightSide).longValue();
        case Smaller:
        	return ((Number)leftSide).longValue() < ((Number)rightSide).longValue();
        case LargerEqual:
        	return ((Number)leftSide).longValue() >= ((Number)rightSide).longValue();
        case SmallerEqual:
        	return ((Number)leftSide).longValue() <= ((Number)rightSide).longValue();
        case Equal:
        	return leftSide.equals(rightSide);
        default:
        	return null;
        }
    }
	
	@Override
	public Object evaluate(IEvaluationContext context) throws ControlFlowException {
	    Object left = getLeftSide().evaluate(context);
	    Object right = getRightSide().evaluate(context);
	    if (left != null && right != null)
	    	return evaluateOn(left, right);
	    else
	    	return null;
	}
	
	@Override
	public boolean isConstant() {
		// CNAT_Left | CNAT_Right are considered constant for example
		switch (getOperator()) {
		case BitOr: case BitAnd:
			return getLeftSide().isConstant() && getRightSide().isConstant();
		default:
			return false;
		}
	}

}