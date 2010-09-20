package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.C4Type;
import net.arctics.clonk.parser.c4script.IType;

public class Sequence extends Value {
	/**
	 * 
	 */
	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	protected ExprElm[] elements;
	public Sequence(ExprElm... elms) {
		elements = elms;
		ExprElm prev = null;
		for (ExprElm e : elements) {
			e.setPredecessorInSequence(prev);
			if (prev != null)
				prev.setSuccessorInSequence(e);
			e.setParent(this);
			prev = e;
		}
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
	public IType getType(C4ScriptParser context) {
		return (elements == null || elements.length == 0) ? C4Type.UNKNOWN : elements[elements.length-1].getType(context);
	}
	@Override
	public boolean modifiable(C4ScriptParser context) {
		return elements != null && elements.length > 0 && elements[elements.length-1].modifiable(context);
	}
	@Override
	public void reportErrors(C4ScriptParser parser) throws ParsingException {
		for (ExprElm e : elements) {
			e.reportErrors(parser);
		}
	}
	public ExprElm[] getElements() {
		return elements;
	}
	public ExprElm getLastElement() {
		return elements != null && elements.length > 1 ? elements[elements.length-1] : null;
	}
	@Override
	public IStoredTypeInformation createStoredTypeInformation(C4ScriptParser parser) {
		ExprElm last = getLastElement();
		if (last != null)
			// things in sequences should take into account their predecessors
			return last.createStoredTypeInformation(parser);
		return super.createStoredTypeInformation(parser);
	}

}