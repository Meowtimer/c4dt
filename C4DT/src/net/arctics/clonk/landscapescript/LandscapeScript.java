package net.arctics.clonk.landscapescript;

import net.arctics.clonk.Core;
import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.index.Engine;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

public class LandscapeScript extends Overlay {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private IFile file;
	@Override
	public IFile file() { return file; }
	@Override
	public void setFile(final IFile file) { this.file = file; }
	@Override
	public IResource resource() { return file; }
	public LandscapeScript(final IFile file) {
		super();
		this.file = file;
	}
	public static void register() {
		registerStructureFactory((resource, duringBuild) -> {
			if (resource instanceof IFile && resource.getName().equalsIgnoreCase("Landscape.txt")) { //$NON-NLS-1$
				final LandscapeScript script = new LandscapeScript((IFile) resource);
				final LandscapeScriptParser parser = new LandscapeScriptParser(script);
				parser.parse();
				return script;
			}
			return null;
		});
	}
	@Override
	public Engine engine() {
		final ClonkProjectNature nature = ClonkProjectNature.get(file);
		return nature != null && nature.index() != null ? nature.index().engine() : super.engine();
	}
}
