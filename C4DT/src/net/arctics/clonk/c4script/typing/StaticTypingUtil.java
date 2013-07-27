package net.arctics.clonk.c4script.typing;

import static net.arctics.clonk.util.StringUtil.multiply;
import static net.arctics.clonk.util.Utilities.as;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.IASTVisitor;
import net.arctics.clonk.ast.SourceLocation;
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

import org.eclipse.core.resources.IContainer;
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
 * Helper class to purge type annotations from C4ScriptST source files so that the resulting scripts will be accepted by the engine.
 * @author madeen
 *
 */
public class StaticTypingUtil {

	public static final QualifiedName ANNOTATION_LOCATIONS_PROPERTY = new QualifiedName(Core.PLUGIN_ID, "staticTypingAnnotations");

	/**
	 * Retrieve list of static typing annotation locations for a given file saved earlier via {@link #storeAnnotationLocations(IFile, List)}
	 * @param file The file to retrieve the information for
	 * @return The list or null if some error occurred.
	 */
	public static List<TypeAnnotation> annotationLocations(IFile file) {
		final Script script = Script.get(file, true);
		if (script != null) {
			final List<TypeAnnotation> annotations = script.typeAnnotations();
			return annotations;
		} else
			return null;
	}

	/**
	 * If {@link #storeAnnotationLocations(IFile, List)} was called for the given file
	 * this function will return the contents of the file with typing annotations replaced with whitespace.
	 * If no information was stored for the file the result is null.
	 * @param file The file to operate on
	 * @return Contents of the file with typing annotations replaced with whitespace or null if no typing annotations stored for the file.
	 */
	public static String contentsWithAnnotationsPurged(IFile file) {
		final List<TypeAnnotation> annotationLocations = annotationLocations(file);
		if (annotationLocations != null) {
			final String text = StreamUtil.stringFromFile(file);
			final StringBuilder builder = new StringBuilder(text);
			for (int i = annotationLocations.size()-1; i >= 0; i--) {
				final SourceLocation loc = annotationLocations.get(i);
				builder.replace(loc.start(), loc.end(), StringUtil.repetitions(" ", loc.getLength()));
			}
			return builder.toString();
		}
		else
			return null;
	}

	/**
	 * Mirror a folder inside a project into a folder not managed by Eclipse, replacing files with typing annotations with versions processed via {@link #contentsWithAnnotationsPurged(IFile)}.
	 * @param original The original folder which may contain scripts with type annotations
	 * @param mirror Mirror folder
	 * @param linkFiles Whether to link files with no typing annotations instead of copying them.
	 */
	public static void mirrorDirectoryWithTypingAnnotationsRemoved(IContainer originalContainer, File mirrorFolder, boolean linkFiles) {
		mirrorFolder.mkdirs();
		final File rawFolder = new File(originalContainer.getLocation().toOSString());
		try {
			for (final IResource m : originalContainer.members()) {
				if (m.getName().startsWith("."))
					continue;
				final File destinationFile = new File(mirrorFolder, m.getName());
				final File originalFile = new File(rawFolder, m.getName());
				final IFile file = as(m, IFile.class);
				final IContainer folder = as(m, IContainer.class);
				if (file != null) {
					String _purged = contentsWithAnnotationsPurged(file);
					if (_purged != null)
						try {
							final Script script = Script.get(file, true);
							if (script != null) {
								final StringBuilder builder = new StringBuilder(_purged);
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
								_purged = builder.toString();
							}
							final String purged = _purged;
							StreamUtil.writeToFile(destinationFile, new StreamWriteRunnable() {
								@Override
								public void run(File file, OutputStream stream, OutputStreamWriter writer) throws IOException {
									writer.write(purged);
								}
							});
						} catch (final IOException e) {
							e.printStackTrace();
						}
					else if (linkFiles)
						try {
							Files.createSymbolicLink(destinationFile.toPath(), originalFile.toPath());
						} catch (final IOException e) {
							System.out.println(String.format("Failed to link %s", originalFile));
						}
					else
						try {
							Files.copy(originalFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
						} catch (final IOException e) {
							System.out.println(String.format("Failed to copy %s", originalFile));
						}
				}
				else if (folder != null)
					mirrorDirectoryWithTypingAnnotationsRemoved(folder, destinationFile, linkFiles);
			}
		} catch (final CoreException e) {
			e.printStackTrace();
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
				new WorkspaceJob(String.format("Migrating '%s' to static typing", projectName)) {
					@Override
					public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
						nature.getProject().build(IncrementalProjectBuilder.CLEAN_BUILD, monitor);
						return Status.OK_STATUS;
					}
				}.schedule();
			} else
				UI.message(String.format("'%s' is already in that typing mode", projectName));
		}
	}

	@CommandFunction
	public static void NoSTMirror(Object context, String project, String destinationFolder) {
		final IProject p = ResourcesPlugin.getWorkspace().getRoot().getProject(project);
		mirrorDirectoryWithTypingAnnotationsRemoved(p, new File(destinationFolder), true);
	}

	@CommandFunction
	public static  void SetTyping(Object context, String projectName, String typingMode) {
		final ClonkProjectNature nature = ClonkProjectNature.get(projectName);
		final Typing typing = Typing.valueOf(typingMode);
		nature.settings().typing = typing;
		nature.saveSettings();
	}
}
