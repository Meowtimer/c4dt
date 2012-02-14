package net.arctics.clonk.index;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.Variable;

public final class DocumentedVariable extends Variable implements IDocumentedDeclaration {
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
		fetchDocumentation();
		return super.type();
	}

	@Override
	public boolean fetchDocumentation() {
		if (!fleshedOut)
			return fleshedOut = this.engine().repositoryDocImporter().fleshOutPlaceholder(this, fleshedOut);
		else
			return false;
	}
}