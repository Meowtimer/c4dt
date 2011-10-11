package net.arctics.clonk.resource;

import static net.arctics.clonk.util.ArrayUtil.listFromIterable;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.DefinitionParser;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.ProjectIndex;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.ID;
import net.arctics.clonk.parser.IHasIncludes;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.parser.c4script.SystemScript;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.refactoring.RenameDeclarationProcessor;
import net.arctics.clonk.resource.c4group.C4Group;
import net.arctics.clonk.resource.c4group.C4Group.GroupType;
import net.arctics.clonk.ui.editors.ClonkTextEditor;
import net.arctics.clonk.ui.editors.actions.c4script.RenameDeclarationAction;
import net.arctics.clonk.util.Pair;
import net.arctics.clonk.util.SynchronizedCounter;
import net.arctics.clonk.util.UI;

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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonNavigator;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * An incremental builder for all project data.<br>
 * This builder launches the parser that indexes all
 * {@link Definition}s and standalone scripts and highlights syntax errors in one project.<br>
 * Each project has its own ClonkBuilder instance.
 * @author ZokRadonh
 */
public class ClonkBuilder extends IncrementalProjectBuilder {
	
	private static final boolean INDEX_C4GROUPS = true;

	private static final class UIRefresher implements Runnable {
		
		private final List<Script> resourcesToBeRefreshed;
		
		public UIRefresher(List<Script> resourcesToBeRefreshed) {
			super();
			this.resourcesToBeRefreshed = resourcesToBeRefreshed;
		}

		@Override
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
				CommonNavigator projectExplorer = UI.projectExplorer(window);
				if (projectExplorer != null)
					for (Script s : resourcesToBeRefreshed)
						UI.refreshAllProjectExplorers(s.getResource());
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
				Definition obj = Definition.definitionCorrespondingToFolder((IContainer) resource);
				if (obj != null)
					obj.setObjectFolder(null);
			}
			else if (resource instanceof IFile) {
				Structure.unPinFrom((IFile) resource);
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
		
		private final int operation;
		
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
		@Override
		public boolean visit(IResourceDelta delta) throws CoreException {
			if (delta == null) 
				return false;

			boolean success = false;
			If: if (delta.getResource() instanceof IFile) {
				IFile file = (IFile) delta.getResource();
				Script script;
				switch (delta.getKind()) {
				case IResourceDelta.CHANGED: case IResourceDelta.ADDED:
					script = Script.get(file, true);
					if (script == null) {
						// create if new file
						IContainer folder = delta.getResource().getParent();
						DefinitionParser objParser;
						// script in a resource group
						if (delta.getResource().getName().toLowerCase().endsWith(".c") && folder.getName().toLowerCase().endsWith(".c4g")) //$NON-NLS-1$ //$NON-NLS-2$
							script = new SystemScript(null, file);
						// object script
						else if (delta.getResource().getName().equals("Script.c") && (objParser = DefinitionParser.create(folder, getIndex())) != null) //$NON-NLS-1$
							script = objParser.createObject();
						// some other file but a script is still needed so get the definition for the folder
						else
							script = Definition.definitionCorrespondingToFolder(folder);
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
					script = SystemScript.scriptCorrespondingTo(file);
					if (script != null && file.equals(script.getScriptStorage()))
						script.getIndex().removeScript(script);
				}
				success = true;
			}
			else if (delta.getResource() instanceof IContainer) {
				if (!INDEX_C4GROUPS)
					if (EFS.getStore(delta.getResource().getLocationURI()) instanceof C4Group) {
						success = false;
						break If;
					}
				// make sure the object has a reference to its folder (not to some obsolete deleted one)
				Definition object;
				switch (delta.getKind()) {
				case IResourceDelta.ADDED:
					object = Definition.definitionCorrespondingToFolder((IContainer)delta.getResource());
					if (object != null)
						object.setObjectFolder((IContainer) delta.getResource());
					break;
				case IResourceDelta.REMOVED:
					// remove object when folder is removed
					object = Definition.definitionCorrespondingToFolder((IContainer)delta.getResource());
					if (object != null)
						object.getIndex().removeDefinition(object);
					break;
				}
				success = true;
			}
			monitor.worked(1);
			return success;
		}
		@Override
		public boolean visit(IResource resource) throws CoreException {
			if (monitor.isCanceled())
				return false;
			if (resource instanceof IContainer) {
				if (!INDEX_C4GROUPS)
					if (EFS.getStore(resource.getLocationURI()) instanceof C4Group)
						return false;
				DefinitionParser parser = DefinitionParser.create((IContainer) resource, getIndex());
				if (parser != null) { // is complete c4d (with DefCore.txt Script.c and Graphics)
					Definition object = parser.createObject();
					if (object != null) {
						queueScript(object);
					}
				}
				return true;
			}
			else if (resource instanceof IFile) {
				IFile file = (IFile) resource;
				// only create standalone-scripts for *.c files residing in System groups
				String systemName = nature.getIndex().getEngine().groupName("System", GroupType.ResourceGroup); //$NON-NLS-1$
				if (
					resource.getName().toLowerCase().endsWith(".c") && //$NON-NLS-1$
					systemName.equals(resource.getParent().getName())
				) {
					Script script = SystemScript.pinnedScript(file, true);
					if (script == null) {
						script = new SystemScript(getIndex(), file);
					}
					queueScript(script);
					return true;
				}
				else if (processAuxiliaryFiles(file, Script.get(file, true))) {
					return true;
				}
			}
			return false;
		}
		private boolean processAuxiliaryFiles(IFile file, Script script) throws CoreException {
			boolean result = true;
			Structure structure;
			if ((structure = Structure.createStructureForFile(file, true)) != null) {
				structure.commitTo(script, ClonkBuilder.this);
				structure.pinTo(file);
			}
			else
				structure = Structure.pinned(file, false, true);
			if (structure != null)
				gatheredStructures.add(structure);
			// not parsed as Structure - let definition process the file
			else if (script instanceof Definition) {
				Definition def = (Definition)script;
				try {
					def.processFile(file);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			else
				result = false;
			return result;
		}
	}

	private IProgressMonitor monitor;
	private ClonkProjectNature nature;

	/**
	 *  Gathered list of scripts to be parsed
	 */
	private final Map<Script, C4ScriptParser> parserMap = new HashMap<Script, C4ScriptParser>();

	/**
	 * Set of structures that have been validated during one build round - keeping track of them so when parsing dependent scripts, scripts that might lose some warnings
	 * due to structure files having been revalidated can also be reparsed (string tables and such)
	 */
	private final Set<Structure> gatheredStructures = Collections.synchronizedSet(new HashSet<Structure>());
	
	/**
	 * Set of {@link Definition}s whose ids have been changed by reparsing of their DefCore.txt files
	 */
	private final Set<Pair<Definition, ID>> renamedDefinitions = Collections.synchronizedSet(new HashSet<Pair<Definition, ID>>());

	private Index getIndex() {
		return ClonkProjectNature.get(getProject()).getIndex();
	}
	
	@Override
	protected void clean(IProgressMonitor monitor) throws CoreException {
		System.out.println(String.format(Messages.ClonkBuilder_CleaningProject, getProject().getName()));
		// clean up this project
		if (monitor != null)
			monitor.beginTask(Messages.CleaningUp, 1);
		IProject proj = this.getProject();
		if (proj != null) {
			proj.deleteMarkers(null, true, IResource.DEPTH_INFINITE);
			ProjectIndex projIndex = ClonkProjectNature.get(proj).forceIndexRecreation();
			proj.accept(new ResourceCounterAndCleaner(0));
			projIndex.clear();
		}
		if (monitor != null) {
			monitor.worked(1);
			monitor.done();
		}
	}
	
	private int buildKind;

	@Override
	@SuppressWarnings({"rawtypes"})
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {
		this.buildKind = kind;
		this.monitor = monitor;
		clearState();
		List<IResource> listOfResourcesToBeRefreshed = new LinkedList<IResource>();
		clearUIOfReferencesBeforeBuild();
		IProject proj = getProject();
		ClonkProjectNature.get(proj).getIndex().beginModification();
		try {
			try {

				performBuildPhases(
					listOfResourcesToBeRefreshed, proj,
					getDelta(proj)
				);

				if (monitor.isCanceled()) {
					monitor.done();
					return null;
				}

				handleDefinitionRenaming();

				// validate files related to the scripts that have been parsed
				for (Script script : parserMap.keySet()) {
					validateRelatedFiles(script);
				}
				
				return new IProject[] { proj };
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		} finally {
			ClonkProjectNature.get(proj).getIndex().endModification();
			clearState();
		}
	}

	private void handleDefinitionRenaming() {
		for (Pair<Definition, ID> rnd : renamedDefinitions) {
			final Definition def = rnd.first();
			final ID newID = rnd.second();
			final ID oldID = def.id();
			if (oldID == null || oldID == ID.NULL)
				return;
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					// FIXME: implement for CR?
					Variable var = def.proxyVar();
					if (var != null && UI.confirm(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
						String.format(Messages.ClonkBuilder_RenameRefactoringPrompt, oldID.stringValue(), newID.stringValue()),
						String.format(Messages.ClonkBuilder_RenameRefactoringTitle, oldID.stringValue()))
					)
						// perform a refactoring - the RenameDeclarationProcessor will take care of renaming the proxyvar which in turn will cause the id of the definition to actually be changed
						RenameDeclarationAction.performRenameRefactoring(var, newID.stringValue(), RenameDeclarationProcessor.CONSIDER_DEFCORE_ID_ALREADY_CHANGED);
					else
						// simply set the new id
						def.setId(newID); 
				}
			});
		}
	}

	private static <T extends IResourceVisitor & IResourceDeltaVisitor> void visitDeltaOrWholeProject(IResourceDelta delta, IProject proj, T ultimateVisit0r) throws CoreException {
		if (delta != null)
			delta.accept(ultimateVisit0r);
		else
			proj.accept(ultimateVisit0r);
	}
	
	private static class SaveScriptsJob extends Job {
		private final Script[] scriptsToSave;
		private final IProject project;
		public SaveScriptsJob(IProject project, Script... scriptsToSave) {
			super(Messages.ClonkBuilder_SaveIndexFilesForParsedScripts);
			this.scriptsToSave = scriptsToSave;
			this.project = project;
		}
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			monitor.beginTask(Messages.ClonkBuilder_SavingScriptIndexFiles, scriptsToSave.length+3);
			try {
				for (Script s : scriptsToSave)
					try {
						s.save();
						monitor.worked(1);
					} catch (IOException e) {
						e.printStackTrace();
					}
				ClonkProjectNature.get(project).getIndex().saveShallow();
				monitor.worked(3);
				return Status.OK_STATUS;
			} finally {
				monitor.done();
			}
		}
	}
	
	private void performBuildPhases(
		List<IResource> listOfResourcesToBeRefreshed,
		IProject proj,
		IResourceDelta delta
	) throws CoreException {
		
		nature = ClonkProjectNature.get(proj); 
		Index index = nature.getIndex();
		
		// visit files to open C4Groups if files are contained in c4group file system
		visitDeltaOrWholeProject(delta, proj, new C4GroupStreamHandler(C4GroupStreamHandler.OPEN));
		try {

			// count num of resources to build
			ResourceCounter resourceCounter = new ResourceCounter(ResourceCounter.COUNT_CONTAINER);
			visitDeltaOrWholeProject(delta, proj, resourceCounter);

			// initialize progress monitor
			monitor.beginTask(String.format(Messages.BuildProject, proj.getName()), buildKind == CLEAN_BUILD || buildKind == FULL_BUILD ? parserMap.size()*2 : IProgressMonitor.UNKNOWN);
			
			// populate parserMap with first batch of parsers for directly modified scripts
			parserMap.clear();
			visitDeltaOrWholeProject(delta, proj, new ScriptGatherer());
			
			// delete old declarations
			for (Script script : parserMap.keySet())
				script.clearDeclarations();
			index.refreshIndex();
			
			// parse declarations
			monitor.subTask(Messages.ClonkBuilder_ParseDeclarations);
			int parserMapSize;
			Map<Script, C4ScriptParser> newlyEnqueuedParsers = new HashMap<Script, C4ScriptParser>();
			Map<Script, C4ScriptParser> enqueuedFromLastIteration = new HashMap<Script, C4ScriptParser>();
			newlyEnqueuedParsers.putAll(parserMap);
			do {
				parserMapSize = parserMap.size();
				final SynchronizedCounter poolJobsCountdown = new SynchronizedCounter(newlyEnqueuedParsers.keySet().size());
				final ExecutorService phaseOnePool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
				for (final Script script : newlyEnqueuedParsers.keySet()) {
					if (monitor.isCanceled())
						return;
					phaseOnePool.execute(new Runnable() {
						@Override
						public void run() {
							performBuildPhaseOne(script);
							System.out.println("Worked!");
							monitor.worked(1);
							if (poolJobsCountdown.decrement() == 0)
								phaseOnePool.shutdown();
						}
					});
				}
				try {
					phaseOnePool.awaitTermination(100, TimeUnit.HOURS);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				Display.getDefault().asyncExec(new UIRefresher(listFromIterable(newlyEnqueuedParsers.keySet())));
				// refresh now so gathered structures will be validated with an index that has valid appendages maps and such.
				// without refreshing the index here, error markers would be created for TimerCall=... etc. assignments in ActMaps for example
				// if the function being referenced is defined in an #appendto from this index
				index.refreshIndex();
				// don't queue dependent scripts during a clean build - if everything works right all scripts will have been added anyway
				if (buildKind == CLEAN_BUILD || buildKind == FULL_BUILD)
					break;
				enqueuedFromLastIteration.clear();
				enqueuedFromLastIteration.putAll(newlyEnqueuedParsers);
				newlyEnqueuedParsers.clear();
				queueDependentScripts(enqueuedFromLastIteration, newlyEnqueuedParsers);
			}
			while (parserMapSize != parserMap.size());
			
			if (delta != null)
				listOfResourcesToBeRefreshed.add(delta.getResource());
			
			// parse function code
			monitor.subTask(Messages.ClonkBuilder_ParseFunctionCode);
			Script[] scripts = parserMap.keySet().toArray(new Script[parserMap.keySet().size()]);
			for (Script s : scripts)
				s.generateFindDeclarationCache();
			final ExecutorService phaseTwoPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
			final SynchronizedCounter poolJobsCountdown = new SynchronizedCounter(scripts.length);
			for (final Script script : scripts) {
				if (monitor.isCanceled())
					return;
				phaseTwoPool.execute(new Runnable() {
					@Override
					public void run() {
						performBuildPhaseTwo(script);
						monitor.worked(1);
						if (poolJobsCountdown.decrement() == 0)
							phaseTwoPool.shutdown();
					}
				});
			}
			try {
				phaseTwoPool.awaitTermination(100, TimeUnit.HOURS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			Display.getDefault().asyncExec(new UIRefresher(Arrays.asList(scripts)));
			new SaveScriptsJob(proj, scripts).schedule();
			
			applyLatentMarkers();

		} finally {
			monitor.done();
			visitDeltaOrWholeProject(delta, proj, new C4GroupStreamHandler(C4GroupStreamHandler.CLOSE));
		}
	}

	private void clearState() {
		gatheredStructures.clear();
		parserMap.clear();
		renamedDefinitions.clear();
	}

	private void queueDependentScripts(Map<Script, C4ScriptParser> scriptsToQueueDependenciesFrom, Map<Script, C4ScriptParser> newlyAddedParsers) {
		for (C4ScriptParser parser : scriptsToQueueDependenciesFrom.values()) {
			if (monitor.isCanceled())
				break;
			if (parser == null)
				continue;
			//System.out.println("Queueing dependent scripts for " + parser.getContainer().toString());
			for (Script dep : parser.getContainer().dependentScripts()) {
				if (!parserMap.containsKey(dep)) {
					C4ScriptParser p = queueScript(dep);
					newlyAddedParsers.put(dep, p);
				}
			}
		}
		for (Structure s : gatheredStructures) {
			s.validate();
			if (s.requiresScriptReparse()) {
				Script script = Script.get(s.getResource(), false);
				if (script != null) {
					C4ScriptParser p = queueScript(script);
					newlyAddedParsers.put(script, p);
				}
			}
		}
		gatheredStructures.clear();
	}

	private void applyLatentMarkers() {
		for (C4ScriptParser p : parserMap.values()) {
			p.applyLatentMarkers();
		}
	}

	private void validateRelatedFiles(Script script) throws CoreException {
		if (script instanceof Definition) {
			Definition def = (Definition) script;
			for (IResource r : def.definitionFolder().members()) {
				if (r instanceof IFile) {
					Structure pinned = Structure.pinned(r, false, true);
					if (pinned != null)
						pinned.validate();
				}
			}
		}
	}

	private void clearUIOfReferencesBeforeBuild() {
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
									Declaration topLevelDeclaration = ed.topLevelDeclaration();
									if (
										topLevelDeclaration != null &&
										topLevelDeclaration.getResource() != null &&
										ClonkBuilder.this.getProject().equals(topLevelDeclaration.getResource().getProject())
									)
										ed.clearOutline();
								}
							}
						}
					}
				}
				finally {
					synchronized (ClonkBuilder.this) {
						ClonkBuilder.this.notify();
					}
				}
			}
		});
	}
	
	private C4ScriptParser queueScript(Script script) {
		C4ScriptParser result;
		if (!parserMap.containsKey(script)) {
			IStorage storage = script.getScriptStorage();
			if (storage != null) {
				result = new C4ScriptParser(script);
				result.setAllowInterleavedFunctionParsing(true);
			} else
				result = null;
			parserMap.put(script, result);
		} else
			result = parserMap.get(script);
		return result;
	}
	
	private void performBuildPhaseOne(Script script) {
		C4ScriptParser parser;
		synchronized (parserMap) {
			parser = parserMap.get(script);
		}
		nature.getIndex().addScript(script);
		if (parser != null) {
			parser.clean();
			parser.parseDeclarations();
		}
	}
	
	/**
	 * Parse function code/validate variable initialization code.
	 * An attempt is made to parse included scripts before the passed one.
	 * @param script The script to parse
	 */
	private void performBuildPhaseTwo(Script script) {
		C4ScriptParser parser;
		synchronized (parserMap) {
			parser = parserMap.containsKey(script) ? parserMap.remove(script) : null;
		}
		if (parser != null) {
			try {
				// parse #included scripts before this one
				for (IHasIncludes include : script.getIncludes(nature.getIndex(), false)) {
					if (include instanceof Script)
						performBuildPhaseTwo((Script) include);
				}
				//System.out.print("-");
				//long s = System.currentTimeMillis();
				parser.parseCodeOfFunctionsAndValidate();
				/*System.out.print(System.currentTimeMillis()-s);
					System.out.print("-");*/
			} catch (ParsingException e) {
				e.printStackTrace();
			}
		}
		//nature.getIndex().refreshIndex();
	}

	/**
	 * Inform the builder about a Definition renaming caused by modifying its DefCore.txt file.
	 * @param def The {@link Definition} whose DefCore.txt id value changed
	 * @param ID newID The new id the builder is supposed to assign to the definition eventually
	 */
	public void queueDefinitionRenaming(Definition def, ID newID) {
		renamedDefinitions.add(new Pair<Definition, ID>(def, newID));
	}

}