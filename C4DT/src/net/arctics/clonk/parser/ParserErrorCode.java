package net.arctics.clonk.parser;

import net.arctics.clonk.parser.c4script.Keywords;
import net.arctics.clonk.parser.c4script.ast.ExprElm;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IRegion;

public enum ParserErrorCode {

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
	TooManyParameters(Messages.TooManyParameters, Messages.ParserErrorCode_Arg_ExpectedNumber, Messages.ParserErrorCode_Arg_ActualNumber, Messages.ParserErrorCode_Arg_Function),
	StringTooLong(Messages.StringTooLong, Messages.ParserErrorCode_Arg_Size, Messages.ParserErrorCode_Arg_MaximumSize),
	MissingLocalizations(Messages.MissingLocalizations, Messages.ParserErrorCode_Arg_LanguageId),
	IdentShadowed(Messages.IdentShadowed, Messages.ParserErrorCode_Arg_Identifier, Messages.ParserErrorCode_Arg_ShadowedIdentifier),
	VarUsedBeforeItsDeclaration(Messages.VarUsedBeforeItsDeclaration, Messages.ParserErrorCode_Arg_Variable),
	MissingDirectiveArgs(Messages.MissingDirectiveArgs, Messages.ParserErrorCode_Arg_Directive),
	EngineDoesNotSupportRefs(Messages.EngineDoesNotSupportRefs, Messages.ParserErrorCode_Arg_Engine),
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
	DotNotationInsteadOfArrow(Messages.DotNotationInsteadOfArrow, Messages.ParserErrorCode_Arg_Declaration),
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
	DuplicateDeclaration(Messages.DuplicateDeclaration, Messages.DuplicateDeclarationName);

	public static final String MARKER_ERRORCODE = "c4ScriptErrorCode"; //$NON-NLS-1$
	public static final String MARKER_EXPRESSIONSTART = "c4ScriptErrorExpressionStart"; //$NON-NLS-1$
	public static final String MARKER_EXPRESSIONEND = "c4ScriptErrorExpressionEnd"; //$NON-NLS-1$
	public static final String MARKER_DECLARATIONTAG = "c4ScriptErrorDeclarationTag"; //$NON-NLS-1$
	
	public static String[] MARKER_ARGS;
	
	static {
		MARKER_ARGS = new String[3];
		for (int i = 0; i < MARKER_ARGS.length; i++)
			MARKER_ARGS[i] = String.format("c4ScriptErrorArg%d", i); //$NON-NLS-1$
	}
	
	private String message;
	private String[] formatArgumentDescriptions;
	
	ParserErrorCode(String message) {
		this.message = message;
	}
	
	ParserErrorCode(String message, String... formatArgumentDescriptions) {
		this(message);
		this.formatArgumentDescriptions = formatArgumentDescriptions;
	}
	
	public String makeErrorString(Object... format) {
		return String.format(message, format);
	}

	public String message() {
		return message;
	}
	
	public String[] formatArgumentDescriptions() {
		return formatArgumentDescriptions;
	}
	
	public static ParserErrorCode errorCode(IMarker marker) {
		try {
			return values()[marker.getAttribute(MARKER_ERRORCODE, -1)];
		} catch (ArrayIndexOutOfBoundsException e) {
			return null;
		}
	}
	
	public static IRegion expressionLocation(IMarker marker) {
		return new SourceLocation(marker.getAttribute(MARKER_EXPRESSIONSTART, -1), marker.getAttribute(MARKER_EXPRESSIONEND, -1));
	}
	
	public static void setExpressionLocation(IMarker marker, IRegion location) {
		try {
			marker.setAttribute(MARKER_EXPRESSIONSTART, location != null ? location.getOffset() : -1);
			marker.setAttribute(MARKER_EXPRESSIONEND, location != null ? location.getOffset()+location.getLength() : -1);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
	
	public static String arg(IMarker marker, int index) {
		return marker.getAttribute(String.format(MARKER_ARGS[index], index), ""); //$NON-NLS-1$
	}
	
	public IMarker createMarker(IFile file, Declaration declarationAssociatedWithFile, String markerType, int start, int end, int severity, String problem) {
		if (file == null)
			return null;
		try {
			IMarker marker = file.createMarker(markerType);
			marker.setAttributes(
				new String[] {IMarker.SEVERITY, IMarker.TRANSIENT, IMarker.MESSAGE, IMarker.CHAR_START, IMarker.CHAR_END, IMarker.LOCATION, MARKER_ERRORCODE},
				new Object[] {severity, false, problem, start, end, declarationAssociatedWithFile != null ? declarationAssociatedWithFile.toString() : null, this.ordinal()}
			);
			return marker;
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public IMarker createMarker(IFile file, Declaration declarationAssociatedWithFile, String markerType, int start, int end, int severity, IRegion expressionRegion, Object... args) {
		IMarker marker = createMarker(file, declarationAssociatedWithFile, markerType, start, end, severity, makeErrorString(args));
		if (expressionRegion instanceof ExprElm)
			try {
				marker.setAttribute(IMarker.LOCATION, expressionRegion.toString());
			} catch (CoreException e1) {
				e1.printStackTrace();
			}
		for (int i = 0; i < Math.min(args.length, MARKER_ARGS.length); i++)
			try {
				marker.setAttribute(MARKER_ARGS[i], args[i] != null ? args[i].toString() : ""); //$NON-NLS-1$
			} catch (CoreException e) {
				e.printStackTrace();
			}
		if (expressionRegion != null)
			setExpressionLocation(marker, expressionRegion);
		return marker;
	}

	public static void setDeclarationTag(IMarker marker, String declarationTag) {
		try {
			marker.setAttribute(MARKER_DECLARATIONTAG, declarationTag);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
	
	public static String declarationTag(IMarker marker) {
		return marker.getAttribute(MARKER_DECLARATIONTAG, ""); //$NON-NLS-1$
	}

	public String messageWithFormatArgumentDescriptions() {
		String msg = message();
		if (formatArgumentDescriptions != null)
			try {
				msg = String.format(msg, (Object[])formatArgumentDescriptions);
			} catch (Exception e) {
				// ignore
			}
		return msg;
	}
	
}