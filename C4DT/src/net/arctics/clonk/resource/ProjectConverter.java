package net.arctics.clonk.resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import net.arctics.clonk.Core;
import net.arctics.clonk.Core.IDocumentAction;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.ProjectConversionConfiguration;
import net.arctics.clonk.parser.ExprElm;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.parser.ExprElm.ITransformer;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.parser.c4script.ast.AccessVar;
import net.arctics.clonk.parser.c4script.ast.IDLiteral;
import net.arctics.clonk.parser.inireader.ActMapUnit;
import net.arctics.clonk.resource.c4group.C4Group.GroupType;
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
public class ProjectConverter implements IResourceVisitor {
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
	public ProjectConverter(IProject sourceProject, IProject destinationProject) {
		this.sourceProject = ClonkProjectNature.get(sourceProject);
		this.destinationProject = ClonkProjectNature.get(destinationProject);
		this.configuration = destinationEngine().projectConversionConfigurationForEngine(sourceEngine());
		assert(sourceEngine() != destinationEngine());
	}
	private IPath convertPath(IPath path) {
		IPath result = new Path("");
		for (int i = 0; i < path.segmentCount(); i++) {
			String segment = path.segment(i);
			GroupType groupType = sourceEngine().groupTypeForFileName(segment);
			if (groupType != GroupType.OtherGroup)
				segment = destinationEngine().groupName(StringUtil.rawFileName(segment), groupType);
			result = result.append(segment);
		}
		return result;
	}
	/**
	 * Perform the conversion, reporting progress back to a {@link IProgressMonitor}
	 * @param monitor The monitor to report progress to
	 */
	public void convert(IProgressMonitor monitor) {
		this.monitor = monitor;
		try {
			sourceProject.getProject().accept(this);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
	/**
	 * By letting this converter visit the source project the actual conversion is performed.
	 */
	@Override
	public boolean visit(IResource resource) throws CoreException {
		if (resource instanceof IProject || skipResource(resource))
			return true;
		IPath path = convertPath(resource.getProjectRelativePath());
		if (resource instanceof IFile) {
			IFile sourceFile = (IFile) resource;
			IFile file = destinationProject.getProject().getFile(path);
			if (!file.exists()) {
				InputStream contents = sourceFile.getContents();
				try {
					file.create(contents, true, monitor);
					convertFileContents(sourceFile, file);
				} finally {
					try {
						contents.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		} else if (resource instanceof IFolder) {
			IFolder container = destinationProject.getProject().getFolder(path);
			if (!container.exists())
				container.create(true, true, monitor);
		}
		return true;
	}
	private final CodeConverter codeConverter = new CodeConverter() {
		@Override
		protected ExprElm performConversion(C4ScriptParser parser, ExprElm expression) {
			if (configuration == null)
				return expression;
			return (ExprElm)(new ITransformer() {
				@Override
				public Object transform(ExprElm prev, Object prevT, ExprElm expression) {
					if (expression == null)
						return null;
					if (expression instanceof IDLiteral || (expression instanceof AccessVar && (((AccessVar)expression).proxiedDefinition()) != null)) {
						String mapped = configuration.idMap().get(expression.toString());
						if (mapped != null)
							return new AccessVar(mapped);
					}
					for (ProjectConversionConfiguration.CodeTransformation ct : configuration.transformations()) {
						Map<String, Object> matched = ct.template().match(expression);
						if (matched != null)
							return ct.transformation().transform(matched);
					}
					return expression.transformSubElements(this);
				}
			}).transform(null, null, expression);
		}
	};
	private boolean skipResource(IResource sourceResource) { return false; }
	private void convertFileContents(IFile sourceFile, IFile destinationFile) throws CoreException {
		final Script script = Script.get(sourceFile, true);
		if (script != null)
			Core.instance().performActionsOnFileDocument(destinationFile, new IDocumentAction<Object>() {
				@Override
				public Object run(IDocument document) {
					codeConverter.runOnDocument(script, null, new C4ScriptParser(script), document);
					if (script instanceof Definition) {
						Definition def = (Definition) script;
						ActMapUnit unit = (ActMapUnit) Structure.pinned(def.definitionFolder().findMember("ActMap.txt"), true, false);
					}
					return null;
				}
			});
	}
}
