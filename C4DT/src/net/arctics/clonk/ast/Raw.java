package net.arctics.clonk.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.c4script.typing.IType;

public class Raw extends ASTNode {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	public final String text;
	public final IType type;

	@Override
	public IType ty() {
		return type;
	};

	public Raw(String text, IType type) {
		this.text = text;
		this.type = type;
	}

	@Override
	public void doPrint(ASTNodePrinter output, int depth) {
		output.append(this.text);
	}

}
