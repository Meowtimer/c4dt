package net.arctics.clonk.resource;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.arctics.clonk.index.C4ObjectIntern;
import net.arctics.clonk.index.C4ObjectParser;
import net.arctics.clonk.index.ProjectIndex;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.c4script.C4ScriptIntern;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.C4Structure;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.resource.c4group.C4Group;
import net.arctics.clonk.resource.c4group.C4Group.C4GroupType;
import net.arctics.clonk.ui.editors.ClonkTextEditor;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonNavigator;

/**
 * An incremental builder for all project data.<br>
 * This builder launches the parser that indexes all
 * c4objects and highlights syntax errors in one project.<br>
 * Each project has its own ClonkBuilder instance.
 * @author ZokRadonh
 */
public class ClonkBuilder extends IncrementalProjectBuilder {
	
	private static final boolean INDEX_C4GROUPS = true;

	private static final class UIRefresher implements Runnable {
		
		private List<IResource> listOfResourcesToBeRefreshed;
		
		public UIRefresher(List<IResource> listOfResourcesToBeRefreshed) {
			super();
			this.listOfResourcesToBeRefreshed = listOfResourcesToBeRefreshed;
		}

		public void run() {
			IWorkbench w = PlatformUI.getWorkbench();
			for (IWorkbenchWindow window : w.getWorkbenchWindows()) {
				for (IWorkbenchPage page : window.getPages()) {
					for (IEditorReference ref : page.getEditorReferences()) {
						IEditorPart part = ref.getEditor(false);
						if (part != null && part instanceof ClonkTextEditor) {
							((ClonkTextEditor)part).refreshOutline();
						}
					}
				}
				CommonNavigator projectExplorer = Utilities.getProjectExplorer(window);
				if (projectExplorer != null) {
					for (IResource r : listOfResourcesToBeRefreshed) {
						projectExplorer.getCommonViewer().refresh(r);
					}
				}
			}
		}
	}

	private static class ResourceCounterAndCleaner extends ResourceCounter {
		public ResourceCounterAndCleaner(int countFlags) {
			super(countFlags);
		}
		@Override
		public boolean visit(IResource resource) throws CoreException {
			if (resource instanceof IContainer) {
				C4ObjectIntern obj = C4ObjectIntern.objectCorrespondingTo((IContainer) resource);
				if (obj != null)
					obj.setObjectFolder(null);
			}
			else if (resource instanceof IFile) {
				C4Structure.unPinFrom((IFile) resource);
			}
			return super.visit(resource);
		}
		@Override
		public boolean visit(IResourceDelta delta) throws CoreException {
			if (delta.getKind() == IResourceDelta.CHANGED)
				return super.visit(delta);
			return true;
		}
	}
	
	private class C4GroupStreamHandler implements IResourceDeltaVisitor, IResourceVisitor {
		
		public static final int OPEN = 0;
		public static final int CLOSE = 1;
		
		private int operation;
		
		public C4GroupStreamHandler(int operation) {
			this.operation = operation;
		}
		
		@Override
		public boolean visit(IResourceDelta delta) throws CoreException {
			IResource res = delta.getResource();
			return visit(res);
		}

		@Override
		public boolean visit(IResource res) throws CoreException {
			if (res.getParent() != null && res.getParent().equals(res.getProject()) && res instanceof IContainer) {
				URI uri = null;
				try {
					uri = res.getLocationURI();
				} catch (Exception e) {
					System.out.println(res.getFullPath().toString());
				}
				IFileStore store = EFS.getStore(uri);
				if (store instanceof C4Group) {
					C4Group group = (C4Group) store;
					try {
						switch (operation) {
						case OPEN:
							group.requireStream();
							break;
						case CLOSE:
							group.releaseStream();
							break;
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			return res instanceof IProject;
		}
	}
	
	public class ScriptGatherer implements IResourceDeltaVisitor, IResourceVisitor {
		public boolean visit(IResourceDelta delta) throws CoreException {
			if (delta == null) 
				return false;

			if (delta.getResource() instanceof IFile) {
				IFile file = (IFile) delta.getResource();
				C4ScriptBase script;
				switch (delta.getKind()) {
				case IResourceDelta.CHANGED: case IResourceDelta.ADDED:
					script = C4ScriptBase.get(file, true);
					if (script == null) {
						// create if new file
						IContainer folder = delta.getResource().getParent();
						C4ObjectParser objParser;
						// script in a resource group
						if (delta.getResource().getName().toLowerCase().endsWith(".c") && folder.getName().toLowerCase().endsWith(".c4g")) { //$NON-NLS-1$ //$NON-NLS-2$
							script = new C4ScriptIntern(file);
						}
						// object script
						else if (delta.getResource().getName().equals("Script.c") && (objParser = C4ObjectParser.create(folder)) != null) { //$NON-NLS-1$
							script = objParser.createObject();
						}
						// some other file but a script is still needed so get the object for the folder
						else {
							script = C4ObjectIntern.objectCorrespondingTo(folder);
						}
					}
					if (script != null && delta.getResource().equals(script.getScriptStorage())) {
						queueScript(script);
					} else
						processAuxiliaryFiles(file, script);
					// packed file
					//				else if (C4Group.getGroupType(file.getName()) != C4GroupType.OtherGroup) {
					//					try {
					//						C4Group g = C4Group.openFile(file);
					//						g.explode();
					//					} catch (IOException e) {
					//						e.printStackTrace();
					//					}
					//				}
					break;
				case IResourceDelta.REMOVED:
					script = C4ScriptIntern.scriptCorrespondingTo(file);
					if (script != null && file.equals(script.getScriptStorage()))
						script.getIndex().removeScript(script);
				}
				return true;
			}
			else if (delta.getResource() instanceof IContainer) {
				if (!INDEX_C4GROUPS)
					if (EFS.getStore(delta.getResource().getLocationURI()) instanceof C4Group)
						return false;
				// make sure the object has a reference to its folder (not to some obsolete deleted one)
				C4ObjectIntern object;
				switch (delta.getKind()) {
				case IResourceDelta.ADDED:
					object = C4ObjectIntern.objectCorrespondingTo((IContainer)delta.getResource());
					if (object != null)
						object.setObjectFolder((IContainer) delta.getResource());
					break;
				case IResourceDelta.REMOVED:
					// remove object when folder is removed
					object = C4ObjectIntern.objectCorrespondingTo((IContainer)delta.getResource());
					if (object != null)
						object.getIndex().removeObject(object);
					break;
				}
				return true;
			}
			return false;
		}
		public boolean visit(IResource resource) throws CoreException {
			if (resource instanceof IContainer) {
				if (!INDEX_C4GROUPS)
					if (EFS.getStore(resource.getLocationURI()) instanceof C4Group)
						return false;
				C4ObjectParser parser = C4ObjectParser.create((IContainer) resource);
				if (parser != null) { // is complete c4d (with DefCore.txt Script.c and Graphics)
					C4ObjectIntern object = parser.createObject();
					if (object != null) {
						queueScript(object);
					}
				}
				return true;
			}
			else if (resource instanceof IFile) {
				IFile file = (IFile) resource;
				if (resource.getName().toLowerCase().endsWith(".c") && C4Group.groupTypeFromFolderName(resource.getParent().getName()) == C4GroupType.ResourceGroup) { //$NON-NLS-1$ //$NON-NLS-2$
					C4ScriptBase script = C4ScriptIntern.pinnedScript(file, true);
					if (script == null) {
						script = new C4ScriptIntern(file);
					}
					queueScript(script);
					return true;
				}
				else if (processAuxiliaryFiles(file, C4ScriptBase.get(file, true))) {
					return true;
				}
			}
			return false;
		}
		private boolean processAuxiliaryFiles(IFile file, C4ScriptBase script) throws CoreException {
			boolean result = true;
			C4Structure structure;
			if ((structure = C4Structure.createStructureForFile(file, true)) != null) {
				structure.commitTo(script);
				structure.pinTo(file);
			}
			else if ((structure = C4Structure.pinned(file, false, true)) != null) {
				gatheredStructures.add(structure);
			}
			else {
				C4ObjectIntern obj = C4ObjectIntern.objectCorrespondingTo(file.getParent());
				if (obj != null) {
					try {
						obj.processFile(file);
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else {
					result = false;
				}
			}
			return result;
		}
	}

	private IProgressMonitor monitor;
	private IProgressMonitor currentSubProgressMonitor;

	// gathered list of scripts to be parsed
	private Map<C4ScriptBase, C4ScriptParser> parserMap = new HashMap<C4ScriptBase, C4ScriptParser>();
	// set of structures that have been validated during one build round - keeping track of them so when parsing dependent scripts, scripts that might lose some warnings
	// due to structure files having been revalidated can also be reparsed (string tables and such)
	private Set<C4Structure> gatheredStructures = new HashSet<C4Structure>();

	@Override
	protected void clean(IProgressMonitor monitor) throws CoreException {
		
		// clean up this project
		if (monitor != null) monitor.beginTask(Messages.CleaningUp, 1);
		IProject proj = this.getProject();
		if (proj != null) {
			ProjectIndex projIndex = ClonkProjectNature.get(proj).getIndex();
			proj.accept(new ResourceCounterAndCleaner(0));
			projIndex.clear();
		}
		if (monitor != null) {
			monitor.worked(1);
			monitor.done();
		}
	}

	@SuppressWarnings({"rawtypes"})
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {
		
		clearState();
		List<IResource> listOfResourcesToBeRefreshed = new LinkedList<IResource>();
		
		clearUIOfReferencesBeforeBuild();
		
		try {
			try {
				this.monitor = monitor;
				IProject proj = getProject();

				performBuildPhases(
					monitor, listOfResourcesToBeRefreshed, proj,
					getDelta(proj)
				);

				if (monitor.isCanceled()) {
					monitor.done();
					return null;
				}

				// mark index as dirty so it will be saved when eclipse is shut down
				ClonkProjectNature.get(proj).getIndex().setDirty(true);

				refreshUIAfterBuild(listOfResourcesToBeRefreshed);

				// validate files related to the scripts that have been parsed
				for (C4ScriptBase script : parserMap.keySet()) {
					validateRelatedFiles(script);
				}
				
				return new IProject[] { proj };
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		} finally {
			clearState();
		}
	}

	private static <T extends IResourceVisitor & IResourceDeltaVisitor> void visitDeltaOrWholeProject(IResourceDelta delta, IProject proj, T ultimateVisit0r) throws CoreException {
		if (delta != null) {
			delta.accept(ultimateVisit0r);
		} else{
			proj.accept(ultimateVisit0r);
		}
	}
	
	private void performBuildPhases(
		IProgressMonitor monitor,
		List<IResource> listOfResourcesToBeRefreshed,
		IProject proj,
		IResourceDelta delta
	) throws CoreException {

		// visit files to open C4Groups if files are contained in c4group file system
		visitDeltaOrWholeProject(delta, proj, new C4GroupStreamHandler(C4GroupStreamHandler.OPEN));
		try {

			// count num of resources to build
			ResourceCounter counter = new ResourceCounter(ResourceCounter.COUNT_CONTAINER);
			visitDeltaOrWholeProject(delta, proj, counter);

			// initialize progress monitor
			monitor.beginTask(String.format(Messages.BuildProject, proj.getName()), counter.getCount()*2);
			
			// populate toBeParsed list
			parserMap.clear();
			visitDeltaOrWholeProject(delta, proj, new ScriptGatherer());
			
			// parse declarations
			currentSubProgressMonitor = new SubProgressMonitor(monitor, parserMap.size());
			currentSubProgressMonitor.beginTask("Parse declarations", parserMap.size());
			for (C4ScriptBase script : parserMap.keySet()) {
				performBuildPhaseOne(script);
			}
			currentSubProgressMonitor.done();

			if (delta != null) {
				listOfResourcesToBeRefreshed.add(delta.getResource());
			}

			// refresh global func and static var cache
			ClonkProjectNature.get(proj).getIndex().refreshIndex();
			
			// parse function code
			currentSubProgressMonitor = new SubProgressMonitor(monitor, parserMap.size());
			currentSubProgressMonitor.beginTask("Parse code", parserMap.size());
			while (!parserMap.isEmpty()) {
				performBuildPhaseTwo(parserMap.keySet().iterator().next());
			}
			currentSubProgressMonitor.done();

			applyLatentMarkers();
			reparseDependentScripts();
			
			monitor.done();

		} finally {
			visitDeltaOrWholeProject(delta, proj, new C4GroupStreamHandler(C4GroupStreamHandler.CLOSE));
		}
	}

	private void clearState() {
		gatheredStructures.clear();
		parserMap.clear();
	}

	private void reparseDependentScripts() throws CoreException {
		Set<C4ScriptBase> scripts = new HashSet<C4ScriptBase>();
		for (C4ScriptParser parser: parserMap.values()) {
			for (C4ScriptBase dep : parser.getContainer().getIndex().dependentScripts(parser.getContainer())) {
				if (!parserMap.containsKey(dep)) {
					scripts.add(dep);
				}
			}
		}
		for (C4Structure s : gatheredStructures) {
			s.validate();
			if (s.requiresScriptReparse()) {
				C4ScriptBase script = C4ScriptBase.get(s.getResource(), true);
				if (script != null)
					scripts.add(script);
			}
		}
		IProgressMonitor dependentScriptsProgress = new SubProgressMonitor(this.monitor, scripts.size());
		dependentScriptsProgress.beginTask(Messages.ReparseDependentScripts, scripts.size());
		for (int buildPhase = 0; buildPhase < 2; buildPhase++) {
			for (C4ScriptBase s : scripts) {
				IResource r = s.getResource();
				if (r != null) {
					dependentScriptsProgress.subTask(s.toString());
					performBuildPhase(buildPhase, s);
				}
				dependentScriptsProgress.worked(1);
			}
		}
		dependentScriptsProgress.done();
	}

	private void applyLatentMarkers() {
		for (C4ScriptParser p : parserMap.values()) {
			p.applyLatentMarkers();
		}
	}

	private void validateRelatedFiles(C4ScriptBase script) throws CoreException {
		if (script instanceof C4ObjectIntern) {
			C4ObjectIntern obj = (C4ObjectIntern) script;
			for (IResource r : obj.getObjectFolder().members()) {
				if (r instanceof IFile) {
					C4Structure pinned = C4Structure.pinned((IFile) r, false, true);
					if (pinned != null)
						pinned.validate();
				}
			}
		}
	}

	private void clearUIOfReferencesBeforeBuild() {
		final ClonkBuilder builder = this;
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				try {
					IWorkbench w = PlatformUI.getWorkbench();
					for (IWorkbenchWindow window : w.getWorkbenchWindows()) {
						if (window.getActivePage() != null) {
							IWorkbenchPage page = window.getActivePage();
							for (IEditorReference ref : page.getEditorReferences()) {
								IEditorPart part = ref.getEditor(false);
								if (part != null && part instanceof ClonkTextEditor) {
									ClonkTextEditor ed = (ClonkTextEditor) part;
									// only if building the project this element is declared in
									if (
										ed.getTopLevelDeclaration() != null &&
										ed.getTopLevelDeclaration().getResource() != null &&
										builder.getProject() == ed.getTopLevelDeclaration().getResource().getProject()
									)
										ed.clearOutline();
								}
							}
						}
					}
				}
				finally {
					synchronized (builder) {
						builder.notify();
					}
				}
			}
		});
	}
	
	private void refreshUIAfterBuild(List<IResource> listOfResourcesToBeRefreshed) {
		// refresh outlines
		Display.getDefault().asyncExec(new UIRefresher(listOfResourcesToBeRefreshed));
	}
	
	private void queueScript(C4ScriptBase script) {
		if (!parserMap.containsKey(script)) {
			IStorage storage = script.getScriptStorage();
			parserMap.put(script, storage != null ? new C4ScriptParser(script) : null);
		}
	}
	
	private void performBuildPhaseOne(C4ScriptBase script) {
		C4ScriptParser parser = parserMap.get(script);
		ClonkProjectNature.get(getProject()).getIndex().addScript(script);
		if (parser != null) {
			parser.clean();
			parser.parseDeclarations();
		}
		if (currentSubProgressMonitor != null) {
			currentSubProgressMonitor.worked(1);
		}
	}
	
	private void performBuildPhaseTwo(C4ScriptBase script) {
		if (parserMap.containsKey(script)) {
			C4ScriptParser parser = parserMap.remove(script);
			if (parser != null) {
				try {
					parser.parseCodeOfFunctionsAndValidate();
				} catch (ParsingException e) {
					e.printStackTrace();
				}
			}
			if (currentSubProgressMonitor != null) {
				currentSubProgressMonitor.worked(1);
			}
		}
	}
	
	private void performBuildPhase(int phase, C4ScriptBase script) {
		switch (phase) {
		case 0:
			performBuildPhaseOne(script);
			break;
		case 1:
			performBuildPhaseTwo(script);
			break;
		}
	}

}