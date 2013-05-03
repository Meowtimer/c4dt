package net.arctics.clonk;

import net.arctics.clonk.c4script.Keywords;

/**
 * Enumeration for all problems which c4dt might potentially add to the problems view.
 * @author madeen
 *
 */
public enum Problem {
	TokenExpected(Messages.TokenExpected, Messages.ParserErrorCode_Arg_Token),
	NotAllowedHere(Messages.NotAllowedHere, Messages.ParserErrorCode_Arg_DisallowedToken),
	MissingClosingBracket(Messages.MissingClosingBracket, Messages.ParserErrorCode_Arg_Missing),
	InvalidExpression(Messages.InvalidExpression),
	InternalError(Messages.InternalError, Messages.ParserErrorCode_Arg_ExceptionMessage),
	ExpressionExpected(Messages.ExpressionExpected),
	UnexpectedEnd(Messages.UnexpectedEnd),
	NameExpected(Messages.NameExpected),
	ReturnAsFunction(Messages.ReturnAsFunction),
	ExpressionNotModifiable(Messages.ExpressionNotModifiable),
	OperatorNeedsRightSide(Messages.OperatorNeedsRightSide),
	NoAssignment(Messages.NoAssignment),
	NoSideEffects(Messages.NoSideEffects),
	KeywordInWrongPlace(Messages.KeywordInWrongPlace, Messages.ParserErrorCode_Arg_Keyword),
	UndeclaredIdentifier(Messages.UndeclaredIdentifier, Messages.ParserErrorCode_Arg_Identifier),
	OldStyleFunc(Messages.OldStyleFunc),
	ValueExpected(Messages.ValueExpected),
	TuplesNotAllowed(Messages.TuplesNotAllowed),
	EmptyParentheses(Messages.EmptyParentheses),
	ExpectedCode(Messages.ExpectedCode),
	ConstantValueExpected(Messages.ConstantValueExpected),
	CommaOrSemicolonExpected(Messages.CommaOrSemicolonExpected),
	IncompatibleTypes(Messages.IncompatibleTypes, Messages.ParserErrorCode_Arg_Type1, Messages.ParserErrorCode_Arg_Type2),
	VariableCalled(Messages.VariableCalled, Messages.ParserErrorCode_Arg_Variable, Messages.ParserErrorCode_Arg_VariableType),
	TypeAsName(Messages.TypeAsName, Messages.ParserErrorCode_Arg_Typename),
	BlockNotClosed(Messages.BlockNotClosed),
	UnknownDirective(Messages.UnknownDirective),
	StatementExpected(Messages.StatementExpected),
	ConditionExpected(Messages.ConditionExpected),
	OutOfIntRange(Messages.OutOfIntRange, Messages.ParserErrorCode_Arg_Integer),
	NoInheritedFunction(Messages.NoInheritedFunction, Messages.ParserErrorCode_Arg_FunctionName),
	FunctionRedeclared(Messages.FunctionRedeclared),
	NeverReached(Messages.NeverReached),
	ObsoleteOperator(Messages.ObsoleteOperator, Messages.ParserErrorCode_Arg_Operator),
	StringNotClosed(Messages.StringNotClosed),
	UnexpectedToken(Messages.UnexpectedToken, Messages.ParserErrorCode_Arg_Token),
	NotFinished(Messages.NotFinished),
	GenericError(Messages.GenericError, Messages.ParserErrorCode_Arg_GenericError),
	ConditionAlwaysTrue(Messages.ConditionAlwaysTrue, Messages.ParserErrorCode_Arg_Condition),
	ConditionAlwaysFalse(Messages.ConditionAlwaysFalse, Messages.ParserErrorCode_Arg_Condition),
	InfiniteLoop(Messages.InfiniteLoop),
	LocalUsedInGlobal(Messages.LocalUsedInGlobal),
	ParameterCountMismatch(Messages.ParameterCountMismatch, Messages.ParserErrorCode_Arg_ExpectedNumber, Messages.ParserErrorCode_Arg_ActualNumber, Messages.ParserErrorCode_Arg_Function),
	StringTooLong(Messages.StringTooLong, Messages.ParserErrorCode_Arg_Size, Messages.ParserErrorCode_Arg_MaximumSize),
	MissingLocalizations(Messages.MissingLocalizations, Messages.ParserErrorCode_Arg_LanguageId),
	IdentShadowed(Messages.IdentShadowed, Messages.ParserErrorCode_Arg_Identifier, Messages.ParserErrorCode_Arg_ShadowedIdentifier),
	VarUsedBeforeItsDeclaration(Messages.VarUsedBeforeItsDeclaration, Messages.ParserErrorCode_Arg_Variable),
	MissingDirectiveArgs(Messages.MissingDirectiveArgs, Messages.ParserErrorCode_Arg_Directive),
	BoolLiteralAsOpArg(Messages.BoolLiteralAsOpArg, Messages.ParserErrorCode_Arg_Bool),
	InheritedDisabledInStrict0(Messages.InheritedDisabledInStrict0),
	CallingMethodOnNonObject(Messages.CallingMethodOnNonObject, Messages.ParserErrorCode_Arg_GuessedType),
	NotAProplist(Messages.NotAProplist),
	UnknownSection(Messages.UnknownSection, Messages.ParserErrorCode_Arg_SectionName),
	Unused(Messages.Unused, Messages.ParserErrorCode_Arg_Variable),
	UnusedParameter(Messages.UnusedParameter, Messages.ParserErrorCode_Parameter_Name),
	NotAnArrayOrProplist(Messages.NotAnArrayOrProplist),
	VarOutsideFunction(Messages.VarOutsideFunction, Keywords.VarNamed, Keywords.GlobalNamed, Keywords.LocalNamed),
	Garbage(Messages.Garbage, Messages.ParserErrorCode_Arg_Garbage),
	NotANumber(Messages.NotANumber, Messages.ParserErrorCode_Arg_WeirdoNumber),
	StaticInsideFunction(Messages.StaticInsideFunction, Keywords.GlobalNamed),
	DotNotationNotSupported(Messages.DotNotationNotSupported, Messages.ParserErrorCode_Arg_Declaration),
	MissingStatement(Messages.MissingStatement),
	MissingExpression(Messages.MissingExpression),
	MemberOperatorWithTildeNoSpace(Messages.MemberOperatorWithTildeNoSpace),
	MissingFormatArg(Messages.MissingFormatArg, Messages.ParserErrorCode_Arg_FormatArgument, Messages.ParserErrorCode_Arg_FormatString, Messages.ParserErrorCode_Arg_Origin),
	IncompatibleFormatArgType(Messages.IncompatibleFormatArgType, Messages.ParserErrorCode_Arg_FormatArgument, Messages.ParserErrorCode_Arg_ActualType, Messages.ParserErrorCode_Arg_FormatString, Messages.ParserErrorCode_Arg_Origin),
	DragonsHere(Messages.DragonsHere),
	TypingJudgment("Typing judgment for '%s': %s", Messages.ParserErrorCode_Arg_Expression, Messages.ParserErrorCode_Arg_Type), //$NON-NLS-1$
	NotSupported(Messages.NotSupported, Messages.ParserErrorCode_Arg_Feature),
	MissingBrackets(Messages.MissingBrackets, Messages.ParserErrorCode_Arg_NumberOfMissingBrackets, Messages.ParserErrorCode_Arg_BracketType),
	OnlyRefAllowedAsReturnType(Messages.OnlyRefAllopwedAsReturnType),
	FunctionRefNotAllowed(Messages.FunctionRefNotAllowed, Messages.ParserErrorCode_Arg_Engine),
	CallingExpression(Messages.CallingExpression),
	FloatNumbersNotSupported(Messages.FloatNumbersNotSupported),
	DuplicateDeclaration(Messages.DuplicateDeclaration, Messages.DuplicateDeclarationName),
	PrimitiveTypeNotSupported(Messages.PrimitiveTypeNotSupported),
	NonConstGlobalVarAssignment(Messages.NonConstGlobalVarAssignment),
	TypeExpected(Messages.TypeExpected),
	InvalidType(Messages.InvalidType),
	UnexpectedBlock(Messages.UnexpectedBlock),
	ConcreteArgumentMismatch(Messages.ConcreteArgumentMismatch,
		Messages.CAM_Arg, Messages.CAM_Par, Messages.CAM_Callee, Messages.CAM_Expected, Messages.CAM_Got),
	DeclarationNotFound(Messages.DeclarationNotFound, Messages.DNF_DeclarationName, Messages.DNF_Container);

	private String message;
	private String[] formatArgumentDescriptions;

	Problem(String message) { this.message = message; }

	Problem(String message, String... formatArgumentDescriptions) {
		this(message);
		this.formatArgumentDescriptions = formatArgumentDescriptions;
	}

	public String makeErrorString(Object... format) { return String.format(message, format); }
	public String message() { return message; }
	public String[] formatArgumentDescriptions() { return formatArgumentDescriptions; }

	public String messageWithFormatArgumentDescriptions() {
		String msg = message();
		if (formatArgumentDescriptions != null)
			try {
				msg = String.format(msg, (Object[])formatArgumentDescriptions);
			} catch (final Exception e) {
				// ignore
			}
		return msg;
	}

}