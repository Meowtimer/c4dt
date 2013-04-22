package net.arctics.clonk.landscapescript;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.Structure;
import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.index.Engine;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

public class LandscapeScript extends Overlay {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	
	private IFile file;

	public IFile file() {
		return file;
	}

	public void setFile(IFile file) {
		this.file = file;
	}

	public LandscapeScript(IFile file) {
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
					LandscapeScript script = new LandscapeScript((IFile) resource);
					LandscapeScriptParser parser = new LandscapeScriptParser(script);
					parser.parse();
					return script;
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
