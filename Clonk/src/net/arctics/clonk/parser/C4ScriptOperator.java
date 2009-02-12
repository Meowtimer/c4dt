package net.arctics.clonk.parser;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author madeen
 * an operator
 */
public enum C4ScriptOperator {
	Not(C4Type.UNKNOWN, null, C4Type.BOOL, "!", 15, "Not"),
	BitNot(C4Type.INT, C4Type.INT, C4Type.INT, "~", 15),
	Power(C4Type.INT, C4Type.INT, C4Type.INT, "**", 14),
	Divide(C4Type.INT, C4Type.INT, C4Type.INT, "/", 13, "Div"),
	Multiply(C4Type.INT, C4Type.INT, C4Type.INT, "*", 13, "Mul"),
	Modulo(C4Type.INT, C4Type.INT, C4Type.INT, "%", 13),
	Subtract(C4Type.INT, C4Type.INT, C4Type.INT, "-", 12, "Sub"),
	Add(C4Type.INT, C4Type.INT, C4Type.INT, "+", 12, "Sum"),
	Smaller(C4Type.INT, C4Type.INT, C4Type.BOOL, "<", 10, "LessThan"),
	SmallerEqual(C4Type.INT, C4Type.INT, C4Type.BOOL, "<=", 10),
	Larger(C4Type.INT, C4Type.INT, C4Type.BOOL, ">", 10, "GreaterThan"),
	LargerEqual(C4Type.INT, C4Type.INT, C4Type.BOOL, ">=", 10),
	Equal(C4Type.UNKNOWN, C4Type.UNKNOWN, C4Type.BOOL, "==", 9, "Equal"),
	NotEqual(C4Type.UNKNOWN, C4Type.UNKNOWN, C4Type.BOOL, "!=", 9),
	StringEqual(C4Type.STRING, C4Type.STRING, C4Type.BOOL, "S=", 9, "SEqual"),
	eq(C4Type.STRING, C4Type.STRING, C4Type.BOOL, "eq", 9),
	ne(C4Type.STRING, C4Type.STRING, C4Type.BOOL, "ne", 9),
	And(C4Type.UNKNOWN, C4Type.UNKNOWN, C4Type.BOOL, "&&", 5, "And"),
	Or(C4Type.UNKNOWN, C4Type.UNKNOWN, C4Type.BOOL, "||", 4, "Or"),
	BitAnd(C4Type.INT, C4Type.INT, C4Type.INT, "&", 8, "BitAnd"),
	BitXOr(C4Type.INT, C4Type.INT, C4Type.INT, "^", 6),
	BitOr(C4Type.INT, C4Type.INT, C4Type.INT, "|", 6),
	Decrement(C4Type.INT, null, C4Type.INT, "--", 15, "Dec"),
	Increment(C4Type.INT, null, C4Type.INT, "++", 15, "Inc"),
	ShiftLeft(C4Type.INT, C4Type.INT, C4Type.INT, "<<", 11),
	ShiftRight(C4Type.INT, C4Type.INT, C4Type.INT, ">>", 11),
	Assign(C4Type.UNKNOWN, C4Type.UNKNOWN, C4Type.UNKNOWN, "=", 2),
	AssignAdd(C4Type.INT, C4Type.INT, C4Type.INT, "+=", 2),
	AssignSubtract(C4Type.INT, C4Type.INT, C4Type.INT, "-=", 2),
	AssignMultiply(C4Type.INT, C4Type.INT, C4Type.INT, "*=", 2),
	AssignDivide(C4Type.INT, C4Type.INT, C4Type.INT, "/=", 2),
	AssignModulo(C4Type.INT, C4Type.INT, C4Type.INT, "%=", 2),
	AssignOr(C4Type.BOOL, C4Type.BOOL, C4Type.BOOL, "|=", 2),
	AssignAnd(C4Type.BOOL, C4Type.BOOL, C4Type.BOOL, "&=", 2),
	AssignXOr(C4Type.INT, C4Type.INT, C4Type.INT, "^=", 2);
	
	// enums can have fields! cool
	C4Type firstArgType, secondArgType, resultType;
	String operatorName, oldStyleFunctionEquivalent;
	int priority;
	boolean rightAssociative;
	
	public static final Map<String, C4ScriptOperator> stringToOperatorMap;
	
	static {
		HashMap<String, C4ScriptOperator> workInProgress = new HashMap<String, C4ScriptOperator>();
		for (C4ScriptOperator op : values()) {
			workInProgress.put(op.getOperatorName(), op);
		}
		stringToOperatorMap = Collections.unmodifiableMap(workInProgress);
	}
	
	public static C4ScriptOperator getOperator(String opName) {
		return stringToOperatorMap.get(opName);
	}
	
	private C4ScriptOperator(C4Type firstArgType, C4Type secondArgType,
			C4Type resultType, String operatorName, int priority,
			String oldStyleFunctionEquivalent) {
		this.firstArgType = firstArgType;
		this.secondArgType = secondArgType;
		this.resultType = resultType;
		this.operatorName = operatorName;
		this.oldStyleFunctionEquivalent = oldStyleFunctionEquivalent;
		this.priority = priority;
		this.rightAssociative = name().startsWith("Assign");
	}
	
	private C4ScriptOperator(C4Type firstArgType, C4Type secondArgType,
			C4Type resultType, String operatorName, int priority) {
		this(firstArgType, secondArgType, resultType, operatorName, priority, null);
	}
	
//	private static String typeInCode(C4Type type) {
//		if (type == null)
//			return "C4Type.UNKNOWN";
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
	
	public C4Type getFirstArgType() {
		return firstArgType;
	}
	
	public C4Type getSecondArgType() {
		return secondArgType;
	}
	
	public C4Type getResultType() {
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
		return this == Increment || this == Decrement || this.name().startsWith("Assign");
	}
	
	public String getOldStyleFunctionEquivalent() {
		return oldStyleFunctionEquivalent;
	}
	
	public static C4ScriptOperator oldStyleFunctionReplacement(String funcName) {
		for (C4ScriptOperator o : values()) {
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
	
	public boolean spaceNeededBetweenMeAnd(C4ScriptOperator other) {
		return
			((this == Add || this == Increment) && (other == Add || other == Increment)) ||
			((this == Subtract || this == Decrement) && (other == Subtract || other == Decrement));
	}

}