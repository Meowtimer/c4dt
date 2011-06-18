package net.arctics.clonk.debug;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.ui.editors.ClonkTextEditor;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.IConverter;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.ui.console.IConsole;
import org.eclipse.debug.ui.console.IConsoleLineTracker;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IHyperlink;
import org.eclipse.ui.ide.IDE;

public class ConsoleOutputLineTracker implements IConsoleLineTracker {

	private IConsole console;
	private IProject project;
	private ClonkProjectNature nature;
	private List<IResource> resourcesInRelevantProjects;
	
	@Override
	public void init(IConsole console) {
		this.console = console;
		String projName;
		try {
			projName = console.getProcess().getLaunch().getLaunchConfiguration().getAttribute(ClonkLaunchConfigurationDelegate.ATTR_PROJECT_NAME, "");
		} catch (CoreException e) {
			e.printStackTrace();
			return;
		}
		project = ResourcesPlugin.getWorkspace().getRoot().getProject(projName);
		nature = ClonkProjectNature.get(project);
		resourcesInRelevantProjects = getSubResourcesFromResourceCollection(ArrayUtil.map(nature.getIndex().relevantIndexes(), new IConverter<ClonkIndex, IResource>() {
			@Override
			public IResource convert(ClonkIndex from) {
				return from.getProject();
			}
		}), null);
	}
	
	private static class FileHyperlink implements IHyperlink {
		
		private IFile file;
		private int line;

		public FileHyperlink(IFile file, int line) {
			super();
			this.file = file;
			this.line = line;
		}

		@Override
		public void linkEntered() {}

		@Override
		public void linkExited() {}

		@Override
		public void linkActivated() {
			try {
				IEditorPart part = IDE.openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), file);
				if (part instanceof ClonkTextEditor) {
					((ClonkTextEditor)part).selectAndRevealLine(line);
				}
			} catch (PartInitException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	private static List<IResource> getSubResourcesFromResourceCollection(Iterable<IResource> parentResources, String parentName) {
		if (parentName != null)
			parentName = parentName.toUpperCase();
		List<IResource> result = new LinkedList<IResource>();
		for (IResource r : parentResources) {
			if (r instanceof IContainer && (parentName == null || r.getName().toUpperCase().equals(parentName)))
				try {
					result.addAll(Arrays.asList(((IContainer)r).members()));
				} catch (CoreException e) {
					e.printStackTrace();
				}
		}
		return result;
	}

	@Override
	public void lineAppended(IRegion line) {
		try {
			String lineStr = console.getDocument().get(line.getOffset(), line.getLength());
			try {
				createResourceLinksInLine(lineStr, resourcesInRelevantProjects, console, line);
			} catch (Exception e) {
				System.out.println("Failed for " + lineStr);
				e.printStackTrace();
			}
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}

	public static void createResourceLinksInLine(String lineStr, List<IResource> resourcesInRelevantProjects, IConsole console, IRegion lineRegion) {
		List<IResource> resourceCandidatesAtCurrentFolderLevel = new ArrayList<IResource>(resourcesInRelevantProjects.size());
		resourceCandidatesAtCurrentFolderLevel.addAll(resourcesInRelevantProjects);
		int folderNameCharacterIndex = 0;
		int folderNameStart = 0;
		int pathStart = 0;
		boolean started = false;
		for (int i = 0; i < lineStr.length(); i++) {
			char c = Character.toUpperCase(lineStr.charAt(i));
			if (c == '/' && started) {
				resourceCandidatesAtCurrentFolderLevel = getSubResourcesFromResourceCollection(resourceCandidatesAtCurrentFolderLevel, lineStr.substring(folderNameStart, i));
				if (resourceCandidatesAtCurrentFolderLevel.size() == 0) {
					resourceCandidatesAtCurrentFolderLevel.addAll(resourcesInRelevantProjects);
					pathStart = i+1;
				}
				folderNameCharacterIndex = 0;
				folderNameStart = i+1;
				started = false;
				continue;
			} else if (c == ':' && started && resourceCandidatesAtCurrentFolderLevel.size() > 0) {
				String lastPathPart = lineStr.substring(folderNameStart, i).toUpperCase();
				int lineStart;
				for (lineStart = ++i; i < lineStr.length() && Character.isDigit(lineStr.charAt(i)); i++);
				try {
					int lineNumber = Integer.parseInt(lineStr.substring(lineStart, i));
					for (IResource r : resourceCandidatesAtCurrentFolderLevel) {
						if (r instanceof IFile && r.getName().toUpperCase().equals(lastPathPart)) {
							console.addLink(new FileHyperlink((IFile) r, lineNumber), lineRegion.getOffset()+pathStart, i-pathStart);
							break;
						}
					}
				} catch (Exception e) {
					// probably no line number
					i++;
				}
				pathStart = folderNameStart = i--;
			}
			Iterator<IResource> it = resourceCandidatesAtCurrentFolderLevel.iterator();
			while (it.hasNext()) {
				IResource r = it.next();
				String n = r.getName();
				if (folderNameCharacterIndex >= n.length()) {it.remove(); continue;}
				char chr = n.charAt(folderNameCharacterIndex);
				if (Character.toUpperCase(chr) != c)
					it.remove();
			}
			if (resourceCandidatesAtCurrentFolderLevel.size() == 0) {
				folderNameCharacterIndex = 0;
				folderNameStart = pathStart = i+1;
				resourceCandidatesAtCurrentFolderLevel.addAll(resourcesInRelevantProjects);
				started = false;
			}
			else {
				folderNameCharacterIndex++;
				started = true;
			}
		}
	}

	@Override
	public void dispose() {
		console = null;
	}

}
