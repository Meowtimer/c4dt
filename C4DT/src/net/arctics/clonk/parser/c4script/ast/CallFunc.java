package net.arctics.clonk.parser.c4script.ast;

import java.util.List;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.C4Object;
import net.arctics.clonk.index.CachedEngineFuncs;
import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.ArrayType;
import net.arctics.clonk.parser.c4script.C4Function;
import net.arctics.clonk.parser.c4script.C4ObjectType;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.c4script.C4ScriptOperator;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.C4Type;
import net.arctics.clonk.parser.c4script.C4TypeSet;
import net.arctics.clonk.parser.c4script.C4Variable;
import net.arctics.clonk.parser.c4script.FindDeclarationInfo;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.Keywords;
import net.arctics.clonk.parser.c4script.C4Function.C4FunctionScope;
import net.arctics.clonk.parser.c4script.C4ScriptParser.IMarkerListener;
import net.arctics.clonk.parser.c4script.ast.UnaryOp.Placement;
import net.arctics.clonk.parser.c4script.ast.evaluate.IEvaluationContext;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.Region;

public class CallFunc extends AccessDeclaration {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	public final static class FunctionReturnTypeInformation extends StoredTypeInformation {
		private C4Function function;

		public FunctionReturnTypeInformation(C4Function function) {
			super();
			this.function = function;
			if (function != null)
				type = function.getReturnType();
		}
		
		@Override
		public void storeType(IType type) {
			// don't store if function.getReturnType() already specifies a type (including any)
			// this is to prevent cases where for example the result of EffectVar in one instance is
			// used as int and then as something else which leads to an erroneous type incompatibility warning
			if (type == C4Type.UNKNOWN)
				super.storeType(type);
		}
		
		@Override
		public boolean expressionRelevant(ExprElm expr, C4ScriptParser parser) {
			if (expr instanceof CallFunc) {
				CallFunc callFunc = (CallFunc) expr;
				if (callFunc.getDeclaration() == this.function)
					return true;
			}
			return false;
		}
		
		@Override
		public boolean sameExpression(IStoredTypeInformation other) {
			return other instanceof CallFunc.FunctionReturnTypeInformation && ((CallFunc.FunctionReturnTypeInformation)other).function == this.function;
		}
		
		@Override
		public String toString() {
			return "function " + function + " " + super.toString();
		}
		
		@Override
		public void apply(boolean soft) {
			if (function == null)
				return;
			function = (C4Function) function.latestVersion();
			if (!soft && !function.isEngineDeclaration()) {
				function.forceType(getType());
			}
		}
		
	}
	
	private final static class VarFunctionsTypeInformation extends StoredTypeInformation {
		private C4Function varFunction;
		private long varIndex;

		private VarFunctionsTypeInformation(C4Function function, long val) {
			varFunction = function;
			varIndex = val;
		}

		public boolean expressionRelevant(ExprElm expr, C4ScriptParser parser) {
			if (expr instanceof CallFunc) {
				CallFunc callFunc = (CallFunc) expr;
				Object ev;
				return
					callFunc.getDeclaration() == varFunction &&
					callFunc.getParams().length == 1 && // don't bother with more complex cases
					callFunc.getParams()[0].getType(parser) == C4Type.INT &&
					((ev = callFunc.getParams()[0].evaluateAtParseTime(parser.getContainer())) != null) &&
					ev.equals(varIndex);
			}
			return false;
		}

		public boolean sameExpression(IStoredTypeInformation other) {
			if (other.getClass() == CallFunc.VarFunctionsTypeInformation.class) {
				CallFunc.VarFunctionsTypeInformation otherInfo = (CallFunc.VarFunctionsTypeInformation) other;
				return otherInfo.varFunction == this.varFunction && otherInfo.varIndex == this.varIndex; 
			}
			else
				return false;
		}
		
		@Override
		public String toString() {
			return String.format("%s(%d)", varFunction.getName(), varIndex); //$NON-NLS-1$
		}
		
	}

	private ExprElm[] params;
	private int parmsStart, parmsEnd;

	public void setParmsRegion(int start, int end) {
		parmsStart = start;
		parmsEnd   = end;
	}

	public int getParmsStart() {
		return parmsStart;
	}

	public int getParmsEnd() {
		return parmsEnd;
	}

	public CallFunc(String funcName, ExprElm... parms) {
		super(funcName);
		params = parms;
		assignParentToSubElements();
	}
	
	public CallFunc(C4Function function, ExprElm... parms) {
		this(function.getName());
		this.declaration = function;
	}
	
	@Override
	public void doPrint(ExprWriter output, int depth) {
		super.doPrint(output, depth);
		output.append("("); //$NON-NLS-1$
		if (params != null) {
			for (int i = 0; i < params.length; i++) {
				if (params[i] != null)
					params[i].print(output, depth+1);
				if (i < params.length-1)
					output.append(", "); //$NON-NLS-1$
			}
		}
		output.append(")"); //$NON-NLS-1$
	}
	@Override
	public boolean modifiable(C4ScriptParser context) {
		IType t = getType(context);
		return t.canBeAssignedFrom(C4TypeSet.REFERENCE_OR_ANY_OR_UNKNOWN);
	}
	@Override
	public boolean hasSideEffects() {
		return true;
	}
	@Override
	public IType getType(C4ScriptParser context) {
		C4Declaration d = getDeclaration(context);
		
		// look for gathered type information
		IType stored = context.queryTypeOfExpression(this, null);
		if (stored != null)
			return stored;
		
		// calling this() as function -> return object type belonging to script
		if (params.length == 0 && (d == getCachedFuncs(context).This || d == C4Variable.THIS)) {
			C4Object obj = context.getContainerObject();
			if (obj != null)
				return obj;
		}
		
		// it's a criteria search (FindObjects etc) so guess return type from arguments passed to the criteria search function
		if (isCriteriaSearch()) {
			IType t = searchCriteriaAssumedResult(context);
			if (t != null) {
				C4Function f = (C4Function) d;
				if (f.getReturnType() == C4Type.ARRAY)
					return new ArrayType(t);
				else
					return t;
			}
		}
		
		// it's either a Find* or Create* function that takes some object type specifier as first argument - return that type
		// (FIXME: could be triggered for functions which don't actually return objects matching the type passed as first argument)
		if (params != null && params.length >= 1 && d instanceof C4Function && ((C4Function)d).getReturnType() == C4Type.OBJECT && (declarationName.startsWith("Create") || declarationName.startsWith("Find"))) { //$NON-NLS-1$ //$NON-NLS-2$
			IType t = params[0].getType(context);
			if (t instanceof C4ObjectType)
				return ((C4ObjectType)t).getType();
		}
		
		// GetID() for this
		if (params.length == 0 && d == getCachedFuncs(context).GetID) { //$NON-NLS-1$
			IType t = getPredecessorInSequence() == null ? context.getContainerObject() : getPredecessorInSequence().getType(context);
			if (t instanceof C4Object)
				return ((C4Object)t).getObjectType();
		}

		// function that does not return a reference: return typeset out of object type and generic type
		if (d instanceof C4Function && ((C4Function)d).getReturnType() != C4Type.REFERENCE) {
			C4Function function = (C4Function) d;
			return function.getCombinedType();
		}
		else
			return super.getType(context);
	}
	@Override
	public boolean isValidInSequence(ExprElm elm, C4ScriptParser context) {
		return super.isValidInSequence(elm, context) || elm instanceof MemberOperator;	
	}
	@Override
	public C4Declaration obtainDeclaration(C4ScriptParser parser) {
		if (declarationName.equals(Keywords.Return))
			return null;
		if (declarationName.equals(Keywords.Inherited) || declarationName.equals(Keywords.SafeInherited)) {
			C4Function activeFunc = parser.getActiveFunc();
			return activeFunc != null ? activeFunc.getInherited() : null;
		}
		ExprElm p = getPredecessorInSequence();
		C4ScriptBase lookIn = p == null ? parser.getContainer() : p.guessObjectType(parser);
		if (lookIn != null) {
			FindDeclarationInfo info = new FindDeclarationInfo(parser.getContainer().getIndex());
			info.setSearchOrigin(parser.getContainer());
			C4Declaration field = lookIn.findFunction(declarationName, info);
			// might be a variable called as a function (not after '->')
			if (field == null && p == null)
				field = lookIn.findVariable(declarationName, info);
			return field;
		} else if (p != null) {
			// find global function
			C4Declaration declaration = parser.getContainer().getIndex().findGlobalFunction(declarationName);
			if (declaration == null)
				declaration = parser.getContainer().getIndex().getEngine().findFunction(declarationName);

			// only return found declaration if it's the only choice 
			if (declaration != null) {
				List<C4Declaration> allFromLocalIndex = parser.getContainer().getIndex().getDeclarationMap().get(declarationName);
				C4Declaration decl = parser.getContainer().getEngine().findLocalFunction(declarationName, false);
				if (
						(allFromLocalIndex != null ? allFromLocalIndex.size() : 0) +
						(decl != null ? 1 : 0) == 1
				)
					return declaration;
			}
		}
		return null;
	}
	@Override
	public void reportErrors(final C4ScriptParser context) throws ParsingException {
		super.reportErrors(context);
		
		// notify parser about unnamed parameter usage
		if (declaration == getCachedFuncs(context).Par) {
			if (params.length > 0) {
				context.unnamedParamaterUsed(params[0]);
			}
			else
				context.unnamedParamaterUsed(NumberLiteral.ZERO);
		}
		// return as function
		else if (declarationName.equals(Keywords.Return)) {
			if (context.getStrictLevel() >= 2)
				context.errorWithCode(ParserErrorCode.ReturnAsFunction, this, true);
			else
				context.warningWithCode(ParserErrorCode.ReturnAsFunction, this);
		}
		else {
			
			// inherited/_inherited not allowed in non-strict mode
			if (context.getStrictLevel() <= 0) {
				if (declarationName.equals(Keywords.Inherited) || declarationName.equals(Keywords.SafeInherited)) {
					context.errorWithCode(ParserErrorCode.InheritedDisabledInStrict0, this);
				}
			}
			
			// variable as function
			if (declaration instanceof C4Variable) {
				if (params.length == 0) {
					// no warning when in #strict mode
					if (context.getStrictLevel() >= 2)
						context.warningWithCode(ParserErrorCode.VariableCalled, this, declaration.getName());
				} else {
					context.errorWithCode(ParserErrorCode.VariableCalled, this, true, declaration.getName());
				}
			}
			else if (declaration instanceof C4Function) {
				C4Function f = (C4Function)declaration;
				if (f.getVisibility() == C4FunctionScope.GLOBAL) {
					context.getContainer().addUsedProjectScript(f.getScript());
				}
				int givenParam = 0;
				boolean specialCaseHandled = false;
				// yay for special cases ~ blörgs, don't bother
				/*CachedEngineFuncs cachedFuncs = getCachedFuncs(context);
				if (params.length >= 3 &&  (f == cachedFuncs.AddCommand || f == cachedFuncs.AppendCommand || f == cachedFuncs.SetCommand)) {
					// look if command is "Call"; if so treat parms 2, 3, 4 as any
					Object command = params[1].evaluateAtParseTime(context.getContainer());
					if (command instanceof String && command.equals("Call")) { //$NON-NLS-1$
						for (C4Variable parm : f.getParameters()) {
							if (givenParam >= params.length)
								break;
							ExprElm given = params[givenParam++];
							if (given == null)
								continue;
							IType parmType = givenParam >= 2 && givenParam <= 4 ? C4Type.ANY : parm.getType();
							if (!given.validForType(parmType, context))
								context.warningWithCode(ParserErrorCode.IncompatibleTypes, given, parmType, given.getType(context));
							else
								given.expectedToBeOfType(parmType, context);
						}
						specialCaseHandled = true;
					}
				}*/
				
				// another one: Schedule ... parse passed expression and check it's correctness
				if (!specialCaseHandled && params.length >= 1 && f.getName().equals("Schedule")) {
					IType objType = params.length >= 4 ? params[3].getType(context) : context.getContainerObject();
					C4ScriptBase script = objType != null ? C4TypeSet.objectIngredient(objType) : null;
					if (script == null)
						script = context.getContainer(); // fallback
					Object scriptExpr = params[0].evaluateAtParseTime(script);
					if (scriptExpr instanceof String) {
						try {
							C4ScriptParser.parseStandaloneStatement((String)scriptExpr, context.getActiveFunc(), null, new IMarkerListener() {
								@Override
								public WhatToDo markerEncountered(ParserErrorCode code, int markerStart, int markerEnd, boolean noThrow, int severity, Object... args) {
									// ignore complaining about missing ';'
									if (code == ParserErrorCode.TokenExpected && args[0].equals(";"))
										return WhatToDo.DropCharges;
									try {
										// pass through to the 'real' script parser
										context.markerWithCode(code, params[0].getExprStart()+1+markerStart, params[0].getExprStart()+1+markerEnd, true, severity, args);
									} catch (ParsingException e) {
										// shouldn't happen
										e.printStackTrace();
									}
									return WhatToDo.PassThrough;
								}
							});
						} catch (ParsingException e) {
							// that on slipped through - pretend nothing happened
						}
					}
				}
				
				// not a special case... check regular parameter types
				if (!specialCaseHandled) {
					for (C4Variable parm : f.getParameters()) {
						if (givenParam >= params.length)
							break;
						ExprElm given = params[givenParam++];
						if (given == null)
							continue;
						if (!given.validForType(parm.getType(), context))
							context.warningWithCode(ParserErrorCode.IncompatibleTypes, given, parm.getType(), given.getType(context));
						else
							given.expectedToBeOfType(parm.getType(), context);
					}
				}
				
				// warn about too many parameters
				// try again, but only for engine functions
				if (f.isEngineDeclaration() && !declarationName.equals(Keywords.SafeInherited) && f.tooManyParameters(actualParmsNum())) {
					context.addLatentMarker(ParserErrorCode.TooManyParameters, this, IMarker.SEVERITY_WARNING, f, f.getParameters().size(), actualParmsNum(), f.getName());
				}
				
			}
			else if (declaration == null && getPredecessorInSequence() == null) {
				if (declarationName.equals(Keywords.Inherited)) {
					C4Function activeFunc = context.getActiveFunc();
					if (activeFunc != null) {
						context.errorWithCode(ParserErrorCode.NoInheritedFunction, getExprStart(), getExprStart()+declarationName.length(), true, context.getActiveFunc().getName(), true);
					} else {
						context.errorWithCode(ParserErrorCode.NotAllowedHere, getExprStart(), getExprStart()+declarationName.length(), true, declarationName);
					}
				}
				// _inherited yields no warning or error
				else if (!declarationName.equals(Keywords.SafeInherited)) {
					context.errorWithCode(ParserErrorCode.UndeclaredIdentifier, getExprStart(), getExprStart()+declarationName.length(), true, declarationName, true);
				}
			}
		}
	}
	public int actualParmsNum() {
		int result = params.length;
		while (result > 0 && params[result-1] instanceof Ellipsis)
			result--;
		return result;
	}
	@Override
	public ExprElm[] getSubElements() {
		return params;
	}
	@Override
	public void setSubElements(ExprElm[] elms) {
		params = elms;
	}
	private boolean isCriteriaSearch() {
		return declaration instanceof C4Function && ((C4Function)declaration).isCriteriaSearch;
	}
	protected BinaryOp applyOperatorTo(C4ScriptParser parser, ExprElm[] parms, C4ScriptOperator operator) throws CloneNotSupportedException {
		BinaryOp op = new BinaryOp(operator);
		BinaryOp result = op;
		for (int i = 0; i < parms.length; i++) {
			ExprElm one = parms[i].optimize(parser);
			ExprElm two = i+1 < parms.length ? parms[i+1] : null;
			if (op.getLeftSide() == null)
				op.setLeftSide(one);
			else if (two == null) {
				op.setRightSide(one);
			}
			else {
				BinaryOp nu = new BinaryOp(operator);
				op.setRightSide(nu);
				nu.setLeftSide(one);
				op = nu;
			}
		}
		return result;
	}
	@Override
	public ExprElm optimize(C4ScriptParser parser) throws CloneNotSupportedException {

		// And(ugh, blugh) -> ugh && blugh
		C4ScriptOperator replOperator = C4ScriptOperator.oldStyleFunctionReplacement(declarationName);
		if (replOperator != null && params.length == 1) {
			// LessThan(x) -> x < 0
			if (replOperator.getNumArgs() == 2)
				return new BinaryOp(replOperator, params[0].optimize(parser), NumberLiteral.ZERO);
			ExprElm n = params[0].optimize(parser);
			if (n instanceof BinaryOp)
				n = new Parenthesized(n);
			return new UnaryOp(replOperator, replOperator.isPostfix() ? UnaryOp.Placement.Postfix : UnaryOp.Placement.Prefix, n);
		}
		if (replOperator != null && params.length >= 2) {
			return applyOperatorTo(parser, params, replOperator);
		}

		// ObjectCall(ugh, "UghUgh", 5) -> ugh->UghUgh(5)
		if (params.length >= 2 && declaration == getCachedFuncs(parser).ObjectCall && params[1] instanceof StringLiteral && (Conf.alwaysConvertObjectCalls || !this.containedInLoopHeaderOrNotStandaloneExpression()) && !params[0].hasSideEffects()) {
			ExprElm[] parmsWithoutObject = new ExprElm[params.length-2];
			for (int i = 0; i < parmsWithoutObject.length; i++)
				parmsWithoutObject[i] = params[i+2].optimize(parser);
			String lit = ((StringLiteral)params[1]).stringValue();
			if (lit.length() > 0 && lit.charAt(0) != '~') {
				return Conf.alwaysConvertObjectCalls && this.containedInLoopHeaderOrNotStandaloneExpression()
					? new Sequence(new ExprElm[] {
							params[0].optimize(parser),
							new MemberOperator(false, true, null, 0),
							new CallFunc(((StringLiteral)params[1]).stringValue(), parmsWithoutObject)}
					)
					: new IfStatement(params[0].optimize(parser),
							new SimpleStatement(new Sequence(new ExprElm[] {
									params[0].optimize(parser),
									new MemberOperator(false, true, null, 0),
									new CallFunc(((StringLiteral)params[1]).stringValue(), parmsWithoutObject)}
							)),
							null
					);
			}
		}

		// OCF_Awesome() -> OCF_Awesome
		if (params.length == 0 && declaration instanceof C4Variable) {
			return new AccessVar(declarationName);
		}

		// also check for not-nullness since in OC Var/Par are gone and declaration == ...Par returns true -.-
		
		// Par(5) -> nameOfParm6
		if (params.length <= 1 && declaration != null && declaration == getCachedFuncs(parser).Par && (params.length == 0 || params[0] instanceof NumberLiteral)) {
			NumberLiteral number = params.length > 0 ? (NumberLiteral) params[0] : NumberLiteral.ZERO;
			C4Function activeFunc = parser.getActiveFunc();
			if (activeFunc != null) {
				if (number.intValue() >= 0 && number.intValue() < activeFunc.getParameters().size() && activeFunc.getParameters().get(number.intValue()).isActualParm())
					return new AccessVar(parser.getActiveFunc().getParameters().get(number.intValue()).getName());
			}
		}
		
		// SetVar(5, "ugh") -> Var(5) = "ugh"
		if (params.length == 2 && declaration != null && (declaration == getCachedFuncs(parser).SetVar || declaration == getCachedFuncs(parser).SetLocal || declaration == getCachedFuncs(parser).AssignVar)) {
			return new BinaryOp(C4ScriptOperator.Assign, new CallFunc(declarationName.substring(declarationName.equals("AssignVar") ? "Assign".length() : "Set".length()), params[0].optimize(parser)), params[1].optimize(parser)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		// DecVar(0) -> Var(0)--
		if (params.length <= 1 && declaration != null && (declaration == getCachedFuncs(parser).DecVar || declaration == getCachedFuncs(parser).IncVar)) {
			return new UnaryOp(declaration == getCachedFuncs(parser).DecVar ? C4ScriptOperator.Decrement : C4ScriptOperator.Increment, Placement.Prefix,
					new CallFunc(getCachedFuncs(parser).Var.getName(), new ExprElm[] {
						params.length == 1 ? params[0].optimize(parser) : NumberLiteral.ZERO
					})
			);
		}

		// Call("Func", 5, 5) -> Func(5, 5)
		if (params.length >= 1 && declaration != null && declaration == getCachedFuncs(parser).Call && params[0] instanceof StringLiteral) {
			String lit = ((StringLiteral)params[0]).stringValue();
			if (lit.length() > 0 && lit.charAt(0) != '~') {
				ExprElm[] parmsWithoutName = new ExprElm[params.length-1];
				for (int i = 0; i < parmsWithoutName.length; i++)
					parmsWithoutName[i] = params[i+1].optimize(parser);
				return new CallFunc(((StringLiteral)params[0]).stringValue(), parmsWithoutName);
			}
		}

		return super.optimize(parser);
	}

	private boolean containedInLoopHeaderOrNotStandaloneExpression() {
		SimpleStatement simpleStatement = null;
		for (ExprElm p = getParent(); p != null; p = p.getParent()) {
			if (p instanceof Block)
				break;
			if (p instanceof ILoop) {
				if (simpleStatement != null && simpleStatement == ((ILoop)p).getBody())
					return false;
				return true;
			}
			if (!(p instanceof SimpleStatement))
				return true;
			else
				simpleStatement = (SimpleStatement) p;
		} 
		return false;
	}

	@Override
	public DeclarationRegion declarationAt(int offset, C4ScriptParser parser) {
		return new DeclarationRegion(getDeclaration(parser), new Region(getExprStart(), declarationName.length()));
	}

	public C4Object searchCriteriaAssumedResult(C4ScriptParser context) {
		C4Object result = null;
		// parameters to FindObjects itself are also &&-ed together
		if (declarationName.equals("Find_And") || isCriteriaSearch()) { //$NON-NLS-1$
			for (ExprElm parm : params) {
				if (parm instanceof CallFunc) {
					CallFunc call = (CallFunc)parm;
					C4Object t = call.searchCriteriaAssumedResult(context);
					if (t != null) {
						if (result == null)
							result = t;
						else {
							if (t.includes(result))
								result = t;
						}
					}
				}
			}
		}
		else if (declarationName.equals("Find_ID")) { //$NON-NLS-1$
			if (params.length > 0) {
				result = params[0].guessObjectType(context);
			}
		}
		return result;
	}
	public ExprElm getReturnArg() {
		if (params.length == 1)
			return params[0];
		return new Tuple(params);
	}
	@Override
	public ControlFlow getControlFlow() {
		return declarationName.equals(Keywords.Return) ? ControlFlow.Return : super.getControlFlow();
	}
	public ExprElm[] getParams() {
		return params;
	}
	public int indexOfParm(ExprElm parm) {
		for (int i = 0; i < params.length; i++)
			if (params[i] == parm)
				return i;
		return -1;
	}
	@Override
	public IStoredTypeInformation createStoredTypeInformation(C4ScriptParser parser) {
		C4Declaration d = getDeclaration();
		CachedEngineFuncs cache = getCachedFuncs(parser);
		if (Utilities.isAnyOf(d, cache.Var, cache.Local, cache.Par)) {
			Object ev;
			if (getParams().length == 1 && (ev = getParams()[0].evaluateAtParseTime(parser.getContainer())) != null) {
				if (ev instanceof Number) {
					// Var() with a sane constant number
					return new VarFunctionsTypeInformation((C4Function) d, ((Number)ev).intValue());
				}
			}
		}
		else if (d instanceof C4Function) {
			C4Function f = (C4Function) d;
			IType retType = f.getReturnType();
			if (retType == null || !(retType.containsAnyTypeOf(C4Type.ANY, C4Type.REFERENCE)))
				return new FunctionReturnTypeInformation((C4Function)d);
		}
		return super.createStoredTypeInformation(parser);
	}
	
	@Override
	public Object evaluate(IEvaluationContext context) {
	    if (declaration instanceof C4Function) {
	    	Object[] args = Utilities.map(getParams(), Object.class, Conf.EVALUATE_EXPR);
	    	return ((C4Function)declaration).invoke(args);
	    }
	    else
	    	return null;
	}
}