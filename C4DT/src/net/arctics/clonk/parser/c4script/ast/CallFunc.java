package net.arctics.clonk.parser.c4script.ast;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.CachedEngineFuncs;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.DeclarationRegion;
import net.arctics.clonk.parser.IHasIncludes;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.ConstrainedProplist;
import net.arctics.clonk.parser.c4script.DeclarationObtainmentContext;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.IHasConstraint;
import net.arctics.clonk.parser.c4script.ScriptBase;
import net.arctics.clonk.parser.c4script.Operator;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.SpecialScriptRules.SpecialRule;
import net.arctics.clonk.parser.c4script.TypeSet;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.FindDeclarationInfo;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.Keywords;
import net.arctics.clonk.parser.c4script.SpecialScriptRules;
import net.arctics.clonk.parser.c4script.Function.FunctionScope;
import net.arctics.clonk.parser.c4script.IHasConstraint.ConstraintKind;
import net.arctics.clonk.parser.c4script.SpecialScriptRules.SpecialFuncRule;
import net.arctics.clonk.parser.c4script.ast.UnaryOp.Placement;
import net.arctics.clonk.parser.c4script.ast.evaluate.IEvaluationContext;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.Region;

/**
 * A function call.
 * @author madeen
 *
 */
public class CallFunc extends AccessDeclaration {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	private final static class FunctionReturnTypeInformation extends StoredTypeInformation {
		private Function function;

		public FunctionReturnTypeInformation(Function function) {
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
			if (type == PrimitiveType.UNKNOWN)
				super.storeType(type);
		}
		
		@Override
		public boolean storesTypeInformationFor(ExprElm expr, C4ScriptParser parser) {
			if (expr instanceof CallFunc) {
				CallFunc callFunc = (CallFunc) expr;
				if (callFunc.getDeclaration() == this.function)
					return true;
			}
			return false;
		}
		
		@Override
		public boolean refersToSameExpression(IStoredTypeInformation other) {
			return other instanceof CallFunc.FunctionReturnTypeInformation && ((CallFunc.FunctionReturnTypeInformation)other).function == this.function;
		}
		
		@Override
		public String toString() {
			return "function " + function + " " + super.toString();
		}
		
		@Override
		public void apply(boolean soft, C4ScriptParser parser) {
			if (function == null)
				return;
			function = (Function) function.latestVersion();
			if (!soft && !function.isEngineDeclaration()) {
				function.forceType(getType());
			}
		}
		
	}
	
	private final static class VarFunctionsTypeInformation extends StoredTypeInformation {
		private Function varFunction;
		private long varIndex;

		private VarFunctionsTypeInformation(Function function, long val) {
			varFunction = function;
			varIndex = val;
		}

		public boolean storesTypeInformationFor(ExprElm expr, C4ScriptParser parser) {
			if (expr instanceof CallFunc) {
				CallFunc callFunc = (CallFunc) expr;
				Object ev;
				return
					callFunc.getDeclaration() == varFunction &&
					callFunc.getParams().length == 1 && // don't bother with more complex cases
					callFunc.getParams()[0].getType(parser) == PrimitiveType.INT &&
					((ev = callFunc.getParams()[0].evaluateAtParseTime(parser.getCurrentFunc())) != null) &&
					ev.equals(varIndex);
			}
			return false;
		}

		public boolean refersToSameExpression(IStoredTypeInformation other) {
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

	@Override
	protected void offsetExprRegion(int amount, boolean start, boolean end) {
		super.offsetExprRegion(amount, start, end);
		if (start) {
			parmsStart += amount;
			parmsEnd += amount;
		}
	}
	
	/**
	 * Set the region containing the parameters.
	 * @param start Start of the region
	 * @param end End of the region
	 */
	public void setParmsRegion(int start, int end) {
		parmsStart = start;
		parmsEnd   = end;
	}

	/**
	 * Return the start offset of the parameters region.
	 * @return The start offset
	 */
	public int getParmsStart() {
		return parmsStart;
	}

	/**
	 * Return the end offset of the parameters region.
	 * @return The end offset
	 */
	public int getParmsEnd() {
		return parmsEnd;
	}

	/**
	 * Create a CallFunc with a function name and parameter expressions.
	 * @param funcName The function name
	 * @param parms Parameter expressions
	 */
	public CallFunc(String funcName, ExprElm... parms) {
		super(funcName);
		params = parms;
		assignParentToSubElements();
	}
	
	/**
	 * Create a CallFunc that directly refers to a {@link Function} object. 
	 * @param function The {@link Function} the new CallFunc will refer to.
	 * @param parms Parameter expressions
	 */
	public CallFunc(Function function, ExprElm... parms) {
		this(function.getName());
		this.declaration = function;
		assignParentToSubElements();
	}
	
	@Override
	public void doPrint(ExprWriter output, int depth) {
		super.doPrint(output, depth);
		printParmString(output, depth);
	}

	/**
	 * Print a parameter string.
	 * @param output Output to print to
	 * @param depth Indentation level of parameter expressions.
	 */
	public void printParmString(ExprWriter output, int depth) {
		output.append("("); //$NON-NLS-1$
		if (params != null) {
			for (int i = 0; i < params.length; i++) {
				if (params[i] != null)
					params[i].print(output, depth);
				if (i < params.length-1)
					output.append(", "); //$NON-NLS-1$
			}
		}
		output.append(")"); //$NON-NLS-1$
	}
	@Override
	public boolean modifiable(C4ScriptParser context) {
		IType t = getType(context);
		return t.canBeAssignedFrom(TypeSet.REFERENCE_OR_ANY_OR_UNKNOWN);
	}
	@Override
	public boolean hasSideEffects() {
		return true;
	}
	
	/**
	 * Return a {@link SpecialFuncRule} applying to {@link CallFunc}s with the same name as this one.
	 * @param context Context used to obtain the {@link Engine}, which supplies the pool of {@link SpecialRule}s (see {@link Engine#getSpecialScriptRules()})
	 * @param role Role mask passed to {@link SpecialScriptRules#getFuncRuleFor(String, int)}
	 * @return The {@link SpecialFuncRule} applying to {@link CallFunc}s such as this one, or null.
	 */
	public SpecialFuncRule getSpecialRule(DeclarationObtainmentContext context, int role) {
		Engine engine = context.getContainer().getEngine();
		if (engine != null && engine.getSpecialScriptRules() != null) {
			return engine.getSpecialScriptRules().getFuncRuleFor(declarationName, role);
		} else {
			return null;
		}
	}
	
	@Override
	protected IType callerType(DeclarationObtainmentContext context) {
		if (getPredecessorInSequence() != null)
			return getPredecessorInSequence().getType(context);
		else
			return super.callerType(context);
	}
	
	@Override
	protected IType obtainType(DeclarationObtainmentContext context) {
		Declaration d = getDeclaration(context);
		
		// look for gathered type information
		IType stored = context.queryTypeOfExpression(this, null);
		if (stored != null)
			return stored;
		
		// calling this() as function -> return object type belonging to script
		if (params.length == 0 && (d == getCachedFuncs(context).This || d == Variable.THIS)) {
			Definition obj = context.getContainerAsDefinition();
			if (obj != null)
				return obj;
		}

		// Some special rule applies and the return type is set accordingly
		SpecialFuncRule rule = getSpecialRule(context, SpecialScriptRules.RETURNTYPE_MODIFIER);
		if (rule != null) {
			IType returnType = rule.returnType(context, this);
			if (returnType != null) {
				return returnType;
			}
		}
		
		if (d instanceof Function) {
			return ((Function)d).getReturnType();
		}

		return super.obtainType(context);
	}
	@Override
	public boolean isValidInSequence(ExprElm elm, C4ScriptParser context) {
		return super.isValidInSequence(elm, context) || elm instanceof MemberOperator;	
	}
	
	private transient boolean multiplePotentialDeclarations;
	
	@Override
	public Declaration obtainDeclaration(DeclarationObtainmentContext context) {
		super.obtainDeclaration(context);
		List<Declaration> decs = new LinkedList<Declaration>();
		_obtainDeclaration(decs, context);
		return decs.size() > 0 ? decs.get(0) : null;
	}

	protected void _obtainDeclaration(Collection<Declaration> decs, DeclarationObtainmentContext context) {
		if (declarationName.equals(Keywords.Return))
			return;
		if (declarationName.equals(Keywords.Inherited) || declarationName.equals(Keywords.SafeInherited)) {
			Function activeFunc = context.getCurrentFunc();
			if (activeFunc != null) {
				Function inher = activeFunc.getInherited();
				if (inher != null)
					decs.add(inher);
				return;
			}
		}
		ExprElm p = getPredecessorInSequence();
		findFunctionUsingPredecessor(p, declarationName, context, decs);
		multiplePotentialDeclarations = decs.size() > 1;
	}

	/**
	 * Find a {@link Function} for some hypothetical {@link CallFunc}, using contextual information such as the {@link ExprElm#getType(DeclarationObtainmentContext)} of the {@link ExprElm} preceding this {@link CallFunc} in the {@link Sequence}.
	 * @param p The predecessor of the hypothetical {@link CallFunc} ({@link ExprElm#getPredecessorInSequence()})
	 * @param functionName Name of the function to look for. Would correspond to the hypothetical {@link CallFunc}'s {@link #getDeclarationName()}
	 * @param context Context to use for searching
	 * @param listToAddPotentialDeclarationsTo When supplying a non-null value to this parameter, potential declarations will be added to the collection. Such potential declarations would be obtained by querying the {@link Index}'s {@link Index#declarationMap()}.
	 * @return The {@link Function} that is very likely to be the one actually intended to be referenced by the hypothetical {@link CallFunc}.
	 */
	public static Declaration findFunctionUsingPredecessor(ExprElm p, String functionName, DeclarationObtainmentContext context, Collection<Declaration> listToAddPotentialDeclarationsTo) {
		IType lookIn = p == null ? context.getContainer() : p.getType(context);
		if (lookIn != null) for (IType ty : lookIn) {
			if (!(ty instanceof ScriptBase))
				continue;
			ScriptBase script = (ScriptBase)ty;
			FindDeclarationInfo info = new FindDeclarationInfo(context.getContainer().getIndex());
			info.setSearchOrigin(context.getContainer());
			Declaration dec = script.findFunction(functionName, info);
			// parse function before this one
			if (dec != null && context.getCurrentFunc() != null) {
				try {
					context.parseCodeOfFunction((Function) dec, true);
				} catch (ParsingException e) {
					e.printStackTrace();
				}
			}
			// might be a variable called as a function (not after '->')
			if (dec == null && p == null)
				dec = script.findVariable(functionName, info);
			if (dec != null) {
				if (listToAddPotentialDeclarationsTo == null)
					return dec;
				else
					listToAddPotentialDeclarationsTo.add(dec);
			}
		}
		if (p != null) {
			// find global function
			Declaration declaration;
			try {
				declaration = context.getContainer().getIndex().findGlobal(Function.class, functionName);
			} catch (Exception e) {
				e.printStackTrace();
				if (context == null)
					System.out.println("No context");
				if (context.getContainer() == null)
					System.out.println("No container");
				if (context.getContainer().getIndex() == null)
					System.out.println("No index");
				return null;
			}
			// find engine function
			if (declaration == null)
				declaration = context.getContainer().getIndex().getEngine().findFunction(functionName);

			List<Declaration> allFromLocalIndex = context.getContainer().getIndex().declarationMap().get(functionName);
			Declaration decl = context.getContainer().getEngine().findLocalFunction(functionName, false);
			int numCandidates = 0;
			if (allFromLocalIndex != null)
				numCandidates += allFromLocalIndex.size();
			if (decl != null)
				numCandidates++;
			
			// only return found global function if it's the only choice 
			if (declaration != null && numCandidates == 1) {
				if (listToAddPotentialDeclarationsTo == null)
					return declaration;
				else
					listToAddPotentialDeclarationsTo.add(declaration);
			} else {
				if (listToAddPotentialDeclarationsTo != null && allFromLocalIndex != null)
					listToAddPotentialDeclarationsTo.addAll(allFromLocalIndex);
			}
		}
		if ((lookIn == PrimitiveType.ANY || lookIn == PrimitiveType.UNKNOWN) && listToAddPotentialDeclarationsTo != null) {
			List<IType> typesWithThatMember = new LinkedList<IType>();
			for (Declaration d : listToAddPotentialDeclarationsTo)
				if (!d.isGlobal() && d instanceof Function && d.getParentDeclaration() instanceof IHasIncludes)
					typesWithThatMember.add(new ConstrainedProplist((IHasIncludes)d.getParentDeclaration(), ConstraintKind.Includes));
			if (typesWithThatMember.size() > 0) {
				IType ty = TypeSet.create(typesWithThatMember);
				ty.setTypeDescription(String.format(Messages.AccessDeclaration_TypesSporting, functionName));
				p.expectedToBeOfType(ty, (C4ScriptParser) context, TypeExpectancyMode.Force);
			}
		}
		return listToAddPotentialDeclarationsTo != null && listToAddPotentialDeclarationsTo.size() > 0 ? listToAddPotentialDeclarationsTo.iterator().next() : null;
	}
	private boolean unknownFunctionShouldBeError(C4ScriptParser parser) {
		ExprElm pred = getPredecessorInSequence();
		if (pred == null)
			return true;
		IType predType = pred.getType(parser);
		if (predType == null)
			return false;
		if (pred instanceof MemberOperator)
			if (((MemberOperator)pred).hasTilde())
				return false;
		boolean anythingNonPrimitive = false;
		for (IType t : predType) {
			if (t instanceof PrimitiveType)
				continue;
			if (!(t instanceof IHasConstraint))
				return false;
			else {
				IHasConstraint hasConstraint = (IHasConstraint) t;
				anythingNonPrimitive = true;
				// something resolved to something less specific than a ScriptBase? drop
				if (!(hasConstraint.resolve(parser, callerType(parser)) instanceof ScriptBase))
					return false;
			}
		}
		return anythingNonPrimitive;
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
				context.errorWithCode(ParserErrorCode.ReturnAsFunction, this, C4ScriptParser.NO_THROW);
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
			
			if (multiplePotentialDeclarations)
				return; // pfft, no checking
			
			// variable as function
			if (declaration instanceof Variable) {
				if (params.length == 0) {
					// no warning when in #strict mode
					if (context.getStrictLevel() >= 2)
						context.warningWithCode(ParserErrorCode.VariableCalled, this, declaration.getName());
				} else {
					context.errorWithCode(ParserErrorCode.VariableCalled, this, C4ScriptParser.NO_THROW, declaration.getName());
				}
			}
			else if (declaration instanceof Function) {
				Function f = (Function)declaration;
				if (f.getVisibility() == FunctionScope.GLOBAL || getPredecessorInSequence() != null)
					context.getContainer().addUsedScript(f.getScript());
				boolean specialCaseHandled = false;
				
				SpecialFuncRule rule = this.getSpecialRule(context, SpecialScriptRules.ARGUMENT_VALIDATOR);
				if (rule != null) {
					specialCaseHandled = rule.validateArguments(this, params, context);
				}
				
				// not a special case... check regular parameter types
				if (!specialCaseHandled) {
					int givenParam = 0;
					for (Variable parm : f.getParameters()) {
						if (givenParam >= params.length)
							break;
						ExprElm given = params[givenParam++];
						if (given == null)
							continue;
						if (!given.validForType(parm.getType(), context))
							context.warningWithCode(ParserErrorCode.IncompatibleTypes, given, parm.getType().typeName(false), given.getType(context).typeName(false));
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
			else if (declaration == null) {
				if (unknownFunctionShouldBeError(context)) {
					if (declarationName.equals(Keywords.Inherited)) {
						Function activeFunc = context.getCurrentFunc();
						if (activeFunc != null)
							context.errorWithCode(ParserErrorCode.NoInheritedFunction, getExprStart(), getExprStart()+declarationName.length(), C4ScriptParser.NO_THROW, context.getCurrentFunc().getName(), true);
						else
							context.errorWithCode(ParserErrorCode.NotAllowedHere, getExprStart(), getExprStart()+declarationName.length(), C4ScriptParser.NO_THROW, declarationName);
					}
					// _inherited yields no warning or error
					else if (!declarationName.equals(Keywords.SafeInherited))
						context.errorWithCode(ParserErrorCode.UndeclaredIdentifier, getExprStart(), getExprStart()+declarationName.length(), C4ScriptParser.NO_THROW, declarationName, true);
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
	protected BinaryOp applyOperatorTo(C4ScriptParser parser, ExprElm[] parms, Operator operator) throws CloneNotSupportedException {
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
		Operator replOperator = Operator.oldStyleFunctionReplacement(declarationName);
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
		if (params.length == 0 && declaration instanceof Variable) {
			return new AccessVar(declarationName);
		}

		// also check for not-nullness since in OC Var/Par are gone and declaration == ...Par returns true -.-
		
		// Par(5) -> nameOfParm6
		if (params.length <= 1 && declaration != null && declaration == getCachedFuncs(parser).Par && (params.length == 0 || params[0] instanceof NumberLiteral)) {
			NumberLiteral number = params.length > 0 ? (NumberLiteral) params[0] : NumberLiteral.ZERO;
			Function activeFunc = parser.getCurrentFunc();
			if (activeFunc != null) {
				if (number.intValue() >= 0 && number.intValue() < activeFunc.getParameters().size() && activeFunc.getParameters().get(number.intValue()).isActualParm())
					return new AccessVar(parser.getCurrentFunc().getParameters().get(number.intValue()).getName());
			}
		}
		
		// SetVar(5, "ugh") -> Var(5) = "ugh"
		if (params.length == 2 && declaration != null && (declaration == getCachedFuncs(parser).SetVar || declaration == getCachedFuncs(parser).SetLocal || declaration == getCachedFuncs(parser).AssignVar)) {
			return new BinaryOp(Operator.Assign, new CallFunc(declarationName.substring(declarationName.equals("AssignVar") ? "Assign".length() : "Set".length()), params[0].optimize(parser)), params[1].optimize(parser)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		// DecVar(0) -> Var(0)--
		if (params.length <= 1 && declaration != null && (declaration == getCachedFuncs(parser).DecVar || declaration == getCachedFuncs(parser).IncVar)) {
			return new UnaryOp(declaration == getCachedFuncs(parser).DecVar ? Operator.Decrement : Operator.Increment, Placement.Prefix,
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
		Set<Declaration> list = new HashSet<Declaration>();
		_obtainDeclaration(list, parser);
		return new DeclarationRegion(list, new Region(getExprStart(), declarationName.length()));
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
		Declaration d = getDeclaration();
		CachedEngineFuncs cache = getCachedFuncs(parser);
		if (Utilities.isAnyOf(d, cache.Var, cache.Local, cache.Par)) {
			Object ev;
			if (getParams().length == 1 && (ev = getParams()[0].evaluateAtParseTime(parser.getCurrentFunc())) != null) {
				if (ev instanceof Number) {
					// Var() with a sane constant number
					return new VarFunctionsTypeInformation((Function) d, ((Number)ev).intValue());
				}
			}
		}
		else if (d instanceof Function) {
			Function f = (Function) d;
			if (f.typeIsInvariant()) {
				return null;
			}
			IType retType = f.getReturnType();
			if (retType == null || !retType.containsAnyTypeOf(PrimitiveType.ANY, PrimitiveType.REFERENCE))
				return new FunctionReturnTypeInformation((Function)d);
			if (d.isEngineDeclaration())
				return null;
		}
		return super.createStoredTypeInformation(parser);
	}
	
	@Override
	public Object evaluate(IEvaluationContext context) {
	    if (declaration instanceof Function) {
	    	Object[] args = ArrayUtil.map(getParams(), Object.class, Conf.EVALUATE_EXPR);
	    	return ((Function)declaration).invoke(args);
	    }
	    else
	    	return null;
	}
	
	@Override
	public Class<? extends Declaration> declarationClass() {
		return Function.class;
	}
}