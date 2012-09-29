package net.arctics.clonk.parser.c4script.statictyping;

import static net.arctics.clonk.util.ArrayUtil.map;
import static net.arctics.clonk.util.Utilities.as;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.util.IConverter;
import net.arctics.clonk.util.StreamUtil;
import net.arctics.clonk.util.StreamUtil.StreamWriteRunnable;
import net.arctics.clonk.util.StringUtil;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;

/**
 * Helper class to purge type annotations from C4ScriptST source files so that the resulting scripts will be accepted by the engine.
 * @author madeen
 *
 */
public class StaticTypingUtil {
	
	public static final QualifiedName ANNOTATION_LOCATIONS_PROPERTY = new QualifiedName(Core.PLUGIN_ID, "staticTypingAnnotations");
	
	/**
	 * Store information about static typing annotation locations for the given file.
	 * @param file The file to store the information for
	 * @param annotationLocations The list of static typing annotation locations
	 */
	public static void storeAnnotationLocations(IFile file, List<? extends SourceLocation> annotationLocations) {
		String text = StringUtil.blockString("", "", ";", annotationLocations);
		try {
			file.setPersistentProperty(ANNOTATION_LOCATIONS_PROPERTY, text);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Retrieve list of static typing annotation locations for a given file saved earlier via {@link #storeAnnotationLocations(IFile, List)}
	 * @param file The file to retrieve the information for
	 * @return The list or null if some error occured.
	 */
	public static List<SourceLocation> annotationLocations(IFile file) {
		try {
			String annotationsAsText = file.getPersistentProperty(ANNOTATION_LOCATIONS_PROPERTY);
			if (annotationsAsText == null)
				return null;
			return Arrays.asList(map(annotationsAsText.split(";"), SourceLocation.class, new IConverter<String, SourceLocation>() {
				@Override
				public SourceLocation convert(String from) {
					return new SourceLocation(from);
				}
			}));
		} catch (CoreException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * If {@link #storeAnnotationLocations(IFile, List)} was called for the given file
	 * this function will return the contents of the file with typing annotations replaced with whitespace.
	 * If no information was stored for the file the result is null. 
	 * @param file The file to operate on 
	 * @return Contents of the file with typing annotations replaced with whitespace or null if no typing annotations stored for the file.
	 */
	public static String contentsWithAnnotationsPurged(IFile file) {
		List<SourceLocation> annotationLocations = annotationLocations(file);
		String text = StreamUtil.stringFromFile(file);
		if (annotationLocations != null) {
			StringBuilder builder = new StringBuilder(text);
			for (int i = annotationLocations.size()-1; i >= 0; i--) {
				SourceLocation loc = annotationLocations.get(i);
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
		File rawFolder = new File(originalContainer.getLocation().toOSString());
		try {
			for (IResource m : originalContainer.members()) {
				File destinationFile = new File(mirrorFolder, m.getName());
				File originalFile = new File(rawFolder, m.getName());
				IFile file = as(m, IFile.class);
				IContainer folder = as(m, IContainer.class);
				if (file != null) {
					final String annotationPurgedContents = contentsWithAnnotationsPurged(file);
					if (annotationPurgedContents != null)
						try {
							StreamUtil.writeToFile(destinationFile, new StreamWriteRunnable() {
								@Override
								public void run(File file, OutputStream stream, OutputStreamWriter writer) throws IOException {
									writer.write(annotationPurgedContents);
								}
							});
						} catch (IOException e) {
							e.printStackTrace();
						}
					else if (linkFiles)
						try {
							Files.createSymbolicLink(destinationFile.toPath(), originalFile.toPath());
						} catch (IOException e) {
							System.out.println(String.format("Failed to link %s", originalFile));
						}
					else
						try {
							Files.copy(originalFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
						} catch (IOException e) {
							System.out.println(String.format("Failed to copy %s", originalFile));
						}
				}
				else if (folder != null)
					mirrorDirectoryWithTypingAnnotationsRemoved(folder, destinationFile, linkFiles);
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
}
