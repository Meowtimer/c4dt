package net.arctics.clonk.resource;

import static net.arctics.clonk.util.ArrayUtil.listFromIterable;

import java.io.IOException;
import java.util.Collection;
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
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.IHasIncludes;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.C4ScriptParser.Markers;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.resource.c4group.C4Group.GroupType;
import net.arctics.clonk.ui.editors.ClonkTextEditor;

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

/**
 * An incremental builder for all project data.<br>
 * This builder launches the parser that indexes all
 * {@link Definition}s and standalone scripts and highlights syntax errors in one project.<br>
 * Each project has its own ClonkBuilder instance.
 * @author ZokRadonh
 */
public class ClonkBuilder extends IncrementalProjectBuilder {

	private IProgressMonitor monitor;
	private ClonkProjectNature nature;
	private final Map<Script, C4ScriptParser> parserMap = new HashMap<Script, C4ScriptParser>();
	/**
	 * Set of structures that have been validated during one build round - keeping track of them so when parsing dependent scripts, scripts that might lose some warnings
	 * due to structure files having been revalidated can also be reparsed (string tables and such)
	 */
	private final Set<Structure> gatheredStructures = Collections.synchronizedSet(new HashSet<Structure>());
	private final Markers markers = new Markers();
	private int buildKind;
	private Set<Function> problemReporters;
	
	public void addGatheredStructure(Structure structure) { gatheredStructures.add(structure); }
	public Markers markers() { return markers; }
	public Index index() { return ClonkProjectNature.get(getProject()).index(); }
	public IProgressMonitor monitor() { return monitor; }

	public boolean isSystemScript(IResource resource) {
		return resource instanceof IFile && resource.getName().toLowerCase().endsWith(".c") && isSystemGroup(resource.getParent()); //$NON-NLS-1$
	}

	private boolean isSystemGroup(IContainer container) {
		return index().engine().groupName("System", GroupType.ResourceGroup).equals(container.getName()); //$NON-NLS-1$
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

				performBuildPhases(listOfResourcesToBeRefreshed, proj, getDelta(proj));

				if (monitor.isCanceled()) {
					monitor.done();
					return null;
				}

				// validate files related to the scripts that have been parsed
				for (Script script : parserMap.keySet())
					validateRelatedFiles(script);

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

	private static <T extends IResourceVisitor & IResourceDeltaVisitor> void visitDeltaOrWholeProject(IResourceDelta delta, IProject proj, T visitor) throws CoreException {
		if (delta != null)
			delta.accept(visitor);
		else
			proj.accept(visitor);
	}

	private static class SaveScriptsJob extends Job {
		private final Script[] scriptsToSave;
		private final IProject project;
		public SaveScriptsJob(IProject project, Collection<Script> scriptsToSave) {
			super(buildTask(Messages.ClonkBuilder_SaveIndexFilesForParsedScripts, project));
			this.scriptsToSave = scriptsToSave.toArray(new Script[scriptsToSave.size()]);
			this.project = project;
		}
		@Override
		protected IStatus run(final IProgressMonitor monitor) {
			monitor.beginTask(buildTask(Messages.ClonkBuilder_SavingScriptIndexFiles, project), scriptsToSave.length+3);
			try {
				for (final Script s : scriptsToSave)
					try {
						s.save();
					} catch (IOException e) {
						e.printStackTrace();
					}
				Index index = ClonkProjectNature.get(project).index();
				index.saveShallow();
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
		visitDeltaOrWholeProject(delta, proj, new C4GroupStreamOpener(C4GroupStreamOpener.OPEN));
		try {

			// count num of resources to build
			ResourceCounter resourceCounter = new ResourceCounter(ResourceCounter.COUNT_CONTAINER);
			visitDeltaOrWholeProject(delta, proj, resourceCounter);

			// initialize progress monitor
			monitor.beginTask(buildTask(Messages.BuildProject), buildKind == CLEAN_BUILD || buildKind == FULL_BUILD ? 3000 : IProgressMonitor.UNKNOWN);

			// populate parserMap with first batch of parsers for directly modified scripts
			parserMap.clear();
			monitor.subTask(buildTask(Messages.ClonkBuilder_GatheringScripts));
			visitDeltaOrWholeProject(delta, proj, new ScriptGatherer(this));

			// delete old declarations
			for (Script script : parserMap.keySet())
				script.clearDeclarations();
			index.refreshIndex();

			phaseOne(index);

			if (delta != null)
				listOfResourcesToBeRefreshed.add(delta.getResource());
			
			Script[] scripts = parserMap.keySet().toArray(new Script[parserMap.keySet().size()]);
			C4ScriptParser[] parsers = parserMap.values().toArray(new C4ScriptParser[parserMap.values().size()]);
			phaseTwo(scripts);
			phaseThree(parsers);
			
			new SaveScriptsJob(proj, parserMap.keySet()).schedule();
		} finally {
			monitor.done();
			visitDeltaOrWholeProject(delta, proj, new C4GroupStreamOpener(C4GroupStreamOpener.CLOSE));
		}
	}

	private void phaseOne(Index index) {
		// parse declarations
		monitor.subTask(buildTask(Messages.ClonkBuilder_ParseDeclarations));
		int parserMapSize;
		Map<Script, C4ScriptParser> newlyEnqueuedParsers = new HashMap<Script, C4ScriptParser>();
		Map<Script, C4ScriptParser> enqueuedFromLastIteration = new HashMap<Script, C4ScriptParser>();
		newlyEnqueuedParsers.putAll(parserMap);
		do {
			parserMapSize = parserMap.size();
			final ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
			for (final Script script : newlyEnqueuedParsers.keySet()) {
				if (monitor.isCanceled())
					break;
				pool.execute(new Runnable() {
					@Override
					public void run() {
						performBuildPhaseOne(script);
						monitor.worked(1);
					}
				});
			}
			pool.shutdown();
			try {
				pool.awaitTermination(20, TimeUnit.MINUTES);
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
		markers.deploy();
	}

	private void phaseTwo(Script[] scripts) {
		// parse function code
		monitor.subTask(buildTask(Messages.ClonkBuilder_ParseFunctionCode));
		for (C4ScriptParser parser : parserMap.values())
			if (parser != null)
				parser.prepareForFunctionParsing();

		for (Script s : scripts)
			s.generateFindDeclarationCache();
		final ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		for (final Script script : scripts) {
			if (monitor.isCanceled())
				break;
			pool.execute(new Runnable() {
				@Override
				public void run() {
					performBuildPhaseTwo(script);
					monitor.worked(1);
				}
			});
		}
		pool.shutdown();
		try {
			pool.awaitTermination(20, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private void phaseThree(C4ScriptParser[] parsers) {
		// report problems
		monitor.subTask(String.format(Messages.ClonkBuilder_ReportingProblems, getProject().getName()));
		final ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		problemReporters = new HashSet<Function>();
		for (final C4ScriptParser p : parsers) {
			if (monitor.isCanceled())
				break;
			if (p == null)
				continue;
			pool.execute(new Runnable() {
				@Override
				public void run() {
					System.out.println("Reporting problems for " + p.script().toString());
					p.reportProblems();
					monitor.worked(1);
				}
			});
		}
		pool.shutdown();
		try {
			pool.awaitTermination(20, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		problemReporters = null;
		markers.deploy();
		Display.getDefault().asyncExec(new UIRefresher(parserMap.keySet()));
	}

	public final Set<Function> problemReporters() {
		return problemReporters;
	}
	
	public C4ScriptParser parserFor(Script script) {
		return parserMap.get(script);
	}
	
	private void clearState() {
		gatheredStructures.clear();
		parserMap.clear();
	}

	private void queueDependentScripts(Map<Script, C4ScriptParser> scriptsToQueueDependenciesFrom, Map<Script, C4ScriptParser> newlyAddedParsers) {
		for (C4ScriptParser parser : scriptsToQueueDependenciesFrom.values()) {
			if (monitor.isCanceled())
				break;
			if (parser == null)
				continue;
			//System.out.println("Queueing dependent scripts for " + parser.getContainer().toString());
			for (Script dep : parser.containingScript().dependentScripts())
				if (!parserMap.containsKey(dep)) {
					C4ScriptParser p = queueScript(dep);
					newlyAddedParsers.put(dep, p);
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

	private void validateRelatedFiles(Script script) throws CoreException {
		if (script instanceof Definition) {
			Definition def = (Definition) script;
			for (IResource r : def.definitionFolder().members())
				if (r instanceof IFile) {
					Structure pinned = Structure.pinned(r, false, true);
					if (pinned != null)
						pinned.validate();
				}
		}
	}

	private void clearUIOfReferencesBeforeBuild() {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				IWorkbench w = PlatformUI.getWorkbench();
				for (IWorkbenchWindow window : w.getWorkbenchWindows())
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
		});
	}

	public C4ScriptParser queueScript(Script script) {
		C4ScriptParser result;
		if (!parserMap.containsKey(script)) {
			IStorage storage = script.scriptStorage();
			if (storage != null) {
				result = new C4ScriptParser(script);
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
		if (parser != null)
			parser.reportProblems(function);
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
		if (parser != null)
			try {
				// parse #included scripts before this one
				for (IHasIncludes include : script.includes(nature.index(), script, 0))
					if (include instanceof Script)
						performBuildPhaseTwo((Script) include);
				parser.parseCodeOfFunctionsAndValidate();
			} catch (ParsingException e) {
				e.printStackTrace();
			}
	}

}