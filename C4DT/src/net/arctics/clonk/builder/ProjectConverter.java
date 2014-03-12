package net.arctics.clonk.builder;

import static java.lang.String.format;
import static java.lang.System.out;
import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.runWithoutAutoBuild;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.ast.Structure;
import net.arctics.clonk.c4group.C4Group.GroupType;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.ProjectConversionConfiguration;
import net.arctics.clonk.ui.editors.actions.c4script.CodeConverter;
import net.arctics.clonk.util.StringUtil;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
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
		public DefinitionConversion(IFolder origin, IFolder target, Definition definition) {
			super();
			this.origin = origin;
			this.target = target;
			this.definition = definition;
		}
		@Override
		public void run() {
			try {
				final NullProgressMonitor npm = new NullProgressMonitor();
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
					void write() throws CoreException {
						final String convString = converted.printed();
						target.setContents(new ByteArrayInputStream(convString.getBytes()), true, true, npm);
					}
				}
				final List<FileConversion> conversions = new ArrayList<>();
				for (final IResource res : origin.members(IResource.FILE)) {
					final IFile file = (IFile)res;
					final Structure struct = Structure.pinned(file, false, false);
					if (struct != null) {
						final IFile target = this.target.getFile(file.getName());
						if (target.exists())
							conversions.add(new FileConversion(struct, target));
					}
				}
				for (final FileConversion c : conversions)
					c.convert();
				for (final FileConversion c : conversions)
					c.write();
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
	private final List<DefinitionConversion> folderConversions = new LinkedList<>();
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
			for (final DefinitionConversion fconv : folderConversions)
				fconv.run();
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
				folderConversions.add(new DefinitionConversion((IFolder) origin, container, def));
			return true;
		} else
			return false;
	}
	private final CodeConverter codeConverter;
	private boolean skipResource(final IResource sourceResource) {
		return sourceResource.getName().equals(".project");
	}
}
