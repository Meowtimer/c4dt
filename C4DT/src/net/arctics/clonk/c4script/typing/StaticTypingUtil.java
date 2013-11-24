package net.arctics.clonk.c4script.typing;

import static java.lang.String.format;
import static net.arctics.clonk.util.StringUtil.multiply;
import static net.arctics.clonk.util.Utilities.defaulting;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.IASTVisitor;
import net.arctics.clonk.ast.TraversalContinuation;
import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.builder.ProjectSettings;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.ast.CastExpression;
import net.arctics.clonk.command.CommandFunction;
import net.arctics.clonk.util.StreamUtil;
import net.arctics.clonk.util.StreamUtil.StreamWriteRunnable;
import net.arctics.clonk.util.StringUtil;
import net.arctics.clonk.util.UI;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IRegion;

/**
 * Helper class to erase type annotations from C4ScriptST source files so that the resulting scripts will be accepted by the engine.
 * @author madeen
 *
 */
public class StaticTypingUtil {

	public static final QualifiedName ANNOTATION_LOCATIONS_PROPERTY = new QualifiedName(Core.PLUGIN_ID, "staticTypingAnnotations");

	/**
	 * If {@link #storeAnnotationLocations(IFile, List)} was called for the given file
	 * this function will return the contents of the file with typing annotations replaced with whitespace.
	 * If no information was stored for the file the result is null.
	 * @param file The file to operate on
	 * @return Contents of the file with typing annotations replaced with whitespace or null if no typing annotations stored for the file.
	 */
	public static String eraseTypeAnnotations(File file) {
		final IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(file.toURI());
		if (files == null || files.length == 0)
			return null;
		final Script script = Script.get(files[0], true);
		if (script == null)
			return null;
		return eraseTypeAnnotations(script);
	}

	public static String eraseTypeAnnotations(final Script script) {
		try {
			return internalErase(script);
		} catch (final Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static String internalErase(final Script script) {
		final List<TypeAnnotation> annotations = allTypeAnnotations(script);
		if (annotations != null) {

			// annotations
			final String text = StreamUtil.stringFromFile(script.file());
			final StringBuilder builder = new StringBuilder(text);
			for (int i = annotations.size()-1; i >= 0; i--) {
				final IRegion loc = annotations.get(i).absolute();
				builder.replace(loc.getOffset(), loc.getOffset()+loc.getLength(), StringUtil.multiply(" ", loc.getLength()));
			}

			// cast expressions
			script.traverse(new IASTVisitor<StringBuilder>() {
				@Override
				public TraversalContinuation visitNode(ASTNode node, StringBuilder context) {
					if (node instanceof CastExpression) {
						final IRegion absolute = node.absolute();
						final IRegion exprAbsolute = ((CastExpression) node).expression().absolute();
						final int preludeLen = exprAbsolute.getOffset()-absolute.getOffset();
						builder.replace(absolute.getOffset(), exprAbsolute.getOffset(), multiply(" ", preludeLen));
					}
					return TraversalContinuation.Continue;
				}
			}, builder);

			return builder.toString();
		}
		else
			return null;
	}

	private static List<TypeAnnotation> allTypeAnnotations(final Script script) {
		script.requireLoaded();
		@SuppressWarnings("serial")
		class Annots extends ArrayList<TypeAnnotation> implements IASTVisitor<Void> {
			public Annots() { super(defaulting(script.typeAnnotations(), Collections.<TypeAnnotation>emptyList())); }
			@Override
			public TraversalContinuation visitNode(ASTNode node, Void context) {
				if (node instanceof TypeAnnotation)
					this.add((TypeAnnotation)node);
				return TraversalContinuation.Continue;
			}
		};
		final Annots annots = new Annots();
		script.traverse(annots, null);
		return annots.size() > 0 ? annots : null;
	}

	public static File toFile(IResource resource) {
		return new File(resource.getLocation().toOSString());
	}

	/**
	 * Mirror a folder inside a project into a folder not managed by Eclipse, replacing files with typing annotations with versions processed via {@link #eraseTypeAnnotations(IFile)}.
	 * @param original The original folder which may contain scripts with type annotations
	 * @param mirror Mirror folder
	 * @param linkFiles Whether to link files with no typing annotations instead of copying them.
	 */
	public static void mirrorDirectoryWithTypingAnnotationsRemoved(File rawFolder, File mirrorFolder, boolean linkFiles) {
		mirrorFolder.mkdirs();
		for (final File originalFile : rawFolder.listFiles()) {
			if (originalFile.getName().startsWith("."))
				continue;
			final File destinationFile = new File(mirrorFolder, originalFile.getName());
			if (originalFile.isFile()) {
				final String erased = eraseTypeAnnotations(originalFile);
				if (erased != null)
					try {
						StreamUtil.writeToFile(destinationFile, new StreamWriteRunnable() {
							@Override
							public void run(File file, OutputStream stream, OutputStreamWriter writer) throws IOException {
								writer.write(erased);
							}
						});
					} catch (final IOException e1) {
						e1.printStackTrace();
					}
				else if (linkFiles && !Script.looksLikeScriptFile(originalFile.getName()))
					try {
						Files.createSymbolicLink(destinationFile.toPath(), originalFile.toPath());
					} catch (final IOException e) {
						System.out.println(format("Failed to link %s", originalFile));
					}
				else
					try {
						Files.copy(originalFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
					} catch (final IOException e) {
						System.out.println(format("Failed to copy %s", originalFile));
					}
			}
			else if (originalFile.isDirectory())
				mirrorDirectoryWithTypingAnnotationsRemoved(originalFile, destinationFile, linkFiles);
		}
	}

	@CommandFunction
	public static void MigrateToTyping(Object context, String projectName, String typingMode) {
		final Typing typing = Typing.valueOf(typingMode);
		final ClonkProjectNature nature = ClonkProjectNature.get(projectName);
		if (nature != null) {
			final ProjectSettings s = nature.settings();
			if (s.typing != typing) {
				s.migrationTyping = typing;
				nature.saveSettings();
				new WorkspaceJob(format("Migrating '%s' to static typing", projectName)) {
					@Override
					public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
						nature.getProject().build(IncrementalProjectBuilder.CLEAN_BUILD, monitor);
						return Status.OK_STATUS;
					}
				}.schedule();
			} else
				UI.message(format("'%s' is already in that typing mode", projectName));
		}
	}

	@CommandFunction
	public static void NoSTMirror(Object context, String project, String destinationFolder) {
		final IProject p = ResourcesPlugin.getWorkspace().getRoot().getProject(project);
		mirrorDirectoryWithTypingAnnotationsRemoved(toFile(p), new File(destinationFolder), true);
	}

	@CommandFunction
	public static  void SetTyping(Object context, String projectName, String typingMode) {
		final ClonkProjectNature nature = ClonkProjectNature.get(projectName);
		final Typing typing = Typing.valueOf(typingMode);
		nature.settings().typing = typing;
		nature.saveSettings();
	}
}
