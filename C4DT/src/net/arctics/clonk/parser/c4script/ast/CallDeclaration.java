package net.arctics.clonk.parser.c4script.ast;

import static net.arctics.clonk.util.ArrayUtil.iterable;
import static net.arctics.clonk.util.Utilities.as;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.CachedEngineDeclarations;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.IIndexEntity;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.EntityRegion;
import net.arctics.clonk.parser.IHasIncludes;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.CallReturnType;
import net.arctics.clonk.parser.c4script.Conf;
import net.arctics.clonk.parser.c4script.ConstrainedProplist;
import net.arctics.clonk.parser.c4script.DeclarationObtainmentContext;
import net.arctics.clonk.parser.c4script.FindDeclarationInfo;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.Function.FunctionScope;
import net.arctics.clonk.parser.c4script.FunctionType;
import net.arctics.clonk.parser.c4script.IHasConstraint;
import net.arctics.clonk.parser.c4script.IHasConstraint.ConstraintKind;
import net.arctics.clonk.parser.c4script.IResolvableType;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.Keywords;
import net.arctics.clonk.parser.c4script.Operator;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.parser.c4script.TypeSet;
import net.arctics.clonk.parser.c4script.SpecialEngineRules;
import net.arctics.clonk.parser.c4script.SpecialEngineRules.SpecialFuncRule;
import net.arctics.clonk.parser.c4script.SpecialEngineRules.SpecialRule;
import net.arctics.clonk.parser.c4script.TypeUtil;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.ast.UnaryOp.Placement;
import net.arctics.clonk.parser.c4script.ast.evaluate.IEvaluationContext;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.StringUtil;
import net.arctics.clonk.util.Utilities;

import org.eclipse.jface.text.Region;

/**
 * An identifier followed by parenthesized parameters. The {@link Declaration} being referenced will more likely be a {@link Function} but may also be a {@link Variable}
 * in which case that variable will probably be typed as {@link FunctionType}/{@link PrimitiveType#FUNCTION}
 * @author madeen
 *
 */
public class CallDeclaration extends AccessDeclaration implements IFunctionCall {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	private final static class FunctionReturnTypeInfo extends TypeInfo {
		private Function function;

		public FunctionReturnTypeInfo(Function function) {
			super();
			this.function = function;
			if (function != null)
				type = function.returnType();
		}
		
		@Override
		public void storeType(IType type) {
			// don't store if function.returunType() already specifies a type (including any)
			// this is to prevent cases where for example the result of EffectVar in one instance is
			// used as int and then as something else which leads to an erroneous type incompatibility warning
			if (type == PrimitiveType.UNKNOWN)
				super.storeType(type);
		}
		
		@Override
		public boolean storesTypeInformationFor(ExprElm expr, C4ScriptParser parser) {
			if (expr instanceof CallDeclaration) {
				CallDeclaration callFunc = (CallDeclaration) expr;
				if (callFunc.declaration() == this.function)
					return true;
			}
			return false;
		}
		
		@Override
		public boolean refersToSameExpression(ITypeInfo other) {
			return other instanceof CallDeclaration.FunctionReturnTypeInfo && ((CallDeclaration.FunctionReturnTypeInfo)other).function == this.function;
		}
		
		@Override
		public String toString() {
			return "function " + function + " " + super.toString(); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		@Override
		public void apply(boolean soft, C4ScriptParser parser) {
			if (function == null)
				return;
			function = (Function) function.latestVersion();
			if (!soft && !function.isEngineDeclaration())
				function.assignType(type(), false);
		}
		
	}
	
	private final static class VarFunctionsTypeInfo extends TypeInfo {
		private final Function varFunction;
		private final long varIndex;

		private VarFunctionsTypeInfo(Function function, long val) {
			varFunction = function;
			varIndex = val;
		}

		@Override
		public boolean storesTypeInformationFor(ExprElm expr, C4ScriptParser parser) {
			if (expr instanceof CallDeclaration) {
				CallDeclaration callFunc = (CallDeclaration) expr;
				Object ev;
				return
					callFunc.declaration() == varFunction &&
					callFunc.params().length == 1 && // don't bother with more complex cases
					callFunc.params()[0].type(parser) == PrimitiveType.INT &&
					((ev = callFunc.params()[0].evaluateAtParseTime(parser.currentFunction())) != null) &&
					ev.equals(varIndex);
			} else if (expr instanceof AccessVar) {
				AccessVar accessVar = (AccessVar) expr;
				if (accessVar.declaration() instanceof Variable)
					return (varFunction == parser.cachedEngineDeclarations().Par
						? parser.currentFunction().parameters()
						: parser.currentFunction().localVars()).indexOf(accessVar) == varIndex;
			}
			return false;
		}

		@Override
		public boolean refersToSameExpression(ITypeInfo other) {
			if (other.getClass() == CallDeclaration.VarFunctionsTypeInfo.class) {
				CallDeclaration.VarFunctionsTypeInfo otherInfo = (CallDeclaration.VarFunctionsTypeInfo) other;
				return otherInfo.varFunction == this.varFunction && otherInfo.varIndex == this.varIndex; 
			}
			else
				return false;
		}
		
		@Override
		public String toString() {
			return String.format("%s(%d)", varFunction.name(), varIndex); //$NON-NLS-1$
		}
		@Override
		public boolean local() {
			return true;
		}
	}

	private ExprElm[] params;
	private int parmsStart, parmsEnd;
	private transient IType unresolvedPredecessorType;
	
	public IType unresolvedPredecessorType() {
		return unresolvedPredecessorType;
	}

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
	@Override
	public int parmsStart() {
		return parmsStart;
	}

	/**
	 * Return the end offset of the parameters region.
	 * @return The end offset
	 */
	@Override
	public int parmsEnd() {
		return parmsEnd;
	}

	/**
	 * Create a CallFunc with a function name and parameter expressions.
	 * @param funcName The function name
	 * @param parms Parameter expressions
	 */
	public CallDeclaration(String funcName, ExprElm... parms) {
		super(funcName);
		params = parms;
		assignParentToSubElements();
	}
	
	/**
	 * Create a CallFunc that directly refers to a {@link Function} object. 
	 * @param function The {@link Function} the new CallFunc will refer to.
	 * @param parms Parameter expressions
	 */
	public CallDeclaration(Function function, ExprElm... parms) {
		this(function.name());
		this.declaration = function;
		assignParentToSubElements();
	}
	
	@Override
	public void doPrint(ExprWriter output, int depth) {
		super.doPrint(output, depth);
		printParmString(output, params, depth);
	}

	/**
	 * Print a parameter string.
	 * @param output Output to print to
	 * @param depth Indentation level of parameter expressions.
	 */
	public static void printParmString(ExprWriter output, ExprElm[] params, int depth) {
		StringUtil.writeBlock(output, "(", ")", ", ", iterable(params)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
	
	@Override
	public boolean isModifiable(C4ScriptParser context) {
		IType t = type(context);
		return t.canBeAssignedFrom(TypeChoice.make(PrimitiveType.REFERENCE, PrimitiveType.UNKNOWN));
	}
	@Override
	public boolean hasSideEffects() {
		return true;
	}
	
	/**
	 * Return a {@link SpecialFuncRule} applying to {@link CallDeclaration}s with the same name as this one.
	 * @param context Context used to obtain the {@link Engine}, which supplies the pool of {@link SpecialRule}s (see {@link Engine#specialRules()})
	 * @param role Role mask passed to {@link SpecialEngineRules#funcRuleFor(String, int)}
	 * @return The {@link SpecialFuncRule} applying to {@link CallDeclaration}s such as this one, or null.
	 */
	public final SpecialFuncRule specialRuleFromContext(DeclarationObtainmentContext context, int role) {
		Engine engine = context.script().engine();
		if (engine != null && engine.specialRules() != null)
			return engine.specialRules().funcRuleFor(declarationName, role);
		else
			return null;
	}
	
	@Override
	protected IType callerType(DeclarationObtainmentContext context) {
		if (predecessorInSequence() != null)
			return predecessorInSequence().type(context);
		else
			return super.callerType(context);
	}
	
	@Override
	public IType unresolvedType(DeclarationObtainmentContext context) {
		IType type = declarationType(context);
		if (type instanceof FunctionType)
			return ((FunctionType)type).prototype().returnType();
		else
			return type;
	}

	private IType declarationType(DeclarationObtainmentContext context) {
		Declaration d = declarationFromContext(context);
		
		// look for gathered type information
		IType stored = context.queryTypeOfExpression(this, null);
		if (stored != null)
			return stored;
		
		// calling this() as function -> return object type belonging to script
		if (params.length == 0 && (d == context.cachedEngineDeclarations().This || d == Variable.THIS)) {
			Definition obj = context.definition();
			if (obj != null)
				return obj;
		}

		if (d instanceof Function) {
			// Some special rule applies and the return type is set accordingly
			SpecialFuncRule rule = specialRuleFromContext(context, SpecialEngineRules.RETURNTYPE_MODIFIER);
			if (rule != null)
				return new CallReturnType(this, rule, context.script());
			Function f = (Function)d;
			if (f.returnType() instanceof IResolvableType)
				return new CallReturnType(this, null, context.script());
			else
				return f.returnType();
		}
		if (d instanceof Variable)
			return ((Variable)d).type();

		return super.unresolvedType(context);
	}

	@Override
	public boolean isValidInSequence(ExprElm elm, C4ScriptParser context) {
		return super.isValidInSequence(elm, context) || elm instanceof MemberOperator;	
	}
	
	@Override
	public Declaration obtainDeclaration(DeclarationObtainmentContext context) {
		super.obtainDeclaration(context);
		return _obtainDeclaration(null, context);
	}
	
	private static IType resolveType(IType type, DeclarationObtainmentContext context, IType callerType) {
		return TypeUtil.resolve(type, context, callerType != null ? callerType : context.script());
	}

	protected Declaration _obtainDeclaration(Set<IIndexEntity> list, DeclarationObtainmentContext context) {
		if (declarationName.equals(Keywords.Return))
			return null;
		if (declarationName.equals(Keywords.Inherited) || declarationName.equals(Keywords.SafeInherited)) {
			Function activeFunc = context.currentFunction();
			if (activeFunc != null) {
				Function inher = activeFunc.inheritedFunction();
				if (inher != null) {
					if (list != null)
						list.add(inher);
					return inher;
				}
			}
		}
		unresolvedPredecessorType = this.unresolvedPredecessorType(context);
		ExprElm p = predecessorInSequence();
		if (p instanceof MemberOperator)
			p = p.predecessorInSequence();
		return findFunction(declarationName,
			unresolvedPredecessorType != null
				? resolveType(unresolvedPredecessorType, context, p.unresolvedPredecessorType(context))
				: null, MemberOperator.unforgiving(p), context, list);
	}

	/**
	 * Find a {@link Function} for some hypothetical {@link CallDeclaration}, using contextual information such as the {@link ExprElm#type(DeclarationObtainmentContext)} of the {@link ExprElm} preceding this {@link CallDeclaration} in the {@link Sequence}.
	 * @param pred The predecessor of the hypothetical {@link CallDeclaration} ({@link ExprElm#predecessorInSequence()})
	 * @param functionName Name of the function to look for. Would correspond to the hypothetical {@link CallDeclaration}'s {@link #declarationName()}
	 * @param context Context to use for searching
	 * @param listToAddPotentialDeclarationsTo When supplying a non-null value to this parameter, potential declarations will be added to the collection. Such potential declarations would be obtained by querying the {@link Index}'s {@link Index#declarationMap()}.
	 * @return The {@link Function} that is very likely to be the one actually intended to be referenced by the hypothetical {@link CallDeclaration}.
	 */
	private static Declaration findFunction(String functionName, IType callerType, boolean errorOnUnknown, DeclarationObtainmentContext context, Set<IIndexEntity> listToAddPotentialDeclarationsTo) {
		IType lookIn = callerType != null ? callerType : context.script();
		if (lookIn != null) for (IType ty : lookIn) {
			if (!(ty instanceof IHasConstraint))
				continue;
			Script script = as(((IHasConstraint)ty).constraint(), Script.class);
			if (script == null)
				continue;
			FindDeclarationInfo info = new FindDeclarationInfo(context.script().index());
			info.searchOrigin = context.script();
			info.contextFunction = context.currentFunction();
			info.findGlobalVariables = callerType == null;
			Declaration dec = script.findDeclaration(functionName, info);
			// parse function before this one
			if (dec instanceof Function && context.currentFunction() != null)
				context.reportProblems((Function)dec);
			if (dec != null)
				if (listToAddPotentialDeclarationsTo == null)
					return dec;
				else
					listToAddPotentialDeclarationsTo.add(dec);
		}
		if (callerType != null) {
			// find global function
			Declaration declaration;
			try {
				declaration = context.script().index().findGlobal(Declaration.class, functionName);
			} catch (Exception e) {
				e.printStackTrace();
				if (context.script() == null)
					System.out.println("No container"); //$NON-NLS-1$
				else if (context.script().index() == null)
					System.out.println("No index"); //$NON-NLS-1$
				return null;
			}
			// find engine function
			if (declaration == null)
				declaration = context.script().index().engine().findFunction(functionName);

			List<Declaration> allFromLocalIndex = context.script().index().declarationMap().get(functionName);
			Declaration decl = context.script().engine().findLocalFunction(functionName, false);
			int numCandidates = 0;
			if (allFromLocalIndex != null)
				numCandidates += allFromLocalIndex.size();
			if (decl != null)
				numCandidates++;

			// only return found global function if it's the only choice 
			if (declaration != null && numCandidates == 1)
				if (listToAddPotentialDeclarationsTo == null)
					return declaration;
				else
					listToAddPotentialDeclarationsTo.add(declaration);
		}
		/*if (errorOnUnknown && (lookIn == PrimitiveType.ANY || lookIn == PrimitiveType.UNKNOWN) && listToAddPotentialDeclarationsTo != null) {
			List<IType> typesWithThatMember = new LinkedList<IType>();
			for (Declaration d : ArrayUtil.filteredIterable(listToAddPotentialDeclarationsTo, Declaration.class))
				if (!d.isGlobal() && d instanceof Function && d.parentDeclaration() instanceof IHasIncludes)
					typesWithThatMember.add(new ConstrainedProplist((IHasIncludes)d.parentDeclaration(), ConstraintKind.Includes));
			if (typesWithThatMember.size() > 0) {
				IType ty = TypeSet.create(typesWithThatMember);
				ty.setTypeDescription(String.format(Messages.AccessDeclaration_TypesSporting, functionName));
				if (context instanceof C4ScriptParser)
					pred.expectedToBeOfType(ty, (C4ScriptParser)context, TypeExpectancyMode.Force);
			}
		}*/
		if (listToAddPotentialDeclarationsTo != null && listToAddPotentialDeclarationsTo.size() > 0)
			return ArrayUtil.filteredIterable(listToAddPotentialDeclarationsTo, Declaration.class).iterator().next();
		else
			return null;
	}
	
	private boolean unknownFunctionShouldBeError(C4ScriptParser parser) {
		ExprElm pred = predecessorInSequence();
		// standalone function? always bark!
		if (pred == null)
			return true;
		// not typed? weird
		IType predType = pred.type(parser);
		if (predType == null)
			return false;
		// called via ~? ok
		if (pred instanceof MemberOperator)
			if (((MemberOperator)pred).hasTilde())
				return false;
		// wat
		boolean anythingNonPrimitive = false;
		// allow this->Unknown()
		if (predType instanceof IHasConstraint && ((IHasConstraint)predType).constraintKind() == ConstraintKind.CallerType)
			return false;
		for (IType t : predType) {
			if (t instanceof PrimitiveType)
				continue;
			if (!(t instanceof IHasConstraint))
				return false;
			else {
				IHasConstraint hasConstraint = (IHasConstraint) t;
				anythingNonPrimitive = true;
				// something resolved to something less specific than a ScriptBase? drop
				if (!(hasConstraint.resolve(parser, callerType(parser)) instanceof Script))
					return false;
			}
		}
		return anythingNonPrimitive;
	}
	@Override
	public void reportProblems(final C4ScriptParser context) throws ParsingException {
		super.reportProblems(context);
		
		CachedEngineDeclarations cachedEngineDeclarations = context.cachedEngineDeclarations();
		
		// notify parser about unnamed parameter usage
		if (Utilities.objectsNonNullEqual(declaration, cachedEngineDeclarations.Par)) {
			if (params.length > 0)
				context.unnamedParamaterUsed(params[0]);
			else
				context.unnamedParamaterUsed(LongLiteral.ZERO);
		}
		// return as function
		else if (declarationName.equals(Keywords.Return)) {
			if (context.strictLevel() >= 2)
				context.error(ParserErrorCode.ReturnAsFunction, this, C4ScriptParser.NO_THROW);
			else
				context.warning(ParserErrorCode.ReturnAsFunction, this, 0);
		}
		else {
			
			// inherited/_inherited not allowed in non-strict mode
			if (context.strictLevel() <= 0)
				if (declarationName.equals(Keywords.Inherited) || declarationName.equals(Keywords.SafeInherited))
					context.error(ParserErrorCode.InheritedDisabledInStrict0, this, C4ScriptParser.NO_THROW);
			
			// variable as function
			if (declaration instanceof Variable) {
				((Variable)declaration).setUsed(true);
				IType type = this.declarationType(context);
				// no warning when in #strict mode
				if (context.strictLevel() >= 2)
					if (declaration != cachedEngineDeclarations.This && declaration != Variable.THIS && !PrimitiveType.FUNCTION.canBeAssignedFrom(type))
						context.warning(ParserErrorCode.VariableCalled, this, 0, declaration.name(), type.typeName(false));
			}
			else if (declaration instanceof Function) {
				Function f = (Function)declaration;
				if (f.visibility() == FunctionScope.GLOBAL || predecessorInSequence() != null)
					context.script().addUsedScript(f.script());
				boolean specialCaseHandled = false;
				
				SpecialFuncRule rule = this.specialRuleFromContext(context, SpecialEngineRules.ARGUMENT_VALIDATOR);
				if (rule != null)
					specialCaseHandled = rule.validateArguments(this, params, context);
				
				// not a special case... check regular parameter types
				if (!specialCaseHandled) {
					int givenParam = 0;
					for (Variable parm : f.parameters()) {
						if (givenParam >= params.length)
							break;
						ExprElm given = params[givenParam++];
						if (given == null)
							continue;
						if (!given.validForType(parm.type(), context))
							context.warning(ParserErrorCode.IncompatibleTypes, given, 0, parm.type().typeName(false), given.type(context).typeName(false));
						else
							given.expectedToBeOfType(parm.type(), context);
					}
				}				
			}
			else if (declaration == null)
				if (unknownFunctionShouldBeError(context)) {
					if (declarationName.equals(Keywords.Inherited)) {
						Function activeFunc = context.currentFunction();
						if (activeFunc != null)
							context.error(ParserErrorCode.NoInheritedFunction, start(), start()+declarationName.length(), C4ScriptParser.NO_THROW, context.currentFunction().name(), true);
						else
							context.error(ParserErrorCode.NotAllowedHere, start(), start()+declarationName.length(), C4ScriptParser.NO_THROW, declarationName);
					}
					// _inherited yields no warning or error
					else if (!declarationName.equals(Keywords.SafeInherited))
						context.error(ParserErrorCode.UndeclaredIdentifier, start(), start()+declarationName.length(), C4ScriptParser.NO_THROW, declarationName, true);
				} else if (context.findDefinitionViaCall() && predecessorInSequence() != null) {
					Index index = context.script().index();
					index.loadScriptsContainingDeclarationsNamed(declarationName);
					List<Declaration> decs = index.declarationMap().get(declarationName);
					if (decs != null) {
						IType[] types = new IType[decs.size()];
						for (int i = 0; i < types.length; i++) {
							types[i] = as(decs.get(i).parentDeclaration(), IType.class);
							if (types[i] instanceof IHasIncludes)
								types[i] = new ConstrainedProplist((IHasIncludes) types[i], ConstraintKind.Includes);
						}
						IType typeSet = TypeUnification.unify(types);
						predecessorInSequence().expectedToBeOfType(typeSet, context);
					}
				}
		}
	}
	@Override
	public ExprElm[] subElements() {
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
			if (op.leftSide() == null)
				op.setLeftSide(one);
			else if (two == null)
				op.setRightSide(one);
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
			if (replOperator.numArgs() == 2)
				return new BinaryOp(replOperator, params[0].optimize(parser), LongLiteral.ZERO);
			ExprElm n = params[0].optimize(parser);
			if (n instanceof BinaryOp)
				n = new Parenthesized(n);
			return new UnaryOp(replOperator, replOperator.isPostfix() ? UnaryOp.Placement.Postfix : UnaryOp.Placement.Prefix, n);
		}
		if (replOperator != null && params.length >= 2)
			return applyOperatorTo(parser, params, replOperator);

		// ObjectCall(ugh, "UghUgh", 5) -> ugh->UghUgh(5)
		if (params.length >= 2 && declaration == parser.cachedEngineDeclarations().ObjectCall && params[1] instanceof StringLiteral && (Conf.alwaysConvertObjectCalls || !this.containedInLoopHeaderOrNotStandaloneExpression()) && !params[0].hasSideEffects()) {
			ExprElm[] parmsWithoutObject = new ExprElm[params.length-2];
			for (int i = 0; i < parmsWithoutObject.length; i++)
				parmsWithoutObject[i] = params[i+2].optimize(parser);
			String lit = ((StringLiteral)params[1]).stringValue();
			if (lit.length() > 0 && lit.charAt(0) != '~')
				return Conf.alwaysConvertObjectCalls && this.containedInLoopHeaderOrNotStandaloneExpression()
					? new Sequence(new ExprElm[] {
							params[0].optimize(parser),
							new MemberOperator(false, true, null, 0),
							new CallDeclaration(((StringLiteral)params[1]).stringValue(), parmsWithoutObject)}
					)
					: new IfStatement(params[0].optimize(parser),
							new SimpleStatement(new Sequence(new ExprElm[] {
									params[0].optimize(parser),
									new MemberOperator(false, true, null, 0),
									new CallDeclaration(((StringLiteral)params[1]).stringValue(), parmsWithoutObject)}
							)),
							null
					);
		}

		// OCF_Awesome() -> OCF_Awesome
		if (params.length == 0 && declaration instanceof Variable)
			if (!parser.script().engine().settings().supportsProplists && predecessorInSequence() != null)
				return new CallDeclaration("LocalN", new StringLiteral(declarationName)); //$NON-NLS-1$
			else
				return new AccessVar(declarationName);

		// also check for not-nullness since in OC Var/Par are gone and declaration == ...Par returns true -.-
		
		// Par(5) -> nameOfParm6
		if (params.length <= 1 && declaration != null && declaration == parser.cachedEngineDeclarations().Par && (params.length == 0 || params[0] instanceof LongLiteral)) {
			LongLiteral number = params.length > 0 ? (LongLiteral) params[0] : LongLiteral.ZERO;
			Function activeFunc = parser.currentFunction();
			if (activeFunc != null)
				if (number.intValue() >= 0 && number.intValue() < activeFunc.numParameters() && activeFunc.parameter(number.intValue()).isActualParm())
					return new AccessVar(parser.currentFunction().parameter(number.intValue()).name());
		}
		
		// SetVar(5, "ugh") -> Var(5) = "ugh"
		if (params.length == 2 && declaration != null && (declaration == parser.cachedEngineDeclarations().SetVar || declaration == parser.cachedEngineDeclarations().SetLocal || declaration == parser.cachedEngineDeclarations().AssignVar))
			return new BinaryOp(Operator.Assign, new CallDeclaration(declarationName.substring(declarationName.equals("AssignVar") ? "Assign".length() : "Set".length()), params[0].optimize(parser)), params[1].optimize(parser)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		// DecVar(0) -> Var(0)--
		if (params.length <= 1 && declaration != null && (declaration == parser.cachedEngineDeclarations().DecVar || declaration == parser.cachedEngineDeclarations().IncVar))
			return new UnaryOp(declaration == parser.cachedEngineDeclarations().DecVar ? Operator.Decrement : Operator.Increment, Placement.Prefix,
					new CallDeclaration(parser.cachedEngineDeclarations().Var.name(), new ExprElm[] {
						params.length == 1 ? params[0].optimize(parser) : LongLiteral.ZERO
					})
			);

		// Call("Func", 5, 5) -> Func(5, 5)
		if (params.length >= 1 && declaration != null && declaration == parser.cachedEngineDeclarations().Call && params[0] instanceof StringLiteral) {
			String lit = ((StringLiteral)params[0]).stringValue();
			if (lit.length() > 0 && lit.charAt(0) != '~') {
				ExprElm[] parmsWithoutName = new ExprElm[params.length-1];
				for (int i = 0; i < parmsWithoutName.length; i++)
					parmsWithoutName[i] = params[i+1].optimize(parser);
				return new CallDeclaration(((StringLiteral)params[0]).stringValue(), parmsWithoutName);
			}
		}

		return super.optimize(parser);
	}

	private boolean containedInLoopHeaderOrNotStandaloneExpression() {
		SimpleStatement simpleStatement = null;
		for (ExprElm p = parent(); p != null; p = p.parent()) {
			if (p instanceof Block)
				break;
			if (p instanceof ILoop) {
				if (simpleStatement != null && simpleStatement == ((ILoop)p).body())
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
	public EntityRegion entityAt(int offset, C4ScriptParser parser) {
		Set<IIndexEntity> list = new HashSet<IIndexEntity>();
		_obtainDeclaration(list, parser);
		return new EntityRegion(list, new Region(start(), declarationName.length()));
	}
	public ExprElm soleParm() {
		if (params.length == 1)
			return params[0];
		return new Tuple(params);
	}
	@Override
	public ControlFlow controlFlow() {
		return declarationName.equals(Keywords.Return) ? ControlFlow.Return : super.controlFlow();
	}
	@Override
	public ExprElm[] params() {
		return params;
	}
	@Override
	public int indexOfParm(ExprElm parm) {
		for (int i = 0; i < params.length; i++)
			if (params[i] == parm)
				return i;
		return -1;
	}
	public Variable parmDefinitionForParmExpression(ExprElm parm) {
		if (declaration instanceof Function) {
			Function f = (Function) declaration;
			int i = indexOfParm(parm);
			return i >= 0 && i < f.numParameters() ? f.parameter(i) : null;
		} else
			return null;
	}
	@Override
	public ITypeInfo createStoredTypeInformation(C4ScriptParser parser) {
		Declaration d = declaration();
		CachedEngineDeclarations cache = parser.cachedEngineDeclarations();
		if (d == cache.Var || d == cache.Local || d == cache.Par) {
			Object ev;
			if (params().length == 1 && (ev = params()[0].evaluateAtParseTime(parser.currentFunction())) != null)
				if (ev instanceof Number)
					// Var() with a sane constant number
					return new VarFunctionsTypeInfo((Function) d, ((Number)ev).intValue());
		}
		else if (d instanceof Function) {
			Function f = (Function) d;
			if (f.staticallyTyped())
				return null;
			IType retType = f.returnType();
			if (retType == null)
				return new FunctionReturnTypeInfo((Function)d);
			if (d.isEngineDeclaration())
				return null;
		}
		else if (d != null)
			return new GenericTypeInfo(this, parser);
		return super.createStoredTypeInformation(parser);
	}
	
	@Override
	public Object evaluate(IEvaluationContext context) {
	    if (declaration instanceof Function) {
	    	Object[] args = ArrayUtil.map(params(), Object.class, Conf.EVALUATE_EXPR);
	    	return ((Function)declaration).invoke(args);
	    }
	    else
	    	return null;
	}
	
	@Override
	public Class<? extends Declaration> declarationClass() {
		return Function.class;
	}

	@Override
	public Function quasiCalledFunction(DeclarationObtainmentContext context) {
		if (declaration instanceof Variable)
			for (IType type : ((Variable)declaration).type())
				if (type instanceof FunctionType)
					return ((FunctionType)type).prototype();
		return as(declaration(), Function.class);
	}
	
	public final Function function(DeclarationObtainmentContext context) {
		return as(declarationFromContext(context), Function.class);
	}
	
	public final Function function() {
		return (Function)declaration;
	}
}
