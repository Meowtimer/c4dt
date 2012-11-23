package net.arctics.clonk.index;

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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.ID;
import net.arctics.clonk.parser.ILatestDeclarationVersionProvider;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.parser.c4script.Directive;
import net.arctics.clonk.parser.c4script.Directive.DirectiveType;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.Function.FunctionScope;
import net.arctics.clonk.parser.c4script.IProplistDeclaration;
import net.arctics.clonk.parser.c4script.ProplistDeclaration;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.parser.c4script.SystemScript;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.Variable.Scope;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.ConvertingIterable;
import net.arctics.clonk.util.IConverter;
import net.arctics.clonk.util.IPredicate;
import net.arctics.clonk.util.Sink;
import net.arctics.clonk.util.Sink.Decision;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
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
 * <p>For indexes specific to Eclipse projects (as pretty much all actual ClonkIndex instances are), see {@link ProjectIndex}.</p>
 * @author madeen
 *
 */
public class Index extends Declaration implements Serializable, ILatestDeclarationVersionProvider {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	public transient Object saveSynchronizer = new Object();
	public transient Object loadSynchronizer = new Object();
	public final Object saveSynchronizer() {return saveSynchronizer;}
	public final Object loadSynchronizer() {return loadSynchronizer;}

	private transient static final IPredicate<Declaration> IS_GLOBAL = new IPredicate<Declaration>() {
		@Override
		public boolean test(Declaration item) {
			return item.isGlobal();
		}
	};

	private final Map<Long, IndexEntity> entities = new HashMap<Long, IndexEntity>();
	private final Map<ID, List<Definition>> indexedDefinitions = new HashMap<ID, List<Definition>>();
	private final List<Script> indexedScripts = new LinkedList<Script>();
	private final List<Scenario> indexedScenarios = new LinkedList<Scenario>();
	private final List<ProplistDeclaration> indexedProplistDeclarations = new LinkedList<ProplistDeclaration>();
	private final List<Declaration> globalsContainers = new LinkedList<Declaration>();
	private Map<ID, List<Script>> appendages = new HashMap<ID, List<Script>>();

	protected File folder;

	protected transient List<Function> globalFunctions = new LinkedList<Function>();
	protected transient List<Variable> staticVariables = new LinkedList<Variable>();
	protected transient Map<String, List<Declaration>> declarationMap = new Hashtable<String, List<Declaration>>();

	public Index(File folder) {
		this.folder = folder;
		if (folder != null)
			folder.mkdirs();
	}

	public Index() {}

	/**
	 * All the entities stored in this Index.
	 * @return A collection containing all entities.
	 */
	public Collection<IndexEntity> entities() {
		return entities.values();
	}

	/**
	 * List of {@link Declaration}s containing global sub declarations.
	 */
	public List<Declaration> globalsContainers() {
		return globalsContainers;
	}

	/**
	 * Return the number of unique ids
	 * @return
	 */
	public int numUniqueIds() {
		return indexedDefinitions.size();
	}

	/**
	 * Get a list of all {@link Definition}s with a certain id.
	 * @param id The id
	 * @return The list
	 */
	public List<? extends Definition> definitionsWithID(ID id) {
		if (indexedDefinitions == null)
			return null;
		else
			return indexedDefinitions.get(id);
	}

	public void postLoad() throws CoreException {
		if (pendingScriptAdds == null)
			pendingScriptAdds = new LinkedList<Script>();
		if (saveSynchronizer == null)
			saveSynchronizer = new Object();
		if (loadSynchronizer == null)
			loadSynchronizer = new Object();
		for (IndexEntity e : entities()) {
			e.index = this;
			e.loaded = false;
		}
		refreshIndex(true);
	}

	/**
	 * Return the object linked to the passed folder.
	 * @param folder The folder to get the {@link Definition} for
	 * @return The definition or null if the folder is not linked to any object
	 */
	public Definition definitionAt(IContainer folder) {
		try {
			// fetch from session cache
			if (folder.getSessionProperty(Core.FOLDER_DEFINITION_REFERENCE_ID) != null)
				return (Definition) folder.getSessionProperty(Core.FOLDER_DEFINITION_REFERENCE_ID);

			// create session cache
			if (folder.getPersistentProperty(Core.FOLDER_C4ID_PROPERTY_ID) == null) return null;
			Iterable<? extends Definition> objects = definitionsWithID(ID.get(folder.getPersistentProperty(Core.FOLDER_C4ID_PROPERTY_ID)));
			if (objects != null)
				for (Definition obj : objects)
					if ((obj instanceof Definition)) {
						Definition projDef = obj;
						if (projDef.relativePath.equalsIgnoreCase(folder.getProjectRelativePath().toPortableString())) {
							projDef.setDefinitionFolder(folder);
							return projDef;
						}
					}
			return null;
		} catch (CoreException e) {
			// likely due to getSessionProperty being called on non-existent resources
			for (List<Definition> list : indexedDefinitions.values())
				for (Definition obj : list)
					if (obj instanceof Definition) {
						Definition def = obj;
						if (def.definitionFolder() != null && def.definitionFolder().equals(folder))
							return def;
					}
			// also try scenarios
			for (Scenario s : indexedScenarios)
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
	public Script scriptAt(IFile file) {
		Script result = Script.get(file, true);
		if (result == null)
			for (Script s : this.indexedScripts)
				if (s.resource() != null && s.resource().equals(file))
					return s;
		return result;
	}

	protected void addToDeclarationMap(Declaration field) {
		List<Declaration> list = declarationMap.get(field.name());
		if (list == null) {
			list = new LinkedList<Declaration>();
			declarationMap.put(field.name(), list);
		}
		list.add(field);
	}

	protected void addToProplistDeclarations(ProplistDeclaration proplistDeclaration) {
		indexedProplistDeclarations.add(proplistDeclaration);
		for (Variable v : proplistDeclaration.components(true))
			addToDeclarationMap(v);
	}

	private void detectAppendages(Script script, Map<ID, List<Script>> detectedAppendages) {
		if (detectedAppendages != null)
			for (Directive d : script.directives())
				if (d.type() == DirectiveType.APPENDTO) {
					List<Script> appendtoList = detectedAppendages.get(d.contentAsID());
					if (appendtoList == null) {
						appendtoList = new LinkedList<Script>();
						detectedAppendages.put(d.contentAsID(), appendtoList);
					}
					appendtoList.add(script);
				}
	}

	protected <T extends Script> void addGlobalsFromScript(T script, Map<ID, List<Script>> detectedAppendages) {
		for (Function func : script.functions()) {
			if (func.visibility() == FunctionScope.GLOBAL)
				globalFunctions.add(func);
			for (Declaration otherDec : func.otherDeclarations())
				if (otherDec instanceof IProplistDeclaration)
					addToProplistDeclarations((ProplistDeclaration) otherDec);
			addToDeclarationMap(func);
		}
		for (Variable var : script.variables()) {
			if (var.scope() == Scope.STATIC || var.scope() == Scope.CONST)
				staticVariables.add(var);
			addToDeclarationMap(var);
		}
		detectAppendages(script, detectedAppendages);
	}

	/**
	 * Call {@link #refreshIndex(boolean)} when not post-loading the index.
	 */
	public void refreshIndex() {
		refreshIndex(false);
	}

	/**
	 * Re-populate the quick-access lists ({@link #globalFunctions()}, {@link #staticVariables()}, {@link #declarationMap()}, {@link #appendagesOf(Definition)}) maintained by the index based on {@link #indexedDefinitions}, {@link #indexedScenarios} and {@link #indexedScripts}.
	 * @param postLoad true if called from {@link #postLoad()}. Will not clear some state in that case since it's assumed that it was properly loaded from the index file.
	 */
	public synchronized void refreshIndex(final boolean postLoad) {
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

		final int[] counts = new int[3];
		allScripts(new IndexEntity.LoadedEntitiesSink<Script>() {
			@Override
			public void receivedObject(Script item) {
				item.indexRefresh();
				if (item.loaded)
					counts[2]++;
				if (item instanceof Definition)
					counts[0]++;
				else
					counts[1]++;
			}
		});

		final Map<ID, List<Script>> newAppendages = postLoad ? null : new HashMap<ID, List<Script>>();
		allScripts(new IndexEntity.LoadedEntitiesSink<Script>() {
			@Override
			public void receivedObject(Script item) {
				addGlobalsFromScript(item, newAppendages);
			}
		});
		if (!postLoad)
			appendages = newAppendages;

		if (postLoad)
			allScripts(new IndexEntity.LoadedEntitiesSink<Script>() {
				@Override
				public void receivedObject(Script item) {
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
	public void addScript(Script script) {
		if (script == null)
			return;
		synchronized (pendingScriptAdds) {
			if (pendingScriptIterations > 0) {
				pendingScriptAdds.add(script);
				return;
			}
			if (script instanceof Scenario) {
				if (!indexedScenarios.contains(script))
					indexedScenarios.add((Scenario) script);
			}
			else if (script instanceof Definition)
				addDefinition((Definition)script);
			else if (!indexedScripts.contains(script))
				indexedScripts.add(script);
		}
	}

	/**
	 * Add an {@link Definition} to the index.<br>
	 * {@link #refreshIndex()} will need to be called manually after this.
	 * @param definition The {@link Definition} to add. Attempts to add {@link Definition}s with no id will be ignored.
	 */
	public void addDefinition(Definition definition) {
		if (definition.id() == null)
			return;
		synchronized (pendingScriptAdds) {
			if (pendingScriptIterations > 0) {
				pendingScriptAdds.add(definition);
				return;
			}
			List<Definition> alreadyDefinedObjects = indexedDefinitions.get(definition.id());
			if (alreadyDefinedObjects == null) {
				alreadyDefinedObjects = new LinkedList<Definition>();
				indexedDefinitions.put(definition.id(), alreadyDefinedObjects);
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

	public <T> void allDefinitions(Sink<T> sink) {
		startScriptIteration();
		try {
			allDefinitionsInternal(sink);
		} finally {
			endScriptIteration();
		}
	}

	@SuppressWarnings("unchecked")
	private <T> void allDefinitionsInternal(Sink<T> sink) {
		Iterator<List<Definition>> defsIt = indexedDefinitions.values().iterator();
		while (defsIt.hasNext()) {
			List<Definition> list = defsIt.next();
			Iterator<Definition> defIt = list.iterator();
			while (defIt.hasNext())
				if (sink.elutriate((T)defIt.next()) == Decision.Purge)
					defIt.remove();
			if (list.size() == 0)
				defsIt.remove();
		}
	}

	public void allScripts(Sink<Script> sink) {
		startScriptIteration();
		try {
			allDefinitionsInternal(sink);
			ArrayUtil.sink(indexedScripts, sink);
			ArrayUtil.sink(indexedScenarios, sink);
		} finally {
			endScriptIteration();
		}
	}

	/**
	 * Remove the script from the index.
	 * @param script Some script. Can be a {@link Definition}, a {@link Scenario} or some other {@link Script} object managed by this index.
	 */
	public void removeScript(Script script) {
		if (script instanceof Definition)
			removeDefinition((Definition)script);
		else
			if (indexedScripts.remove(script))
				scriptRemoved(script);
		entities.remove(script.entityId());
		script.index = null;
	}

	/**
	 * Remove a {@link Definition} from this index.<br>
	 * {@link #refreshIndex()} will need to be called manually after this.
	 * No attempts are made to remove session properties from affected resources (see {@link Core#FOLDER_DEFINITION_REFERENCE_ID})
	 * @param definition The {@link Definition} to remove from the index
	 */
	public void removeDefinition(Definition definition) {
		if (definition instanceof Scenario) {
			removeScenario((Scenario)definition);
			return;
		}
		if (definition.id() == null)
			return;
		List<Definition> alreadyDefinedObjects = indexedDefinitions.get(definition.id());
		if (alreadyDefinedObjects != null)
			if (alreadyDefinedObjects.remove(definition)) {
				if (alreadyDefinedObjects.size() == 0)
					indexedDefinitions.remove(definition.id());
				scriptRemoved(definition);
			}
	}

	/**
	 * Remove a {@link Scenario} from the index.
	 * @param scenario The {@link Scenario} to remove
	 */
	public void removeScenario(Scenario scenario) {
		if (indexedScenarios.remove(scenario))
			scriptRemoved(scenario);
	}

	private void scriptRemoved(final Script script) {
		entities.remove(script.entityId());
		allScripts(new IndexEntity.LoadedEntitiesSink<Script>() {
			@Override
			public void receivedObject(Script item) {
				item.scriptRemovedFromIndex(script);
			}
		});
	}

	public List<Scenario> indexedScenarios() {
		return Collections.unmodifiableList(indexedScenarios);
	}

	public List<Script> indexedScripts() {
		return Collections.unmodifiableList(indexedScripts);
	}

	public List<ProplistDeclaration> indexedProplistDeclarations() {
		return indexedProplistDeclarations;
	}

	public List<Function> globalFunctions() {
		return Collections.unmodifiableList(globalFunctions);
	}

	public List<Variable> staticVariables() {
		return Collections.unmodifiableList(staticVariables);
	}

	public Map<String, List<Declaration>> declarationMap() {
		return Collections.unmodifiableMap(declarationMap);
	}

	/**
	 * Return the last {@link Definition} with the specified {@link ID}, whereby 'last' is arbitrary, maybe loosely based on the directory and lexical structure of the project. 
	 * @param id The id
	 * @return The 'last' {@link Definition} with that id
	 */
	public Definition lastDefinitionWithId(ID id) {
		Iterable<? extends Definition> objs = definitionsWithID(id);
		if (objs != null) {
			Definition result = null;
			for (Definition def : objs)
				result = def;
			return result;
		}
		return null;
	}

	public static void addIndexesFromReferencedProjects(List<Index> result, Index index) {
		if (index instanceof ProjectIndex) {
			ProjectIndex projIndex = (ProjectIndex) index;
			try {
				List<Index> newOnes = new LinkedList<Index>();
				for (IProject p : projIndex.project().getReferencedProjects()) {
					ClonkProjectNature n = ClonkProjectNature.get(p);
					if (n != null && n.index() != null && !result.contains(n.index()))
						newOnes.add(n.index());
				}
				result.addAll(newOnes);
				for (Index i : newOnes)
					addIndexesFromReferencedProjects(result, i);
			} catch (CoreException e) {
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
	public Definition definitionNearestTo(IResource resource, ID id) {
		Definition best = null;
		for (Index index : relevantIndexes()) {
			if (resource != null) {
				List<? extends Definition> objs = index.definitionsWithID(id);
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
	public Definition anyDefinitionWithID(ID id) {
		return definitionNearestTo(null, id);
	}

	/**
	 * Return all declarations with the specified name that are instances of the specified class.
	 * @param <T> Genericly typed so no casting necessary
	 * @param name The name
	 * @param declarationClass The class of the declarations to return 
	 * @return An Iterable to iterate over the matching declarations
	 */
	public <T extends Declaration> Iterable<T> declarationsWithName(String name, final Class<T> declarationClass) {
		List<Declaration> list = this.declarationMap.get(name);
		if (list == null)
			list = new LinkedList<Declaration>();
		return ArrayUtil.filteredIterable(list, declarationClass);
	}

	/**
	 * Find a global what-have-you (either {@link Function} or {@link Variable}) with the given name.
	 * @param whatYouWant The {@link Declaration} class
	 * @param name The name
	 * @return The global object with a matching name or null.
	 */
	@SuppressWarnings("unchecked")
	public <T extends Declaration> T findGlobal(Class<T> whatYouWant, String name) {
		List<Declaration> decs = declarationMap.get(name);
		if (decs == null)
			return null;
		for (Declaration d : decs)
			if (d.isGlobal() && whatYouWant.isInstance(d))
				return (T)d;
		return null;
	}

	/**
	 * Find a global declaration (static var or global func) with the specified name. If there are multiple global declarations with that name, the one nearest to 'pivot' will be returned.
	 * @param declName The declaration name
	 * @param pivot The pivot of the call, acting as a tie breaker if there are multiple global declarations with that name. The one nearest to the pivot will be returned. Can be null, in which case the 'first' match will be returned.
	 * @return A global {@link Declaration} with a matching name or null.
	 */
	public Declaration findGlobalDeclaration(String declName, IResource pivot) {
		List<Declaration> declarations = declarationMap.get(declName);
		if (declarations != null)
			return pivot != null
			? Utilities.pickNearest(declarations, pivot, IS_GLOBAL)
				: IS_GLOBAL.test(declarations.get(0))
				? declarations.get(0)
					: null;
				return null;
	}

	/**
	 * Clear the index so it won't manage any objects after this call.
	 */
	public synchronized void clear() {
		synchronized(indexedDefinitions) {
			indexedDefinitions.clear();
		}
		indexedScripts.clear();
		indexedScenarios.clear();
		indexedProplistDeclarations.clear();
		clearEntityFiles();
		entities.clear();
		entityIdCounter = 0;
		refreshIndex();
	}

	private void clearEntityFiles() {
		for (IndexEntity e : entities())
			entityFile(e).delete();
	}

	/**
	 * Return an {@link Iterable} to iterate over all {@link Definition}s managed by this index, but in the case of multiple {@link Definition}s with the same name yielding only the one nearest to pivot.
	 * @param pivot The pivot dictating the perspective of the call
	 * @return An {@link Iterable} to iterate over this presumably large subset of all the {@link Definition}s managed by the index.
	 */
	public Iterable<Definition> definitionsIgnoringRemoteDuplicates(final IResource pivot) {
		return new ConvertingIterable<List<Definition>, Definition>(new IConverter<List<Definition>, Definition>() {
			@Override
			public Definition convert(List<Definition> from) {
				return Utilities.pickNearest(from, pivot, null);
			}
		}, indexedDefinitions.values());
	}

	/**
	 * Return all scripts that append themselves to the specified {@link Definition}
	 * @param definition The definition to return 'appendages' of
	 * @return The appendages
	 */
	public List<Script> appendagesOf(Definition definition) {
		if (appendages == null)
			return null;
		List<Script> a = appendages.get(definition.id());
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
	public static <T extends Index> T loadShallow(Class<T> indexClass, File indexFolder, File fallbackFileLocation, final Engine engine) {
		if (!indexFolder.isDirectory())
			return null;
		try {
			InputStream in = new GZIPInputStream(new FileInputStream(new File(indexFolder, "index")));
			T index;
			try {
				ObjectInputStream objStream = new IndexEntityInputStream(new Index() {
					private static final long serialVersionUID = 1L;
					@Override
					public Engine engine() {
						return engine;
					}
				}, in);
				try {
					index = indexClass.cast(objStream.readObject());
					for (IndexEntity e : index.entities())
						e.index = index;
				} finally {
					objStream.close();
				}
			} finally {
				in.close();
			}
			index.folder = indexFolder;
			return index;
		} catch (Exception e) {
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
	public Script findScriptByPath(String path) {
		return null;
	}

	/**
	 * Return associated project. Returns null in base implementation. See {@link ProjectIndex#project()}.
	 * @return The project
	 */
	public IProject project() {return null;}

	@Override
	public boolean equals(Object obj) {
		return obj == this || (obj instanceof Index && ((Index)obj).project() == this.project());
	}

	/**
	 * Implementation whose merits can rightfully be argued. Will return the hashcode of the index's project's name, if there is some associated project, or call the super implementation instead.
	 */
	@Override
	public int hashCode() {
		if (project() != null)
			return project().getName().hashCode(); // project name should be unique
		else
			return super.hashCode();
	}

	/**
	 * Sink all {@link #relevantIndexes()} into some {@link Sink}
	 * @param The {@link Sink}
	 */
	public void forAllRelevantIndexes(Sink<Index> sink) {
		for (Index index : relevantIndexes())
			sink.elutriate(index);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Declaration> T latestVersionOf(T from) {
		try {
			if (from instanceof Script)
				return (T) Utilities.scriptForResource(from.resource());
			else if (from instanceof Structure && from.resource() instanceof IFile)
				return (T) Structure.pinned(from.resource(), false, false);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void loadEntity(IndexEntity entity) throws FileNotFoundException, IOException, ClassNotFoundException {
		//System.out.println("Load entity " + entity.toString());
		try {
			ObjectInputStream inputStream = newEntityInputStream(entity);
			if (inputStream != null) try {
				entity.load(inputStream);
			} finally {
				inputStream.close();
			}
			entity.postLoad(this, this);
			if (entity instanceof Script)
				addGlobalsFromScript((Script)entity, appendages);
		} catch (Exception e) {
			e.printStackTrace();
		}
	};

	public void saveEntity(IndexEntity entity) throws IOException {
		ObjectOutputStream s = newEntityOutputStream(entity);
		try {
			entity.save(s);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			s.close();
		}
	}

	private long entityIdCounter = 0;

	/**
	 * Add a new entity to the index, returning it's designated id.
	 * @param entity
	 * @return The id of the entity. Is supposed to be unique.
	 */
	protected long addEntityReturningId(IndexEntity entity) {
		synchronized (this) {
			long id = entityIdCounter++;
			this.entities.put(id, entity);
			if (newEntities != null)
				newEntities.add(entity);
			return id;
		}
	}

	private File entityFile(IndexEntity entity) {
		return new File(folder, Long.toString(entity.entityId()));
	}

	public ObjectOutputStream newEntityOutputStream(IndexEntity entity) throws FileNotFoundException, IOException {
		return new IndexEntityOutputStream(this, entity, new GZIPOutputStream(new FileOutputStream(entityFile(entity))));
	}

	public ObjectInputStream newEntityInputStream(IndexEntity entity) throws FileNotFoundException, IOException {
		try {
			return new IndexEntityInputStream(this, new GZIPInputStream(new FileInputStream(entityFile(entity))));
		} catch (FileNotFoundException e) {
			// might not be necessary to have an entity file
			return null;
		}
	}

	private static class EntityId implements Serializable, ISerializationResolvable {
		private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
		protected long referencedEntityId;
		protected Object referencedEntityToken;
		public EntityId(IndexEntity referencedEntity) {
			this.referencedEntityId = referencedEntity.entityId();
			this.referencedEntityToken = referencedEntity.additionalEntityIdentificationToken();
		}
		@Override
		public String toString() {
			return String.format("(%d, %s)", referencedEntityId, referencedEntityToken != null ? referencedEntityToken.toString() : "<No Token>");
		}
		protected Index index(Index context) {
			return context; // ;>
		}
		@Override
		public IndexEntity resolve(Index index) {
			IndexEntity result = null;
			Index externalIndex = index(index);
			if (externalIndex != null) {
				result = externalIndex.entityWithId(referencedEntityId);
				if (result == null || !Utilities.objectsEqual(result.additionalEntityIdentificationToken(), referencedEntityToken))
					if (referencedEntityToken != null)
						for (IndexEntity e : externalIndex.entities()) {
							Object token = e.additionalEntityIdentificationToken();
							if (e != null && referencedEntityToken.equals(token)) {
								result = e;
								break;
							}
						}
				if (result == null)
					System.out.println(String.format("Couldn't find entity '%s' in '%s'", this.toString(), externalIndex.project().getName()));
			}
			else
				System.out.println(String.format("Warning: Failed to obtain index when resolving '%s'", this.toString()));
			return result;
		}
	}

	private static class EntityReference extends EntityId {
		private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
		protected String referencedProjectName;
		public EntityReference(IndexEntity referencedEntity) {
			super(referencedEntity);
			if (referencedEntity != null && referencedEntity.index() != null)
				referencedProjectName = referencedEntity.index().name();
		}
		@Override
		protected Index index(Index context) {
			if (referencedProjectName != null) {
				ClonkProjectNature nat = ClonkProjectNature.get(referencedProjectName);
				return nat != null ? nat.index() : null;
			} else
				return null;
		}
		@Override
		public String toString() {
			return String.format("(%s, %d, %s)", referencedProjectName, referencedEntityId, referencedEntityToken != null ? referencedEntityToken.toString() : "<No Token>");
		}
	}

	private static class EntityDeclaration implements Serializable, ISerializationResolvable {
		private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
		private final IndexEntity containingEntity;
		private final String declarationPath;
		private final Class<? extends Declaration> declarationClass;
		public EntityDeclaration(Declaration declaration, IndexEntity containingEntity) {
			this.containingEntity = containingEntity;
			this.declarationPath = declaration.pathRelativeToIndexEntity();
			this.declarationClass = declaration.getClass();
		}
		@Override
		public Declaration resolve(Index index) {
			if (containingEntity instanceof Structure)
				return containingEntity.findDeclarationByPath(declarationPath, declarationClass);
			else
				return null;
		}
	}

	private static class EngineRef implements Serializable, ISerializationResolvable {
		private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
		private final String engineName;
		public EngineRef(Engine engine) {
			this.engineName = engine.name();
		}
		@Override
		public Object resolve(Index index) {
			return Core.instance().loadEngine(engineName);
		}
	}

	public IndexEntity entityWithId(long entityId) {
		return entities.get(entityId);
	}

	/**
	 * Return an object that will be serialized instead of an entity. It will implement {@link ISerializationResolvable}.
	 * @param entity
	 * @return
	 */
	public ISerializationResolvable saveReplacementForEntity(IndexEntity entity) {
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
		File indexFile = new File(folder, "index");
		folder.mkdirs();
		try {
			indexFile.createNewFile();
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}
		try {
			OutputStream out = new GZIPOutputStream(new FileOutputStream(indexFile));
			removeNullsInScriptLists();
			try {
				IndexEntityOutputStream objStream = new IndexEntityOutputStream(this, null, out) {
					@Override
					protected Object replaceObject(Object obj) throws IOException {
						// disable replacing entities with EntityId objects which won't resolve properly because this here is the place where entities are actually saved.
						if (obj instanceof IndexEntity && ((IndexEntity)obj).index == Index.this)
							return obj;
						return super.replaceObject(obj);
					}
				};
				objStream.writeObject(index());
				objStream.close();
			} finally {
				out.close();
			}
			purgeUnusedIndexFiles(indexFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void removeNullsInScriptLists() {
		if (indexedScripts.removeAll(Collections.singleton(null)))
			System.out.println("There were nulls in indexedScripts");
		if (indexedScenarios.removeAll(Collections.singleton(null)))
			System.out.println("There were nulls in indexedScenarios");
	}

	protected void purgeUnusedIndexFiles(File indexFile) {
		synchronized (this) {
			File[] files = folder.listFiles();
			List<File> filesToBePurged = new ArrayList<File>(files.length);
			filesToBePurged.addAll(Arrays.asList(files));
			filesToBePurged.remove(indexFile);
			for (IndexEntity e : entities())
				filesToBePurged.remove(entityFile(e));
			for (File f : filesToBePurged)
				if (!f.getName().startsWith("."))
					f.delete();
		}
	}


	private List<IndexEntity> newEntities;

	public void endModification() {
		synchronized (this) {
			for (IndexEntity e : newEntities)
				if (e.loaded && e.saveCalledByIndex())
					try {
						e.save();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
			newEntities = null;
		}
	}

	public void beginModification() {
		synchronized (this) {
			newEntities = new LinkedList<IndexEntity>();
			relevantIndexes = null; // force rebuild of list
		}
	}

	public Object saveReplacementForEntityDeclaration(Declaration obj, IndexEntity entity) {
		Index objIndex = obj.index();
		IndexEntity owningEntity = obj.firstParentDeclarationOfType(IndexEntity.class);
		if (objIndex == null || (objIndex == this && entity == owningEntity))
			return obj;
		else
			return new EntityDeclaration(obj, owningEntity);
	}

	public void loadScriptsContainingDeclarationsNamed(final String name) {
		allScripts(new Sink<Script>() {
			@Override
			public void receivedObject(Script item) {
				item.requireLoaded();
			}
			@Override
			public boolean filter(Script s) {
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
	public List<Declaration> snapshotOfDeclarationsNamed(String name) {
		List<Declaration> decs = declarationMap().get(name);
		if (decs == null)
			return null;
		ArrayList<Declaration> result = new ArrayList<Declaration>(decs.size());
		result.addAll(decs);
		return result;
	}

}
