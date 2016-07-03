package net.arctics.clonk.debug;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.ui.editors.StructureTextEditor;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.ui.console.IConsole;
import org.eclipse.debug.ui.console.IConsoleLineTracker;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IHyperlink;
import org.eclipse.ui.ide.IDE;

/**
 * Tracks console output and inserts links to file locations.
 * This works for complete project-relative paths such as Objects.ocd/Clonk.ocd/Script.c:123
 * @author madeen
 *
 */
public class ConsoleOutputLineTracker implements IConsoleLineTracker {
	private IConsole console;
	private IProject project;
	private ClonkProjectNature nature;
	private List<IResource> resourcesInRelevantProjects;
	private final Pattern lineNumberCharacterMessage = Pattern.compile("(\\d+)(:\\d+)?");

	@Override
	public void init(final IConsole console) {
		this.console = console;
		String projName;
		try {
			projName = console.getProcess().getLaunch().getLaunchConfiguration().getAttribute(ClonkLaunchConfigurationDelegate.ATTR_PROJECT_NAME, "");
		} catch (final CoreException e) {
			e.printStackTrace();
			return;
		}
		project = ResourcesPlugin.getWorkspace().getRoot().getProject(projName);
		nature = ClonkProjectNature.get(project);
		resourcesInRelevantProjects = projectResources();
	}

	private List<IResource> projectResources() {
		final List<IResource> result = new LinkedList<>();
		for (final Index ndx : nature.index().relevantIndexes()) {
			final IProject proj = ndx.nature() != null ? ndx.nature().getProject() : null;
			if (proj != null)
				try {
					for (final IResource r : proj.members(IResource.FOLDER))
						result.add(r);
				} catch (final CoreException e) {
					e.printStackTrace();
				}
		}
		return result;
	}

	private static class FileHyperlink implements IHyperlink {
		private final IFile file;
		private final int line, character;
		public FileHyperlink(final IFile file, final int line, final int character) {
			super();
			this.file = file;
			this.line = line;
			this.character = character;
		}
		@Override
		public void linkEntered() {}
		@Override
		public void linkExited() {}
		@Override
		public void linkActivated() {
			try {
				final IEditorPart part = IDE.openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), file);
				if (part instanceof StructureTextEditor) {
					final StructureTextEditor ed = (StructureTextEditor) part;
					final IDocument d = ed.sourceViewer().getDocument();
					try {
						final int lineOffset = d.getLineOffset(line-1);
						final IRegion r = new Region(lineOffset+character-1, 0);
						ed.selectAndReveal(r);
					} catch (final BadLocationException e) {
						e.printStackTrace();
					}
				}
			} catch (final PartInitException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private static List<IResource> getSubResourcesFromResourceCollection(final Iterable<IResource> parentResources, String parentName) {
		if (parentName != null)
			parentName = parentName.toUpperCase();
		final List<IResource> result = new LinkedList<IResource>();
		for (final IResource r : parentResources)
			if (r instanceof IContainer && (parentName == null || r.getName().toUpperCase().equals(parentName)))
				try {
					result.addAll(Arrays.asList(((IContainer)r).members()));
				} catch (final CoreException e) {
					e.printStackTrace();
				}
		return result;
	}

	@Override
	public void lineAppended(final IRegion line) {
		try {
			final String lineStr = console.getDocument().get(line.getOffset(), line.getLength());
			try {
				createResourceLinksInLine(lineStr, resourcesInRelevantProjects, console, line);
			} catch (final Exception e) {
				System.out.println("Failed for " + lineStr);
				e.printStackTrace();
			}
		} catch (final BadLocationException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create resource links in the specified line.
	 * @param input The console output line as a string
	 * @param resourcesInRelevantProjects List of top-level resources whose contained files are considered valid link targets. Those would include all top-level folders in relevant projects.
	 * @param console The {@link IConsole} links will be added to
	 * @param lineRegion Region of the line in the console's text
	 */
	private void createResourceLinksInLine(final String input, final List<IResource> resourcesInRelevantProjects, final IConsole console, final IRegion lineRegion) {
		List<IResource> candidates = new ArrayList<IResource>(resourcesInRelevantProjects);
		int folderNameCharacterIndex = 0;
		int folderNameStart = 0;
		int pathStart = 0;
		boolean started = false;
		for (int i = 0; i < input.length(); i++) {
			final char c = Character.toUpperCase(input.charAt(i));
			if ((c == '/' || c == '\\') && started) {
				candidates = getSubResourcesFromResourceCollection(candidates, input.substring(folderNameStart, i));
				if (candidates.size() == 0) {
					candidates.addAll(resourcesInRelevantProjects);
					pathStart = i+1;
				}
				folderNameCharacterIndex = 0;
				folderNameStart = i+1;
				started = false;
				continue;
			} else if (c == ':' && started && candidates.size() > 0) {
				final String lastPathPart = input.substring(folderNameStart, i).toUpperCase();
				final Matcher m = lineNumberCharacterMessage.matcher(input.substring(i+1));
				if (m.lookingAt())
					try {
						final int line = Integer.parseInt(m.group(1));
						final int character = m.group(2) != null ? Integer.parseInt(m.group(2).substring(1)) : 1;
						for (final IResource r : candidates)
							if (r instanceof IFile && r.getName().toUpperCase().equals(lastPathPart)) {
								console.addLink(new FileHyperlink((IFile) r, line, character), lineRegion.getOffset()+pathStart, i-pathStart+m.group().length()+1);
								break;
							}
					} catch (final Exception e) {
						// probably no line number
						i++;
					}
				else
					pathStart = folderNameStart = i;
			}
			final Iterator<IResource> it = candidates.iterator();
			while (it.hasNext()) {
				final IResource r = it.next();
				final String n = r.getName();
				if (folderNameCharacterIndex >= n.length()) {it.remove(); continue;}
				final char chr = n.charAt(folderNameCharacterIndex);
				if (Character.toUpperCase(chr) != c)
					it.remove();
			}
			if (candidates.isEmpty()) {
				folderNameCharacterIndex = 0;
				folderNameStart = pathStart = i+1;
				candidates.addAll(resourcesInRelevantProjects);
				started = false;
			}
			else {
				folderNameCharacterIndex++;
				started = true;
			}
		}
	}

	@Override
	public void dispose() { console = null; }
}
