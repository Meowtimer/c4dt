package net.arctics.clonk.index;

import java.util.regex.Matcher;

import net.arctics.clonk.parser.c4script.IIndexEntity;
import net.arctics.clonk.util.IHasRelatedResource;

import org.eclipse.core.resources.IResource;

/**
 * Wrapper for a resource inside a project which can be passed around as {@link IIndexEntity} 
 * @author madeen
 *
 */
public class ProjectResource implements IIndexEntity, IHasRelatedResource {

	private final IResource resource;
	private final ProjectIndex index;
	
	@Override
	public IResource resource() {
		return resource;
	}
	
	public ProjectResource(ProjectIndex index, IResource resource) {
		this.index = index;
		this.resource = resource;
	}
	
	@Override
	public String name() {
		return resource.getProjectRelativePath().toOSString();
	}

	@Override
	public Index index() {
		return index;
	}

	@Override
	public boolean matchedBy(Matcher matcher) {
		return matcher.reset(name()).matches();
	}

	@Override
	public String infoText() {
		return name();
	}

}
