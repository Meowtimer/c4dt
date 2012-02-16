package net.arctics.clonk.parser.mapcreator;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.resource.ClonkProjectNature;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

public class MapCreator extends MapCreatorMap {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	
	private IFile file;

	public IFile file() {
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
	public IResource resource() {
		return file;
	}
	
	public static void register() {
		registerStructureFactory(new IStructureFactory() {
			@Override
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
	public Engine engine() {
		ClonkProjectNature nature = ClonkProjectNature.get(file);
		return nature != null && nature.index() != null ? nature.index().engine() : super.engine();
	}

}
