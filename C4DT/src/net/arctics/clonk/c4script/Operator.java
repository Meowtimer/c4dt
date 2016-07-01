package net.arctics.clonk.c4script;

import static java.util.Arrays.stream;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.arctics.clonk.c4script.typing.PrimitiveType;

/**
 *
 * @author madeen
 * an operator
 */
public enum Operator {
	Not(PrimitiveType.UNKNOWN, null, PrimitiveType.BOOL, "!", 15, "Not"), //$NON-NLS-1$ //$NON-NLS-2$
	BitNot(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, "~", 15), //$NON-NLS-1$
	Power(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, "**", 14), //$NON-NLS-1$
	Divide(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, "/", 13, "Div"), //$NON-NLS-1$ //$NON-NLS-2$
	Multiply(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, "*", 13, "Mul", 4), //$NON-NLS-1$ //$NON-NLS-2$
	Modulo(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, "%", 13, "Mod"), //$NON-NLS-1$
	Subtract(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, "-", 12, "Sub"), //$NON-NLS-1$ //$NON-NLS-2$
	Add(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, "+", 12, "Sum", 4), //$NON-NLS-1$ //$NON-NLS-2$
	Smaller(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.BOOL, "<", 10, "LessThan"), //$NON-NLS-1$ //$NON-NLS-2$
	SmallerEqual(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.BOOL, "<=", 10), //$NON-NLS-1$
	Larger(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.BOOL, ">", 10, "GreaterThan"), //$NON-NLS-1$ //$NON-NLS-2$
	LargerEqual(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.BOOL, ">=", 10), //$NON-NLS-1$
	Equal(PrimitiveType.UNKNOWN, PrimitiveType.UNKNOWN, PrimitiveType.BOOL, "==", 9, "Equal", 4), //$NON-NLS-1$ //$NON-NLS-2$
	NotEqual(PrimitiveType.UNKNOWN, PrimitiveType.UNKNOWN, PrimitiveType.BOOL, "!=", 9), //$NON-NLS-1$
	StringEqual(PrimitiveType.STRING, PrimitiveType.STRING, PrimitiveType.BOOL, "S=", 9, "SEqual"), //$NON-NLS-1$ //$NON-NLS-2$
	eq(PrimitiveType.STRING, PrimitiveType.STRING, PrimitiveType.BOOL, "eq", 9), //$NON-NLS-1$
	ne(PrimitiveType.STRING, PrimitiveType.STRING, PrimitiveType.BOOL, "ne", 9), //$NON-NLS-1$
	And(PrimitiveType.UNKNOWN, PrimitiveType.UNKNOWN, PrimitiveType.BOOL, "&&", 5, "And"), //$NON-NLS-1$ //$NON-NLS-2$
	Or(PrimitiveType.UNKNOWN, PrimitiveType.UNKNOWN, PrimitiveType.BOOL, "||", 4, "Or"), //$NON-NLS-1$ //$NON-NLS-2$
	BitAnd(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, "&", 8, "BitAnd"), //$NON-NLS-1$ //$NON-NLS-2$
	BitXOr(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, "^", 6), //$NON-NLS-1$
	BitOr(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, "|", 6), //$NON-NLS-1$
	// can't use the flags constants here >:oo ? rage
	Decrement(PrimitiveType.INT, null, PrimitiveType.INT, "--", 15, "Dec", 1/*RETURNS_REF*/), //$NON-NLS-1$ //$NON-NLS-2$
	Increment(PrimitiveType.INT, null, PrimitiveType.INT, "++", 15, "Inc", 1/*RETURNS_REF*/), //$NON-NLS-1$ //$NON-NLS-2$
	ShiftLeft(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, "<<", 11), //$NON-NLS-1$
	ShiftRight(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, ">>", 11), //$NON-NLS-1$
	JumpNotNil(PrimitiveType.UNKNOWN, PrimitiveType.UNKNOWN, PrimitiveType.UNKNOWN, "??", 3, null),
	Assign(PrimitiveType.UNKNOWN, PrimitiveType.UNKNOWN, PrimitiveType.UNKNOWN, "=", 2, null, 1/*RETURNS_REF*/), //$NON-NLS-1$
	AssignAdd(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, "+=", 2, null, 1/*RETURNS_REF*/), //$NON-NLS-1$
	AssignSubtract(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, "-=", 2, null, 1/*RETURNS_REF*/), //$NON-NLS-1$
	AssignMultiply(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, "*=", 2, null, 1/*RETURNS_REF*/), //$NON-NLS-1$
	AssignDivide(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, "/=", 2, null, 1/*RETURNS_REF*/), //$NON-NLS-1$
	AssignModulo(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, "%=", 2, null, 1/*RETURNS_REF*/), //$NON-NLS-1$
	AssignOr(PrimitiveType.BOOL, PrimitiveType.BOOL, PrimitiveType.BOOL, "|=", 2, null, 1/*RETURNS_REF*/), //$NON-NLS-1$
	AssignAnd(PrimitiveType.BOOL, PrimitiveType.BOOL, PrimitiveType.BOOL, "&=", 2, null, 1/*RETURNS_REF*/), //$NON-NLS-1$
	AssignXOr(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, "^=", 2, null, 1/*RETURNS_REF*/), //$NON-NLS-1$
	Transform(PrimitiveType.UNKNOWN, PrimitiveType.UNKNOWN, PrimitiveType.UNKNOWN, "=>", 2, null),
	TransformExhaustive(PrimitiveType.UNKNOWN, PrimitiveType.UNKNOWN, PrimitiveType.UNKNOWN, "==>", 2, null),
	Identical(PrimitiveType.ANY, PrimitiveType.ANY, PrimitiveType.BOOL, "!==", 9),
	NotIdentical(PrimitiveType.ANY, PrimitiveType.ANY, PrimitiveType.BOOL, "===", 9);

	public static final int RETURNS_REF = 1;

	public static final int RIGHTASSOCIATIVE = 2;

	public static final int ASSOCIATIVE_OP = 4;

	final PrimitiveType firstArgType, secondArgType, resultType;
	final String operatorName, oldStyleFunctionEquivalent;
	final int priority;
	final int flags;

	public static final Map<String, Operator> stringToOperatorMap;

	static {
		final HashMap<String, Operator> workInProgress = new HashMap<String, Operator>();
		for (final Operator op : values()) {
			workInProgress.put(op.operatorName(), op);
		}
		stringToOperatorMap = Collections.unmodifiableMap(workInProgress);
	}

	public static Operator get(final String opName) { return stringToOperatorMap.get(opName); }

	private Operator(final PrimitiveType firstArgType, final PrimitiveType secondArgType,
			final PrimitiveType resultType, final String operatorName, final int priority,
			final String oldStyleFunctionEquivalent, int flags) {

		if (name().startsWith("Assign")) {
			flags |= RIGHTASSOCIATIVE;
		}

		this.firstArgType = firstArgType;
		this.secondArgType = secondArgType;
		this.resultType = resultType;
		this.operatorName = operatorName;
		this.oldStyleFunctionEquivalent = oldStyleFunctionEquivalent;
		this.priority = priority;
		this.flags = flags;
	}

	private Operator(final PrimitiveType firstArgType, final PrimitiveType secondArgType,
			final PrimitiveType resultType, final String operatorName, final int priority, final String oldStyleFunctionEquivalent) {
		this(firstArgType, secondArgType, resultType, operatorName, priority, oldStyleFunctionEquivalent, 0);
	}

	private Operator(final PrimitiveType firstArgType, final PrimitiveType secondArgType,
			final PrimitiveType resultType, final String operatorName, final int priority) {
		this(firstArgType, secondArgType, resultType, operatorName, priority, null, 0);
	}

	public String operatorName() { return operatorName; }

	public PrimitiveType firstArgType() { return firstArgType; }

	public PrimitiveType secondArgType() { return secondArgType; }

	public PrimitiveType returnType() { return resultType; }

	public int numArgs() { return secondArgType != null ? 2 : 1; }

	public boolean isUnary() {
		switch (this) {
		case Not: case Increment: case Decrement: case Add: case Subtract:
			return true;
		default:
			return false;
		}
	}

	public boolean isBinary() { return !isUnary() || this == Add || this == Subtract; }

	public boolean isPostfix() { return this == Increment || this == Decrement; }

	public boolean isPrefix() {
		switch (this) {
		case Increment: case Decrement: case Not: case Add: case Subtract: case BitNot:
			return true;
		default:
			return false;
		}
	}

	public boolean modifiesArgument() { return this == Increment || this == Decrement || isAssignment(); }

	public String oldStyleFunctionEquivalent() { return oldStyleFunctionEquivalent; }

	public boolean isAssignment() {
		switch (this) {
		case Assign: case AssignAdd: case AssignSubtract: case AssignMultiply: case AssignDivide:
		case AssignAnd: case AssignOr: case AssignModulo: case AssignXOr:
			return true;
		default:
			return false;
		}
	}

	public static Operator oldStyleFunctionReplacement(final String funcName) {
		for (final Operator o : values()) {
			if (o.oldStyleFunctionEquivalent() != null && o.oldStyleFunctionEquivalent().equals(funcName)) {
				return o;
			}
		}
		return null;
	}

	public int priority() { return priority; }

	public boolean isRightAssociative() { return (flags & RIGHTASSOCIATIVE) != 0; }

	public boolean spaceNeededBetweenMeAnd(final Operator other) {
		switch (this) {
		case Add: case Increment:
			return other == Add || other == Increment;
		case Subtract: case Decrement:
			return other == Subtract || other == Decrement;
		default:
			return false;
		}
	}

	public boolean returnsRef() { return (flags & RETURNS_REF) != 0; }

	public boolean isAssociative() { return (flags & ASSOCIATIVE_OP) != 0; }

	public static String[] operatorNames() {
		return stream(values()).map(Operator::operatorName).toArray(length -> new String[length]);
	}

	@Override
	public String toString() {
		return String.format("%s (%s)", super.toString(), operatorName());
	}

}