package net.arctics.clonk.parser.c4script.ast;

import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.DeclarationObtainmentContext;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.Operator;
import net.arctics.clonk.parser.c4script.ast.evaluate.IEvaluationContext;

public class BinaryOp extends OperatorExpression {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	@Override
	public IType unresolvedType(DeclarationObtainmentContext context) {
		switch (operator()) {
		// &&/|| special: they return either the left or right side of the operator so the return type is the lowest common denominator of the argument types
		case And: case Or: case JumpNotNil:
			IType leftSideType = leftSide().type(context);
			IType rightSideType = rightSide().type(context);
			if (leftSideType == rightSideType)
				return leftSideType;
			else
				return TypeUnification.unify(leftSideType, rightSideType);
		case Assign:
			return rightSide().type(context);
		default:
			return super.unresolvedType(context);
		}
	}

	@Override
	public ExprElm optimize(C4ScriptParser context) throws CloneNotSupportedException {
		// #strict 2: ne -> !=, S= -> ==
		if (context.strictLevel() >= 2) {
			Operator op = operator();
			if (op == Operator.StringEqual || op == Operator.eq)
				op = Operator.Equal;
			else if (op == Operator.ne)
				op = Operator.NotEqual;
			if (op != operator())
				return new BinaryOp(op, leftSide().optimize(context), rightSide().optimize(context));
		}

		// blub() && blab() && return(1); -> {blub(); blab(); return(1);}
		if ((operator() == Operator.And || operator() == Operator.Or) && (parent() instanceof SimpleStatement)) {// && getRightSide().isReturn()) {
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
		Operator hackOp = this.operator();
		// gather left sides (must not be operators)
		for (r = leftSide(); r instanceof BinaryOp; r = ((BinaryOp)r).leftSide()) {
			BinaryOp op = (BinaryOp)r;
			if (op.operator() != hackOp) {
				works = false;
				break;
			}
			if (op.rightSide() instanceof BinaryOp) {
				works = false;
				break;
			}
			leftSideArguments.addLast(op.rightSide());
		}
		// return at the right end signals this should rather be a block
		if (works) {
			leftSideArguments.addFirst(r);
			List<Statement> statements = new LinkedList<Statement>();
			// wrap expressions in statements
			for (ExprElm ex : leftSideArguments)
				statements.add(new SimpleStatement(ex.optimize(context)));
			// convert func call to proper return statement
			if (rightSide().controlFlow() == ControlFlow.Return)
				statements.add(new ReturnStatement(((CallDeclaration)rightSide()).soleParm().optimize(context)));
			else
				statements.add(new SimpleStatement(rightSide().optimize(context)));
			return new Block(statements);
		}
		return null;
	}

	private ExprElm leftSide, rightSide;

	@Override
	public ExprElm[] subElements() {
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
		if (!operator().modifiesArgument())
			parser.warning(ParserErrorCode.NoAssignment, this, 0);
	}

	public BinaryOp(Operator op) {
		super(op);
	}

	public ExprElm leftSide() {
		return leftSide;
	}

	public ExprElm rightSide() {
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

	@Override
	public void doPrint(ExprWriter output, int depth) {
		// put brackets around operands in case some transformation messed up prioritization
		boolean needsBrackets = leftSide instanceof BinaryOp && operator().priority() > ((BinaryOp)leftSide).operator().priority();
		if (needsBrackets)
			output.append("("); //$NON-NLS-1$
		leftSide.print(output, depth);
		if (needsBrackets)
			output.append(")"); //$NON-NLS-1$

		output.append(" "); //$NON-NLS-1$
		output.append(operator().operatorName());
		output.append(" "); //$NON-NLS-1$

		needsBrackets = rightSide instanceof BinaryOp && operator().priority() > ((BinaryOp)rightSide).operator().priority();
		if (needsBrackets)
			output.append("("); //$NON-NLS-1$
		if (rightSide instanceof PropListExpression)
			Conf.blockPrelude(output, depth);
		rightSide.print(output, depth);
		if (needsBrackets)
			output.append(")"); //$NON-NLS-1$
	}

	@Override
	public void reportProblems(C4ScriptParser context) throws ParsingException {
		// sanity
		setExprRegion(leftSide().start(), rightSide().end());
		// i'm an assignment operator and i can't modify my left side :C
		if (operator().modifiesArgument() && !leftSide().isModifiable(context))
			context.error(ParserErrorCode.ExpressionNotModifiable, leftSide(), C4ScriptParser.NO_THROW);
		// obsolete operators in #strict 2
		if ((operator() == Operator.StringEqual || operator() == Operator.ne) && (context.strictLevel() >= 2))
			context.warning(ParserErrorCode.ObsoleteOperator, this, 0, operator().operatorName());
		// wrong parameter types
		if (!leftSide().validForType(operator().firstArgType(), context))
			context.warning(ParserErrorCode.IncompatibleTypes, leftSide(), 0, operator().firstArgType(), leftSide().type(context));
		if (!rightSide().validForType(operator().secondArgType(), context))
			context.warning(ParserErrorCode.IncompatibleTypes, rightSide(), 0, operator().secondArgType(), rightSide().type(context));

		IType expectedLeft, expectedRight;
		switch (operator()) {
		case Assign: case Equal:
			expectedLeft = expectedRight = null;
			break;
		default:
			expectedLeft  = operator().firstArgType();
			expectedRight = operator().secondArgType();
		}
		
		if (expectedLeft != null)
			leftSide().expectedToBeOfType(expectedLeft, context);
		if (expectedRight != null)
			rightSide().expectedToBeOfType(expectedRight, context);

		switch (operator()) {
		case Assign: case AssignAdd: case AssignSubtract: case AssignMultiply: case AssignModulo: case AssignDivide:
			leftSide().assignment(rightSide(), context);
			break;
		default:
			break;
		}
	}

	@Override
	public Object evaluateAtParseTime(IEvaluationContext context) {
		try {
			Object leftSide  = operator().firstArgType().convert(this.leftSide().evaluateAtParseTime(context));
			Object rightSide = operator().secondArgType().convert(this.rightSide().evaluateAtParseTime(context));
			if (leftSide != null && leftSide != ExprElm.EVALUATION_COMPLEX) {
				switch (operator()) {
				case And:
					// false && <anything> => false
					if (leftSide.equals(false))
						return false;
				case Or:
					// true || <anything> => true 
					if (leftSide.equals(true))
						return true;
				default:
					break;
				}
				if (rightSide != null && rightSide != ExprElm.EVALUATION_COMPLEX)
					return evaluateOn(leftSide, rightSide);
			}
		}
		catch (ClassCastException e) {}
		catch (NullPointerException e) {}
		return super.evaluateAtParseTime(context);
	}

	private Object evaluateOn(Object leftSide, Object rightSide) {
        switch (operator()) {
        case Add:
        	return ((Number)leftSide).doubleValue() + ((Number)rightSide).doubleValue();
        case Subtract:
        	return ((Number)leftSide).doubleValue() - ((Number)rightSide).doubleValue();
        case Multiply:
        	return ((Number)leftSide).doubleValue() * ((Number)rightSide).doubleValue();
        case Divide:
        	return ((Number)leftSide).doubleValue() / ((Number)rightSide).doubleValue();
        case Modulo:
        	return ((Number)leftSide).doubleValue() % ((Number)rightSide).doubleValue();
        case Larger:
        	return ((Number)leftSide).doubleValue() > ((Number)rightSide).doubleValue();
        case Smaller:
        	return ((Number)leftSide).doubleValue() < ((Number)rightSide).doubleValue();
        case LargerEqual:
        	return ((Number)leftSide).doubleValue() >= ((Number)rightSide).doubleValue();
        case SmallerEqual:
        	return ((Number)leftSide).doubleValue() <= ((Number)rightSide).doubleValue();
        case StringEqual:
        case Equal:
        	return leftSide.equals(rightSide);
        default:
        	return null;
        }
    }
	
	@Override
	public Object evaluate(IEvaluationContext context) throws ControlFlowException {
	    Object left = leftSide().evaluate(context);
	    Object right = rightSide().evaluate(context);
	    if (left != null && right != null)
	    	return evaluateOn(left, right);
	    else
	    	return null;
	}
	
	@Override
	public boolean isConstant() {
		// CNAT_Left | CNAT_Right are considered constant for example
		switch (operator()) {
		case BitOr: case BitAnd:
			return leftSide().isConstant() && rightSide().isConstant();
		default:
			return false;
		}
	}

}