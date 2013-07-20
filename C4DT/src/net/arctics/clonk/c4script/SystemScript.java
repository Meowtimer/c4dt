package net.arctics.clonk.c4script;

import static net.arctics.clonk.util.Utilities.as;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.Structure;
import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.c4script.Directive.DirectiveType;
import net.arctics.clonk.c4script.typing.PrimitiveType;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.ID;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.ProjectIndex;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

/**
 * Standalone-script inside a project.
 */
public class SystemScript extends Script implements Serializable {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	private transient IFile scriptFile;
	private String scriptFilePath;

	public SystemScript(Index index, IFile scriptFile) throws CoreException {
		super(index);
		this.name = scriptFile.getName();
		setScriptFile(scriptFile);
	}

	@Override
	public IFile source() {
		return scriptFile;
	}

	@Override
	public void setScriptFile(IFile f) {
		if (Utilities.eq(scriptFile, f))
			return;
		if (scriptFile != null)
			super.unPinFrom(scriptFile);
		scriptFile = f;
		if (f != null) {
			super.pinTo(scriptFile);
			final ClonkProjectNature nature = ClonkProjectNature.get(scriptFile);
			index = nature != null ? nature.index() : null;
		}
		scriptFilePath = f != null ? f.getProjectRelativePath().toPortableString() : ""; //$NON-NLS-1$
	}

	public String scriptFilePath() {
		return scriptFilePath;
	}

	public static SystemScript pinned(IResource resource, boolean duringBuild) {
		return as(Structure.pinned(resource, true, duringBuild), SystemScript.class);
	}

	@Override
	public IResource resource() {
		return source();
	}

	@Override
	public void pinTo(IResource resource) {
		assert(resource instanceof IFile);
		setScriptFile((IFile) resource);
	}

	public boolean refreshFileReference(IProject project) throws CoreException {
		final Path projectPath = new Path(scriptFilePath());
		final IResource res = project.findMember(projectPath);
		if (res instanceof IFile) {
			setScriptFile((IFile) res);
			return true;
		}
		else
			return false;
	}

	public static SystemScript scriptCorrespondingTo(IFile file) {
		final ProjectIndex index = ProjectIndex.fromResource(file);
		final Script script = index != null ? index.scriptAt(file) : null;
		return script instanceof SystemScript ? (SystemScript)script : null;
	}

	@Override
	public Object additionalEntityIdentificationToken() {
		return scriptFilePath;
	}

	public static void register() {
		registerStructureFactory(new IStructureFactory() {
			@Override
			public Structure create(IResource resource, boolean duringBuild) {
				if (!resource.getName().endsWith(".c"))
					return null;
				final ProjectIndex index = ProjectIndex.fromResource(resource);
				if (index != null)
					for (final Script script : index.scripts()) {
						final SystemScript sysScript = as(script, SystemScript.class);
						if (sysScript != null && sysScript.file() != null && sysScript.file().equals(resource)) {
							try {
								index.loadEntity(sysScript);
							} catch (final Exception e) {
								e.printStackTrace();
							}
							return sysScript;
						}
					}
				return null;
			}
		});
	}

	@Override
	public String typeName(boolean special) {
		if (!special)
			return PrimitiveType.OBJECT.typeName(false);
		final List<Definition> targets = new ArrayList<>(3);
		for (final Directive d : directives())
			if (d.type() == DirectiveType.APPENDTO) {
				final ID id = d.contentAsID();
				if (id != null) {
					final Definition def = index().definitionNearestTo(file(), id);
					if (def != null)
						targets.add(def);
				}
			}
		if (targets.size() == 1)
			return String.format("%s+", targets.get(0).typeName(true));
		else
			return super.typeName(special);
	}

}
