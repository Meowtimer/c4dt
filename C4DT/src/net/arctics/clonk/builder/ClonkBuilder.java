package net.arctics.clonk.builder;

import static net.arctics.clonk.util.Utilities.as;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import net.arctics.clonk.Flags;
import net.arctics.clonk.ProblemException;
import net.arctics.clonk.ast.Structure;
import net.arctics.clonk.c4group.C4GroupStreamOpener;
import net.arctics.clonk.c4script.C4ScriptParser;
import net.arctics.clonk.c4script.ProblemReportingStrategy;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.IndexEntity;
import net.arctics.clonk.index.ProjectIndex;
import net.arctics.clonk.parser.Markers;
import net.arctics.clonk.util.Sink;
import net.arctics.clonk.util.TaskExecution;
import net.arctics.clonk.util.UI;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;
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
	private Index index;

	public void addGatheredStructure(Structure structure) { gatheredStructures.add(structure); }
	public Markers markers() { return markers; }
	public Index index() { return index; }
	public IProgressMonitor monitor() { return monitor; }
	public Collection<C4ScriptParser> parsers() { return parserMap.values(); }
	public Collection<Script> scripts() { return parserMap.keySet(); }

	static String buildTask(String text, IProject project) {
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
		final IProject proj = this.getProject();
		if (proj != null) {
			proj.deleteMarkers(null, true, IResource.DEPTH_INFINITE);
			final ProjectIndex projIndex = ClonkProjectNature.get(proj).forceIndexRecreation();
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
		final IProject proj = getProject();
		this.index = ClonkProjectNature.get(getProject()).index();
		markers.applyProjectSettings(this.index);

		if (kind == FULL_BUILD) {
			if (index.built())
				return new IProject[] { proj };
			index.built(true);
		}

		this.buildKind = kind;
		this.monitor = monitor;
		clearState();

		//clearUIOfReferencesBeforeBuild();
		ClonkProjectNature.get(proj).index().beginModification();
		try {
			try {

				final Script[] scripts = performBuildPhases(proj, getDelta(proj));

				if (monitor.isCanceled()) {
					monitor.done();
					return null;
				}

				// validate files related to the scripts that have been parsed
				for (final Script script : scripts)
					validateRelatedFiles(script);

				return new IProject[] { proj };
			} catch (final Exception e) {
				e.printStackTrace();
				return null;
			}
		} finally {
			ClonkProjectNature.get(proj).index().endModification();
			clearState();
		}
	}

	private <T extends IResourceVisitor & IResourceDeltaVisitor> void visitDeltaOrWholeProject(IResourceDelta delta, IProject proj, T visitor) throws CoreException {
		if (delta != null)
			delta.accept(visitor);
		else if (buildKind == FULL_BUILD || buildKind == CLEAN_BUILD)
			proj.accept(visitor);
		else
			System.out.println("ClonkBuilder: Not visiting things - no delta but no full build either");
	}

	private Script[] performBuildPhases(
		final IProject proj,
		IResourceDelta delta
	) throws CoreException {

		nature = ClonkProjectNature.get(proj);
		final Index index = nature.index();

		// visit files to open C4Groups if files are contained in c4group file system
		visitDeltaOrWholeProject(delta, proj, new C4GroupStreamOpener(C4GroupStreamOpener.OPEN));
		try {

			// count num of resources to build
			final ResourceCounter resourceCounter = new ResourceCounter(ResourceCounter.COUNT_CONTAINER);
			visitDeltaOrWholeProject(delta, proj, resourceCounter);

			// initialize progress monitor
			monitor.beginTask(buildTask(Messages.BuildProject), buildKind == CLEAN_BUILD || buildKind == FULL_BUILD ? 3000 : IProgressMonitor.UNKNOWN);

			// populate parserMap with first batch of parsers for directly modified scripts
			gatherScripts(proj, delta);
			clearScripts(index);
			parseDeclarations(index);

			final Script[] scripts = parserMap.keySet().toArray(new Script[parserMap.keySet().size()]);
			final C4ScriptParser[] parsers = parserMap.values().toArray(new C4ScriptParser[parserMap.values().size()]);

			reportProblems(parsers, scripts);

			for (final C4ScriptParser parser : parsers)
				if (parser != null && parser.script() != null)
					parser.script().setTypeAnnotations(parser.typeAnnotations());

			new SaveScriptsJob(proj, scripts).schedule();

			performRequestedTypingMigration(proj, parsers);
			return scripts;
		} finally {
			monitor.done();
			visitDeltaOrWholeProject(delta, proj, new C4GroupStreamOpener(C4GroupStreamOpener.CLOSE));
		}
	}
	private void gatherScripts(final IProject proj, IResourceDelta delta) throws CoreException {
		parserMap.clear();
		monitor.subTask(buildTask(Messages.ClonkBuilder_GatheringScripts));
		final ScriptGatherer gatherer = new ScriptGatherer(this);
		visitDeltaOrWholeProject(delta, proj, gatherer);
		gatherer.removeObsoleteScripts();
	}

	private void clearScripts(final Index index) {
		// delete old declarations
		for (final Script script : parserMap.keySet())
			script.clearDeclarations();
		index.refresh(false);
	}

	private void performRequestedTypingMigration(final IProject proj, final C4ScriptParser[] parsers) {
		final ProjectSettings settings = nature.settings();
		if (buildKind == FULL_BUILD)
			if (settings.migrationTyping != null) switch (settings.migrationTyping) {
			case STATIC:
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						if (UI.confirm(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
							String.format("Scripts in '%s' will now be migrated to static typing. This cannot be undone. Continue?", proj.getName()),
							"Migration to Static Typing"
						))
							migrateToStaticTyping(parsers, settings);
					}
				});
				break;
			case DYNAMIC: case PARAMETERS_OPTIONALLY_TYPED:
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						if (UI.confirm(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
							String.format("Scripts in '%s' will now be migrated to dynamic typing. This cannot be undone. Continue?", proj.getName()),
							"Migration to Dynamic Typing"
						))
							migrateToDynamicTyping(parsers, settings);
					}
				});
				break;
			default:
				break;
			}
	}

	private void migrateToStaticTyping(final C4ScriptParser[] parsers, final ProjectSettings settings) {
		new StaticTypingMigrationJob("Static Typing Migration", nature, settings, parsers).schedule();
	}

	private void migrateToDynamicTyping(final C4ScriptParser[] parsers, final ProjectSettings settings) {
		new DynamicTypingMigrationJob(nature, "Dynamic Typing Migration", parsers, settings).schedule();
	}

	private void parseDeclarations(Index index) {
		// parse declarations
		monitor.subTask(buildTask(Messages.ClonkBuilder_ParseDeclarations));
		int parserMapSize;
		Map<Script, C4ScriptParser>
			newEnqueued = new HashMap<Script, C4ScriptParser>(),
			lastEnqueued = new HashMap<Script, C4ScriptParser>();
		newEnqueued.putAll(parserMap);
		do {
			parserMapSize = parserMap.size();
			for (final Script s : newEnqueued.keySet())
				nature.index().addScript(s);
			if (newEnqueued.size() < 8)
				parseDeclarationsUsingSingleThread(newEnqueued);
			else
				parseDeclarationsUsingThreadPool(newEnqueued);
			if (monitor.isCanceled())
				return;
			// don't queue dependent scripts during a clean build - if everything works right all scripts will have been added anyway
			if (buildKind == CLEAN_BUILD || buildKind == FULL_BUILD)
				break;
			lastEnqueued = newEnqueued;
			newEnqueued = new HashMap<Script, C4ScriptParser>();
			indexRefresh(index);
			queueDependentScripts(lastEnqueued, newEnqueued);
		}
		while (parserMapSize != parserMap.size());
		refreshUI(newEnqueued);
		indexRefresh(index);
		markers.deploy();
	}

	private void parseDeclarationsUsingSingleThread(final Map<Script, C4ScriptParser> newEnqueued) {
		for (final Script script : newEnqueued.keySet()) {
			if (monitor.isCanceled())
				return;
			performParseDeclarations(script);
			monitor.worked(1);
		}
	}

	private void parseDeclarationsUsingThreadPool(final Map<Script, C4ScriptParser> newEnqueued) {
		TaskExecution.threadPool(new Sink<ExecutorService>() {
			@Override
			public void receivedObject(ExecutorService pool) {
				for (final Script script : newEnqueued.keySet())
					pool.execute(new Runnable() {
						@Override
						public void run() {
							if (monitor.isCanceled())
								return;
							performParseDeclarations(script);
							monitor.worked(1);
						}
					});
			}
		}, 20);
	}

	private void indexRefresh(Index index) {
		// refresh now so gathered structures will be validated with an index that has valid appendages maps and such.
		// without refreshing the index here, error markers would be created for TimerCall=... etc. assignments in ActMaps for example
		// if the function being referenced is defined in an #appendto from this index
		index.refresh(false);
		for (final Script script : parserMap.keySet())
			script.deriveInformation();
	}

	private void refreshUI(final Map<Script, C4ScriptParser> newEnqueued) {
		Display.getDefault().asyncExec(new UIRefresher(newEnqueued.keySet().toArray(new Script[newEnqueued.keySet().size()])));
	}

	private void reportProblems(final C4ScriptParser[] parsers, Script[] scripts) {
		if (Flags.DEBUG)
			System.out.println(String.format("%s: Reporting problems", getProject().getName()));
		// report problems
		monitor.subTask(String.format(Messages.ClonkBuilder_ReportingProblems, getProject().getName()));
		for (final ProblemReportingStrategy strategy : index.nature().instantiateProblemReportingStrategies(0)) {
			strategy.initialize(markers, this.monitor(), scripts);
			strategy.run();
		}
		markers.deploy();
		Display.getDefault().asyncExec(new UIRefresher(scripts));
	}

	private void clearState() {
		gatheredStructures.clear();
		parserMap.clear();
	}

	private void queueDependentScripts(Map<Script, C4ScriptParser> scriptsToQueueDependenciesFrom, final Map<Script, C4ScriptParser> newlyAddedParsers) {
		for (final C4ScriptParser parser : scriptsToQueueDependenciesFrom.values()) {
			if (monitor.isCanceled())
				break;
			if (parser == null)
				continue;
			final Definition def = as(parser.script(), Definition.class);
			if (def != null)
				index().allScripts(new IndexEntity.LoadedEntitiesSink<Script>() {
					@Override
					public void receivedObject(Script item) {
						if (!parserMap.containsKey(item) && item.directlyIncludes(def))
							newlyAddedParsers.put(item, queueScript(item));
					}
				});
		}
		for (final Structure s : gatheredStructures) {
			try {
				s.validate(markers);
			} catch (final ProblemException e) {}
			if (s.requiresScriptReparse()) {
				final Script script = Script.get(s.resource(), false);
				if (script != null) {
					final C4ScriptParser p = queueScript(script);
					newlyAddedParsers.put(script, p);
				}
			}
		}
		gatheredStructures.clear();
	}

	private void validateRelatedFiles(Script script) throws CoreException {
		if (script instanceof Definition) {
			final Definition def = (Definition) script;
			for (final IResource r : def.definitionFolder().members())
				if (r instanceof IFile) {
					final Structure pinned = Structure.pinned(r, false, true);
					if (pinned != null)
						try {
							pinned.validate(markers);
						} catch (final ProblemException e) {}
				}
		}
	}

	public C4ScriptParser queueScript(Script script) {
		C4ScriptParser result;
		if (!parserMap.containsKey(script)) {
			if (script.source() != null)
				try {
					result = new C4ScriptParser(script);
					result.setMarkers(markers);
				} catch (final Exception e) {
					System.out.println(script.resource().getProjectRelativePath().toOSString());
					e.printStackTrace();
					result = null;
				}
			else
				result = null;

			parserMap.put(script, result);
		} else
			result = parserMap.get(script);
		return result;
	}

	private void performParseDeclarations(Script script) {
		C4ScriptParser parser;
		synchronized (parserMap) {
			parser = parserMap.get(script);
		}
		if (parser != null)
			try {
				parser.parse();
			} catch (final ProblemException e) {
				e.printStackTrace();
			}
	}

}