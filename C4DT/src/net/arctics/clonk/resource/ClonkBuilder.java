package net.arctics.clonk.resource;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.C4Object;
import net.arctics.clonk.index.C4ObjectIntern;
import net.arctics.clonk.index.C4ObjectParser;
import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.index.ExternalLibsLoader;
import net.arctics.clonk.index.ProjectIndex;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.c4script.C4ScriptIntern;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.C4Structure;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.preferences.ClonkPreferences;
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
public class ClonkBuilder extends IncrementalProjectBuilder implements IResourceDeltaVisitor, IResourceVisitor {
	
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
	
	private int buildPhase;
	private IProgressMonitor monitor;
	private boolean cleanedUI;

	// keeps track of parsers created for specific scripts
	private Map<C4ScriptBase, C4ScriptParser> parserMap = new HashMap<C4ScriptBase, C4ScriptParser>();
	
	private String[] externalLibs;

	public ClonkBuilder() {
		super();
		// ensure lib builder object
		ClonkCore.getDefault().getLibBuilder();
	}

	public void worked(int count) {
		monitor.worked(count);
	}

	@Override
	protected void clean(IProgressMonitor monitor) throws CoreException {
		// clean external libs - this does not rebuild
		ClonkCore.getDefault().getLibBuilder().clean();
		
		// clean up this project
		if (monitor != null) monitor.beginTask(Messages.CleaningUp, 1);
		IProject proj = this.getProject();
		if (proj != null) {
			ProjectIndex projIndex = ClonkProjectNature.get(proj).getIndex();
			externalLibs = projIndex.getLibPaths();
			projIndex.clear();
			proj.accept(new ResourceCounterAndCleaner(0));
		}
		if (monitor != null) {
			monitor.worked(1);
			monitor.done();
		}
	}

	@SuppressWarnings({"rawtypes"})
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {
		
		parserMap.clear();
		List<IResource> listOfResourcesToBeRefreshed = new LinkedList<IResource>();
		
		synchronized (this) {
			clearUIOfReferencesBeforeBuild();
			while (!cleanedUI)
				try {
					wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
		}
		
		try {
			try {
				this.monitor = monitor;
				IProject proj = getProject();

				switch(kind) {
				case AUTO_BUILD:
				case INCREMENTAL_BUILD:
					IResourceDelta delta = getDelta(proj);
					if (delta != null) {
						delta.accept(new C4GroupStreamHandler(C4GroupStreamHandler.OPEN));
						try {

							// count num of resources to build
							ResourceCounter counter = new ResourceCounter(ResourceCounter.COUNT_CONTAINER);
							delta.accept(counter);

							// initialize progress monitor
							monitor.beginTask(String.format(Messages.BuildProject, proj.getName()), counter.getCount());

							// parse declarations
							buildPhase = 0;
							delta.accept(this);
							listOfResourcesToBeRefreshed.add(delta.getResource());
							// refresh global func and static var cache
							ClonkProjectNature.get(proj).getIndex().refreshCache();

							// parse function code
							buildPhase = 1;
							delta.accept(this);
							applyLatentMarkers();
							reparseDependentScripts(monitor);

						} finally {
							delta.accept(new C4GroupStreamHandler(C4GroupStreamHandler.CLOSE));
						}
					}
					break;

				case FULL_BUILD:
					proj.accept(new C4GroupStreamHandler(C4GroupStreamHandler.OPEN));
					try {
						ProjectIndex projIndex = ClonkProjectNature.get(proj).getIndex();

						// calculate build duration
						int[] operations = new int[5];

						// count num of resources to build and also clean...
						ResourceCounterAndCleaner counter = new ResourceCounterAndCleaner(ResourceCounter.COUNT_CONTAINER);
						proj.accept(counter);
						operations[0] = counter.getCount() * 2;
						operations[1] = counter.getCount();

						operations[2] = 0;
						operations[3] = 0;
						ClonkLibBuilder libBuilder = ClonkCore.getDefault().getLibBuilder();
						if (libBuilder.isBuildNeeded()) {
							String[] externalLibs = getExternalLibNames();
							for(String lib : externalLibs) {
								File file = new File(lib);
								if (file.exists()) {
									operations[2] += (int) (file.length() / 7000); // approximate time
								}
							}
							operations[3] = operations[2] / 2; // approximate save time
						}

						operations[4] = externalLibs != null ? externalLibs.length : 0;

						int workSum = 0;
						for (int work : operations)
							workSum += work;

						// initialize progress monitor
						monitor.beginTask(String.format(Messages.BuildProject, proj.getName()), workSum);

						// build external lib if needed
						if (libBuilder.isBuildNeeded()) {
							monitor.subTask(Messages.ParsingLibraries);
							libBuilder.build(new SubProgressMonitor(monitor,operations[2]));

							monitor.subTask(Messages.SavingLibraries);
							ClonkCore.getDefault().saveExternIndex(new SubProgressMonitor(monitor,operations[3]));
						}

						if (externalLibs != null)
							ExternalLibsLoader.readExternalLibs(projIndex, new SubProgressMonitor(monitor, operations[4]), externalLibs);

						// build project
						monitor.subTask(String.format(Messages.IndexProject, proj.getName()));
						// parse declarations
						buildPhase = 0;
						proj.accept(this);
						ClonkProjectNature.get(proj).getIndex().refreshCache();
						if (monitor.isCanceled()) {
							monitor.done();
							return null;
						}
						monitor.subTask(String.format(Messages.ParseProject, proj.getName()));
						// parse function code
						buildPhase = 1;
						proj.accept(this);

						applyLatentMarkers();

						listOfResourcesToBeRefreshed.add(proj);
					}
					finally {
						proj.accept(new C4GroupStreamHandler(C4GroupStreamHandler.CLOSE));
					}
					break;

				}

				if (monitor.isCanceled()) {
					monitor.done();
					return null;
				}
				monitor.subTask(Messages.SavingData);

				// mark index as dirty so it will be saved when eclipse is shut down
				ClonkProjectNature.get(proj).getIndex().setDirty(true);

				monitor.done();

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
			parserMap.clear();
		}
	}

	private void reparseDependentScripts(IProgressMonitor monitor) throws CoreException {
		Set<C4ScriptBase> scripts = new HashSet<C4ScriptBase>();
		for (C4ScriptParser parser : parserMap.values()) {
			for (C4ScriptBase dep : parser.getContainer().getIndex().dependentScripts(parser.getContainer())) {
				if (parserMap.get(dep) == null)
					scripts.add(dep);
			}
		}
		monitor.beginTask(Messages.ReparseDependentScripts, scripts.size());
		for (buildPhase = 0; buildPhase < 2; buildPhase++) {
			for (C4ScriptBase s : scripts) {
				IResource r = s.getResource();
				if (r != null) {
					monitor.subTask(s.toString());
					//System.out.println("Triggered reparsing of " + r);
					this.visit(r);
				}
				monitor.worked(1);
			}
		}
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
		cleanedUI = false;
		final ClonkBuilder builder = this;
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				try {
					IWorkbench w = PlatformUI.getWorkbench();
					if (w != null && w.getActiveWorkbenchWindow() != null && w.getActiveWorkbenchWindow().getActivePage() != null) {
						IWorkbenchPage page = w.getActiveWorkbenchWindow().getActivePage();
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
				finally {
					cleanedUI = true;
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

	private String[] getExternalLibNames() {
		String optionString = ClonkCore.getDefault().getPreferenceStore().getString(ClonkPreferences.STANDARD_EXT_LIBS);
		return optionString.split("<>"); //$NON-NLS-1$
	}

	private C4ScriptParser getParserFor(C4ScriptBase script) {
		C4ScriptParser result = parserMap.get(script);
		if (result == null && script != null && script.getScriptFile() instanceof IFile)
			parserMap.put(script, result = new C4ScriptParser(script));
		return result;
	}

	public boolean visit(IResourceDelta delta) throws CoreException {
		if (delta == null) 
			return false;

		if (delta.getResource() instanceof IFile) {
			IFile file = (IFile) delta.getResource();
			C4ScriptBase script;
			switch (delta.getKind()) {
			case IResourceDelta.CHANGED: case IResourceDelta.ADDED:
				script = Utilities.getScriptForFile(file);
				if (script == null && buildPhase == 0) {
					// create if new file
					IContainer folder = delta.getResource().getParent();
					C4ObjectParser objParser;
					// script in a resource group
					if (delta.getResource().getName().toLowerCase().endsWith(".c") && folder.getName().toLowerCase().endsWith(".c4g")) { //$NON-NLS-1$ //$NON-NLS-2$
						script = new C4ScriptIntern(file);
						ClonkProjectNature.get(delta.getResource()).getIndex().addScript(script);
					}
					// object script
					else if (delta.getResource().getName().equals("Script.c") && (objParser = C4ObjectParser.create(folder)) != null) {
						script = objParser.createObject();
						objParser.parseScript(getParserFor(script));
					}
					// some other file but a script is still needed so get the object for the folder
					else {
						script = C4ObjectIntern.objectCorrespondingTo(folder);
					}
				}
				if (script != null && delta.getResource().equals(script.getScriptFile())) {
					if (script != null) {
						C4ScriptParser parser = getParserFor(script);
						// phase 0: clean and parse declarations
						// phase 1: parse code of functions
						switch (buildPhase) {
						case 0:
							parser.clean();
							parser.parseDeclarations();
							break;
						case 1:
							try {
								parser.parseCodeOfFunctions();
							} catch (ParsingException e) {
								e.printStackTrace();
							}
						}
					}
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
				if (buildPhase == 0) {
					script = C4ScriptIntern.scriptCorrespondingTo(file);
					if (script != null && file.equals(script.getScriptFile()))
						script.getIndex().removeScript(script);
				}
			}
			if (monitor != null)
				monitor.worked(1);
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

	private boolean processAuxiliaryFiles(IFile file, C4ScriptBase script) throws CoreException {
		C4Structure structure;
		if (buildPhase == 0 && (structure = C4Structure.createStructureForFile(file, true)) != null) {
			structure.commitTo(script);
			structure.pinTo(file);
			return true;
		}
		else if (buildPhase == 1 && (structure = C4Structure.pinned(file, false, true)) != null) {
			structure.validate();
			return true;
		}
		else if (buildPhase == 0) {
			C4ObjectIntern obj = C4ObjectIntern.objectCorrespondingTo(file.getParent());
			if (obj != null)
				try {
					obj.processFile(file);
				} catch (IOException e) {
					e.printStackTrace();
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
			switch (buildPhase) {
			case 0:
				// first phase: just gather declarations
				C4ObjectParser parser = C4ObjectParser.create((IContainer) resource);
				if (parser != null) { // is complete c4d (with DefCore.txt Script.c and Graphics)
					C4ObjectIntern object = parser.createObject();
					{
						C4ScriptParser scriptParser = getParserFor(object);
						if (scriptParser == null && object.getScriptFile() != null) {
							parserMap.put(object, scriptParser = new C4ScriptParser(object));
						}
						parser.parseScript(scriptParser);
					}
				}
				if (monitor != null) monitor.worked(2);
				return true;
			case 1:
				// check correctness of function code
				ClonkIndex index = ClonkProjectNature.get(resource).getIndex();
				C4Object obj = index.getObject((IContainer)resource);
				IFile scriptFile = (IFile) ((obj != null) ? obj.getScriptFile() : null);
				if (scriptFile != null) {
					try {
						getParserFor(obj).parseCodeOfFunctions();
					} catch (ParsingException e) {
						e.printStackTrace();
					}
				}
				if (monitor != null) monitor.worked(1);
				return true;
			}
		}
		else if (resource instanceof IFile) {
			IFile file = (IFile) resource;
			if (resource.getName().toLowerCase().endsWith(".c") && C4Group.groupTypeFromFolderName(resource.getParent().getName()) == C4GroupType.ResourceGroup) { //$NON-NLS-1$ //$NON-NLS-2$
				C4ScriptBase script = C4ScriptIntern.pinnedScript(file, true);
				switch (buildPhase) {
				case 0:
					if (script == null) {
						script = new C4ScriptIntern(file);
					}
					ClonkProjectNature.get(resource).getIndex().addScript(script);
					C4ScriptParser parser = getParserFor(script);
					parser.clean();
					parser.parseDeclarations();
					return true;
				case 1:
					if (script != null) {
						try {
							getParserFor(script).parseCodeOfFunctions();
						} catch (ParsingException e) {
							e.printStackTrace();
						}
					}
					return true;
				}
			}
			else if (processAuxiliaryFiles(file, Utilities.getScriptForFile(file)))
				return true;
		}
		return false;
	}

}