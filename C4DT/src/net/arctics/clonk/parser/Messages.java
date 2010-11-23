package net.arctics.clonk.parser;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "net.arctics.clonk.parser.messages"; //$NON-NLS-1$
	public static String TokenExpected;
	public static String NotAllowedHere;
	public static String NotAProplist;
	public static String OperatorNeedsRightSide;
	public static String NoAssignment;
	public static String NoSideEffects;
	public static String KeywordInWrongPlace;
	public static String UndeclaredIdentifier;
	public static String OldStyleFunc;
	public static String ValueExpected;
	public static String TuplesNotAllowed;
	public static String EmptyParentheses;
	public static String EngineDoesNotSupportRefs;
	public static String ExpectedCode;
	public static String MissingDirectiveArgs;
	public static String MissingClosingBracket;
	public static String MissingLocalizations;
	public static String CallingMethodOnNonObject;
	public static String ConstantValueExpected;
	public static String CommaOrSemicolonExpected;
	public static String IdentShadowed;
	public static String IncompatibleTypes;
	public static String VariableCalled;
	public static String TypeAsName;
	public static String BlockNotClosed;
	public static String BoolLiteralAsOpArg;
	public static String UnknownDirective;
	public static String UnknownSection;
	public static String StatementExpected;
	public static String ConditionExpected;
	public static String OutOfIntRange;
	public static String InvalidExpression;
	public static String NoInheritedFunction;
	public static String FunctionRedeclared;
	public static String NeverReached;
	public static String ObsoleteOperator;
	public static String StringNotClosed;
	public static String StringTooLong;
	public static String UnexpectedToken;
	public static String NotFinished;
	public static String GenericError;
	public static String ConditionAlwaysTrue;
	public static String ConditionAlwaysFalse;
	public static String InternalError;
	public static String InfiniteLoop;
	public static String InheritedDisabledInStrict0;
	public static String LocalUsedInGlobal;
	public static String ExpressionExpected;
	public static String UnexpectedEnd;
	public static String Unused;
	public static String VarUsedBeforeItsDeclaration;
	public static String NameExpected;
	public static String ReturnAsFunction;
	public static String ExpressionNotModifiable;
	public static String TooManyParameters;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
