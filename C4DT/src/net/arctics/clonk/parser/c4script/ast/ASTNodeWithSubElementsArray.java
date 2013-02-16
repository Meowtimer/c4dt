package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ASTNode;

public class ASTNodeWithSubElementsArray extends ASTNode {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	protected ASTNode[] elements;
	public ASTNodeWithSubElementsArray(ASTNode... elms) {
		this.elements = elms;
		assignParentToSubElements();
	}
	@Override
	public ASTNode[] subElements() {
		return elements;
	}
	@Override
	public void setSubElements(ASTNode[] elms) {
		elements = elms;
	}
	public ASTNode lastElement() {
		return elements != null && elements.length > 1 ? elements[elements.length-1] : null;
	}
}
