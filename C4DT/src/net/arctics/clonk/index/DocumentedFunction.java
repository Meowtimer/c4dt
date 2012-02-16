package net.arctics.clonk.index;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.Variable;

public final class DocumentedFunction extends Function implements IDocumentedDeclaration {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private boolean fleshedOut;
	private String originInfo;
	
	public String originInfo() {
		return originInfo;
	}
	
	public void setOriginInfo(String originInfo) {
		this.originInfo = originInfo;
		if (originInfo != null)
			System.out.println(name());
	}

	public DocumentedFunction(String name, IType returnType) {
		super(name, returnType);
	}
	
	public DocumentedFunction(String name, IType returnType, String origin) {
		this(name, returnType);
		setOriginInfo(origin);
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
	
	@Override
	public String infoText() {
		String sup = super.infoText();
		if (originInfo != null)
			sup += originInfo;
		return sup;
	}
}