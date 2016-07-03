package net.arctics.clonk.index;


import net.arctics.clonk.Core;
import net.arctics.clonk.c4script.typing.IType;

public final class DocumentedVariable extends EngineVariable implements IDocumentedDeclaration {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private boolean fleshedOut;
	public DocumentedVariable(final String name, final IType type) { super(name, type); }
	public DocumentedVariable(final Scope scope, final String name) { super(scope, name); }
	public DocumentedVariable() { super(); }
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