package net.arctics.clonk.index;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.Variable;

public final class DocumentedFunction extends Function implements IDocumentedDeclaration {
	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	private boolean fleshedOut;

	public DocumentedFunction(String name, IType returnType) {
		super(name, returnType);
	}
	
	public DocumentedFunction(String name, FunctionScope scope) {
		super(name, scope);
	}

	@Override
	public synchronized Iterable<? extends Variable> parameters() {
		fetchDocumentation();
		return super.parameters();
	}

	@Override
	public synchronized IType returnType() {
		fetchDocumentation();
		return super.returnType();
	}

	@Override
	public boolean fetchDocumentation() {
		if (!fleshedOut)
			return fleshedOut = this.engine().repositoryDocImporter().fleshOutPlaceholder(this, fleshedOut);
		else
			return false;
	}
}