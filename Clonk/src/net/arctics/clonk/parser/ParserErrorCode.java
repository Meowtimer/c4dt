package net.arctics.clonk.parser;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;

public enum ParserErrorCode {
	TokenExpected(Messages.ParserErrorCode_0),
	NotAllowedHere(Messages.ParserErrorCode_1),
	MissingClosingBracket(Messages.ParserErrorCode_2),
	InvalidExpression(Messages.ParserErrorCode_3),
	InternalError(Messages.ParserErrorCode_4),
	ExpressionExpected(Messages.ParserErrorCode_5),
	UnexpectedEnd(Messages.ParserErrorCode_6),
	NameExpected(Messages.ParserErrorCode_7),
	ReturnAsFunction(Messages.ParserErrorCode_8),
	ExpressionNotModifiable(Messages.ParserErrorCode_9),
	OperatorNeedsRightSide(Messages.ParserErrorCode_10),
	NoAssignment(Messages.ParserErrorCode_11),
	NoSideEffects(Messages.ParserErrorCode_12),
	KeywordInWrongPlace(Messages.ParserErrorCode_13),
	UndeclaredIdentifier(Messages.ParserErrorCode_14),
	OldStyleFunc(Messages.ParserErrorCode_15),
	ValueExpected(Messages.ParserErrorCode_16),
	TuplesNotAllowed(Messages.ParserErrorCode_17),
	EmptyParentheses(Messages.ParserErrorCode_18),
	ExpectedCode(Messages.ParserErrorCode_19),
	ConstantValueExpected(Messages.ParserErrorCode_20),
	CommaOrSemicolonExpected(Messages.ParserErrorCode_21),
	IncompatibleTypes(Messages.ParserErrorCode_22),
	VariableCalled(Messages.ParserErrorCode_23),
	TypeAsName(Messages.ParserErrorCode_24),
	BlockNotClosed(Messages.ParserErrorCode_25),
	UnknownDirective(Messages.ParserErrorCode_26),
	StatementExpected(Messages.ParserErrorCode_27),
	ConditionExpected(Messages.ParserErrorCode_28),
	OutOfIntRange(Messages.ParserErrorCode_29),
	NoInheritedFunction(Messages.ParserErrorCode_30),
	FunctionRedeclared(Messages.ParserErrorCode_31),
	NeverReached(Messages.ParserErrorCode_32),
	ObsoleteOperator(Messages.ParserErrorCode_33),
	StringNotClosed(Messages.ParserErrorCode_34),
	UnexpectedToken(Messages.ParserErrorCode_35),
	NotFinished(Messages.ParserErrorCode_36),
	GenericError(Messages.ParserErrorCode_37),
	ConditionAlwaysTrue(Messages.ParserErrorCode_38),
	ConditionAlwaysFalse(Messages.ParserErrorCode_39),
	InfiniteLoop(Messages.ParserErrorCode_40);
	
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