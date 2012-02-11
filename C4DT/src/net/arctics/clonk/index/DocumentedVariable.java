package net.arctics.clonk.index;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.Variable;

public final class DocumentedVariable extends Variable {
	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	private boolean fleshedOut;

	public DocumentedVariable(String name, IType type) {
		super(name, type);
	}
	
	public DocumentedVariable(String name, Scope scope) {
		super(name, scope);
	}

	@Override
	public synchronized IType type() {
		fleshedOut = engine().repositoryDocImporter().fleshOutPlaceholder(this, fleshedOut);
		return super.type();
	}
}