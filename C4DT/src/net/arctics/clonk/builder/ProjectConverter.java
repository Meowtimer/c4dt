package net.arctics.clonk.builder;

import static java.lang.String.format;
import static java.lang.System.out;
import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.findMemberCaseInsensitively;
import static net.arctics.clonk.util.Utilities.runWithoutAutoBuild;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ast.Structure;
import net.arctics.clonk.c4group.C4Group.GroupType;
import net.arctics.clonk.c4script.ast.IDLiteral;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.ID;
import net.arctics.clonk.index.ProjectConversionConfiguration;
import net.arctics.clonk.ini.IniEntry;
import net.arctics.clonk.ini.IniUnit;
import net.arctics.clonk.landscapescript.LandscapeScript;
import net.arctics.clonk.util.StringUtil;
import net.arctics.clonk.util.TaskExecution;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

/**
 * Converts projects for one engine into projects for another engine.
 * @author madeen
 *
 */
public class ProjectConverter implements IResourceVisitor, Runnable {

	class DeclarationConversion implements Runnable {
		final IResource origin;
		final IResource target;
		final Declaration declaration;
		public DeclarationConversion(IResource origin, IResource target, Declaration declaration) {
			super();
			this.origin = origin;
			this.target = target;
			this.declaration = declaration;
		}
		class FileConversion {
			final Structure original;
			final IFile target;
			Structure converted;
			public FileConversion(Structure original, IFile target) {
				super();
				this.original = original;
				this.target = target;
			}
			FileConversion convert() {
				converted = as(codeConverter.convert(original, original), Structure.class);
				return this;
			}
			void write() {
				final String convString = converted.printed();
				try {
					target.setContents(new ByteArrayInputStream(convString.getBytes()), true, true, null);
				} catch (final CoreException e) {
					e.printStackTrace();
				}
			}
		}
		@Override
		public void run() {
			try {
				final IFolder originFolder = as(origin, IFolder.class);
				final IFolder targetFolder = as(target, IFolder.class);
				if (originFolder != null && targetFolder != null) {
					final Map<IFile, FileConversion> sub = new HashMap<>();
					Arrays.stream(originFolder.members(IResource.FILE)).filter(r -> r instanceof IFile).forEach(res -> {
						final IFile file = (IFile)res;
						final Structure struct = Structure.pinned(file, false, false);
						if (struct != null) {
							if (struct instanceof LandscapeScript)
								return; // can't handle that
							final IFile target = targetFolder.getFile(file.getName());
							if (target.exists())
								sub.put(file, new FileConversion(struct, target));
						}
					});

					sub.values().forEach(FileConversion::convert);

					final Definition definition = as(declaration, Definition.class);
					if (definition != null && originFolder != null) {
						final Supplier<ID> mapID = () -> {
							final ID mapped = configuration.idMap().getOrDefault(definition.id(), null);
							if (mapped != null)
								return mapped;
							else
								return definition.id();
						};
						final ID mapped = mapID.get();
						if (mapped != definition.id()) {
							final FileConversion defCore = sub.get(findMemberCaseInsensitively(originFolder, "DefCore.txt"));
							final IniUnit defCoreUnit = defCore != null ? as(defCore.converted, IniUnit.class) : null;
							if (defCoreUnit != null) {
								final IniEntry entry = defCoreUnit.entryInSection("DefCore", "id");
								if (entry != null)
									entry.value(new IDLiteral(mapped));
							}
						}
					}

					sub.values().forEach(FileConversion::write);
				} else if (target instanceof IFile && declaration instanceof Structure)
					new FileConversion((Structure) declaration, (IFile)target).convert().write();
			} catch (final CoreException e) {
				e.printStackTrace();
			}
		}
	}

	private final ClonkProjectNature sourceProject, destinationProject;
	private final ProjectConversionConfiguration configuration;
	private Engine sourceEngine() { return sourceProject.index().engine(); }
	private Engine destinationEngine() { return destinationProject.index().engine(); }
	private IProgressMonitor monitor;
	private final List<DeclarationConversion> conversions = new LinkedList<>();
	public ProjectConversionConfiguration configuration() { return configuration; }
	/**
	 * Create a new converter by specifying source and destination.
	 * @param sourceProject Source project
	 * @param destinationProject Destination project
	 */
	public ProjectConverter(final IProject sourceProject, final IProject destinationProject) {
		this.sourceProject = ClonkProjectNature.get(sourceProject);
		this.destinationProject = ClonkProjectNature.get(destinationProject);
		this.configuration = destinationEngine().loadProjectConversionConfiguration(sourceEngine());
		this.codeConverter = configuration;
		assert(sourceEngine() != destinationEngine());
	}
	private IPath convertPath(final IPath path) {
		IPath result = new Path("");
		for (int i = 0; i < path.segmentCount(); i++) {
			String segment = path.segment(i);
			final GroupType groupType = sourceEngine().groupTypeForFileName(segment);
			if (groupType != GroupType.OtherGroup)
				segment = destinationEngine().groupName(StringUtil.rawFileName(segment), groupType);
			result = result.append(segment);
		}
		return result;
	}
	@Override
	public void run() {
		try {
			sourceProject.getProject().accept(this);
			TaskExecution.threadPool(conversions, 20);
		} catch (final CoreException e) {
			e.printStackTrace();
		}
	}
	/**
	 * Perform the conversion, reporting progress back to a {@link IProgressMonitor}
	 * @param monitor The monitor to report progress to
	 */
	public void convert(final IProgressMonitor monitor) {
		this.monitor = monitor;
		runWithoutAutoBuild(this);
	}
	/**
	 * By letting this converter visit the source project the actual conversion is performed.
	 */
	@Override
	public boolean visit(final IResource origin) throws CoreException {
		if (origin instanceof IProject || skipResource(origin))
			return true;
		//System.out.println(format("Copying %s", other.getFullPath()));
		final IPath path = convertPath(origin.getProjectRelativePath());
		if (origin instanceof IFile) {
			final IFile sourceFile = (IFile) origin;
			final IFile targetFile = destinationProject.getProject().getFile(path);
			try (InputStream contents = sourceFile.getContents()) {
				if (targetFile.exists())
					targetFile.setContents(contents, true, true, monitor);
				else
					targetFile.create(contents, true, monitor);
				if (sourceFile.getParent() == null || Definition.at(sourceFile.getParent()) == null) {
					final Structure struct = Structure.pinned(sourceFile, true, false);
					if (struct != null)
						conversions.add(new DeclarationConversion(sourceFile, targetFile, struct));
				}
			} catch (final Exception e) {
				out.println(format("Failed to convert contents of %s: %s", origin.getFullPath(), e.getMessage()));
				e.printStackTrace();
			}
			return true;
		} else if (origin instanceof IFolder) {
			final IFolder container = destinationProject.getProject().getFolder(path);
			if (!container.exists())
				container.create(true, true, monitor);
			final Definition def = Definition.at((IContainer) origin);
			if (def != null)
				conversions.add(new DeclarationConversion(origin, container, def));
			return true;
		} else
			return false;
	}
	private final CodeConverter codeConverter;
	private boolean skipResource(final IResource sourceResource) {
		return sourceResource.getName().equals(".project");
	}
}
