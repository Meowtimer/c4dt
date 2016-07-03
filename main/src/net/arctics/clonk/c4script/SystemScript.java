package net.arctics.clonk.c4script;

import static net.arctics.clonk.util.StreamUtil.ofType;
import static net.arctics.clonk.util.Utilities.as;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

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

/**
 * Standalone-script inside a project.
 */
public class SystemScript extends Script implements Serializable {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	private transient IFile scriptFile;
	private String scriptFilePath;

	public SystemScript(final Index index, final IFile scriptFile) throws CoreException {
		super(index);
		this.name = scriptFile.getName();
		setScriptFile(scriptFile);
	}

	@Override
	public IFile source() {
		return scriptFile;
	}

	@Override
	public void setScriptFile(final IFile f) {
		if (Utilities.eq(scriptFile, f)) {
			return;
		}
		if (scriptFile != null) {
			super.unPinFrom(scriptFile);
		}
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

	public static SystemScript pinned(final IResource resource, final boolean duringBuild) {
		return as(Structure.pinned(resource, true, duringBuild), SystemScript.class);
	}

	@Override
	public IResource resource() {
		return source();
	}

	@Override
	public void pinTo(final IResource resource) {
		assert(resource instanceof IFile);
		setScriptFile((IFile) resource);
	}

	public boolean refreshFileReference(final IProject project) throws CoreException {
		final Path projectPath = new Path(scriptFilePath());
		final IResource res = project.findMember(projectPath);
		if (res instanceof IFile) {
			setScriptFile((IFile) res);
			return true;
		} else {
			return false;
		}
	}

	public static SystemScript scriptCorrespondingTo(final IFile file) {
		final ProjectIndex index = ProjectIndex.fromResource(file);
		final Script script = index != null ? index.scriptAt(file) : null;
		return script instanceof SystemScript ? (SystemScript)script : null;
	}

	@Override
	public Object additionalEntityIdentificationToken() {
		return scriptFilePath;
	}

	public static void register() {
		registerStructureFactory((resource, duringBuild) -> {
			if (!Script.looksLikeScriptFile(resource.getName())) {
				return null;
			}
			final ProjectIndex index = ProjectIndex.fromResource(resource);
			final Script sysScript = ofType(index.scripts().stream(), SystemScript.class)
				.filter(s -> s.file() != null && s.file().equals(resource))
				.findFirst()
				.orElse(null);
			if (sysScript != null) {
				try {
					index.loadEntity(sysScript);
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
			return sysScript;
		});
	}

	private Definition definitionFromDirective(Directive directive) {
		final ID id = directive.contentAsID();
		return id != null ? index().definitionNearestTo(file(), id) : null;
	}

	@Override
	public String typeName(final boolean special) {
		if (!special) {
			return PrimitiveType.OBJECT.typeName(false);
		}
		final List<Definition> targets = directives().stream()
			.filter(d -> d.type() == DirectiveType.APPENDTO)
			.map(this::definitionFromDirective)
			.filter(x -> x != null)
			.collect(Collectors.toList());
		return targets.size() == 1 ? String.format("%s+", targets.get(0).typeName(true)) : super.typeName(special);
	}

	@Override
	public String patternMatchingText() {
		return scriptFile != null ? scriptFile.getProjectRelativePath().toString() : super.patternMatchingText();
	}

}
