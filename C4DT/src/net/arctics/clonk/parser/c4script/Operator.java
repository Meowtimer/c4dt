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
	Multiply(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, "*", 13, "Mul"), //$NON-NLS-1$ //$NON-NLS-2$
	Modulo(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, "%", 13), //$NON-NLS-1$
	Subtract(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, "-", 12, "Sub"), //$NON-NLS-1$ //$NON-NLS-2$
	Add(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, "+", 12, "Sum"), //$NON-NLS-1$ //$NON-NLS-2$
	Smaller(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.BOOL, "<", 10, "LessThan"), //$NON-NLS-1$ //$NON-NLS-2$
	SmallerEqual(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.BOOL, "<=", 10), //$NON-NLS-1$
	Larger(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.BOOL, ">", 10, "GreaterThan"), //$NON-NLS-1$ //$NON-NLS-2$
	LargerEqual(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.BOOL, ">=", 10), //$NON-NLS-1$
	Equal(PrimitiveType.ANY, PrimitiveType.ANY, PrimitiveType.BOOL, "==", 9, "Equal"), //$NON-NLS-1$ //$NON-NLS-2$
	NotEqual(PrimitiveType.ANY, PrimitiveType.ANY, PrimitiveType.BOOL, "!=", 9), //$NON-NLS-1$
	StringEqual(PrimitiveType.STRING, PrimitiveType.STRING, PrimitiveType.BOOL, "S=", 9, "SEqual"), //$NON-NLS-1$ //$NON-NLS-2$
	eq(PrimitiveType.STRING, PrimitiveType.STRING, PrimitiveType.BOOL, "eq", 9), //$NON-NLS-1$
	ne(PrimitiveType.STRING, PrimitiveType.STRING, PrimitiveType.BOOL, "ne", 9), //$NON-NLS-1$
	And(PrimitiveType.ANY, PrimitiveType.ANY, PrimitiveType.BOOL, "&&", 5, "And"), //$NON-NLS-1$ //$NON-NLS-2$
	Or(PrimitiveType.ANY, PrimitiveType.ANY, PrimitiveType.BOOL, "||", 4, "Or"), //$NON-NLS-1$ //$NON-NLS-2$
	BitAnd(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, "&", 8, "BitAnd"), //$NON-NLS-1$ //$NON-NLS-2$
	BitXOr(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, "^", 6), //$NON-NLS-1$
	BitOr(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, "|", 6), //$NON-NLS-1$
	Decrement(PrimitiveType.INT, null, PrimitiveType.INT, "--", 15, "Dec", true), //$NON-NLS-1$ //$NON-NLS-2$
	Increment(PrimitiveType.INT, null, PrimitiveType.INT, "++", 15, "Inc", true), //$NON-NLS-1$ //$NON-NLS-2$
	ShiftLeft(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, "<<", 11), //$NON-NLS-1$
	ShiftRight(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, ">>", 11), //$NON-NLS-1$
	Assign(PrimitiveType.ANY, PrimitiveType.ANY, PrimitiveType.ANY, "=", 2, null, true), //$NON-NLS-1$
	AssignAdd(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, "+=", 2, null, true), //$NON-NLS-1$
	AssignSubtract(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, "-=", 2, null, true), //$NON-NLS-1$
	AssignMultiply(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, "*=", 2, null, true), //$NON-NLS-1$
	AssignDivide(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, "/=", 2, null, true), //$NON-NLS-1$
	AssignModulo(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, "%=", 2, null, true), //$NON-NLS-1$
	AssignOr(PrimitiveType.BOOL, PrimitiveType.BOOL, PrimitiveType.BOOL, "|=", 2, null, true), //$NON-NLS-1$
	AssignAnd(PrimitiveType.BOOL, PrimitiveType.BOOL, PrimitiveType.BOOL, "&=", 2, null, true), //$NON-NLS-1$
	AssignXOr(PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, "^=", 2, null, true); //$NON-NLS-1$
	
	PrimitiveType firstArgType, secondArgType, resultType;
	String operatorName, oldStyleFunctionEquivalent;
	int priority;
	boolean rightAssociative;
	boolean returnsRef;
	
	public static final Map<String, Operator> stringToOperatorMap;
	
	static {
		HashMap<String, Operator> workInProgress = new HashMap<String, Operator>();
		for (Operator op : values()) {
			workInProgress.put(op.getOperatorName(), op);
		}
		stringToOperatorMap = Collections.unmodifiableMap(workInProgress);
	}
	
	public static Operator getOperator(String opName) {
		return stringToOperatorMap.get(opName);
	}
	
	private Operator(PrimitiveType firstArgType, PrimitiveType secondArgType,
			PrimitiveType resultType, String operatorName, int priority,
			String oldStyleFunctionEquivalent) {
		this.firstArgType = firstArgType;
		this.secondArgType = secondArgType;
		this.resultType = resultType;
		this.operatorName = operatorName;
		this.oldStyleFunctionEquivalent = oldStyleFunctionEquivalent;
		this.priority = priority;
		this.rightAssociative = name().startsWith("Assign"); //$NON-NLS-1$
	}
	
	private Operator(PrimitiveType firstArgType, PrimitiveType secondArgType,
			PrimitiveType resultType, String operatorName, int priority,
			String oldStyleFunctionEquivalent, boolean returnsRef) {
		this(firstArgType, secondArgType, resultType, operatorName, priority, oldStyleFunctionEquivalent);
		this.returnsRef = returnsRef;
	}
	
	private Operator(PrimitiveType firstArgType, PrimitiveType secondArgType,
			PrimitiveType resultType, String operatorName, int priority) {
		this(firstArgType, secondArgType, resultType, operatorName, priority, null);
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

	public String getOperatorName() {
		return operatorName;
	}
	
	public PrimitiveType getFirstArgType() {
		return firstArgType;
	}
	
	public PrimitiveType getSecondArgType() {
		return secondArgType;
	}
	
	public PrimitiveType getResultType() {
		return resultType;
	}
	
	public int getNumArgs() {
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
	
	public String getOldStyleFunctionEquivalent() {
		return oldStyleFunctionEquivalent;
	}
	
	public static Operator oldStyleFunctionReplacement(String funcName) {
		for (Operator o : values()) {
			if (o.getOldStyleFunctionEquivalent() != null && o.getOldStyleFunctionEquivalent().equals(funcName))
				return o;
		}
		return null;
	}
	
	public int getPriority() {
		return priority;
	}

	public boolean isRightAssociative() {
		return rightAssociative;
	}
	
	public boolean spaceNeededBetweenMeAnd(Operator other) {
		return
			((this == Add || this == Increment) && (other == Add || other == Increment)) ||
			((this == Subtract || this == Decrement) && (other == Subtract || other == Decrement));
	}
	
	public boolean returnsRef() {
		return returnsRef;
	}
	
	public static String[] arrayOfOperatorNames() {
		String[] result = new String[values().length];
		for (Operator o : values()) {
			result[o.ordinal()] = o.getOperatorName();
		}
		return result;
	}

}