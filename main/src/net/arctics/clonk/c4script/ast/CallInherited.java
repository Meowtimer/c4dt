package net.arctics.clonk.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.c4script.Keywords;

public class CallInherited extends CallDeclaration {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	private final boolean failsafe;

	public boolean failsafe() { return failsafe; }

	public CallInherited(final boolean failsafe, final ASTNode[] parameters) {
		super((String)null, parameters);
		this.failsafe = failsafe;
	}

	@Override
	public String name() { return failsafe ? Keywords.SafeInherited : Keywords.Inherited; }

}
