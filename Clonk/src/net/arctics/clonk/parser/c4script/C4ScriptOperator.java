package net.arctics.clonk.parser.c4script;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * 
 * @author madeen
 * an operator
 */
public enum C4ScriptOperator {
	Not(C4Type.ANY, null, C4Type.BOOL, "!", 15, "Not"), //$NON-NLS-1$ //$NON-NLS-2$
	BitNot(C4Type.INT, C4Type.INT, C4Type.INT, "~", 15), //$NON-NLS-1$
	Power(C4Type.INT, C4Type.INT, C4Type.INT, "**", 14), //$NON-NLS-1$
	Divide(C4Type.INT, C4Type.INT, C4Type.INT, "/", 13, "Div"), //$NON-NLS-1$ //$NON-NLS-2$
	Multiply(C4Type.INT, C4Type.INT, C4Type.INT, "*", 13, "Mul"), //$NON-NLS-1$ //$NON-NLS-2$
	Modulo(C4Type.INT, C4Type.INT, C4Type.INT, "%", 13), //$NON-NLS-1$
	Subtract(C4Type.INT, C4Type.INT, C4Type.INT, "-", 12, "Sub"), //$NON-NLS-1$ //$NON-NLS-2$
	Add(C4Type.INT, C4Type.INT, C4Type.INT, "+", 12, "Sum"), //$NON-NLS-1$ //$NON-NLS-2$
	Smaller(C4Type.INT, C4Type.INT, C4Type.BOOL, "<", 10, "LessThan"), //$NON-NLS-1$ //$NON-NLS-2$
	SmallerEqual(C4Type.INT, C4Type.INT, C4Type.BOOL, "<=", 10), //$NON-NLS-1$
	Larger(C4Type.INT, C4Type.INT, C4Type.BOOL, ">", 10, "GreaterThan"), //$NON-NLS-1$ //$NON-NLS-2$
	LargerEqual(C4Type.INT, C4Type.INT, C4Type.BOOL, ">=", 10), //$NON-NLS-1$
	Equal(C4Type.ANY, C4Type.ANY, C4Type.BOOL, "==", 9, "Equal"), //$NON-NLS-1$ //$NON-NLS-2$
	NotEqual(C4Type.ANY, C4Type.ANY, C4Type.BOOL, "!=", 9), //$NON-NLS-1$
	StringEqual(C4Type.STRING, C4Type.STRING, C4Type.BOOL, "S=", 9, "SEqual"), //$NON-NLS-1$ //$NON-NLS-2$
	eq(C4Type.STRING, C4Type.STRING, C4Type.BOOL, "eq", 9), //$NON-NLS-1$
	ne(C4Type.STRING, C4Type.STRING, C4Type.BOOL, "ne", 9), //$NON-NLS-1$
	And(C4Type.ANY, C4Type.ANY, C4Type.BOOL, "&&", 5, "And"), //$NON-NLS-1$ //$NON-NLS-2$
	Or(C4Type.ANY, C4Type.ANY, C4Type.BOOL, "||", 4, "Or"), //$NON-NLS-1$ //$NON-NLS-2$
	BitAnd(C4Type.INT, C4Type.INT, C4Type.INT, "&", 8, "BitAnd"), //$NON-NLS-1$ //$NON-NLS-2$
	BitXOr(C4Type.INT, C4Type.INT, C4Type.INT, "^", 6), //$NON-NLS-1$
	BitOr(C4Type.INT, C4Type.INT, C4Type.INT, "|", 6), //$NON-NLS-1$
	Decrement(C4Type.INT, null, C4Type.INT, "--", 15, "Dec", true), //$NON-NLS-1$ //$NON-NLS-2$
	Increment(C4Type.INT, null, C4Type.INT, "++", 15, "Inc", true), //$NON-NLS-1$ //$NON-NLS-2$
	ShiftLeft(C4Type.INT, C4Type.INT, C4Type.INT, "<<", 11), //$NON-NLS-1$
	ShiftRight(C4Type.INT, C4Type.INT, C4Type.INT, ">>", 11), //$NON-NLS-1$
	Assign(C4Type.ANY, C4Type.ANY, C4Type.ANY, "=", 2, null, true), //$NON-NLS-1$
	AssignAdd(C4Type.INT, C4Type.INT, C4Type.INT, "+=", 2, null, true), //$NON-NLS-1$
	AssignSubtract(C4Type.INT, C4Type.INT, C4Type.INT, "-=", 2, null, true), //$NON-NLS-1$
	AssignMultiply(C4Type.INT, C4Type.INT, C4Type.INT, "*=", 2, null, true), //$NON-NLS-1$
	AssignDivide(C4Type.INT, C4Type.INT, C4Type.INT, "/=", 2, null, true), //$NON-NLS-1$
	AssignModulo(C4Type.INT, C4Type.INT, C4Type.INT, "%=", 2, null, true), //$NON-NLS-1$
	AssignOr(C4Type.BOOL, C4Type.BOOL, C4Type.BOOL, "|=", 2, null, true), //$NON-NLS-1$
	AssignAnd(C4Type.BOOL, C4Type.BOOL, C4Type.BOOL, "&=", 2, null, true), //$NON-NLS-1$
	AssignXOr(C4Type.INT, C4Type.INT, C4Type.INT, "^=", 2, null, true); //$NON-NLS-1$
	
	C4Type firstArgType, secondArgType, resultType;
	String operatorName, oldStyleFunctionEquivalent;
	int priority;
	boolean rightAssociative;
	boolean returnsRef;
	
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
		this.rightAssociative = name().startsWith("Assign"); //$NON-NLS-1$
	}
	
	private C4ScriptOperator(C4Type firstArgType, C4Type secondArgType,
			C4Type resultType, String operatorName, int priority,
			String oldStyleFunctionEquivalent, boolean returnsRef) {
		this(firstArgType, secondArgType, resultType, operatorName, priority, oldStyleFunctionEquivalent);
		this.returnsRef = returnsRef;
	}
	
	private C4ScriptOperator(C4Type firstArgType, C4Type secondArgType,
			C4Type resultType, String operatorName, int priority) {
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
		return this == Increment || this == Decrement || this.name().startsWith("Assign"); //$NON-NLS-1$
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
	
	public boolean returnsRef() {
		return returnsRef;
	}
	
	public static String[] arrayOfOperatorNames() {
		String[] result = new String[values().length];
		for (C4ScriptOperator o : values()) {
			result[o.ordinal()] = o.getOperatorName();
		}
		return result;
	}

}