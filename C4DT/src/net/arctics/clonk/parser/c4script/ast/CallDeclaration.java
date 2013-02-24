package net.arctics.clonk.parser.c4script.ast;

import static net.arctics.clonk.util.ArrayUtil.iterable;
import static net.arctics.clonk.util.ArrayUtil.set;
import static net.arctics.clonk.util.Utilities.as;

import java.util.Set;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.IIndexEntity;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.ASTNodePrinter;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.EntityRegion;
import net.arctics.clonk.parser.IEvaluationContext;
import net.arctics.clonk.parser.c4script.Conf;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.FunctionType;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.Keywords;
import net.arctics.clonk.parser.c4script.Operator;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.ProblemReportingContext;
import net.arctics.clonk.parser.c4script.SpecialEngineRules;
import net.arctics.clonk.parser.c4script.SpecialEngineRules.SpecialFuncRule;
import net.arctics.clonk.parser.c4script.SpecialEngineRules.SpecialRule;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.ast.UnaryOp.Placement;
import net.arctics.clonk.util.StringUtil;

import org.eclipse.jface.text.Region;

/**
 * An identifier followed by parenthesized parameters. The {@link Declaration} being referenced will more likely be a {@link Function} but may also be a {@link Variable}
 * in which case that variable will probably be typed as {@link FunctionType}/{@link PrimitiveType#FUNCTION}
 * @author madeen
 *
 */
public class CallDeclaration extends AccessDeclaration implements IFunctionCall {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	private ASTNode[] params;
	private int parmsStart, parmsEnd;
	private transient Set<IIndexEntity> potentialDeclarations;

	public Set<IIndexEntity> potentialDeclarations() { return potentialDeclarations; }
	public void setPotentialDeclarations(Set<IIndexEntity> potentialDeclarations)
		{ this.potentialDeclarations = potentialDeclarations; }

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
	public CallDeclaration(String funcName, ASTNode... parms) {
		super(funcName);
		params = parms;
		assignParentToSubElements();
	}

	/**
	 * Create a CallFunc that directly refers to a {@link Function} object.
	 * @param function The {@link Function} the new CallFunc will refer to.
	 * @param parms Parameter expressions
	 */
	public CallDeclaration(Function function, ASTNode... parms) {
		this(function.name());
		this.declaration = function;
		assignParentToSubElements();
	}

	@Override
	public void doPrint(ASTNodePrinter output, int depth) {
		super.doPrint(output, depth);
		printParmString(output, params, depth);
	}

	/**
	 * Print a parameter string.
	 * @param output Output to print to
	 * @param depth Indentation level of parameter expressions.
	 */
	public static void printParmString(ASTNodePrinter output, ASTNode[] params, int depth) {
		StringUtil.writeBlock(output, "(", ")", ", ", iterable(params)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	@Override
	public boolean hasSideEffects() { return true; }

	/**
	 * Return a {@link SpecialFuncRule} applying to {@link CallDeclaration}s with the same name as this one.
	 * @param context Context used to obtain the {@link Engine}, which supplies the pool of {@link SpecialRule}s (see {@link Engine#specialRules()})
	 * @param role Role mask passed to {@link SpecialEngineRules#funcRuleFor(String, int)}
	 * @return The {@link SpecialFuncRule} applying to {@link CallDeclaration}s such as this one, or null.
	 */
	public final SpecialFuncRule specialRuleFromContext(ProblemReportingContext context, int role) {
		Engine engine = context.script().engine();
		if (engine != null && engine.specialRules() != null)
			return engine.specialRules().funcRuleFor(declarationName, role);
		else
			return null;
	}

	@Override
	public boolean isValidInSequence(ASTNode elm) {
		return super.isValidInSequence(elm) || elm instanceof MemberOperator;
	}

	@Override
	public ASTNode[] subElements() {
		return params;
	}
	@Override
	public void setSubElements(ASTNode[] elms) {
		params = elms;
	}
	protected BinaryOp applyOperatorTo(ProblemReportingContext context, ASTNode[] parms, Operator operator) throws CloneNotSupportedException {
		BinaryOp op = new BinaryOp(operator);
		BinaryOp result = op;
		for (int i = 0; i < parms.length; i++) {
			ASTNode one = parms[i].optimize(context);
			ASTNode two = i+1 < parms.length ? parms[i+1] : null;
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
	public ASTNode optimize(ProblemReportingContext context) throws CloneNotSupportedException {

		// And(ugh, blugh) -> ugh && blugh
		Operator replOperator = Operator.oldStyleFunctionReplacement(declarationName);
		if (replOperator != null && params.length == 1) {
			// LessThan(x) -> x < 0
			if (replOperator.numArgs() == 2)
				return new BinaryOp(replOperator, params[0].optimize(context), IntegerLiteral.ZERO);
			ASTNode n = params[0].optimize(context);
			if (n instanceof BinaryOp)
				n = new Parenthesized(n);
			return new UnaryOp(replOperator, replOperator.isPostfix() ? UnaryOp.Placement.Postfix : UnaryOp.Placement.Prefix, n);
		}
		if (replOperator != null && params.length >= 2)
			return applyOperatorTo(context, params, replOperator);

		// ObjectCall(ugh, "UghUgh", 5) -> ugh->UghUgh(5)
		if (params.length >= 2 && declaration == context.cachedEngineDeclarations().ObjectCall && params[1] instanceof StringLiteral && (Conf.alwaysConvertObjectCalls || !this.containedInLoopHeaderOrNotStandaloneExpression()) && !params[0].hasSideEffects()) {
			ASTNode[] parmsWithoutObject = new ASTNode[params.length-2];
			for (int i = 0; i < parmsWithoutObject.length; i++)
				parmsWithoutObject[i] = params[i+2].optimize(context);
			String lit = ((StringLiteral)params[1]).stringValue();
			if (lit.length() > 0 && lit.charAt(0) != '~')
				return Conf.alwaysConvertObjectCalls && this.containedInLoopHeaderOrNotStandaloneExpression()
					? new Sequence(new ASTNode[] {
						params[0].optimize(context),
						new MemberOperator(false, true, null, 0),
						new CallDeclaration(((StringLiteral)params[1]).stringValue(), parmsWithoutObject)}
					)
					: new IfStatement(params[0].optimize(context),
						new SimpleStatement(new Sequence(new ASTNode[] {
							params[0].optimize(context),
							new MemberOperator(false, true, null, 0),
							new CallDeclaration(((StringLiteral)params[1]).stringValue(), parmsWithoutObject)}
						)),
						null
					);
		}

		// OCF_Awesome() -> OCF_Awesome
		if (params.length == 0 && declaration instanceof Variable)
			if (!context.script().engine().settings().supportsProplists && predecessorInSequence() != null)
				return new CallDeclaration("LocalN", new StringLiteral(declarationName)); //$NON-NLS-1$
			else
				return new AccessVar(declarationName);

		// also check for not-nullness since in OC Var/Par are gone and declaration == ...Par returns true -.-

		// Par(5) -> nameOfParm6
		if (params.length <= 1 && declaration != null && declaration == context.cachedEngineDeclarations().Par && (params.length == 0 || params[0] instanceof IntegerLiteral)) {
			IntegerLiteral number = params.length > 0 ? (IntegerLiteral) params[0] : IntegerLiteral.ZERO;
			Function func = this.parentOfType(Function.class);
			if (func != null)
				if (number.intValue() >= 0 && number.intValue() < func.numParameters() && func.parameter(number.intValue()).isActualParm())
					return new AccessVar(parentOfType(Function.class).parameter(number.intValue()).name());
		}

		// SetVar(5, "ugh") -> Var(5) = "ugh"
		if (params.length == 2 && declaration != null && (declaration == context.cachedEngineDeclarations().SetVar || declaration == context.cachedEngineDeclarations().SetLocal || declaration == context.cachedEngineDeclarations().AssignVar))
			return new BinaryOp(Operator.Assign, new CallDeclaration(declarationName.substring(declarationName.equals("AssignVar") ? "Assign".length() : "Set".length()), params[0].optimize(context)), params[1].optimize(context)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		// DecVar(0) -> Var(0)--
		if (params.length <= 1 && declaration != null && (declaration == context.cachedEngineDeclarations().DecVar || declaration == context.cachedEngineDeclarations().IncVar))
			return new UnaryOp(declaration == context.cachedEngineDeclarations().DecVar ? Operator.Decrement : Operator.Increment, Placement.Prefix,
					new CallDeclaration(context.cachedEngineDeclarations().Var.name(), new ASTNode[] {
						params.length == 1 ? params[0].optimize(context) : IntegerLiteral.ZERO
					})
			);

		// Call("Func", 5, 5) -> Func(5, 5)
		if (params.length >= 1 && declaration != null && declaration == context.cachedEngineDeclarations().Call && params[0] instanceof StringLiteral) {
			String lit = ((StringLiteral)params[0]).stringValue();
			if (lit.length() > 0 && lit.charAt(0) != '~') {
				ASTNode[] parmsWithoutName = new ASTNode[params.length-1];
				for (int i = 0; i < parmsWithoutName.length; i++)
					parmsWithoutName[i] = params[i+1].optimize(context);
				return new CallDeclaration(((StringLiteral)params[0]).stringValue(), parmsWithoutName);
			}
		}

		return super.optimize(context);
	}

	private boolean containedInLoopHeaderOrNotStandaloneExpression() {
		SimpleStatement simpleStatement = null;
		for (ASTNode p = parent(); p != null; p = p.parent()) {
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
	public EntityRegion entityAt(int offset, ProblemReportingContext context) {
		Set<? extends IIndexEntity> entities = potentialDeclarations != null ? potentialDeclarations : set(declaration());
		return new EntityRegion(entities, new Region(start(), name().length()));
	}
	public ASTNode soleParm() {
		if (params.length == 1)
			return params[0];
		return new Tuple(params);
	}
	@Override
	public ControlFlow controlFlow() {
		return name().equals(Keywords.Return) ? ControlFlow.Return : super.controlFlow();
	}
	@Override
	public ASTNode[] params() {
		return params;
	}
	@Override
	public int indexOfParm(ASTNode parm) {
		for (int i = 0; i < params.length; i++)
			if (params[i] == parm)
				return i;
		return -1;
	}
	public Variable parmDefinitionForParmExpression(ASTNode parm) {
		if (declaration instanceof Function) {
			Function f = (Function) declaration;
			int i = indexOfParm(parm);
			return i >= 0 && i < f.numParameters() ? f.parameter(i) : null;
		} else
			return null;
	}

	@Override
	public Object evaluate(IEvaluationContext context) {
	    if (declaration instanceof Function) {
	    	Object[] args = new Object[params().length];
	    	for (int i = 0; i < args.length; i++)
				try {
					args[i] = params()[i] != null ? params()[i].evaluate(context) : null;
				} catch (ControlFlowException e) {
					args[i] = null;
					e.printStackTrace();
				}
	    	Function f = (Function)declaration;
			return f.invoke(f.new FunctionInvocation(args, context, context != null ? context.cookie() : null));
	    }
	    else
	    	return null;
	}

	@Override
	public Class<? extends Declaration> declarationClass() {
		return Function.class;
	}

	@Override
	public Function quasiCalledFunction(ProblemReportingContext context) {
		if (declaration instanceof Variable)
			for (IType type : ((Variable)declaration).type())
				if (type instanceof FunctionType)
					return ((FunctionType)type).prototype();
		return function();
	}

	public final Function function(ProblemReportingContext context) {
		return as(context.obtainDeclaration(this), Function.class);
	}

	public final Function function() {
		return as(declaration, Function.class);
	}

	@Override
	public IType concreteParameterType(Variable parameter, ProblemReportingContext context) {
		if (declaration instanceof Function) {
			Function f = (Function)declaration;
			int ndx = f.parameters().indexOf(parameter);
			if (ndx != -1 && ndx < params.length)
				return context.typeOf(params[ndx]);
		}
		return PrimitiveType.UNKNOWN;
	}

}