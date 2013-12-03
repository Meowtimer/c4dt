package net.arctics.clonk.c4script.ast;

import static net.arctics.clonk.c4script.Conf.alwaysConvertObjectCalls;
import static net.arctics.clonk.c4script.Conf.printNodeList;
import static net.arctics.clonk.util.ArrayUtil.set;
import static net.arctics.clonk.util.Utilities.as;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodePrinter;
import net.arctics.clonk.ast.ControlFlow;
import net.arctics.clonk.ast.ControlFlowException;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ast.EntityRegion;
import net.arctics.clonk.ast.ExpressionLocator;
import net.arctics.clonk.ast.IEvaluationContext;
import net.arctics.clonk.ast.Sequence;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.Keywords;
import net.arctics.clonk.c4script.Operator;
import net.arctics.clonk.c4script.ProblemReporter;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.c4script.ast.UnaryOp.Placement;
import net.arctics.clonk.c4script.typing.FunctionType;
import net.arctics.clonk.c4script.typing.PrimitiveType;
import net.arctics.clonk.index.EngineFunction;

import org.eclipse.jface.text.Region;

/**
 * An identifier followed by parenthesized parameters. The {@link Declaration} being referenced will more likely be a {@link Function} but may also be a {@link Variable}
 * in which case that variable will probably be typed as {@link FunctionType}/{@link PrimitiveType#FUNCTION}
 * @author madeen
 *
 */
public class CallDeclaration extends AccessDeclaration implements IFunctionCall, ITidyable {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	private ASTNode[] params;
	private int parmsStart, parmsEnd;

	@Override
	protected void offsetExprRegion(final int amount, final boolean start, final boolean end) {
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
	public void setParmsRegion(final int start, final int end) {
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
	public CallDeclaration(final String funcName, final ASTNode... parms) {
		super(funcName);
		params = parms;
		assignParentToSubElements();
	}

	/**
	 * Create a CallFunc that directly refers to a {@link Function} object.
	 * @param function The {@link Function} the new CallFunc will refer to.
	 * @param parms Parameter expressions
	 */
	public CallDeclaration(final Function function, final ASTNode... parms) {
		this(function.name(), parms);
		this.declaration = function;
		assignParentToSubElements();
	}

	@Override
	public void doPrint(final ASTNodePrinter output, final int depth) {
		super.doPrint(output, depth);
		printNodeList(output, params, depth, "(", ")");
	}

	@Override
	public boolean hasSideEffects() { return true; }

	@Override
	public boolean isValidInSequence(final ASTNode elm) {
		return super.isValidInSequence(elm) || elm instanceof MemberOperator;
	}

	@Override
	public ASTNode[] subElements() {
		return params;
	}
	@Override
	public void setSubElements(final ASTNode[] elms) {
		params = elms;
	}
	protected BinaryOp applyOperatorTo(final Tidy tidy, final ASTNode[] parms, final Operator operator) throws CloneNotSupportedException {
		BinaryOp op = new BinaryOp(operator);
		final BinaryOp result = op;
		for (int i = 0; i < parms.length; i++) {
			final ASTNode one = tidy.tidy(parms[i]);
			final ASTNode two = i+1 < parms.length ? parms[i+1] : null;
			if (op.leftSide() == null)
				op.setLeftSide(one);
			else if (two == null)
				op.setRightSide(one);
			else {
				final BinaryOp nu = new BinaryOp(operator);
				op.setRightSide(nu);
				nu.setLeftSide(one);
				op = nu;
			}
		}
		return result;
	}
	boolean isEngineFunction(final String name) {
		return declaration instanceof EngineFunction && declaration.name().equals(name);
	}
	@Override
	public ASTNode tidy(final Tidy tidy) throws CloneNotSupportedException {

		// And(ugh, blugh) -> ugh && blugh
		final Operator replOperator = Operator.oldStyleFunctionReplacement(declarationName);
		if (replOperator != null && params.length == 1) {
			// LessThan(x) -> x < 0
			if (replOperator.numArgs() == 2)
				return new BinaryOp(replOperator, tidy.tidy(params[0]), IntegerLiteral.ZERO);
			ASTNode n = tidy.tidy(params[0]);
			if (n instanceof BinaryOp)
				n = new Parenthesized(n);
			return new UnaryOp(replOperator, replOperator.isPostfix() ? UnaryOp.Placement.Postfix : UnaryOp.Placement.Prefix, n);
		}
		if (replOperator != null && params.length >= 2)
			return applyOperatorTo(tidy, params, replOperator);

		// ObjectCall(ugh, "UghUgh", 5) -> ugh->UghUgh(5)
		if (params.length >= 2 && isEngineFunction("ObjectCall") && params[1] instanceof StringLiteral && (alwaysConvertObjectCalls || !this.containedInLoopHeaderOrNotStandaloneExpression()) && !params[0].hasSideEffects()) {
			final ASTNode[] parmsWithoutObject = new ASTNode[params.length-2];
			for (int i = 0; i < parmsWithoutObject.length; i++)
				parmsWithoutObject[i] = tidy.tidy(params[i+2]);
			final String lit = ((StringLiteral)params[1]).stringValue();
			if (lit.length() > 0 && lit.charAt(0) != '~')
				return alwaysConvertObjectCalls && this.containedInLoopHeaderOrNotStandaloneExpression()
					? new Sequence(new ASTNode[] {
						tidy.tidy(params[0]),
						new MemberOperator(false, true, null, 0),
						new CallDeclaration(((StringLiteral)params[1]).stringValue(), parmsWithoutObject)}
					)
					: new IfStatement(tidy.tidy(params[0]),
						new SimpleStatement(new Sequence(new ASTNode[] {
							tidy.tidy(params[0]),
							new MemberOperator(false, true, null, 0),
							new CallDeclaration(((StringLiteral)params[1]).stringValue(), parmsWithoutObject)}
						)),
						null
					);
		}

		// OCF_Awesome() -> OCF_Awesome
		if (params.length == 0 && declaration instanceof Variable)
			if (!parent(Script.class).engine().settings().supportsProplists && predecessor() != null)
				return new CallDeclaration("LocalN", new StringLiteral(declarationName)); //$NON-NLS-1$
			else
				return new AccessVar(declarationName);

		// also check for not-nullness since in OC Var/Par are gone and declaration == ...Par returns true -.-

		// Par(5) -> nameOfParm6
		if (params.length <= 1 && declaration != null && isEngineFunction("Par") && (params.length == 0 || params[0] instanceof IntegerLiteral)) {
			final IntegerLiteral number = params.length > 0 ? (IntegerLiteral) params[0] : IntegerLiteral.ZERO;
			final Function func = this.parent(Function.class);
			if (func != null)
				if (number.intValue() >= 0 && number.intValue() < func.numParameters() && func.parameter(number.intValue()).isActualParm())
					return new AccessVar(parent(Function.class).parameter(number.intValue()).name());
		}

		// SetVar(5, "ugh") -> Var(5) = "ugh"
		if (params.length == 2 && (isEngineFunction("SetVar") || isEngineFunction("SetLocal") || isEngineFunction("AssignVar")))
			return new BinaryOp(Operator.Assign, new CallDeclaration(declarationName.substring(declarationName.equals("AssignVar") ? "Assign".length() : "Set".length()), tidy.tidy(params[0])), tidy.tidy(params[1])); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		// DecVar(0) -> Var(0)--
		if (params.length <= 1 && (isEngineFunction("DecVar") || isEngineFunction("IncVar")))
			return new UnaryOp(isEngineFunction("DecVar") ? Operator.Decrement : Operator.Increment, Placement.Prefix,
					new CallDeclaration("Var", new ASTNode[] {
						params.length == 1 ? tidy.tidy(params[0]) : IntegerLiteral.ZERO
					})
			);

		// Call("Func", 5, 5) -> Func(5, 5)
		if (params.length >= 1 && isEngineFunction("Call") && params[0] instanceof StringLiteral) {
			final String lit = ((StringLiteral)params[0]).stringValue();
			if (lit.length() > 0 && lit.charAt(0) != '~') {
				final ASTNode[] parmsWithoutName = new ASTNode[params.length-1];
				for (int i = 0; i < parmsWithoutName.length; i++)
					parmsWithoutName[i] = tidy.tidy(params[i+1]);
				return new CallDeclaration(((StringLiteral)params[0]).stringValue(), parmsWithoutName);
			}
		}

		return this;
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
	public EntityRegion entityAt(final int offset, final ExpressionLocator<?> locator) {
		return new EntityRegion(set(declaration()), new Region(start(), name().length()));
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
	public int indexOfParm(final ASTNode parm) {
		for (int i = 0; i < params.length; i++)
			if (params[i] == parm)
				return i;
		return -1;
	}
	public Variable parmDefinitionForParmExpression(final ASTNode parm) {
		if (declaration instanceof Function) {
			final Function f = (Function) declaration;
			final int i = indexOfParm(parm);
			return i >= 0 && i < f.numParameters() ? f.parameter(i) : null;
		} else
			return null;
	}

	@Override
	public Object evaluate(final IEvaluationContext context) throws ControlFlowException {
		final Object self = value(predecessor() != null ? predecessor().evaluate(context) : context.self());
		final Object[] args = new Object[params().length];
    	for (int i = 0; i < args.length; i++)
			try {
				args[i] = value(params()[i] != null ? params()[i].evaluate(context) : null);
			} catch (final ControlFlowException e) {
				args[i] = null;
				e.printStackTrace();
			}
	    if (declaration instanceof Function) {
	    	final Function f = (Function)declaration;
			return f.invoke(f.new FunctionInvocation(args, context, self));
	    }
	    if (self == null)
	    	return null;
	    for (final Method m : self.getClass().getMethods())
			if (m.getName().equals(declarationName))
				try {
	    			return m.invoke(self, args);
	    		} catch (final IllegalArgumentException a) {
	    			// continue search
	    		} catch (final IllegalAccessException | InvocationTargetException e) {
					throw new ReturnException(e);
				}
	    return null;
	}

	@Override
	public Class<? extends Declaration> declarationClass() {
		return Function.class;
	}

	public final Function function(final ProblemReporter context) {
		return as(context.obtainDeclaration(this), Function.class);
	}

	public final Function function() {
		return as(declaration, Function.class);
	}

}