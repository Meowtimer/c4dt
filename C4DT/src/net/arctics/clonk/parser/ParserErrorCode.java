package net.arctics.clonk.parser;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;

public enum ParserErrorCode {

	TokenExpected(Messages.TokenExpected),
	NotAllowedHere(Messages.NotAllowedHere),
	MissingClosingBracket(Messages.MissingClosingBracket),
	InvalidExpression(Messages.InvalidExpression),
	InternalError(Messages.InternalError),
	ExpressionExpected(Messages.ExpressionExpected),
	UnexpectedEnd(Messages.UnexpectedEnd),
	NameExpected(Messages.NameExpected),
	ReturnAsFunction(Messages.ReturnAsFunction),
	ExpressionNotModifiable(Messages.ExpressionNotModifiable),
	OperatorNeedsRightSide(Messages.OperatorNeedsRightSide),
	NoAssignment(Messages.NoAssignment),
	NoSideEffects(Messages.NoSideEffects),
	KeywordInWrongPlace(Messages.KeywordInWrongPlace),
	UndeclaredIdentifier(Messages.UndeclaredIdentifier),
	OldStyleFunc(Messages.OldStyleFunc),
	ValueExpected(Messages.ValueExpected),
	TuplesNotAllowed(Messages.TuplesNotAllowed),
	EmptyParentheses(Messages.EmptyParentheses),
	ExpectedCode(Messages.ExpectedCode),
	ConstantValueExpected(Messages.ConstantValueExpected),
	CommaOrSemicolonExpected(Messages.CommaOrSemicolonExpected),
	IncompatibleTypes(Messages.IncompatibleTypes),
	VariableCalled(Messages.VariableCalled),
	TypeAsName(Messages.TypeAsName),
	BlockNotClosed(Messages.BlockNotClosed),
	UnknownDirective(Messages.UnknownDirective),
	StatementExpected(Messages.StatementExpected),
	ConditionExpected(Messages.ConditionExpected),
	OutOfIntRange(Messages.OutOfIntRange),
	NoInheritedFunction(Messages.NoInheritedFunction),
	FunctionRedeclared(Messages.FunctionRedeclared),
	NeverReached(Messages.NeverReached),
	ObsoleteOperator(Messages.ObsoleteOperator),
	StringNotClosed(Messages.StringNotClosed),
	UnexpectedToken(Messages.UnexpectedToken),
	NotFinished(Messages.NotFinished),
	GenericError(Messages.GenericError),
	ConditionAlwaysTrue(Messages.ConditionAlwaysTrue),
	ConditionAlwaysFalse(Messages.ConditionAlwaysFalse),
	InfiniteLoop(Messages.InfiniteLoop),
	LocalUsedInGlobal(Messages.LocalUsedInGlobal),
	TooManyParameters(Messages.TooManyParameters),
	StringTooLong(Messages.StringTooLong),
	MissingLocalizations(Messages.MissingLocalizations),
	IdentShadowed(Messages.IdentShadowed),
	VarUsedBeforeItsDeclaration(Messages.VarUsedBeforeItsDeclaration),
	MissingDirectiveArgs(Messages.MissingDirectiveArgs),
	EngineDoesNotSupportRefs(Messages.EngineDoesNotSupportRefs),
	BoolLiteralAsOpArg(Messages.BoolLiteralAsOpArg);

	public static final String MARKER_ERRORCODE = "c4ScriptErrorCode"; //$NON-NLS-1$
	
	private String message;
	
	ParserErrorCode(String message) {
		this.message = message;
	}
	
	public String getErrorString(Object... format) {
		return String.format(message, format);
	}

	public String getMessage() {
		return message;
	}
	
	public static ParserErrorCode getErrorCode(IMarker marker) {
		try {
			return values()[marker.getAttribute(MARKER_ERRORCODE, -1)];
		} catch (ArrayIndexOutOfBoundsException e) {
			return null;
		}
	}
	
	public IMarker createMarker(IFile file, String markerType, int start, int end, int severity, Object... args) {
		return createMarker(file, markerType, start, end, severity, getErrorString(args));
	}
	
	public IMarker createMarker(IFile file, String markerType, int start, int end, int severity, String problem) {
		if (file == null)
			return null;
		try {
			IMarker marker = file.createMarker(markerType);
			marker.setAttribute(IMarker.SEVERITY, severity);
			marker.setAttribute(IMarker.TRANSIENT, false);
			marker.setAttribute(IMarker.MESSAGE, problem);
			marker.setAttribute(IMarker.CHAR_START, start);
			marker.setAttribute(IMarker.CHAR_END, end);
			marker.setAttribute(MARKER_ERRORCODE, this.ordinal());
			return marker;
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return null;
	}
	
}