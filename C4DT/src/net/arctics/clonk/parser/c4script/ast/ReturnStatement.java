package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.Keywords;
import net.arctics.clonk.parser.c4script.ast.evaluate.IEvaluationContext;

public class ReturnStatement extends KeywordStatement {


	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	private ExprElm returnExpr;

	@Override
	public Object evaluate(IEvaluationContext context) throws ControlFlowException {
		throw new ReturnException(returnExpr.evaluate(context));
	}
	
	public ReturnStatement(ExprElm returnExpr) {
		super();
		this.returnExpr = returnExpr;
		assignParentToSubElements();
	}

	@Override
	public String keyword() {
		return Keywords.Return;
	}

	@Override
	public void doPrint(ExprWriter builder, int depth) {
		builder.append(keyword());
		if (returnExpr != null) {
			builder.append(" "); //$NON-NLS-1$
			// return(); -> return 0;
			if (returnExpr == ExprElm.NULL_EXPR)
				builder.append("0"); //$NON-NLS-1$
			else
				returnExpr.print(builder, depth+1);
		}
		builder.append(";"); //$NON-NLS-1$
	}

	public ExprElm getReturnExpr() {
		return returnExpr;
	}

	public void setReturnExpr(ExprElm returnExpr) {
		this.returnExpr = returnExpr;
	}

	@Override
	public ExprElm[] subElements() {
		return new ExprElm[] {returnExpr};
	}

	@Override
	public void setSubElements(ExprElm[] elms) {
		returnExpr = elms[0];
	}

	@Override
	public ControlFlow controlFlow() {
		return ControlFlow.Return;
	}

	@Override
	public ExprElm optimize(C4ScriptParser parser) throws CloneNotSupportedException {
		// return (0); -> return 0;
		if (returnExpr instanceof Parenthesized)
			return new ReturnStatement(((Parenthesized)returnExpr).innerExpression().optimize(parser));
		// return (0, Sound("Ugh")); -> { Sound("Ugh"); return 0; }
		// FIXME: should declare temporary variable so that order of expression execution isn't changed
		/*
		if (returnExpr instanceof Tuple) {
			Tuple tuple = (Tuple) returnExpr;
			ExprElm[] tupleElements = tuple.getElements();
			List<Statement> statements = new LinkedList<Statement>();
			for (int i = 1; i < tupleElements.length; i++) {
				statements.add(new SimpleStatement(tupleElements[i].newStyleReplacement(parser)));
			}
			statements.add(new ReturnStatement(tupleElements[0].newStyleReplacement(parser)));
			return getParent() instanceof ConditionalStatement ? new Block(statements) : new BunchOfStatements(statements);
		}
		 */
		return super.optimize(parser);
	}

	private void warnAboutTupleInReturnExpr(C4ScriptParser parser, ExprElm expr, boolean tupleIsError) throws ParsingException {
		if (expr == null)
			return;
		if (expr instanceof Tuple) {
			if (tupleIsError) {
				parser.errorWithCode(ParserErrorCode.TuplesNotAllowed, expr);
			} else {
				if (parser.getStrictLevel() >= 2)
					parser.errorWithCode(ParserErrorCode.ReturnAsFunction, expr, C4ScriptParser.NO_THROW);
			}
		}
		ExprElm[] subElms = expr.subElements();
		for (ExprElm e : subElms) {
			warnAboutTupleInReturnExpr(parser, e, true);
		}
	}
	
	@Override
	public void reportErrors(C4ScriptParser parser) throws ParsingException {
		super.reportErrors(parser);
		warnAboutTupleInReturnExpr(parser, returnExpr, false);
		Function activeFunc = parser.getCurrentFunc();
		if (activeFunc == null)
			parser.errorWithCode(ParserErrorCode.NotAllowedHere, this, C4ScriptParser.NO_THROW, Keywords.Return);
		else if (returnExpr != null)
			parser.getCurrentFunc().expectedToBeOfType(returnExpr.getType(parser), TypeExpectancyMode.Force);
	}
}