package net.arctics.clonk.parser;

import java.util.HashMap;

/**
 * 
 * @author madeen
 * an operator
 */
public enum C4ScriptOperator {
	Not,
	BitNot,

	Power,
	
	Divide,
	Multiply,
	Modulo,
	Subtract,
	Add,

	Smaller,
	SmallerEqual,
	Larger,
	LargerEqual,

	Equal,
	NotEqual,
	StringEqual,
	eq,
	ne,

	And,
	Or,
	BitAnd,
	BitXOr,
	BitOr,

	Decrement,
	Increment,

	ShiftLeft,
	ShiftRight,

	Assign,
	AssignAdd,
	AssignSubtract,
	AssignMultiply,
	AssignDivide,
	AssignModulo,
	AssignOr,
	AssignAnd,
	AssignXOr;
	
	// enums can have fields! cool
	C4Type firstArgType, secondArgType, resultType;
	String operatorName, oldStyleFunctionEquivalent;
	int priority;
	
	public static final HashMap<String, C4ScriptOperator> stringToOperatorMap;
	
	private void setOperatorName(String opName) {
		operatorName = opName;
	}
	
	public String getOperatorName() {
		return operatorName;
	}
	
	private void setArgTypesAndResult(C4Type f, C4Type s, C4Type r) {
		firstArgType  = f;
		secondArgType = s;
		resultType    = r;
	}
	
	private void setArgTypeAndResult(C4Type f, C4Type r) {
		firstArgType = f;
		resultType   = r;
	}
	
	public C4Type getFirstArgType() {
		return firstArgType;
	}
	
	public C4Type getSecondArgType() {
		return secondArgType;
	}
	
	public C4Type getResultType() {
		if (resultType == null)
			resultType = C4Type.ANY;
		return resultType;
	}
	
	public int getNumArgs() {
		return secondArgType != null ? 2 : 1;
	}
	
	// bla bla
	private static final C4ScriptOperator[] ops(C4ScriptOperator... args) {
		return args;
	}
	
	static {
		Not.setOperatorName("!");
		BitNot.setOperatorName("~");
		Power.setOperatorName("**");
		Divide.setOperatorName("/");
		Multiply.setOperatorName("*");
		Modulo.setOperatorName("%");
		Subtract.setOperatorName("-");
		Add.setOperatorName("+");
		Smaller.setOperatorName("<");
		SmallerEqual.setOperatorName("<=");
		Larger.setOperatorName(">");
		LargerEqual.setOperatorName(">=");
		Equal.setOperatorName("==");
		NotEqual.setOperatorName("!=");
		StringEqual.setOperatorName("S=");
		eq.setOperatorName("eq");
		ne.setOperatorName("ne");
		And.setOperatorName("&&");
		Or.setOperatorName("||");
		BitAnd.setOperatorName("&");
		BitXOr.setOperatorName("^");
		BitOr.setOperatorName("|");
		Decrement.setOperatorName("--");
		Increment.setOperatorName("++");
		ShiftLeft.setOperatorName("<<");
		ShiftRight.setOperatorName(">>");
		Assign.setOperatorName("=");
		AssignAdd.setOperatorName("+=");
		AssignSubtract.setOperatorName("-=");
		AssignMultiply.setOperatorName("*=");
		AssignDivide.setOperatorName("/=");
		AssignModulo.setOperatorName("%=");
		AssignOr.setOperatorName("|=");
		AssignAnd.setOperatorName("&=");
		AssignXOr.setOperatorName("^=");
		stringToOperatorMap = new HashMap<String, C4ScriptOperator>(C4ScriptOperator.values().length);
		for (C4ScriptOperator o : values()) {
			stringToOperatorMap.put(o.getOperatorName(), o);
		}
		
		for (C4ScriptOperator o : ops(Not, BitNot, Increment, Decrement)) {
			o.setPriority(15);
		}
		// not argtype bool because that makes the parser think a variable is a bool when in fact it's an object or something different
		// that is being tested for existence
		Not.setArgTypeAndResult(C4Type.ANY, C4Type.BOOL);
		BitNot.setArgTypesAndResult(C4Type.INT, C4Type.INT, C4Type.INT);
		Increment.setArgTypeAndResult(C4Type.INT, C4Type.INT);
		Decrement.setArgTypeAndResult(C4Type.INT, C4Type.INT);
		Power.setPriority(14);
		Power.setArgTypesAndResult(C4Type.INT, C4Type.INT, C4Type.INT);
		for (C4ScriptOperator o : ops(Divide, Multiply, Modulo)) {
			o.setPriority(13);
			o.setArgTypesAndResult(C4Type.INT, C4Type.INT, C4Type.INT);
		}
		for (C4ScriptOperator o : ops(Subtract, Add)) {
			o.setPriority(12);
			o.setArgTypesAndResult(C4Type.INT, C4Type.INT, C4Type.INT);
		}
		for (C4ScriptOperator o : ops(Smaller, SmallerEqual, Larger, LargerEqual)) {
			o.setPriority(10);
			o.setArgTypesAndResult(C4Type.INT, C4Type.INT, C4Type.BOOL);
		}
		for (C4ScriptOperator o : ops(Equal, NotEqual, StringEqual, eq, ne))
			o.setPriority(9);
		for (C4ScriptOperator o : ops(Equal, NotEqual))
			o.setArgTypesAndResult(C4Type.ANY, C4Type.ANY, C4Type.BOOL);
		for (C4ScriptOperator o : ops(StringEqual, eq, ne))
			o.setArgTypesAndResult(C4Type.STRING, C4Type.STRING, C4Type.BOOL);
		BitAnd.setPriority(8);
		Or.setPriority(4);
		And.setPriority(5);
		for (C4ScriptOperator o : ops(BitOr, BitXOr))
			o.setPriority(6);
		for (C4ScriptOperator o : ops(And, Or))
			o.setArgTypesAndResult(C4Type.ANY, C4Type.ANY, C4Type.BOOL);
		for (C4ScriptOperator o : ops(BitAnd, BitXOr, BitOr, ShiftLeft, ShiftRight))
			o.setArgTypesAndResult(C4Type.INT, C4Type.INT, C4Type.INT);
		for (C4ScriptOperator o : ops(ShiftLeft, ShiftRight))
			o.setPriority(11);
		for (C4ScriptOperator o : ops(Assign, AssignAdd, AssignSubtract, AssignMultiply, AssignDivide, AssignModulo, AssignOr, AssignAnd, AssignXOr))
			o.setPriority(2);
		Assign.setArgTypesAndResult(C4Type.ANY, C4Type.ANY, C4Type.ANY);
		for (C4ScriptOperator o : ops(AssignAdd, AssignSubtract, AssignMultiply, AssignDivide, AssignModulo))
			o.setArgTypesAndResult(C4Type.INT, C4Type.INT, C4Type.INT);
		for (C4ScriptOperator o : ops(AssignOr, AssignAnd, AssignXOr))
			o.setArgTypesAndResult(C4Type.BOOL, C4Type.BOOL, C4Type.BOOL);
		
		for (C4ScriptOperator o : ops(And, Or, Equal, Not, Add, BitAnd))
			o.setOldStyleFunctionEquivalent(o.toString());
		Increment.setOldStyleFunctionEquivalent("Inc");
		Decrement.setOldStyleFunctionEquivalent("Dec");
		Larger.setOldStyleFunctionEquivalent("GreaterThan");
		Smaller.setOldStyleFunctionEquivalent("LessThan");
		Multiply.setOldStyleFunctionEquivalent("Mul");
		Divide.setOldStyleFunctionEquivalent("Div");
		Add.setOldStyleFunctionEquivalent("Sum");
		Subtract.setOldStyleFunctionEquivalent("Sub");
		StringEqual.setOldStyleFunctionEquivalent("SEqual");
	}

	public boolean isUnary() {
		return this == Not || this == Increment || this == Decrement || this == Add || this == Subtract;
	}

	private void setPriority(int i) {
		priority = i;
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
	
	private void setOldStyleFunctionEquivalent(String eq) {
		oldStyleFunctionEquivalent = eq;
	}
	
	public static C4ScriptOperator oldStyleFunctionReplacement(String funcName) {
		for (C4ScriptOperator o : values()) {
			if (o.getOldStyleFunctionEquivalent() != null && o.getOldStyleFunctionEquivalent().equals(funcName))
				return o;
		}
		return null;
	}
	
	public int priority() {
		return priority;
	}

	public boolean rightAssociative() {
		int o = ordinal();
		// assignment operators
		return (o >= 25 && o <= 32);
	}
	
	public boolean spaceNeededBetweenMeAnd(C4ScriptOperator other) {
		return
			((this == Add || this == Increment) && (other == Add || other == Increment)) ||
			((this == Subtract || this == Decrement) && (other == Subtract || other == Decrement));
	}

}