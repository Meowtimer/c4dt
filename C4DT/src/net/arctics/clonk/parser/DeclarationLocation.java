package net.arctics.clonk.parser;

import java.io.Serializable;
import java.util.regex.Matcher;

import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.c4script.IIndexEntity;
import net.arctics.clonk.util.IHasRelatedResource;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IRegion;

public class DeclarationLocation implements Serializable, IIndexEntity, IHasRelatedResource {

	private static final long serialVersionUID = 1L;

	private final Declaration declaration;
	private final IRegion location;
	private transient IResource resource;
	
	@Override
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
	@Override
	public String name() {
		return declaration.name();
	}
	@Override
	public boolean matchedBy(Matcher matcher) {
		return declaration.matchedBy(matcher);
	}
	@Override
	public String infoText() {
		return declaration.infoText();
	}
	@Override
	public Index index() {
		return declaration.index();
	}
}