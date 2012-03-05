package net.arctics.clonk.parser.c4script;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * 
 * @author madeen
 * an operator
 */
public enum Operator {
	Not(PrimitiveType.ANY, null, PrimitiveType.BOOL, "!", 15, "Not"), //$NON-NLS-1$ //$NON-NLS-2$
	BitNot(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, "~", 15), //$NON-NLS-1$
	Power(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, "**", 14), //$NON-NLS-1$
	Divide(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, "/", 13, "Div"), //$NON-NLS-1$ //$NON-NLS-2$
	Multiply(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, "*", 13, "Mul", 4), //$NON-NLS-1$ //$NON-NLS-2$
	Modulo(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, "%", 13), //$NON-NLS-1$
	Subtract(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, "-", 12, "Sub"), //$NON-NLS-1$ //$NON-NLS-2$
	Add(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, "+", 12, "Sum", 4), //$NON-NLS-1$ //$NON-NLS-2$
	Smaller(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.BOOL, "<", 10, "LessThan"), //$NON-NLS-1$ //$NON-NLS-2$
	SmallerEqual(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.BOOL, "<=", 10), //$NON-NLS-1$
	Larger(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.BOOL, ">", 10, "GreaterThan"), //$NON-NLS-1$ //$NON-NLS-2$
	LargerEqual(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.BOOL, ">=", 10), //$NON-NLS-1$
	Equal(PrimitiveType.ANY, PrimitiveType.ANY, PrimitiveType.BOOL, "==", 9, "Equal", 4), //$NON-NLS-1$ //$NON-NLS-2$
	NotEqual(PrimitiveType.ANY, PrimitiveType.ANY, PrimitiveType.BOOL, "!=", 9), //$NON-NLS-1$
	StringEqual(PrimitiveType.STRING, PrimitiveType.STRING, PrimitiveType.BOOL, "S=", 9, "SEqual"), //$NON-NLS-1$ //$NON-NLS-2$
	eq(PrimitiveType.STRING, PrimitiveType.STRING, PrimitiveType.BOOL, "eq", 9), //$NON-NLS-1$
	ne(PrimitiveType.STRING, PrimitiveType.STRING, PrimitiveType.BOOL, "ne", 9), //$NON-NLS-1$
	And(PrimitiveType.ANY, PrimitiveType.ANY, PrimitiveType.BOOL, "&&", 5, "And"), //$NON-NLS-1$ //$NON-NLS-2$
	Or(PrimitiveType.ANY, PrimitiveType.ANY, PrimitiveType.BOOL, "||", 4, "Or"), //$NON-NLS-1$ //$NON-NLS-2$
	BitAnd(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, "&", 8, "BitAnd"), //$NON-NLS-1$ //$NON-NLS-2$
	BitXOr(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, "^", 6), //$NON-NLS-1$
	BitOr(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, "|", 6), //$NON-NLS-1$
	// can't use the flags constants here >:oo ? rage
	Decrement(PrimitiveType.INT, null, PrimitiveType.INT, "--", 15, "Dec", 1/*RETURNS_REF*/), //$NON-NLS-1$ //$NON-NLS-2$
	Increment(PrimitiveType.INT, null, PrimitiveType.INT, "++", 15, "Inc", 1/*RETURNS_REF*/), //$NON-NLS-1$ //$NON-NLS-2$
	ShiftLeft(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, "<<", 11), //$NON-NLS-1$
	ShiftRight(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, ">>", 11), //$NON-NLS-1$
	Assign(PrimitiveType.ANY, PrimitiveType.ANY, PrimitiveType.ANY, "=", 2, null, 1/*RETURNS_REF*/), //$NON-NLS-1$
	AssignAdd(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, "+=", 2, null, 1/*RETURNS_REF*/), //$NON-NLS-1$
	AssignSubtract(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, "-=", 2, null, 1/*RETURNS_REF*/), //$NON-NLS-1$
	AssignMultiply(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, "*=", 2, null, 1/*RETURNS_REF*/), //$NON-NLS-1$
	AssignDivide(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, "/=", 2, null, 1/*RETURNS_REF*/), //$NON-NLS-1$
	AssignModulo(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, "%=", 2, null, 1/*RETURNS_REF*/), //$NON-NLS-1$
	AssignOr(PrimitiveType.BOOL, PrimitiveType.BOOL, PrimitiveType.BOOL, "|=", 2, null, 1/*RETURNS_REF*/), //$NON-NLS-1$
	AssignAnd(PrimitiveType.BOOL, PrimitiveType.BOOL, PrimitiveType.BOOL, "&=", 2, null, 1/*RETURNS_REF*/), //$NON-NLS-1$
	AssignXOr(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, "^=", 2, null, 1/*RETURNS_REF*/); //$NON-NLS-1$
	
	public static final int RETURNS_REF = 1;
	public static final int RIGHTASSOCIATIVE = 2;
	public static final int ASSOCIATIVE_OP = 4;
	
	PrimitiveType firstArgType, secondArgType, resultType;
	String operatorName, oldStyleFunctionEquivalent;
	int priority;
	int flags;
	
	public static final Map<String, Operator> stringToOperatorMap;
	
	static {
		HashMap<String, Operator> workInProgress = new HashMap<String, Operator>();
		for (Operator op : values()) {
			workInProgress.put(op.operatorName(), op);
		}
		stringToOperatorMap = Collections.unmodifiableMap(workInProgress);
	}
	
	public static Operator getOperator(String opName) {
		return stringToOperatorMap.get(opName);
	}
	
	private Operator(PrimitiveType firstArgType, PrimitiveType secondArgType,
			PrimitiveType resultType, String operatorName, int priority,
			String oldStyleFunctionEquivalent, int flags) {
		
		if (name().startsWith("Assign")) //$NON-NLS-1$
			flags |= RIGHTASSOCIATIVE;
		
		this.firstArgType = firstArgType;
		this.secondArgType = secondArgType;
		this.resultType = resultType;
		this.operatorName = operatorName;
		this.oldStyleFunctionEquivalent = oldStyleFunctionEquivalent;
		this.priority = priority;	
		this.flags = flags;
	}
	
	private Operator(PrimitiveType firstArgType, PrimitiveType secondArgType,
			PrimitiveType resultType, String operatorName, int priority, String oldStyleFunctionEquivalent) {
		this(firstArgType, secondArgType, resultType, operatorName, priority, oldStyleFunctionEquivalent, 0);
	}
	
	private Operator(PrimitiveType firstArgType, PrimitiveType secondArgType,
			PrimitiveType resultType, String operatorName, int priority) {
		this(firstArgType, secondArgType, resultType, operatorName, priority, null, 0);
	}
	
//	private static String typeInCode(C4Type type) {
//		if (type == null)
//			return "C4Type.ANY";
//		return "C4Type."+type.name();
//	}
//	
//	private static void printConstructedFields() {
//		boolean first = true;
//		for (C4ScriptOperator op : C4ScriptOperator.values()) {
//			if (first)
//				first = false;
//			else {
//				System.out.print(",\n");
//			}
//			System.out.print(
//					op.name() + "("+typeInCode(op.getFirstArgType())+
//					", "+typeInCode(op.getSecondArgType())+", "+typeInCode(op.getResultType())+", \""+op.getOperatorName()+"\", "+op.priority()+
//					(op.getOldStyleFunctionEquivalent()!=null?", \""+op.getOldStyleFunctionEquivalent()+"\"":"")+")"
//			);
//		}
//		System.out.print(";");
//	}

	public String operatorName() {
		return operatorName;
	}
	
	public PrimitiveType firstArgType() {
		return firstArgType;
	}
	
	public PrimitiveType secondArgType() {
		return secondArgType;
	}
	
	public PrimitiveType resultType() {
		return resultType;
	}
	
	public int numArgs() {
		return secondArgType != null ? 2 : 1;
	}

	public boolean isUnary() {
		return this == Not || this == Increment || this == Decrement || this == Add || this == Subtract;
	}

	public boolean isBinary() {
		return !isUnary() || this == Add || this == Subtract; // :D
	}

	public boolean isPostfix() {
		return this == Increment || this == Decrement;
	}
	
	public boolean isPrefix() {
		return this == Increment || this == Decrement || this == Not || this == Add || this == Subtract || this == BitNot;
	}
	
	public boolean modifiesArgument() {
		return this == Increment || this == Decrement || this.name().startsWith("Assign"); //$NON-NLS-1$
	}
	
	public String oldStyleFunctionEquivalent() {
		return oldStyleFunctionEquivalent;
	}
	
	public static Operator oldStyleFunctionReplacement(String funcName) {
		for (Operator o : values()) {
			if (o.oldStyleFunctionEquivalent() != null && o.oldStyleFunctionEquivalent().equals(funcName))
				return o;
		}
		return null;
	}
	
	public int priority() {
		return priority;
	}

	public boolean isRightAssociative() {
		return (flags & RIGHTASSOCIATIVE) != 0;
	}
	
	public boolean spaceNeededBetweenMeAnd(Operator other) {
		return
			((this == Add || this == Increment) && (other == Add || other == Increment)) ||
			((this == Subtract || this == Decrement) && (other == Subtract || other == Decrement));
	}
	
	public boolean returnsRef() {
		return (flags & RETURNS_REF) != 0;
	}
	
	public boolean isAssociative() {
		return (flags & ASSOCIATIVE_OP) != 0;
	}
	
	public static String[] arrayOfOperatorNames() {
		String[] result = new String[values().length];
		for (Operator o : values()) {
			result[o.ordinal()] = o.operatorName();
		}
		return result;
	}

}