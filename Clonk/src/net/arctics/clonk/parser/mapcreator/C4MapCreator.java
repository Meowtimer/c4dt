package net.arctics.clonk.parser.mapcreator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

public class C4MapCreator extends C4Map {

	private static final long serialVersionUID = 1L;
	
	private IFile file;

	public IFile getFile() {
		return file;
	}

	public C4MapCreator(IFile file) {
		super();
		this.file = file;
	}
	
	@Override
	public IResource getResource() {
		return file;
	}

}
