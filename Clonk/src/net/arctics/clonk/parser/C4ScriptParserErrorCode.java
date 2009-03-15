/**
 * 
 */
package net.arctics.clonk.parser;

public enum C4ScriptParserErrorCode {
	TokenExpected("'%s' expected"),
	NotAllowedHere("'%s' not allowed here"),
	MissingClosingBracket("Missing '%s'"),
	InvalidExpression("Invalid expression"),
	InternalError("Internal error: %s"),
	ExpressionExpected("Expression expected"),
	UnexpectedEnd("Unexpected end of script"),
	NameExpected("Name expected"),
	ReturnAsFunction("return should not be treated as function"),
	ExpressionNotModifiable("Expression cannot be modified"),
	OperatorNeedsRightSide("Operator has no right side"),
	NoAssignment("There is no toplevel-assignment in this expression"),
	NoSideEffects("Expression has no side effects"),
	KeywordInWrongPlace("Keyword '%s' misplaced"),
	UndeclaredIdentifier("Undeclared identifier '%s'"),
	OldStyleFunc("Old-style function"),
	ValueExpected("Value expected"),
	TuplesNotAllowed("Tuples not allowed here"),
	EmptyParentheses("Empty parentheses"),
	ExpectedCode("Code expected"),
	ConstantValueExpected("Constant value expected"),
	CommaOrSemicolonExpected("Comma or semicolon expected"),
	IncompatibleTypes("Incompatible types: %s and %s"),
	VariableCalled("Variable %s called as function"),
	TypeAsName("Typename as name: %s"),
	BlockNotClosed("Code block not closed with '}'"),
	UnknownDirective("Unknown directive"),
	StatementExpected("Statement expected"),
	ConditionExpected("Condition expected"),
	OutOfIntRange("Out of integer range: %s"),
	NoInheritedFunction("No inherited version of %s found"),
	FunctionRedeclared("Function overload: this function is already declared in this script"),
	NeverReached("Code never reached"),
	ObsoleteOperator("Obsolete operator '%s'"),
	StringNotClosed("String not closed"),
	UnexpectedToken("Unexpected token: %s");
	
	private String message;
	
	C4ScriptParserErrorCode(String message) {
		this.message = message;
	}
	
	public String getErrorString(Object... format) {
		return String.format(message, format);
	}

	public String getMessage() {
		return message;
	}
	
}