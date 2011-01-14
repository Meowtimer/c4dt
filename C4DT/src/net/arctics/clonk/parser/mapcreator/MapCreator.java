package net.arctics.clonk.parser.mapcreator;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.resource.ClonkProjectNature;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

public class MapCreator extends MapCreatorMap {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	
	private IFile file;

	public IFile getFile() {
		return file;
	}

	public void setFile(IFile file) {
		this.file = file;
	}

	public MapCreator(IFile file) {
		super();
		this.file = file;
	}
	
	@Override
	public IResource getResource() {
		return file;
	}
	
	public static void register() {
		registerStructureFactory(new IStructureFactory() {
			public Structure create(IResource resource, boolean duringBuild) {
				if (resource instanceof IFile && resource.getName().equalsIgnoreCase("Landscape.txt")) { //$NON-NLS-1$
					MapCreator mapCreator = new MapCreator((IFile) resource);
					MapCreatorParser parser = new MapCreatorParser(mapCreator);
					parser.parse();
					return mapCreator;
				}
				return null;
			}
		});
	}
	
	@Override
	public Engine getEngine() {
		ClonkProjectNature nature = ClonkProjectNature.get(file);
		return nature != null && nature.getIndex() != null ? nature.getIndex().getEngine() : super.getEngine();
	}

}
