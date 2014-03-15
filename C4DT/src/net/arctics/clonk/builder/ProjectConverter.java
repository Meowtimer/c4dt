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

import net.arctics.clonk.ast.Structure;
import net.arctics.clonk.c4group.C4Group.GroupType;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.ID;
import net.arctics.clonk.index.ProjectConversionConfiguration;
import net.arctics.clonk.ini.IniEntry;
import net.arctics.clonk.ini.IniSection;
import net.arctics.clonk.ini.IniUnit;
import net.arctics.clonk.landscapescript.LandscapeScript;
import net.arctics.clonk.util.StringUtil;

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

	class DefinitionConversion implements Runnable {
		final IFolder origin;
		final IFolder target;
		final Definition definition;
		public ID mapID() {
			final ID mapped = configuration.idMap().getOrDefault(definition.id(), null);
			if (mapped != null)
				return mapped;
			else
				return definition.id();
		}
		public DefinitionConversion(IFolder origin, IFolder target, Definition definition) {
			super();
			this.origin = origin;
			this.target = target;
			this.definition = definition;
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
			void convert() {
				converted = as(codeConverter.convert(original, original), Structure.class);
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
				final Map<IFile, FileConversion> conversions = new HashMap<>();
				Arrays.stream(origin.members(IResource.FILE)).filter(r -> r instanceof IFile).forEach(res -> {
					final IFile file = (IFile)res;
					final Structure struct = Structure.pinned(file, false, false);
					if (struct != null) {
						if (struct instanceof LandscapeScript)
							return; // can't handle that
						final IFile target = this.target.getFile(file.getName());
						if (target.exists())
							conversions.put(file, new FileConversion(struct, target));
					}
				});
				
				conversions.values().forEach(FileConversion::convert);
				
				final ID mapped = mapID();
				if (mapped != definition.id()) {
					final FileConversion defCore = conversions.get(findMemberCaseInsensitively(origin, "DefCore.txt"));
					final IniUnit defCoreUnit = defCore != null && defCore.converted instanceof IniUnit ? (IniUnit)defCore.converted : null;
					if (defCoreUnit != null) {
						final IniSection sec = defCoreUnit.sectionWithName("DefCore", false);
						final IniEntry entry = sec != null ? as(sec.map().getOrDefault("id", null), IniEntry.class) : null;
						if (entry != null)
							entry.value(mapped.stringValue());
					}
				}
				
				conversions.values().forEach(FileConversion::write);
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
	private final List<DefinitionConversion> definitions = new LinkedList<>();
	public ProjectConversionConfiguration configuration() { return configuration; }
	/**
	 * Create a new converter by specifying source and destination.
	 * @param sourceProject Source project
	 * @param destinationProject Destination project
	 */
	public ProjectConverter(final IProject sourceProject, final IProject destinationProject) {
		this.sourceProject = ClonkProjectNature.get(sourceProject);
		this.destinationProject = ClonkProjectNature.get(destinationProject);
		this.configuration = destinationEngine().projectConversionConfigurationForEngine(sourceEngine());
		this.codeConverter = new TransformationsBasedCodeConverter(configuration);
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
			definitions.forEach(DefinitionConversion::run);
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
			final IFile file = destinationProject.getProject().getFile(path);
			try (InputStream contents = sourceFile.getContents()) {
				if (file.exists())
					file.setContents(contents, true, true, monitor);
				else
					file.create(contents, true, monitor);
				//convertFileContents(sourceFile, file);
			} catch (final Exception e) {
				out.println(format("Failed to convert contents of %s: %s", origin.getFullPath(), e.getMessage()));
				e.printStackTrace();
			}
			return true;
		} else if (origin instanceof IFolder) {
			final IFolder container = destinationProject.getProject().getFolder(path);
			if (!container.exists())
				container.create(true, true, monitor);
			final Definition def = Definition.at(container);
			if (def != null)
				definitions.add(new DefinitionConversion((IFolder) origin, container, def));
			return true;
		} else
			return false;
	}
	private final CodeConverter codeConverter;
	private boolean skipResource(final IResource sourceResource) {
		return sourceResource.getName().equals(".project");
	}
}
