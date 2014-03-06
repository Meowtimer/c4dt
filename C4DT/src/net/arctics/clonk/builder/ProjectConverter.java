package net.arctics.clonk.builder;

import static java.lang.String.format;
import static java.lang.System.out;
import static net.arctics.clonk.util.Utilities.runWithoutAutoBuild;

import java.io.InputStream;

import net.arctics.clonk.Core;
import net.arctics.clonk.Core.IDocumentAction;
import net.arctics.clonk.c4group.C4Group.GroupType;
import net.arctics.clonk.c4script.Script;
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
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.IDocument;

/**
 * Converts projects for one engine into projects for another engine.
 * @author madeen
 *
 */
public class ProjectConverter implements IResourceVisitor, Runnable {
	private final ClonkProjectNature sourceProject, destinationProject;
	private IProgressMonitor monitor;
	private final ProjectConversionConfiguration configuration;
	private Engine sourceEngine() { return sourceProject.index().engine(); }
	private Engine destinationEngine() { return destinationProject.index().engine(); }
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
	public boolean visit(final IResource other) throws CoreException {
		if (other instanceof IProject || skipResource(other))
			return true;
		//System.out.println(format("Copying %s", other.getFullPath()));
		final IPath path = convertPath(other.getProjectRelativePath());
		if (other instanceof IFile) {
			final IFile sourceFile = (IFile) other;
			final IFile file = destinationProject.getProject().getFile(path);
			try (InputStream contents = sourceFile.getContents()) {
				if (file.exists())
					file.setContents(contents, true, true, monitor);
				else
					file.create(contents, true, monitor);
				//file.setCharset(sourceFile.getCharset(), monitor);
				convertFileContents(sourceFile, file);
			} catch (final Exception e) {
				out.println(format("Failed to convert contents of %s: %s", other.getFullPath(), e.getMessage()));
				e.printStackTrace();
			}
		} else if (other instanceof IFolder) {
			final IFolder container = destinationProject.getProject().getFolder(path);
			if (!container.exists())
				container.create(true, true, monitor);
		}
		return true;
	}
	private final CodeConverter codeConverter;
	private boolean skipResource(final IResource sourceResource) {
		return sourceResource.getName().equals(".project");
	}
	private void convertFileContents(final IFile sourceFile, final IFile destinationFile) throws CoreException {
		final Script sourceScript = Script.get(sourceFile, true);
		if (sourceScript != null)
			Core.instance().performActionsOnFileDocument(destinationFile, new IDocumentAction<Object>() {
				@Override
				public Object run(final IDocument document) {
					//final SelfContainedScript s = new SelfContainedScript(sourceScript.name(), document.get(), sourceScript.index());
					// passing the original script here, be careful about modifying it
					codeConverter.runOnDocument(sourceScript, document);
					/*if (script instanceof Definition) {
						Definition def = (Definition) script;
						//ActMapUnit unit = (ActMapUnit) Structure.pinned(def.definitionFolder().findMember("ActMap.txt"), true, false);
					}*/
					return null;
				}
			}, true);
	}
}
