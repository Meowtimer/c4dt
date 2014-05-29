package net.arctics.clonk.index;

import static net.arctics.clonk.Flags.DEBUG;
import static net.arctics.clonk.util.Utilities.as;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ast.ILatestDeclarationVersionProvider;
import net.arctics.clonk.ast.Structure;
import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.c4script.Directive;
import net.arctics.clonk.c4script.Directive.DirectiveType;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.Function.FunctionScope;
import net.arctics.clonk.c4script.IncludesParameters;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.SystemScript;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.c4script.Variable.Scope;
import net.arctics.clonk.c4script.ast.CallDeclaration;
import net.arctics.clonk.c4script.typing.Typing;
import net.arctics.clonk.index.IndexEntity.Loaded;
import net.arctics.clonk.index.serialization.IndexEntityInputStream;
import net.arctics.clonk.index.serialization.IndexEntityOutputStream;
import net.arctics.clonk.index.serialization.replacements.EngineRef;
import net.arctics.clonk.index.serialization.replacements.EntityDeclaration;
import net.arctics.clonk.index.serialization.replacements.EntityId;
import net.arctics.clonk.index.serialization.replacements.EntityReference;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.Sink;
import net.arctics.clonk.util.Sink.Decision;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;

/**
 * <p>An index managing lists of various objects created from parsing the folder structure of a Clonk project. Managed objects are
 * <ul>
 * <li>{@link Definition}s</li>
 * <li>{@link Script}s</li>
 * <li>{@link Scenario}s</li>
 * </ul></p>
 * <p>Additionally, some lookup tables are stored to make access to some datasets quicker, like string -> <list of declarations with that name> maps.
 * The index itself can be directly used to iterate over all {@link Definition}s it manages, while iterating over other indexed {@link Script} objects requires calling {@link #allScripts()}
 * which yields an {@link Iterable} to iterate over {@link Definition}s, {@link SystemScript}s and {@link Scenario}s.</p>
 * <p>For indexes specific to Eclipse projects (as pretty much all actual {@link Index} instances are), see {@link ProjectIndex}.</p>
 * @author madeen
 *
 */
public class Index extends Declaration implements Serializable, ILatestDeclarationVersionProvider {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	public transient Object saveSynchronizer = new Object();
	public transient Object loadSynchronizer = new Object();
	public final Object saveSynchronizer() {return saveSynchronizer;}
	public final Object loadSynchronizer() {return loadSynchronizer;}

	private transient static final Predicate<Declaration> IS_GLOBAL = item -> item.isGlobal();

	private final Map<Long, IndexEntity> entities = new HashMap<Long, IndexEntity>();
	private final Map<ID, List<Definition>> definitions = new HashMap<ID, List<Definition>>();
	private final List<Script> scripts = new LinkedList<Script>();
	private final List<Scenario> scenarios = new LinkedList<Scenario>();
	private final List<Declaration> globalsContainers = new LinkedList<Declaration>();
	private Map<ID, List<Script>> appendages = new HashMap<ID, List<Script>>();

	protected File folder;
	protected Built built = Built.No;

	public enum Built {
		No,
		Yes,
		LeaveAlone
	}

	public Built built() { return built; }
	public void built(final Built b) { built = b; }

	protected transient List<Function> globalFunctions = new LinkedList<Function>();
	protected transient List<Variable> staticVariables = new LinkedList<Variable>();
	protected transient Map<String, List<Declaration>> declarationMap = new HashMap<>();
	protected transient Map<IResource, Script> resourceToScript;

	public Index(final File folder) {
		this.folder = folder;
		if (folder != null)
			folder.mkdirs();
	}

	public Index() {}

	/**
	 * All the entities stored in this Index.
	 * @return A collection containing all entities.
	 */
	public Collection<IndexEntity> entities() { return entities.values(); }
	/**
	 * List of {@link Declaration}s containing global sub declarations.
	 */
	public List<Declaration> globalsContainers() { return globalsContainers; }

	/**
	 * Return the number of unique ids
	 * @return
	 */
	public int numUniqueIds() { return definitions.size(); }

	/**
	 * Get a list of all {@link Definition}s with a certain id.
	 * @param id The id
	 * @return The list
	 */
	public List<? extends Definition> definitionsWithID(final ID id) {
		return definitions == null ? null : definitions.get(id);
	}

	public void postLoad() throws CoreException {
		if (pendingScriptAdds == null)
			pendingScriptAdds = new LinkedList<Script>();
		if (saveSynchronizer == null)
			saveSynchronizer = new Object();
		if (loadSynchronizer == null)
			loadSynchronizer = new Object();
		// make sure all the things are actually in the entity map...
		for (final ASTNode s : subElements())
			if (s instanceof IndexEntity) {
				final IndexEntity e = (IndexEntity) s;
				if (entities.put(e.entityId(), e) == null)
					System.out.println(String.format("%s was missing from entities map", e.toString()));
			}
		for (final IndexEntity e : entities()) {
			e.index = this;
			e.loaded = Loaded.No;
		}
		refresh(true);
	}

	/**
	 * Return the object linked to the passed folder.
	 * @param folder The folder to get the {@link Definition} for
	 * @return The definition or null if the folder is not linked to any object
	 */
	public Definition definitionAt(final IContainer folder) {
		if (resourceToScript != null) {
			final Definition def = as(resourceToScript.get(folder), Definition.class);
			return def;
		}
		try {
			// fetch from session cache
			final Object prop = folder.getSessionProperty(Core.FOLDER_DEFINITION_REFERENCE_ID);
			if (prop != null)
				return (Definition) prop;

			// create session cache
			if (folder.getPersistentProperty(Core.FOLDER_C4ID_PROPERTY_ID) == null) return null;
			final Iterable<? extends Definition> objects = definitionsWithID(ID.get(folder.getPersistentProperty(Core.FOLDER_C4ID_PROPERTY_ID)));
			if (objects != null)
				for (final Definition obj : objects)
					if ((obj instanceof Definition)) {
						final Definition projDef = obj;
						if (projDef.relativePath.equalsIgnoreCase(folder.getProjectRelativePath().toPortableString())) {
							projDef.setDefinitionFolder(folder);
							return projDef;
						}
					}
			return null;
		} catch (final CoreException e) {
			// likely due to getSessionProperty being called on non-existent resources
			for (final List<Definition> list : definitions.values())
				for (final Definition obj : list)
					if (obj instanceof Definition) {
						final Definition def = obj;
						if (def.definitionFolder() != null && def.definitionFolder().equals(folder))
							return def;
					}
			// also try scenarios
			for (final Scenario s : scenarios)
				if (s.definitionFolder() != null && s.definitionFolder().equals(folder))
					return s;
			//e.printStackTrace();
			return null;
		}
	}

	/**
	 * Return the script the given file represents.
	 * @param file The file to get the script for
	 * @return the {@link Script} or null if the file does not represent one.
	 */
	public Script scriptAt(final IFile file) {
		final Script result = Script.get(file, true);
		if (result == null)
			for (final Script s : this.scripts)
				if (s.resource() != null && s.resource().equals(file))
					return s;
		return result;
	}

	protected void addToDeclarationMap(final Declaration field) {
		synchronized (declarationMap) {
			List<Declaration> list = declarationMap.get(field.name());
			if (list == null) {
				list = new LinkedList<Declaration>();
				declarationMap.put(field.name(), list);
			}
			list.add(field);
		}
	}

	private void detectAppendages(final Script script, final Map<ID, List<Script>> detectedAppendages) {
		if (detectedAppendages != null)
			for (final Directive d : script.directives())
				if (d.type() == DirectiveType.APPENDTO) {
					List<Script> appendtoList = detectedAppendages.get(d.contentAsID());
					if (appendtoList == null) {
						appendtoList = new LinkedList<Script>();
						detectedAppendages.put(d.contentAsID(), appendtoList);
					}
					appendtoList.add(script);
				}
	}

	protected <T extends Script> void addGlobalsFromScript(final T script, final Map<ID, List<Script>> detectedAppendages) {
		for (final Function func : script.functions()) {
			if (func.visibility() == FunctionScope.GLOBAL)
				globalFunctions.add(func);
			addToDeclarationMap(func);
		}
		for (final Variable var : script.variables()) {
			if (var.scope() == Scope.STATIC || var.scope() == Scope.CONST)
				staticVariables.add(var);
			addToDeclarationMap(var);
		}
		detectAppendages(script, detectedAppendages);
	}

	public void addStaticVariables(final Collection<? extends Variable> variables) {
		staticVariables.addAll(variables);
		for (final Variable v : variables)
			addToDeclarationMap(v);
	}

	/**
	 * Call {@link #refresh(boolean)} when not post-loading the index.
	 */
	public void refresh() {
		refresh(false);
		built(Built.No);
	}

	/**
	 * Re-populate the quick-access lists ({@link #globalFunctions()}, {@link #staticVariables()}, {@link #declarationMap()}, {@link #appendagesOf(Definition)}) maintained by the index based on {@link #definitions}, {@link #scenarios} and {@link #scripts}.
	 * @param postLoad true if called from {@link #postLoad()}. Will not clear some state in that case since it's assumed that it was properly loaded from the index file.
	 */
	public synchronized void refresh(final boolean postLoad) {
		cachedIncludes = new ConcurrentHashMap<IncludesParameters, Collection<Script>>();
		relevantIndexes = null;
		// delete old cache
		if (globalFunctions == null)
			globalFunctions = new LinkedList<Function>();
		if (staticVariables == null)
			staticVariables = new LinkedList<Variable>();
		if (declarationMap == null)
			declarationMap = new HashMap<String, List<Declaration>>();
		if (appendages == null)
			appendages = new HashMap<ID, List<Script>>();
		globalFunctions.clear();
		staticVariables.clear();
		declarationMap.clear();

		final Map<ID, List<Script>> newAppendages = postLoad ? null : new HashMap<ID, List<Script>>();
		allScripts(new IndexEntity.LoadedEntitiesSink<Script>() {
			@Override
			public void receive(final Script item) {
				addGlobalsFromScript(item, newAppendages);
			}
		});
		if (!postLoad)
			appendages = newAppendages;

		final int[] counts = new int[3];
		allScripts(item -> {
			if (item.loaded == Loaded.Yes)
				counts[2]++;
			if (item instanceof Definition)
				counts[0]++;
			else
				counts[1]++;
		});

		if (postLoad)
			allScripts(new IndexEntity.LoadedEntitiesSink<Script>() {
				@Override
				public void receive(final Script item) {
					item.postLoad(Index.this, Index.this);
				}
			});
	}

	private int pendingScriptIterations;
	private Queue<Script> pendingScriptAdds = new LinkedList<Script>();

	/**
	 * Add some {@link Script} to the index. If the script is a {@link Definition}, {@link #addDefinition(Definition)} will be called internally.
	 * @param script The script to add to the index
	 */
	public void addScript(final Script script) {
		if (script == null)
			return;
		synchronized (pendingScriptAdds) {
			if (pendingScriptIterations > 0) {
				pendingScriptAdds.add(script);
				return;
			}
			if (script instanceof Scenario) {
				if (!scenarios.contains(script))
					scenarios.add((Scenario) script);
			}
			else if (script instanceof Definition)
				addDefinition((Definition)script);
			else if (!scripts.contains(script))
				scripts.add(script);
		}
	}

	/**
	 * Add an {@link Definition} to the index.<br>
	 * {@link #refresh(boolean)} will need to be called manually after this.
	 * @param definition The {@link Definition} to add. Attempts to add {@link Definition}s with no id will be ignored.
	 */
	public void addDefinition(final Definition definition) {
		if (definition.id() == null)
			return;
		synchronized (pendingScriptAdds) {
			if (pendingScriptIterations > 0) {
				pendingScriptAdds.add(definition);
				return;
			}
			List<Definition> alreadyDefinedObjects = definitions.get(definition.id());
			if (alreadyDefinedObjects == null) {
				alreadyDefinedObjects = new LinkedList<Definition>();
				definitions.put(definition.id(), alreadyDefinedObjects);
			} else if (alreadyDefinedObjects.contains(definition))
				return;
			alreadyDefinedObjects.add(definition);
		}
	}

	private void startScriptIteration() {
		synchronized (pendingScriptAdds) {
			pendingScriptIterations++;
		}
	}

	private <T> void endScriptIteration() {
		synchronized (pendingScriptAdds) {
			if (--pendingScriptIterations == 0) {
				Script s;
				while ((s = pendingScriptAdds.poll()) != null)
					addScript(s);
			}
		}
	}

	public <T> void allDefinitions(final Sink<T> sink) {
		startScriptIteration();
		try {
			allDefinitionsInternal(sink);
		} finally {
			endScriptIteration();
		}
	}

	@SuppressWarnings("unchecked")
	private <T> Sink.Decision allDefinitionsInternal(final Sink<T> sink) {
		final Iterator<List<Definition>> defsIt = definitions.values().iterator();
		while (defsIt.hasNext()) {
			final List<Definition> list = defsIt.next();
			final Iterator<Definition> defIt = list.iterator();
			while (defIt.hasNext())
				switch (sink.elutriate((T)defIt.next())) {
				case PurgeItem:
					defIt.remove();
					break;
				case AbortIteration:
					return Decision.AbortIteration;
				default:
					break;
				}
			if (list.size() == 0)
				defsIt.remove();
		}
		return Decision.Continue;
	}

	public void allScripts(final Sink<? super Script> sink) {
		startScriptIteration();
		try {
			if (allDefinitionsInternal(sink) == Decision.AbortIteration)
				return;
			ArrayUtil.sink(scripts, sink);
			ArrayUtil.sink(scenarios, sink);
		} finally {
			endScriptIteration();
		}
	}

	/**
	 * Remove the script from the index.
	 * @param script Some script. Can be a {@link Definition}, a {@link Scenario} or some other {@link Script} object managed by this index.
	 */
	public void removeScript(final Script script) {
		if (script instanceof Definition)
			removeDefinition((Definition)script);
		else
			if (scripts.remove(script))
				scriptRemoved(script);
		entities.remove(script.entityId());
		script.index = null;
	}

	/**
	 * Remove a {@link Definition} from this index.<br>
	 * {@link #refresh()} will need to be called manually after this.
	 * No attempts are made to remove session properties from affected resources (see {@link Core#FOLDER_DEFINITION_REFERENCE_ID})
	 * @param definition The {@link Definition} to remove from the index
	 */
	public void removeDefinition(final Definition definition) {
		if (definition instanceof Scenario) {
			removeScenario((Scenario)definition);
			return;
		}
		if (definition.id() == null)
			return;
		final List<Definition> alreadyDefinedObjects = definitions.get(definition.id());
		if (alreadyDefinedObjects != null)
			if (alreadyDefinedObjects.remove(definition)) {
				if (alreadyDefinedObjects.size() == 0)
					definitions.remove(definition.id());
				scriptRemoved(definition);
			}
	}

	/**
	 * Remove a {@link Scenario} from the index.
	 * @param scenario The {@link Scenario} to remove
	 */
	public void removeScenario(final Scenario scenario) {
		if (scenarios.remove(scenario))
			scriptRemoved(scenario);
	}

	private void scriptRemoved(final Script script) {
		entities.remove(script.entityId());
		allScripts(new IndexEntity.LoadedEntitiesSink<Script>() {
			@Override
			public void receive(final Script item) {
				item.scriptRemovedFromIndex(script);
			}
		});
	}

	public List<Scenario> scenarios() { return Collections.unmodifiableList(scenarios); }
	public List<Script> scripts() { return Collections.unmodifiableList(scripts); }
	public List<Function> globalFunctions() { return Collections.unmodifiableList(globalFunctions); }
	public List<Variable> staticVariables() { return Collections.unmodifiableList(staticVariables); }
	public Map<String, List<Declaration>> declarationMap() { return Collections.unmodifiableMap(declarationMap); }

	/**
	 * Return the last {@link Definition} with the specified {@link ID}, whereby 'last' is arbitrary, maybe loosely based on the directory and lexical structure of the project.
	 * @param id The id
	 * @return The 'last' {@link Definition} with that id
	 */
	public Definition lastDefinitionWithId(final ID id) {
		final Iterable<? extends Definition> objs = definitionsWithID(id);
		if (objs != null) {
			Definition result = null;
			for (final Definition def : objs)
				result = def;
			return result;
		}
		return null;
	}

	public static void addIndexesFromReferencedProjects(final List<Index> result, final Index index) {
		if (index instanceof ProjectIndex) {
			final ProjectIndex projIndex = (ProjectIndex) index;
			try {
				final List<Index> newOnes = new LinkedList<Index>();
				for (final IProject p : projIndex.nature().getProject().getReferencedProjects()) {
					final ClonkProjectNature n = ClonkProjectNature.get(p);
					if (n != null && n.index() != null && !result.contains(n.index()))
						newOnes.add(n.index());
				}
				result.addAll(newOnes);
				for (final Index i : newOnes)
					addIndexesFromReferencedProjects(result, i);
			} catch (final CoreException e) {
				e.printStackTrace();
			}
		}
	}

	private static final Object relevantIndexesSynchronizer = new Object();
	private transient List<Index> relevantIndexes = null;

	public List<Index> relevantIndexes() {
		synchronized (relevantIndexesSynchronizer) {
			if (relevantIndexes == null) {
				relevantIndexes = new ArrayList<Index>(10);
				relevantIndexes.add(this);
				addIndexesFromReferencedProjects(relevantIndexes, this);
			}
			return relevantIndexes;
		}
	}

	/**
	 * Of those definitions with a matching id, choose the one nearest to the specified resource.
	 * @param resource The resource
	 * @param id The id
	 * @return The chosen definition.
	 */
	public Definition definitionNearestTo(final IResource resource, final ID id) {
		Definition best = null;
		for (final Index index : relevantIndexes()) {
			if (resource != null) {
				final List<? extends Definition> objs = index.definitionsWithID(id);
				best = objs == null ? null : objs.size() == 1 ? objs.get(0) : Utilities.pickNearest(objs, resource, null);
			}
			else
				best = index.lastDefinitionWithId(id);
			if (best != null)
				break;
		}
		return best;
	}

	/**
	 * Returns any {@link Definition} that satisfies the condition of having the specified {@link ID}.
	 * @param id The ID
	 * @return A definition with a matching id or null if not found. If there are multiple ones, it is not defined which one was returned.
	 */
	public Definition anyDefinitionWithID(final ID id) {
		return definitionNearestTo(null, id);
	}

	/**
	 * Return all declarations with the specified name that are instances of the specified class.
	 * @param <T> Genericly typed so no casting necessary
	 * @param name The name
	 * @param declarationClass The class of the declarations to return
	 * @return An Iterable to iterate over the matching declarations
	 */
	@SuppressWarnings("unchecked")
	public <T extends Declaration> List<T> declarationsWithName(final String name, final Class<T> declarationClass) {
		List<Declaration> list;
		synchronized (declarationMap) {
			list = this.declarationMap.get(name);
			if (list != null) {
				final ArrayList<T> result = new ArrayList<>(list.size());
				for (final Declaration d : list)
					if (declarationClass.isInstance(d))
						result.add((T)d);
				return result;
			} else
				return Collections.emptyList();
		}
	}

	/**
	 * Find a global what-have-you (either {@link Function} or {@link Variable}) with the given name.
	 * @param whatYouWant The {@link Declaration} class
	 * @param name The name
	 * @return The global object with a matching name or null.
	 */
	@SuppressWarnings("unchecked")
	public <T extends Declaration> T findGlobal(final Class<T> whatYouWant, final String name) {
		synchronized (declarationMap) {
			final List<Declaration> decs = declarationMap.get(name);
			if (decs == null)
				return null;
			for (final Declaration d : decs)
				if (d.isGlobal() && whatYouWant.isInstance(d))
					return (T)d;
			return null;
		}
	}

	/**
	 * Find a global declaration (static var or global func) with the specified name. If there are multiple global declarations with that name, the one nearest to 'pivot' will be returned.
	 * @param declName The declaration name
	 * @param pivot The pivot of the call, acting as a tie breaker if there are multiple global declarations with that name. The one nearest to the pivot will be returned. Can be null, in which case the 'first' match will be returned.
	 * @return A global {@link Declaration} with a matching name or null.
	 */
	public Declaration findGlobalDeclaration(final String declName, final IResource pivot) {
		synchronized (declarationMap) {
			final List<Declaration> declarations = declarationMap.get(declName);
			if (declarations != null)
				return pivot != null
				? Utilities.pickNearest(declarations, pivot, IS_GLOBAL)
					: IS_GLOBAL.test(declarations.get(0))
					? declarations.get(0)
						: null;
					return null;
		}
	}

	/**
	 * Clear the index so it won't manage any objects after this call.
	 */
	public synchronized void clear() {
		synchronized(definitions) {
			definitions.clear();
		}
		scripts.clear();
		scenarios.clear();
		clearEntityFiles();
		entities.clear();
		entityIdCounter = 0;
		refresh(false);
		built(Built.No);
	}

	private void clearEntityFiles() {
		for (final IndexEntity e : entities())
			entityFile(e).delete();
	}

	/**
	 * Return an {@link Iterable} to iterate over all {@link Definition}s managed by this index, but in the case of multiple {@link Definition}s with the same name yielding only the one nearest to pivot.
	 * @param pivot The pivot dictating the perspective of the call
	 * @return An {@link Iterable} to iterate over this presumably large subset of all the {@link Definition}s managed by the index.
	 */
	public Stream<Definition> definitionsIgnoringRemoteDuplicates(final IResource pivot) {
		return definitions.values().stream().map(from -> Utilities.pickNearest(from, pivot, null));
	}

	/**
	 * Return all scripts that append themselves to the specified {@link Definition}
	 * @param definition The definition to return 'appendages' of
	 * @return The appendages
	 */
	public List<Script> appendagesOf(final Definition definition) {
		if (appendages == null)
			return null;
		final List<Script> a = appendages.get(definition.id());
		if (a != null)
			return new ArrayList<Script>(a);
		else
			return null;
	}

	@Override
	public Engine engine() {
		return Core.instance().activeEngine();
	}

	@Override
	public Index index() {
		return this;
	}

	/**
	 * Load an index from disk, instantiating all the high-level entities, but deferring loading detailed entity info until it's needed on an entity-by-entity basis.
	 * @param <T> {@link Index} class to return.
	 * @param indexClass The class to instantiate
	 * @param indexFolder File to load the index from
	 * @param fallbackFileLocation Secondary file location to use if the first one does not exist
	 * @return The loaded index or null if loading the index failed for any reason.
	 */
	public static <T extends Index> T loadShallow(final Class<T> indexClass, final File indexFolder, final File fallbackFileLocation, final Engine engine) {
		if (!indexFolder.isDirectory())
			return null;
		try (
			final InputStream in = new GZIPInputStream(new FileInputStream(new File(indexFolder, "index")));
			final ObjectInputStream objStream = new IndexEntityInputStream(new Index() {
				private static final long serialVersionUID = 1L;
				@Override
				public Engine engine() { return engine; }
			}, null, in)
		) {
			final T index = indexClass.cast(objStream.readObject());
			index.shallowAwake();
			for (final IndexEntity e : index.entities())
				e.index = index;
			index.folder = indexFolder;
			return index;
		} catch (final Exception e) {
			e.printStackTrace();
			// somehow failed - ignore
			return null;
		}
	}

	/**
	 * Finds a script by its path. This may be a path to an actual file or some other kind of path understood by the kind of index. But since the only relevant subclass of ClonkIndex is {@link ProjectIndex}, that's moot!
	 * @param path the path
	 * @return the script or null if not found
	 */
	public Script findScriptByPath(final String path) {
		return null;
	}

	/**
	 * Return associated project. Returns null in base implementation. See {@link ProjectIndex#nature()}.
	 * @return The project
	 */
	public ClonkProjectNature nature() {return null;}

	@Override
	public boolean equals(final Object obj) {
		return obj == this || (obj instanceof Index && ((Index)obj).nature() == this.nature());
	}

	/**
	 * Implementation whose merits can rightfully be argued. Will return the hashcode of the index's project's name, if there is some associated project, or call the super implementation instead.
	 */
	@Override
	public int hashCode() {
		if (nature() != null)
			return nature().getProject().getName().hashCode(); // project name should be unique
		else
			return super.hashCode();
	}

	/**
	 * Sink all {@link #relevantIndexes()} into some {@link Sink}
	 * @param The {@link Sink}
	 */
	public void forAllRelevantIndexes(final Sink<Index> sink) {
		for (final Index index : relevantIndexes())
			sink.elutriate(index);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Declaration> T latestVersionOf(final T from) {
		if (from instanceof Script)
			return (T) Utilities.scriptForResource(from.resource());
		else if (from instanceof Structure && from.resource() instanceof IFile)
			return (T) Structure.pinned(from.resource(), false, false);
		return null;
	}

	private class EntityLoader {
		@SuppressWarnings("serial")
		class PostLoadQueue extends LinkedList<IndexEntity> {}
		private final ThreadLocal<PostLoadQueue> postLoadQueue = new ThreadLocal<PostLoadQueue>() {
			@Override
			protected PostLoadQueue initialValue() { return new PostLoadQueue(); }
		};
		private void doPostLoad(final IndexEntity e) {
			try {
				e.postLoad(Index.this, Index.this);
			} catch (final Exception x) {
				System.out.println(String.format("Error post-loading '%s': %s", e.qualifiedName(), x.getMessage()));
			}
		}
		public void loadEntity(final IndexEntity entity) {
			try {
				final PostLoadQueue queue = postLoadQueue.get();
				final boolean initialCall = queue.size() == 0;
				queue.offer(entity);
				doLoad(entity);
				if (initialCall)
					for (IndexEntity e; (e = queue.poll()) != null;)
						doPostLoad(e);
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
		private void doLoad(final IndexEntity entity) throws IOException {
			try (final ObjectInputStream inputStream = newEntityInputStream(entity)) {
				entity.load(inputStream);
			} catch (final FileNotFoundException fn) {
				// so no file found - big deal
			} catch (final Exception e) {
				System.out.println(String.format("Failed to load entity '%s': %s",
					entity.qualifiedName(),
					e.getMessage()
				));
			}
			if (entity instanceof Script)
				addGlobalsFromScript((Script)entity, appendages);
		}
	}

	private transient EntityLoader entityLoader;

	{ shallowAwake(); }

	protected void shallowAwake() {
		entityLoader = new EntityLoader();
	}

	public void loadAllEntities() {
		for (final IndexEntity e : entities.values())
			e.requireLoaded();
	}

	public void loadEntity(final IndexEntity entity) throws FileNotFoundException, IOException, ClassNotFoundException {
		if (DEBUG)
			System.out.println("Load entity " + entity.toString());
		entityLoader.loadEntity(entity);
	}

	public void saveEntity(final IndexEntity entity) throws IOException {
		try (final ObjectOutputStream s = newEntityOutputStream(entity)) {
			entity.save(s);
		}
	}

	private long entityIdCounter = 0;

	/**
	 * Add a new entity to the index, returning it's designated id.
	 * @param entity
	 * @return The id of the entity. Is supposed to be unique.
	 */
	protected long addEntity(final IndexEntity entity) {
		synchronized (this) {
			final long id = entityIdCounter++;
			this.entities.put(id, entity);
			if (newEntities != null)
				newEntities.add(entity);
			return id;
		}
	}

	private File entityFile(final IndexEntity entity) {
		return new File(folder, Long.toString(entity.entityId()));
	}

	public ObjectOutputStream newEntityOutputStream(final IndexEntity entity) throws FileNotFoundException, IOException {
		return new IndexEntityOutputStream(this, entity, new GZIPOutputStream(new FileOutputStream(entityFile(entity))));
	}

	public ObjectInputStream newEntityInputStream(final IndexEntity entity) throws FileNotFoundException, IOException {
		return new IndexEntityInputStream(this, entity, new GZIPInputStream(new FileInputStream(entityFile(entity))));
	}

	public IndexEntity entityWithId(final long entityId) {
		return entities.get(entityId);
	}

	/**
	 * Return an object that will be serialized instead of an entity. It will implement {@link IDeserializationResolvable}.
	 * @param entity
	 * @return
	 */
	public IDeserializationResolvable saveReplacementForEntity(final IndexEntity entity) {
		if (entity == null)
			return null;
		if (entity instanceof Engine)
			return new EngineRef((Engine)entity);
		else if (entity.index() == this)
			return new EntityId(entity);
		else
			return new EntityReference(entity);
	}

	/**
	 * Save the file storing what entities exist in this index but don't write entity-specific files.
	 */
	public void saveShallow() {
		final File indexFile = new File(folder, "index");
		folder.mkdirs();
		try {
			indexFile.createNewFile();
		} catch (final IOException e1) {
			e1.printStackTrace();
			return;
		}
		try (
			final OutputStream out = new GZIPOutputStream(new FileOutputStream(indexFile));
			final IndexEntityOutputStream objStream = new IndexEntityOutputStream(this, null, out) {
				@Override
				protected Object replaceObject(Object obj) throws IOException {
					// directives are also directly saved
					if (obj instanceof Directive)
						return obj;
					// disable replacing entities with EntityId objects which won't resolve properly because this here is the place where entities are actually saved.
					if (obj instanceof IndexEntity && ((IndexEntity)obj).index == Index.this)
						return obj;
					return super.replaceObject(obj);
				}
			}
		) {
			removeNullsInScriptLists();
			objStream.writeObject(index());
			purgeUnusedIndexFiles(indexFile);
		} catch (final Exception e) {
			System.out.println(String.format("Error saving index for '%s'", this.nature().getProject().getName()));
			e.printStackTrace();
		}
	}

	private void removeNullsInScriptLists() {
		if (scripts.removeAll(Collections.singleton(null)))
			System.out.println("There were nulls in indexedScripts");
		if (scenarios.removeAll(Collections.singleton(null)))
			System.out.println("There were nulls in indexedScenarios");
	}

	protected void purgeUnusedIndexFiles(final File indexFile) {
		synchronized (this) {
			final File[] files = folder.listFiles();
			final List<File> filesToBePurged = new ArrayList<File>(files.length);
			filesToBePurged.addAll(Arrays.asList(files));
			filesToBePurged.remove(indexFile);
			for (final IndexEntity e : entities())
				filesToBePurged.remove(entityFile(e));
			for (final File f : filesToBePurged)
				if (!f.getName().startsWith("."))
					f.delete();
		}
	}


	private List<IndexEntity> newEntities;

	public void populateResourceToScriptMap() {
		final ClonkProjectNature nature = nature();
		if (nature != null) {
			@SuppressWarnings("serial")
			class ResourceToScriptMap extends HashMap<IResource, Script> implements IResourceVisitor {
				@Override
				public boolean visit(final IResource resource) throws CoreException {
					if (resource instanceof IContainer) {
						final Definition def = definitionAt((IContainer) resource);
						this.put(resource, def);
					} else
						this.put(resource, Script.get(resource, false));
					return true;
				}
			};
			final ResourceToScriptMap rts = new ResourceToScriptMap();
			try {
				nature.getProject().accept(rts);
			} catch (final CoreException e) {
				e.printStackTrace();
			}
			resourceToScript = rts;
		}
	}

	public synchronized void beginModification() {
		newEntities = new LinkedList<IndexEntity>();
		relevantIndexes = null; // force rebuild of list
	}

	public synchronized void endModification() {
		resourceToScript = null;
		saveNewEntities();
	}

	private void saveNewEntities() {
		for (final IndexEntity e : newEntities)
			if (e.loaded == Loaded.Yes && e.saveCalledByIndex())
				try {
					e.save();
				} catch (final IOException e1) {
					e1.printStackTrace();
				}
		newEntities = null;
	}


	public Object saveReplacementForEntityDeclaration(final Declaration obj, final IndexEntity entity) {
		final Index objIndex = obj.index();
		final IndexEntity objOwner = obj.parent(IndexEntity.class);
		if (objIndex == null || (objIndex == this && entity == objOwner))
			return obj;
		else {
			if (DEBUG)
				System.out.println(String.format("%s: Replacement for '%s' (from '%s')",
					entity != null ? entity.qualifiedName() : "<index>", obj.qualifiedName(), objOwner != null ? objOwner.qualifiedName() : "<null>"));
			return new EntityDeclaration(obj, objOwner);
		}
	}

	public void loadScriptsContainingDeclarationsNamed(final String name) {
		allScripts(new Sink<Script>() {
			@Override
			public void receive(final Script item) { item.requireLoaded(); }
			@Override
			public boolean filter(final Script s) {
				return s.dictionary() != null && s.dictionary().contains(name);
			}
		});
	}

	/**
	 * Return a copy of the current list of declarations with the given name.
	 * Use this method if you expect your usage of the returned list to trigger loading additional scripts which will in turn
	 * modify the original lists inside {@link #declarationMap()}, causing {@link ConcurrentModificationException}s.
	 * @param name Name key passed to {@link #declarationMap()}
	 * @return The copy or null if no declarations with that name are currently loaded.
	 */
	public List<Declaration> snapshotOfDeclarationsNamed(final String name) {
		synchronized (declarationMap) {
			final List<Declaration> decs = declarationMap.get(name);
			if (decs == null)
				return null;
			final ArrayList<Declaration> result = new ArrayList<Declaration>(decs);
			return result;
		}
	}

	public List<CallDeclaration> callsTo(final String functionName) {
		final List<CallDeclaration> result = new ArrayList<>(10);
		allScripts(item -> {
			final Map<String, List<CallDeclaration>> calls = item.callMap();
			if (calls != null) synchronized (calls) {
				final List<CallDeclaration> list = calls.get(functionName);
				if (list != null)
					result.addAll(list);
			}
		});
		return result.size() > 0 ? result : null;
	}

	@Override
	public ASTNode[] subElements() {
		final List<ASTNode> nodes = new ArrayList<>(100);
		nodes.addAll(scenarios);
		nodes.addAll(scripts);
		for (final List<Definition> defs : definitions.values())
			nodes.addAll(defs);
		return nodes.toArray(new ASTNode[nodes.size()]);
	}

	@Override
	public Typing typing() { return Typing.INFERRED; }

	private transient ConcurrentHashMap<IncludesParameters, Collection<Script>> cachedIncludes;

	public Collection<Script> includes(final IncludesParameters parms) {
		Map<IncludesParameters, Collection<Script>> map = cachedIncludes;
		if (map == null)
			map = cachedIncludes = new ConcurrentHashMap<IncludesParameters, Collection<Script>>();
		Collection<Script> result = map.get(parms);
		if (result == null) {
			result = new HashSet<Script>();
			parms.script.gatherIncludes(this, parms.origin, result, parms.options);
			result.remove(parms.script);
			map.put(parms, result);
		}
		return result;
	}

}
