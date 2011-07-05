package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.ClonkCore;

public class ExprElmWithSubElementsArray extends Value {
	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	protected ExprElm[] elements;
	public ExprElmWithSubElementsArray(ExprElm... elms) {
		this.elements = elms;
		assignParentToSubElements();
	}
	@Override
	public ExprElm[] getSubElements() {
		return elements;
	}
	@Override
	public void setSubElements(ExprElm[] elms) {
		elements = elms;
	}
	public ExprElm getLastElement() {
		return elements != null && elements.length > 1 ? elements[elements.length-1] : null;
	}
}
