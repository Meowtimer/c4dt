package net.arctics.clonk.builder;

import static java.lang.String.format;
import static net.arctics.clonk.util.Utilities.runWithoutAutoBuild;

import java.io.InputStream;
import java.util.Map;

import net.arctics.clonk.Core;
import net.arctics.clonk.Core.IDocumentAction;
import net.arctics.clonk.ProblemException;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ast.ITransformer;
import net.arctics.clonk.c4group.C4Group.GroupType;
import net.arctics.clonk.c4script.Directive;
import net.arctics.clonk.c4script.Directive.DirectiveType;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.ScriptParser;
import net.arctics.clonk.c4script.ast.AccessVar;
import net.arctics.clonk.c4script.ast.IDLiteral;
import net.arctics.clonk.c4script.ast.Tidy;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.ProjectConversionConfiguration;
import net.arctics.clonk.index.ProjectConversionConfiguration.CodeTransformation;
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
	public void convert(IProgressMonitor monitor) {
		this.monitor = monitor;
		runWithoutAutoBuild(this);
	}
	/**
	 * By letting this converter visit the source project the actual conversion is performed.
	 */
	@Override
	public boolean visit(IResource other) throws CoreException {
		if (other instanceof IProject || skipResource(other))
			return true;
		System.out.println(format("Copying %s", other.getFullPath()));
		final IPath path = convertPath(other.getProjectRelativePath());
		if (other instanceof IFile) {
			final IFile sourceFile = (IFile) other;
			final IFile file = destinationProject.getProject().getFile(path);
			try (InputStream contents = sourceFile.getContents()) {
				if (file.exists())
					file.setContents(contents, true, true, monitor);
				else
					file.create(contents, true, monitor);
				convertFileContents(sourceFile, file);
			} catch (final Exception e) {
				System.out.println(format("Failed to convert contents of %s", other.getFullPath()));
				e.printStackTrace();
			}
		} else if (other instanceof IFolder) {
			final IFolder container = destinationProject.getProject().getFolder(path);
			if (!container.exists())
				container.create(true, true, monitor);
		}
		return true;
	}
	private final CodeConverter codeConverter = new CodeConverter() {
		@Override
		protected ASTNode performConversion(ASTNode expression, Declaration declaration, final ICodeConverterContext context) {
			if (configuration == null)
				return expression;
			ASTNode node = (ASTNode)(new ITransformer() {
				@Override
				public Object transform(ASTNode prev, Object prevT, ASTNode expression) {
					if (expression == null)
						return null;
					if (expression instanceof IDLiteral || (expression instanceof AccessVar && (((AccessVar)expression).proxiedDefinition()) != null)) {
						final String mapped = configuration.idMap().get(expression.toString());
						if (mapped != null)
							return new AccessVar(mapped);
					}
					expression = expression.transformSubElements(this);
					for (final ProjectConversionConfiguration.CodeTransformation ct : configuration.transformations()) {
						boolean success = false;
						for (CodeTransformation c = ct; c != null; c = c.chain()) {
							final Map<String, Object> matched = c.template().match(expression);
							if (matched != null) {
								expression = c.transformation().transform(matched, context);
								success = true;
							}
						}
						if (success)
							break;
					}
					return expression;
				}
			}).transform(null, null, expression);
			if (node != null)
				try {
					node = new Tidy().tidyExhaustive(node);
				} catch (final CloneNotSupportedException e) {
					e.printStackTrace();
				}
			return node;
		}
	};
	private boolean skipResource(IResource sourceResource) {
		return sourceResource.getName().equals(".project");
	}
	private void convertFileContents(IFile sourceFile, IFile destinationFile) throws CoreException {
		final Script script = Script.get(sourceFile, true);
		if (script != null)
			Core.instance().performActionsOnFileDocument(destinationFile, new IDocumentAction<Object>() {
				@Override
				public Object run(IDocument document) {
					final ScriptParser parser = new ScriptParser(script);
					try {
						parser.parse();
					} catch (final ProblemException e) {
						e.printStackTrace();
					}
					for (final Directive d : script.directives())
						if (d.type() == DirectiveType.STRICT)
					codeConverter.runOnDocument(script, document);
					/*if (script instanceof Definition) {
						Definition def = (Definition) script;
						//ActMapUnit unit = (ActMapUnit) Structure.pinned(def.definitionFolder().findMember("ActMap.txt"), true, false);
					}*/
					return null;
				}
			}, true);
	}
}
