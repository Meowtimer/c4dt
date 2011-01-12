package net.arctics.clonk.parser.c4script.ast;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.C4Type;
import net.arctics.clonk.parser.c4script.IType;

public class Sequence extends Value {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	
	protected ExprElm[] elements;

	public Sequence(ExprElm... elms) {
		elements = elms;
		ExprElm prev = null;
		for (ExprElm e : elements) {
			e.setPredecessorInSequence(prev);
			e.setParent(this);
			prev = e;
		}
	}
	public Sequence(List<ExprElm> elms) {
		this(elms.toArray(new ExprElm[elms.size()]));
	}
	@Override
	public ExprElm[] getSubElements() {
		return elements;
	}
	@Override
	public void setSubElements(ExprElm[] elms) {
		elements = elms;
	}
	@Override
	public void doPrint(ExprWriter output, int depth) {
		for (ExprElm e : elements) {
			e.print(output, depth+1);
		}
	}
	@Override
	protected IType obtainType(C4ScriptParser context) {
		return (elements == null || elements.length == 0) ? C4Type.UNKNOWN : elements[elements.length-1].getType(context);
	}
	@Override
	public boolean modifiable(C4ScriptParser context) {
		return elements != null && elements.length > 0 && elements[elements.length-1].modifiable(context);
	}
	@Override
	public void reportErrors(C4ScriptParser parser) throws ParsingException {
		// stupid class hierarchy :D
		if (this.getClass() == Sequence.class) {
			//ExprElm p = null;
			for (ExprElm e : elements) {
				/*if (!e.isValidInSequence(p, parser)) {
					parser.errorWithCode(ParserErrorCode.NotAllowedHere, e, true, parser.scriptSubstringAtRegion(e));
				}*/
				e.reportErrors(parser);
				//p = e;
			}
		}
		super.reportErrors(parser);
	}
	public ExprElm[] getElements() {
		return elements;
	}
	public ExprElm getLastElement() {
		return elements != null && elements.length > 1 ? elements[elements.length-1] : null;
	}
	@Override
	public IStoredTypeInformation createStoredTypeInformation(C4ScriptParser parser) {
		return super.createStoredTypeInformation(parser);
		/*ExprElm last = getLastElement();
		if (last != null)
			// things in sequences should take into account their predecessors
			return last.createStoredTypeInformation(parser);
		return super.createStoredTypeInformation(parser); */
	}
	public Statement[] splitIntoValidSubStatements(C4ScriptParser parser) {
		List<ExprElm> currentSequenceExpressions = new LinkedList<ExprElm>();
		List<Statement> result = new ArrayList<Statement>(elements.length);
		ExprElm p = null;
		for (ExprElm e : elements) {
			if (!e.isValidInSequence(p, parser)) {
				result.add(SimpleStatement.wrapExpression(new Sequence(currentSequenceExpressions)));
				currentSequenceExpressions.clear();
			}
			currentSequenceExpressions.add(e);
			p = e;
		}
		if (result.size() == 0) {
			return new Statement[] {SimpleStatement.wrapExpression(this)};
		} else {
			result.add(SimpleStatement.wrapExpression(new Sequence(currentSequenceExpressions)));
			return result.toArray(new Statement[result.size()]);
		}
	}
	public Sequence sequenceWithElementsRemovedFrom(ExprElm elm) {
		List<ExprElm> list = new ArrayList<ExprElm>(elements.length);
		for (ExprElm e : elements) {
			if (e == elm)
				break;
			else
				list.add(e);
		}
		return list.size() > 0 ? new Sequence(list) : null;
	}
}