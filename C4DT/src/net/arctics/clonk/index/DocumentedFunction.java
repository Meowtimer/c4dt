package net.arctics.clonk.index;

import java.util.List;

import net.arctics.clonk.Core;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.c4script.typing.IType;

public final class DocumentedFunction extends EngineFunction implements IDocumentedDeclaration {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private boolean fleshedOut;
	private String originInfo;

	public String originInfo() {
		return originInfo;
	}

	public void setOriginInfo(final String originInfo) {
		this.originInfo = originInfo;
	}

	public DocumentedFunction(final String name, final IType returnType) {
		super(name, returnType);
	}

	public DocumentedFunction(final String name, final IType returnType, final String origin) {
		this(name, returnType);
		setOriginInfo(origin);
	}

	public DocumentedFunction(final String name, final FunctionScope scope) {
		super(name, scope);
	}

	@Override
	public synchronized Variable[] parameters() {
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

	@Override
	public String infoText(final IIndexEntity context) {
		fetchDocumentation();
		String sup = super.infoText(context);
		if (originInfo != null)
			sup += originInfo;
		return sup;
	}
}