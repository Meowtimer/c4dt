package net.arctics.clonk.c4script.ast;

import static net.arctics.clonk.util.ArrayUtil.iterable;
import static net.arctics.clonk.util.ArrayUtil.map;
import static net.arctics.clonk.util.ArrayUtil.set;
import static net.arctics.clonk.util.Utilities.as;
import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodePrinter;
import net.arctics.clonk.ast.ControlFlow;
import net.arctics.clonk.ast.ControlFlowException;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ast.EntityRegion;
import net.arctics.clonk.ast.IEntityLocator;
import net.arctics.clonk.ast.IEvaluationContext;
import net.arctics.clonk.ast.Sequence;
import net.arctics.clonk.c4script.Conf;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.Keywords;
import net.arctics.clonk.c4script.Operator;
import net.arctics.clonk.c4script.ProblemReporter;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.c4script.ast.UnaryOp.Placement;
import net.arctics.clonk.c4script.typing.FunctionType;
import net.arctics.clonk.c4script.typing.PrimitiveType;
import net.arctics.clonk.util.IConverter;
import net.arctics.clonk.util.StringUtil;

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
	public static void printParmString(ASTNodePrinter output, ASTNode[] params, final int depth) {
		final Iterable<String> parmStrings = map(iterable(params), new IConverter<ASTNode, String>() {
			@Override
			public String convert(ASTNode from) { return from.printed(depth+(Conf.braceStyle==BraceStyleType.NewLine?1:0)).trim(); }
		});
		int len = 0;
		for (final String ps : parmStrings)
			len += ps.length();
		if (len < 80)
			StringUtil.writeBlock(output, "(", ")", ", ", parmStrings);
		else {
			final String indent = "\n"+StringUtil.multiply(Conf.indentString, depth+1);
			String startBlock, endBlock;
			switch (Conf.braceStyle) {
			case NewLine:
				startBlock = "\n"+StringUtil.multiply(Conf.indentString, depth)+"("+indent;
				endBlock = "\n"+StringUtil.multiply(Conf.indentString, depth)+")";
				break;
			default:
				startBlock = "(";
				endBlock = ")";
				break;
			}
			StringUtil.writeBlock(output, startBlock, endBlock, ","+indent, parmStrings);
		}
	}

	@Override
	public boolean hasSideEffects() { return true; }

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
	protected BinaryOp applyOperatorTo(Tidy tidy, ASTNode[] parms, Operator operator) throws CloneNotSupportedException {
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
	@Override
	public ASTNode tidy(Tidy tidy) throws CloneNotSupportedException {

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
		if (params.length >= 2 && declaration == tidy.reporter.cachedEngineDeclarations().ObjectCall && params[1] instanceof StringLiteral && (Conf.alwaysConvertObjectCalls || !this.containedInLoopHeaderOrNotStandaloneExpression()) && !params[0].hasSideEffects()) {
			final ASTNode[] parmsWithoutObject = new ASTNode[params.length-2];
			for (int i = 0; i < parmsWithoutObject.length; i++)
				parmsWithoutObject[i] = tidy.tidy(params[i+2]);
			final String lit = ((StringLiteral)params[1]).stringValue();
			if (lit.length() > 0 && lit.charAt(0) != '~')
				return Conf.alwaysConvertObjectCalls && this.containedInLoopHeaderOrNotStandaloneExpression()
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
			if (!tidy.reporter.script().engine().settings().supportsProplists && predecessorInSequence() != null)
				return new CallDeclaration("LocalN", new StringLiteral(declarationName)); //$NON-NLS-1$
			else
				return new AccessVar(declarationName);

		// also check for not-nullness since in OC Var/Par are gone and declaration == ...Par returns true -.-

		// Par(5) -> nameOfParm6
		if (params.length <= 1 && declaration != null && declaration == tidy.reporter.cachedEngineDeclarations().Par && (params.length == 0 || params[0] instanceof IntegerLiteral)) {
			final IntegerLiteral number = params.length > 0 ? (IntegerLiteral) params[0] : IntegerLiteral.ZERO;
			final Function func = this.parentOfType(Function.class);
			if (func != null)
				if (number.intValue() >= 0 && number.intValue() < func.numParameters() && func.parameter(number.intValue()).isActualParm())
					return new AccessVar(parentOfType(Function.class).parameter(number.intValue()).name());
		}

		// SetVar(5, "ugh") -> Var(5) = "ugh"
		if (params.length == 2 && declaration != null && (declaration == tidy.reporter.cachedEngineDeclarations().SetVar || declaration == tidy.reporter.cachedEngineDeclarations().SetLocal || declaration == tidy.reporter.cachedEngineDeclarations().AssignVar))
			return new BinaryOp(Operator.Assign, new CallDeclaration(declarationName.substring(declarationName.equals("AssignVar") ? "Assign".length() : "Set".length()), tidy.tidy(params[0])), tidy.tidy(params[1])); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		// DecVar(0) -> Var(0)--
		if (params.length <= 1 && declaration != null && (declaration == tidy.reporter.cachedEngineDeclarations().DecVar || declaration == tidy.reporter.cachedEngineDeclarations().IncVar))
			return new UnaryOp(declaration == tidy.reporter.cachedEngineDeclarations().DecVar ? Operator.Decrement : Operator.Increment, Placement.Prefix,
					new CallDeclaration(tidy.reporter.cachedEngineDeclarations().Var.name(), new ASTNode[] {
						params.length == 1 ? tidy.tidy(params[0]) : IntegerLiteral.ZERO
					})
			);

		// Call("Func", 5, 5) -> Func(5, 5)
		if (params.length >= 1 && declaration != null && declaration == tidy.reporter.cachedEngineDeclarations().Call && params[0] instanceof StringLiteral) {
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
	public EntityRegion entityAt(int offset, IEntityLocator locator) {
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
	public int indexOfParm(ASTNode parm) {
		for (int i = 0; i < params.length; i++)
			if (params[i] == parm)
				return i;
		return -1;
	}
	public Variable parmDefinitionForParmExpression(ASTNode parm) {
		if (declaration instanceof Function) {
			final Function f = (Function) declaration;
			final int i = indexOfParm(parm);
			return i >= 0 && i < f.numParameters() ? f.parameter(i) : null;
		} else
			return null;
	}

	@Override
	public Object evaluate(IEvaluationContext context) {
	    if (declaration instanceof Function) {
	    	final Object[] args = new Object[params().length];
	    	for (int i = 0; i < args.length; i++)
				try {
					args[i] = params()[i] != null ? params()[i].evaluate(context) : null;
				} catch (final ControlFlowException e) {
					args[i] = null;
					e.printStackTrace();
				}
	    	final Function f = (Function)declaration;
			return f.invoke(f.new FunctionInvocation(args, context, context != null ? context.cookie() : null));
	    }
	    else
	    	return null;
	}

	@Override
	public Class<? extends Declaration> declarationClass() {
		return Function.class;
	}

	public final Function function(ProblemReporter context) {
		return as(context.obtainDeclaration(this), Function.class);
	}

	public final Function function() {
		return as(declaration, Function.class);
	}

}