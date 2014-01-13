package net.arctics.clonk;

import net.arctics.clonk.c4script.Keywords;

/**
 * Enumeration for all problems which c4dt might potentially add to the problems view.
 * @author madeen
 *
 */
public enum Problem {
	TokenExpected(Messages.TokenExpected, Messages.Arg_Token),
	NotAllowedHere(Messages.NotAllowedHere, Messages.Arg_DisallowedToken),
	MissingClosingBracket(Messages.MissingClosingBracket, Messages.Arg_Missing),
	InvalidExpression(Messages.InvalidExpression),
	InternalError(Messages.InternalError, Messages.Arg_ExceptionMessage),
	ExpressionExpected(Messages.ExpressionExpected),
	UnexpectedEnd(Messages.UnexpectedEnd),
	NameExpected(Messages.NameExpected),
	ReturnAsFunction(Messages.ReturnAsFunction),
	ExpressionNotModifiable(Messages.ExpressionNotModifiable),
	OperatorNeedsRightSide(Messages.OperatorNeedsRightSide),
	NoAssignment(Messages.NoAssignment),
	NoSideEffects(Messages.NoSideEffects),
	KeywordInWrongPlace(Messages.KeywordInWrongPlace, Messages.Arg_Keyword),
	UndeclaredIdentifier(Messages.UndeclaredIdentifier, Messages.Arg_Identifier),
	OldStyleFunc(Messages.OldStyleFunc),
	ValueExpected(Messages.ValueExpected),
	TuplesNotAllowed(Messages.TuplesNotAllowed),
	EmptyParentheses(Messages.EmptyParentheses),
	ExpectedCode(Messages.ExpectedCode),
	ConstantValueExpected(Messages.ConstantValueExpected),
	CommaOrSemicolonExpected(Messages.CommaOrSemicolonExpected),
	IncompatibleTypes(Messages.IncompatibleTypes, Messages.Arg_Type1, Messages.Arg_Type2),
	VariableCalled(Messages.VariableCalled, Messages.Arg_Variable, Messages.Arg_VariableType),
	TypeAsName(Messages.TypeAsName, Messages.Arg_Typename),
	BlockNotClosed(Messages.BlockNotClosed),
	UnknownDirective(Messages.UnknownDirective),
	StatementExpected(Messages.StatementExpected),
	ConditionExpected(Messages.ConditionExpected),
	OutOfIntRange(Messages.OutOfIntRange, Messages.Arg_Integer),
	NoInheritedFunction(Messages.NoInheritedFunction, Messages.Arg_FunctionName),
	FunctionRedeclared(Messages.FunctionRedeclared),
	NeverReached(Messages.NeverReached),
	ObsoleteOperator(Messages.ObsoleteOperator, Messages.Arg_Operator),
	StringNotClosed(Messages.StringNotClosed),
	UnexpectedToken(Messages.UnexpectedToken, Messages.Arg_Token),
	NotFinished(Messages.NotFinished),
	GenericError(Messages.GenericError, Messages.Arg_GenericError),
	ConditionAlwaysTrue(Messages.ConditionAlwaysTrue, Messages.Arg_Condition),
	ConditionAlwaysFalse(Messages.ConditionAlwaysFalse, Messages.Arg_Condition),
	InfiniteLoop(Messages.InfiniteLoop),
	LocalUsedInGlobal(Messages.LocalUsedInGlobal),
	ParameterCountMismatch(Messages.ParameterCountMismatch, Messages.Arg_ExpectedNumber, Messages.Arg_ActualNumber, Messages.Arg_Function),
	StringTooLong(Messages.StringTooLong, Messages.Arg_Size, Messages.Arg_MaximumSize),
	MissingLocalizations(Messages.MissingLocalizations, Messages.Arg_LanguageId),
	IdentShadowed(Messages.IdentShadowed, Messages.Arg_Identifier, Messages.Arg_ShadowedIdentifier),
	VarUsedBeforeItsDeclaration(Messages.VarUsedBeforeItsDeclaration, Messages.Arg_Variable),
	MissingDirectiveArgs(Messages.MissingDirectiveArgs, Messages.Arg_Directive),
	BoolLiteralAsOpArg(Messages.BoolLiteralAsOpArg, Messages.Arg_Bool),
	InheritedDisabledInStrict0(Messages.InheritedDisabledInStrict0),
	CallingMethodOnNonObject(Messages.CallingMethodOnNonObject, Messages.Arg_GuessedType),
	NotAProplist(Messages.NotAProplist),
	UnknownSection(Messages.UnknownSection, Messages.Arg_SectionName),
	Unused(Messages.Unused, Messages.Arg_Variable),
	UnusedParameter(Messages.UnusedParameter, Messages.Parameter_Name),
	NotAnArrayOrProplist(Messages.NotAnArrayOrProplist),
	VarOutsideFunction(Messages.VarOutsideFunction, Keywords.VarNamed, Keywords.GlobalNamed, Keywords.LocalNamed),
	Garbage(Messages.Garbage, Messages.Arg_Garbage),
	NotANumber(Messages.NotANumber, Messages.Arg_WeirdoNumber),
	StaticInsideFunction(Messages.StaticInsideFunction, Keywords.GlobalNamed),
	DotNotationNotSupported(Messages.DotNotationNotSupported, Messages.Arg_Declaration),
	MissingStatement(Messages.MissingStatement),
	MissingExpression(Messages.MissingExpression),
	MemberOperatorWithTildeNoSpace(Messages.MemberOperatorWithTildeNoSpace),
	MissingFormatArg(Messages.MissingFormatArg, Messages.Arg_FormatArgument, Messages.Arg_FormatString, Messages.Arg_Origin),
	IncompatibleFormatArgType(Messages.IncompatibleFormatArgType, Messages.Arg_FormatArgument, Messages.Arg_ActualType, Messages.Arg_FormatString, Messages.Arg_Origin),
	DragonsHere(Messages.DragonsHere),
	TypingJudgment("Typing judgment for '%s': %s", Messages.Arg_Expression, Messages.Arg_Type), //$NON-NLS-1$
	NotSupported(Messages.NotSupported, Messages.Arg_Feature),
	MissingBrackets(Messages.MissingBrackets, Messages.Arg_NumberOfMissingBrackets, Messages.Arg_BracketType),
	OnlyRefAllowedAsReturnType(Messages.OnlyRefAllopwedAsReturnType),
	FunctionRefNotAllowed(Messages.FunctionRefNotAllowed, Messages.Arg_Engine),
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
	DeclarationNotFound(Messages.DeclarationNotFound, Messages.DNF_DeclarationName, Messages.DNF_Container),
	LoopVariableUsedInMultipleLoops(Messages.LoopVariableUsedInMultipleLoops, Messages.LoopVariableName),
	LeadsToErrors(Messages.LeadsToErrors),
	LocalOverridesParameter(Messages.LocalOverridesParameter),
	FunctionRefAfterArrow(Messages.FunctionRefAfterArrow),
	FunctionCallAfterDot(Messages.FunctionCallAfterDot),
	GlobalFunctionInAppendTo(Messages.GlobalFunctionInAppendTo),
	NoopAssignment(Messages.NoopAssignment),
	EmptyBody(Messages.EmptyBody);

	private String message;
	private String[] formatArgumentDescriptions;

	Problem(final String message) { this.message = message; }

	Problem(final String message, final String... formatArgumentDescriptions) {
		this(message);
		this.formatArgumentDescriptions = formatArgumentDescriptions;
	}

	public String makeErrorString(final Object... format) { return String.format(message, format); }
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