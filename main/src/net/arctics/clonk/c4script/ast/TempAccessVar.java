package net.arctics.clonk.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.Declaration;

public class TempAccessVar extends AccessVar {

	public TempAccessVar(final Declaration declaration, final ASTNode parent) { super(declaration); setParent(parent); }

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

}
