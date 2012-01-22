package net.arctics.clonk.parser;

import java.io.Serializable;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IRegion;

public class DeclarationLocation implements Serializable {

	private static final long serialVersionUID = 1L;

	private final Declaration declaration;
	private final IRegion location;
	private transient IResource resource;
	public IResource resource() {
		return resource;
	}
	public Declaration declaration() {
		return declaration;
	}
	public IRegion location() {
		return location;
	}
	public DeclarationLocation(Declaration declaration) {
		this(declaration, declaration.location(), declaration.resource());
	}
	public DeclarationLocation(Declaration declaration, IRegion location, IResource resource) {
		super();
		this.declaration = declaration;
		this.location = location;
		this.resource = resource;
	}
	@Override
	public String toString() {
		return declaration.name();
	}
}