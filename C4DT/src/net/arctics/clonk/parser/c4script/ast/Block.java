package net.arctics.clonk.parser.c4script.ast;

import java.util.EnumSet;
import java.util.List;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.c4script.C4ScriptParser;

/**
 * A {} block
 *
 */
public class Block extends Statement {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private Statement[] statements;
	
	public Block(List<Statement> statements) {
		this(statements.toArray(new Statement[statements.size()]));
	}

	public Block(Statement... statements) {
		super();
		this.statements = statements;
		assignParentToSubElements();
	}
	
	// helper constructor that wraps expressions in statement if necessary
	public Block(ExprElm... expressions) {
		this(SimpleStatement.wrapExpressions(expressions));
	}
	
	public Statement[] statements() {
		return statements;
	}

	public void setStatements(Statement[] statements) {
		this.statements = statements;
	}

	@Override
	public void setSubElements(ExprElm[] elms) {
		Statement[] typeAdjustedCopy = new Statement[elms.length];
		System.arraycopy(elms, 0, typeAdjustedCopy, 0, elms.length);
		setStatements(typeAdjustedCopy);
	}

	@Override
	public ExprElm[] subElements() {
		return statements();
	}

	@Override
	public void doPrint(ExprWriter builder, int depth) {
		printBlock(statements, builder, depth);
	}

	public static void printBlock(Statement[] statements, ExprWriter builder, int depth) {
		builder.append("{\n"); //$NON-NLS-1$
		for (Statement statement : statements) {
			//statement.printPrependix(builder, depth);
			Conf.printIndent(builder, depth);
			statement.print(builder, depth+1);
			//statement.printAppendix(builder, depth);
			builder.append("\n"); //$NON-NLS-1$
		}
		Conf.printIndent(builder, depth-1); builder.append("}"); //$NON-NLS-1$
	}
	
	@Override
	public ExprElm optimize(C4ScriptParser parser) throws CloneNotSupportedException {
		if (parent() != null && !(parent() instanceof KeywordStatement) && !(this instanceof BunchOfStatements)) {
			return new BunchOfStatements(statements);
		}
		// uncomment never-reached statements
		boolean notReached = false;
		Statement[] commentedOutList = null;
		for (int i = 0; i < statements.length; i++) {
			Statement s = statements[i];
			if (notReached) {
				if (commentedOutList != null) {
					commentedOutList[i] = s instanceof Comment ? s : s.commentedOut();
				}
				else if (!(s instanceof Comment)) {
					commentedOutList = new Statement[statements.length];
					System.arraycopy(statements, 0, commentedOutList, 0, i);
					commentedOutList[i] = s.commentedOut();
				}
			}
			else
				notReached = s != null && s.controlFlow() != ControlFlow.Continue;
		}
		if (commentedOutList != null)
			return new Block(commentedOutList);
		else
			return super.optimize(parser);
	}
	
	@Override
	public ControlFlow controlFlow() {
		for (Statement s : statements) {
			// look for first statement that breaks execution
			ControlFlow cf = s.controlFlow();
			if (cf != ControlFlow.Continue)
				return cf;
		}
		return ControlFlow.Continue;
	}
	
	@Override
	public EnumSet<ControlFlow> possibleControlFlows() {
		EnumSet<ControlFlow> result = EnumSet.noneOf(ControlFlow.class);
		for (Statement s : statements) {
			ControlFlow cf = s.controlFlow();
			if (cf != ControlFlow.Continue)
				return EnumSet.of(cf);
			EnumSet<ControlFlow> cfs = s.possibleControlFlows();
			result.addAll(cfs);
		}
		return result;
	}

}