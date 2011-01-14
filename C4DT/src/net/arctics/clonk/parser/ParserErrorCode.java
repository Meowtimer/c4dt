package net.arctics.clonk.parser;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IRegion;

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
	BoolLiteralAsOpArg(Messages.BoolLiteralAsOpArg),
	InheritedDisabledInStrict0(Messages.InheritedDisabledInStrict0),
	CallingMethodOnNonObject(Messages.CallingMethodOnNonObject),
	NotAProplist(Messages.NotAProplist),
	UnknownSection(Messages.UnknownSection),
	Unused(Messages.Unused),
	NotAnArrayOrProplist(Messages.NotAnArrayOrProplist),
	VarOutsideFunction(Messages.VarOutsideFunction),
	Garbage(Messages.Garbage),
	NotANumber(Messages.NotANumber),
	StaticInsideFunction(Messages.StaticInsideFunction),
	DotNotationInsteadOfArrow("'%s' should be accessed using '.' and not '->'");

	public static final String MARKER_ERRORCODE = "c4ScriptErrorCode"; //$NON-NLS-1$
	public static final String MARKER_EXPRESSIONSTART = "c4ScriptErrorExpressionStart"; //$NON-NLS-1$
	public static final String MARKER_EXPRESSIONEND = "c4ScriptErrorExpressionEnd"; //$NON-NLS-1$
	
	public static String[] MARKER_ARGS;
	
	static {
		MARKER_ARGS = new String[3];
		for (int i = 0; i < MARKER_ARGS.length; i++)
			MARKER_ARGS[i] = String.format("c4ScriptErrorArg%d", i); //$NON-NLS-1$
	}
	
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
	
	public static IRegion getExpressionLocation(IMarker marker) {
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
	
	public static String getArg(IMarker marker, int index) {
		return marker.getAttribute(String.format(MARKER_ARGS[index], index), ""); //$NON-NLS-1$
	}
	
	public IMarker createMarker(IFile file, Declaration declarationAssociatedWithFile, String markerType, int start, int end, int severity, String problem) {
		if (file == null)
			return null;
		try {
			IMarker marker = file.createMarker(markerType);
			marker.setAttribute(IMarker.SEVERITY, severity);
			marker.setAttribute(IMarker.TRANSIENT, false);
			marker.setAttribute(IMarker.MESSAGE, problem);
			marker.setAttribute(IMarker.CHAR_START, start);
			marker.setAttribute(IMarker.CHAR_END, end);
			if (declarationAssociatedWithFile != null)
				marker.setAttribute(IMarker.LOCATION, declarationAssociatedWithFile.toString());
			
			marker.setAttribute(MARKER_ERRORCODE, this.ordinal());
			return marker;
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public IMarker createMarker(IFile file, Declaration declarationAssociatedWithFile, String markerType, int start, int end, int severity, IRegion expressionRegion, Object... args) {
		IMarker marker = createMarker(file, declarationAssociatedWithFile, markerType, start, end, severity, getErrorString(args));
		for (int i = 0; i < Math.min(args.length, MARKER_ARGS.length); i++) {
			try {
				marker.setAttribute(MARKER_ARGS[i], args[i] != null ? args[i].toString() : ""); //$NON-NLS-1$
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		if (expressionRegion != null) {
			setExpressionLocation(marker, expressionRegion);
		}
		return marker;
	}
	
}