package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.Keywords;
import net.arctics.clonk.parser.c4script.ast.evaluate.IEvaluationContext;

public class ForStatement extends ConditionalStatement implements ILoop {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private ExprElm initializer, increment;
	public ForStatement(ExprElm initializer, ExprElm condition, ExprElm increment, ExprElm body) {
		super(condition, body);
		this.initializer = initializer;
		this.increment = increment;
		assignParentToSubElements();
	}
	@Override
	public String keyword() {
		return Keywords.For;
	}
	@Override
	public void doPrint(ExprWriter builder, int depth) {
		builder.append(keyword() + " ("); //$NON-NLS-1$
		if (initializer != null)
			initializer.print(builder, depth+1);
		else
			builder.append(";"); //$NON-NLS-1$
		builder.append(" "); // no ';' since initializer is already a statement //$NON-NLS-1$
		if (condition != null)
			condition.print(builder, depth+1);
		builder.append("; "); //$NON-NLS-1$
		if (increment != null)
			increment.print(builder, depth);
		builder.append(")"); //$NON-NLS-1$
		printBody(builder, depth);
	}
	@Override
	public ExprElm[] subElements() {
		return new ExprElm[] {initializer, condition, increment, body};
	}

	@Override
	public void setSubElements(ExprElm[] elms) {
		initializer = elms[0];
		condition   = elms[1];
		increment   = elms[2];
		body        = elms[3];
	}
	
	@Override
	public Object evaluate(IEvaluationContext context) throws ControlFlowException {
		if (initializer != null)
			initializer.evaluate(context);
		Object ev = null;
		while (true) {
			if (condition != null && !convertToBool(condition.evaluate(context)))
				break;
			ev = body.evaluate(context);
			if (increment != null)
				increment.evaluate(context);
		}
		return ev;
	}
	
	@Override
	public void reportErrors(C4ScriptParser parser) throws ParsingException {
		super.reportErrors(parser);
		if (initializer != null)
			parser.reportProblemsOf(initializer, true);
		if (increment != null)
			parser.reportProblemsOf(increment, true);
	}
}