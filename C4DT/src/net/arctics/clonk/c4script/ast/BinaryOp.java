package net.arctics.clonk.c4script.ast;

import static net.arctics.clonk.util.Utilities.eq;

import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodePrinter;
import net.arctics.clonk.ast.ControlFlow;
import net.arctics.clonk.ast.ControlFlowException;
import net.arctics.clonk.ast.IEvaluationContext;
import net.arctics.clonk.c4script.Operator;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.ast.evaluate.IVariable;

public class BinaryOp extends OperatorExpression implements ITidyable {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	@Override
	public ASTNode tidy(final Tidy tidy) throws CloneNotSupportedException {
		// #strict 2: ne -> !=, S= -> ==
		final Script s = parent(Script.class);
		if (s == null)
			return this;
		if (s.strictLevel() >= 2) {
			Operator op = operator();
			if (op == Operator.StringEqual || op == Operator.eq)
				op = Operator.Equal;
			else if (op == Operator.ne)
				op = Operator.NotEqual;
			if (op != operator())
				return new BinaryOp(op, tidy.tidy(leftSide()), tidy.tidy(rightSide()));
		}

		// blub() && blab() && return(1); -> {blub(); blab(); return(1);}
		if ((operator() == Operator.And || operator() == Operator.Or) && (parent() instanceof SimpleStatement)) {// && getRightSide().isReturn()) {
			final ASTNode block = convertOperatorHackToBlock(tidy);
			if (block != null)
				return block;
		}

		return this;
	}

	private ASTNode convertOperatorHackToBlock(Tidy tidy) throws CloneNotSupportedException {
		final LinkedList<ASTNode> leftSideArguments = new LinkedList<ASTNode>();
		ASTNode r;
		boolean works = true;
		final Operator hackOp = this.operator();
		// gather left sides (must not be operators)
		for (r = leftSide(); r instanceof BinaryOp; r = ((BinaryOp)r).leftSide()) {
			final BinaryOp op = (BinaryOp)r;
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
			final List<ASTNode> statements = new LinkedList<ASTNode>();
			// wrap expressions in statements
			for (final ASTNode ex : leftSideArguments)
				statements.add(new SimpleStatement(tidy.tidy(ex)));
			// convert func call to proper return statement
			if (rightSide().controlFlow() == ControlFlow.Return)
				statements.add(new ReturnStatement(tidy.tidy(((CallDeclaration)rightSide()).soleParm())));
			else
				statements.add(new SimpleStatement(tidy.tidy(rightSide())));
			return new Block(statements);
		}
		return null;
	}

	private ASTNode leftSide, rightSide;

	@Override
	public ASTNode[] subElements() {
		return new ASTNode[] {leftSide, rightSide};
	}

	@Override
	public void setSubElements(ASTNode[] elements) {
		leftSide  = elements[0];
		rightSide = elements.length > 1 ? elements[1] : null;
		if (rightSide == null)
			System.out.println("woat?");
	}

	public BinaryOp(Operator operator, ASTNode leftSide, ASTNode rightSide) {
		super(operator);
		setLeftSide(leftSide);
		setRightSide(rightSide);
	}

	public BinaryOp(Operator op) {
		super(op);
	}

	public ASTNode leftSide() {
		return leftSide;
	}

	public ASTNode rightSide() {
		return rightSide;
	}

	public void setLeftSide(ASTNode leftSide) {
		this.leftSide = leftSide;
		leftSide.setParent(this);
	}

	public void setRightSide(ASTNode rightSide) {
		this.rightSide = rightSide;
		rightSide.setParent(this);
	}

	@Override
	public void doPrint(ASTNodePrinter output, int depth) {
		// put brackets around operands in case some transformation messed up prioritization
		boolean needsBrackets = leftSide instanceof BinaryOp && operator().priority() > ((BinaryOp)leftSide).operator().priority();
		if (needsBrackets)
			output.append("("); //$NON-NLS-1$
		leftSide.print(output, depth);
		if (needsBrackets)
			output.append(")"); //$NON-NLS-1$

		output.append(" "); //$NON-NLS-1$
		output.append(operator().operatorName());

		needsBrackets = rightSide instanceof BinaryOp && operator().priority() > ((BinaryOp)rightSide).operator().priority();
		if (needsBrackets)
			output.append(" ("); //$NON-NLS-1$
		else {
			final String printed = rightSide.printed(depth);
			if (!printed.startsWith("\n"))
				output.append(" ");
			output.append(printed);
		}
		if (needsBrackets)
			output.append(")"); //$NON-NLS-1$
	}


	@Override
	public Object evaluateStatic(IEvaluationContext context) {
		try {
			final Object leftSide  = operator().firstArgType().convert(this.leftSide().evaluateStatic(context));
			final Object rightSide = operator().secondArgType().convert(this.rightSide().evaluateStatic(context));
			if (leftSide != null && leftSide != ASTNode.EVALUATION_COMPLEX) {
				switch (operator()) {
				case And:
					// false && <anything> => false
					if (leftSide.equals(false))
						return false;
					break;
				case Or:
					// true || <anything> => true
					if (leftSide.equals(true))
						return true;
					break;
				default:
					break;
				}
				if (rightSide != null && rightSide != ASTNode.EVALUATION_COMPLEX)
					return evaluateOn(leftSide, rightSide);
			}
		}
		catch (final ClassCastException e) {}
		catch (final NullPointerException e) {}
		return super.evaluateStatic(context);
	}

	private Object evaluateOn(Object leftSide, Object rightSide) {
		rightSide = value(rightSide);
		switch (operator()) {
		case Assign: {
        	final IVariable lv = (IVariable)leftSide;
        	lv.set(value(rightSide));
        	return lv.get();
		}
		case AssignAdd: {
			final IVariable lv = (IVariable)leftSide;
			lv.set(((Number)lv.get()).doubleValue() + ((Number)rightSide).doubleValue());
			return lv.get();
		}
		case AssignSubtract: {
			final IVariable lv = (IVariable)leftSide;
			lv.set(((Number)lv.get()).doubleValue() - ((Number)rightSide).doubleValue());
			return lv.get();
		}
		case AssignDivide: {
			final IVariable lv = (IVariable)leftSide;
			lv.set(((Number)lv.get()).doubleValue() / ((Number)rightSide).doubleValue());
			return lv.get();
		}
		case AssignMultiply: {
			final IVariable lv = (IVariable)leftSide;
			lv.set(((Number)lv.get()).doubleValue() * ((Number)rightSide).doubleValue());
			return lv.get();
		}
		case AssignModulo: {
			final IVariable lv = (IVariable)leftSide;
			lv.set(((Number)lv.get()).doubleValue() % ((Number)rightSide).doubleValue());
			return lv.get();
		}
		case AssignAnd: {
			final IVariable lv = (IVariable)leftSide;
			lv.set(eq(Boolean.TRUE, lv.get()) && eq(Boolean.TRUE, rightSide));
			return lv.get();
		}
		case AssignOr: {
			final IVariable lv = (IVariable)leftSide;
			lv.set(eq(Boolean.TRUE, lv.get()) && eq(Boolean.TRUE, rightSide));
			return lv.get();
		}
		case AssignXOr: {
			final IVariable lv = (IVariable)leftSide;
			lv.set(((Number)lv.get()).longValue() & ((Number)rightSide).longValue());
			return lv.get();
		}
        default:
        	break;
		}
		leftSide = value(leftSide);
        switch (operator()) {
        case Add:
        	if (leftSide instanceof String || rightSide instanceof String)
        		return leftSide.toString() + rightSide.toString();
        	else
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
        	return eq(leftSide, rightSide);
        case NotEqual:
        	return !eq(leftSide, rightSide);
        case And:
        	return eq(leftSide, Boolean.TRUE) && eq(rightSide, Boolean.TRUE);
        case Or:
        	return eq(leftSide, Boolean.TRUE) || eq(rightSide, Boolean.TRUE);
        default:
        	return null;
        }
    }

	@Override
	public Object evaluate(IEvaluationContext context) throws ControlFlowException {
		switch (operator()) {
		case Or:
			if (eq(Boolean.TRUE, value(leftSide().evaluate(context))))
				return true;
			if (eq(Boolean.TRUE, value(rightSide().evaluate(context))))
				return true;
			return false;
		case And:
			if (!eq(Boolean.TRUE, value(leftSide().evaluate(context))))
				return false;
			if (!eq(Boolean.TRUE, value(rightSide().evaluate(context))))
				return false;
			return true;
		default:
			return evaluateOn(leftSide().evaluate(context), rightSide().evaluate(context));
		}
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