package net.arctics.clonk.resource;

import static net.arctics.clonk.util.ArrayUtil.listFromIterable;
import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.findMemberCaseInsensitively;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.ProjectIndex;
import net.arctics.clonk.index.Scenario;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.ID;
import net.arctics.clonk.parser.IHasIncludes;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.parser.c4script.SystemScript;
import net.arctics.clonk.parser.inireader.DefCoreUnit;
import net.arctics.clonk.resource.c4group.C4Group;
import net.arctics.clonk.resource.c4group.C4Group.GroupType;
import net.arctics.clonk.ui.editors.ClonkTextEditor;
import net.arctics.clonk.util.Pair;
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
						UI.refreshAllProjectExplorers(s.resource());
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
					obj.setDefinitionFolder(null);
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
		public Definition createDefinition(IContainer folder) {
			IFile defCore = as(findMemberCaseInsensitively(folder, "DefCore.txt"), IFile.class);
			IFile scenario = defCore != null ? null : as(findMemberCaseInsensitively(folder, "Scenario.txt"), IFile.class);
			if (defCore == null && scenario == null)
				return null;
			try {
				Definition def = Definition.definitionCorrespondingToFolder(folder);
				if (defCore != null) {
					DefCoreUnit defCoreWrapper = (DefCoreUnit) Structure.pinned(defCore, true, false);
					if (def == null)
						def = new Definition(index(), defCoreWrapper.definitionID(), defCoreWrapper.name(), folder);
					else {
						def.setId(defCoreWrapper.definitionID());
						def.setName(defCoreWrapper.name(), false);
					}
				} else if (scenario != null)
					def = new Scenario(index(), folder.getName(), folder);
				return def;
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
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
						// script in a system group
						if (isSystemScript(delta.getResource())) //$NON-NLS-1$ //$NON-NLS-2$
							script = new SystemScript(index(), file);
						// object script
						else
							script = createDefinition(delta.getResource().getParent());
					}
					if (script != null && delta.getResource().equals(script.scriptStorage()))
						queueScript(script);
					else
						processAuxiliaryFiles(file, script);
					break;
				case IResourceDelta.REMOVED:
					script = SystemScript.scriptCorrespondingTo(file);
					if (script != null && file.equals(script.scriptStorage()))
						script.index().removeScript(script);
				}
				success = true;
			}
			else if (delta.getResource() instanceof IContainer) {
				IContainer container = (IContainer)delta.getResource();
				if (!INDEX_C4GROUPS)
					if (EFS.getStore(delta.getResource().getLocationURI()) instanceof C4Group) {
						success = false;
						break If;
					}
				// make sure the object has a reference to its folder (not to some obsolete deleted one)
				Definition definition;
				switch (delta.getKind()) {
				case IResourceDelta.ADDED:
					definition = Definition.definitionCorrespondingToFolder(container);
					if (definition != null)
						definition.setDefinitionFolder(container);
//					else if (isSystemGroup(container))
//						for (IResource res : container.members())
//							if (isSystemScript(res))
//								queueScript(new SystemScript(index(), (IFile)res));
					break;
				case IResourceDelta.REMOVED:
					// remove object when folder is removed
					definition = Definition.definitionCorrespondingToFolder(container);
					if (definition != null)
						definition.index().removeDefinition(definition);
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
				Definition definition = createDefinition((IContainer) resource);
				if (definition != null)
					queueScript(definition);
				return true;
			}
			else if (resource instanceof IFile) {
				IFile file = (IFile) resource;
				// only create standalone-scripts for *.c files residing in System groups
				if (isSystemScript(resource)) {
					Script script = SystemScript.pinnedScript(file, true);
					if (script == null)
						script = new SystemScript(index(), file);
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
					def.processDefinitionFolderFile(file);
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

	private Index index() {
		return ClonkProjectNature.get(getProject()).index();
	}
	
	public boolean isSystemScript(IResource resource) {
		return resource instanceof IFile && resource.getName().toLowerCase().endsWith(".c") && isSystemGroup(resource.getParent());
	}

	private boolean isSystemGroup(IContainer container) {
		return index().engine().groupName("System", GroupType.ResourceGroup).equals(container.getName());
	}
	
	private static String buildTask(String text, IProject project) {
		return String.format(text, project.getName()); 
	}
	
	private String buildTask(String text) {
		return buildTask(text, getProject());
	}
	
	@Override
	protected void clean(IProgressMonitor monitor) throws CoreException {
		System.out.println(buildTask(Messages.ClonkBuilder_CleaningProject));
		// clean up this project
		if (monitor != null)
			monitor.beginTask(buildTask(Messages.CleaningUp), 1);
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
		ClonkProjectNature.get(proj).index().beginModification();
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
			ClonkProjectNature.get(proj).index().endModification();
			clearState();
		}
	}

	private void handleDefinitionRenaming() {
		// let's just... not do that here
		/*for (Pair<Definition, ID> rnd : renamedDefinitions) {
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
					) {
						// perform a refactoring
						RenameDeclarationAction.performRenameRefactoring(var, newID.stringValue(), RenameDeclarationProcessor.CONSIDER_DEFCORE_ID_ALREADY_CHANGED);
					}
					// set id in any case
					def.setId(newID); 
				}
			});
		}*/
	}
	
	

	private static <T extends IResourceVisitor & IResourceDeltaVisitor> void visitDeltaOrWholeProject(IResourceDelta delta, IProject proj, T visitor) throws CoreException {
		if (delta != null)
			delta.accept(visitor);
		else
			proj.accept(visitor);
	}
	
	private static class SaveScriptsJob extends Job {
		private final Script[] scriptsToSave;
		private final IProject project;
		public SaveScriptsJob(IProject project, Script... scriptsToSave) {
			super(buildTask(Messages.ClonkBuilder_SaveIndexFilesForParsedScripts, project));
			this.scriptsToSave = scriptsToSave;
			this.project = project;
		}
		@Override
		protected IStatus run(final IProgressMonitor monitor) {
			monitor.beginTask(buildTask(Messages.ClonkBuilder_SavingScriptIndexFiles, project), scriptsToSave.length+3);
			try {
				ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
				for (final Script s : scriptsToSave) {
					executor.execute(new Runnable() {
						@Override
						public void run() {
							try {
								s.save();
							} catch (IOException e) {
								e.printStackTrace();
							}
							monitor.worked(1);
						}
					});
				}
				executor.shutdown();
				try {
					executor.awaitTermination(20, TimeUnit.MINUTES);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				ClonkProjectNature.get(project).index().saveShallow();
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
		Index index = nature.index();
		
		// visit files to open C4Groups if files are contained in c4group file system
		visitDeltaOrWholeProject(delta, proj, new C4GroupStreamHandler(C4GroupStreamHandler.OPEN));
		try {

			// count num of resources to build
			ResourceCounter resourceCounter = new ResourceCounter(ResourceCounter.COUNT_CONTAINER);
			visitDeltaOrWholeProject(delta, proj, resourceCounter);

			// initialize progress monitor
			monitor.beginTask(buildTask(Messages.BuildProject), buildKind == CLEAN_BUILD || buildKind == FULL_BUILD ? 3000 : IProgressMonitor.UNKNOWN);
			
			// populate parserMap with first batch of parsers for directly modified scripts
			parserMap.clear();
			monitor.subTask(buildTask(Messages.ClonkBuilder_GatheringScripts));
			visitDeltaOrWholeProject(delta, proj, new ScriptGatherer());
			
			// delete old declarations
			for (Script script : parserMap.keySet())
				script.clearDeclarations();
			index.refreshIndex();
			
			// parse declarations
			monitor.subTask(buildTask(Messages.ClonkBuilder_ParseDeclarations));
			int parserMapSize;
			Map<Script, C4ScriptParser> newlyEnqueuedParsers = new HashMap<Script, C4ScriptParser>();
			Map<Script, C4ScriptParser> enqueuedFromLastIteration = new HashMap<Script, C4ScriptParser>();
			newlyEnqueuedParsers.putAll(parserMap);
			do {
				parserMapSize = parserMap.size();
				phase = 1;
				final ExecutorService phaseOnePool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
				for (final Script script : newlyEnqueuedParsers.keySet()) {
					if (monitor.isCanceled())
						break;
					phaseOnePool.execute(new Runnable() {
						@Override
						public void run() {
							performBuildPhaseOne(script);
							monitor.worked(1);
						}
					});
				}
				phaseOnePool.shutdown();
				try {
					phaseOnePool.awaitTermination(20, TimeUnit.MINUTES);
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
			
			for (C4ScriptParser parser : parserMap.values()) {
				if (parser != null)
					parser.prepareForFunctionParsing();
			}
			
			// parse function code
			monitor.subTask(buildTask(Messages.ClonkBuilder_ParseFunctionCode));
			Script[] scripts = parserMap.keySet().toArray(new Script[parserMap.keySet().size()]);
			for (Script s : scripts)
				s.generateFindDeclarationCache();
			phase = 2;
			final ExecutorService phaseTwoPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
			for (final Script script : scripts) {
				if (monitor.isCanceled())
					break;
				phaseTwoPool.execute(new Runnable() {
					@Override
					public void run() {
						performBuildPhaseTwo(script);
						monitor.worked(1);
					}
				});
			}
			phaseTwoPool.shutdown();
			try {
				phaseTwoPool.awaitTermination(20, TimeUnit.MINUTES);
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
			for (Script dep : parser.containingScript().dependentScripts()) {
				if (!parserMap.containsKey(dep)) {
					C4ScriptParser p = queueScript(dep);
					newlyAddedParsers.put(dep, p);
				}
			}
		}
		for (Structure s : gatheredStructures) {
			s.validate();
			if (s.requiresScriptReparse()) {
				Script script = Script.get(s.resource(), false);
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
										topLevelDeclaration.resource() != null &&
										ClonkBuilder.this.getProject().equals(topLevelDeclaration.resource().getProject())
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
			IStorage storage = script.scriptStorage();
			if (storage != null) {
				result = new C4ScriptParser(script);
				result.setAllowInterleavedFunctionParsing(true);
				result.setBuilder(this);
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
		nature.index().addScript(script);
		if (parser != null) {
			parser.clean();
			parser.parseDeclarations();
		}
	}
	
	public void parseFunction(Function function) {
		C4ScriptParser parser;
		synchronized (parserMap) {
			parser = parserMap.get(function.script());
		}
		if (parser != null) {
			try {
				parser.parseCodeOfFunction(function, true);
			} catch (ParsingException e) {
				e.printStackTrace();
			}
		}
	}
	
	private int phase;
	
	/**
	 * Parse function code/validate variable initialization code.
	 * An attempt is made to parse included scripts before the passed one.
	 * @param script The script to parse
	 */
	public void performBuildPhaseTwo(Script script) {
		C4ScriptParser parser;
		if (phase != 2)
			return;
		synchronized (parserMap) {
			parser = parserMap.containsKey(script) ? parserMap.remove(script) : null;
		}
		if (parser != null) {
			try {
				// parse #included scripts before this one
				for (IHasIncludes include : script.includes(nature.index(), 0)) {
					if (include instanceof Script)
						performBuildPhaseTwo((Script) include);
				}
				parser.parseCodeOfFunctionsAndValidate();
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