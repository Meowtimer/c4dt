package net.arctics.clonk.index;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.ID;
import net.arctics.clonk.parser.IHasIncludes;
import net.arctics.clonk.parser.ILatestDeclarationVersionProvider;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.parser.c4script.Directive;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.ProplistDeclaration;
import net.arctics.clonk.parser.c4script.ScriptBase;
import net.arctics.clonk.parser.c4script.StandaloneProjectScript;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.Directive.DirectiveType;
import net.arctics.clonk.parser.c4script.Function.C4FunctionScope;
import net.arctics.clonk.parser.c4script.Variable.Scope;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.resource.ClonkIndexInputStream;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.CompoundIterable;
import net.arctics.clonk.util.ConvertingIterable;
import net.arctics.clonk.util.IConverter;
import net.arctics.clonk.util.IPredicate;
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
 * <li>{@link ScriptBase}s</li>
 * <li>{@link Scenario}s</li>
 * </ul></p> 
 * <p>Additionally, some lookup tables are stored to make access to some datasets quicker, like string -> <list of declarations with that name> maps.
 * The index itself can be directly used to iterate over all {@link Definition}s it manages, while iterating over other indexed {@link ScriptBase} objects requires calling {@link #allScripts()},
 * which yields an {@link Iterable} to iterate over both {@link StandaloneProjectScript}s and {@link Scenario}s.</p>
 * <p>For indexes specific to Eclipse projects (as pretty much all actual ClonkIndex instances are), see {@link ProjectIndex}.</p>
 * @author madeen
 *
 */
public class ClonkIndex extends Declaration implements Serializable, Iterable<Definition>, ILatestDeclarationVersionProvider {
	
	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	private transient static final IPredicate<Declaration> IS_GLOBAL = new IPredicate<Declaration>() {
		@Override
		public boolean test(Declaration item) {
			return item.isGlobal();
		}
	}; 

	private Map<ID, List<Definition>> indexedObjects = new HashMap<ID, List<Definition>>();
	private List<ScriptBase> indexedScripts = new LinkedList<ScriptBase>(); 
	private List<Scenario> indexedScenarios = new LinkedList<Scenario>();
	private List<ProplistDeclaration> indexedProplistDeclarations = new LinkedList<ProplistDeclaration>();
	
	protected transient List<Function> globalFunctions = new LinkedList<Function>();
	protected transient List<Variable> staticVariables = new LinkedList<Variable>();
	protected transient Map<String, List<Declaration>> declarationMap = new HashMap<String, List<Declaration>>();
	protected transient Map<ID, List<ScriptBase>> appendages = new HashMap<ID, List<ScriptBase>>();
	
	/**
	 * Return the number of unique ids
	 * @return
	 */
	public int numUniqueIds() {
		return indexedObjects.size();
	}
	
	/**
	 * Get a list of all {@link Definition}s with a certain id.
	 * @param id The id
	 * @return The list
	 */
	public List<Definition> getDefinitionsWithID(ID id) {
		if (indexedObjects == null)
			return null;
		List<Definition> l = indexedObjects.get(id);
		return l == null ? null : Collections.unmodifiableList(l);
	}
	
	public void postSerialize() throws CoreException {
		refreshIndex();
	}
	
	/**
	 * Return an {@link Iterable} to iterate over all {@link ScriptBase} objects managed by this index.
	 * @return The iterable.
	 */
	@SuppressWarnings("unchecked")
	public Iterable<ScriptBase> allScripts() {
		return new CompoundIterable<ScriptBase>(this, indexedScripts, indexedScenarios);
	}
	
	/**
	 * Called before serialization to give index a chance to prepare itself for serialization.
	 */
	public void preSerialize() {
		for (ScriptBase script : allScripts()) {
			script.preSerialize();
		}
	}
	
	/**
	 * Return the object linked to the passed folder.
	 * @param folder The folder to get the object for
	 * @return The object or null if the folder is not linked to any object
	 */
	public ProjectDefinition getObject(IContainer folder) {
		try {
			// fetch from session cache
			if (folder.getSessionProperty(ClonkCore.FOLDER_DEFINITION_REFERENCE_ID) != null)
				return (ProjectDefinition) folder.getSessionProperty(ClonkCore.FOLDER_DEFINITION_REFERENCE_ID);
			
			// create session cache
			if (folder.getPersistentProperty(ClonkCore.FOLDER_C4ID_PROPERTY_ID) == null) return null;
			List<Definition> objects = getDefinitionsWithID(ID.getID(folder.getPersistentProperty(ClonkCore.FOLDER_C4ID_PROPERTY_ID)));
			if (objects != null) {
				for (Definition obj : objects) {
					if ((obj instanceof ProjectDefinition)) {
						ProjectDefinition projDef = (ProjectDefinition)obj;
						if (projDef.relativePath.equalsIgnoreCase(folder.getProjectRelativePath().toPortableString())) {
							projDef.setObjectFolder(folder);
							return projDef;
						}
					}
				}
			}
			return null;
		} catch (CoreException e) {
			// likely due to getSessionProperty being called on non-existent resources
			for (List<Definition> list : indexedObjects.values()) {
				for (Definition obj : list) {
					if (obj instanceof ProjectDefinition) {
						ProjectDefinition intern = (ProjectDefinition)obj;
						if (intern.definitionFolder() != null && intern.definitionFolder().equals(folder))
							return intern;
					}
				}
			}
			// also try scenarios
			for (Scenario s : indexedScenarios)
				if (s.definitionFolder() != null && s.definitionFolder().equals(folder))
					return s;
			//e.printStackTrace();
			return null;
		}
	}
	
	public ScriptBase getScript(IFile file) {
		ScriptBase result = ScriptBase.get(file, true);
		if (result == null) {
			for (ScriptBase s : this.indexedScripts)
				if (s.getResource() != null && s.getResource().equals(file))
					return s;
		}
		return result;
	}

	protected void addToDeclarationMap(Declaration field) {
		List<Declaration> list = declarationMap.get(field.getName());
		if (list == null) {
			list = new LinkedList<Declaration>();
			declarationMap.put(field.getName(), list);
		}
		list.add(field);
	}
	
	private void addGlobalsFrom(ScriptBase script) {
		for (Function func : script.functions()) {
			if (func.getVisibility() == C4FunctionScope.GLOBAL) {
				globalFunctions.add(func);
			}
			for (Declaration otherDec : func.getOtherDeclarations())
				if (otherDec instanceof ProplistDeclaration) {
					addToProplistDeclarations((ProplistDeclaration) otherDec);
				}
			addToDeclarationMap(func);
		}
		for (Variable var : script.variables()) {
			if (var.getScope() == Scope.STATIC || var.getScope() == Scope.CONST) {
				staticVariables.add(var);
			}
			addToDeclarationMap(var);
		}
	}

	protected void addToProplistDeclarations(ProplistDeclaration proplistDeclaration) {
		indexedProplistDeclarations.add(proplistDeclaration);
		for (Variable v : proplistDeclaration.getComponents())
			addToDeclarationMap(v);
	}
	
	private void detectAppendages(ScriptBase script) {
		for (Directive d : script.directives())
			if (d.getType() == DirectiveType.APPENDTO) {
				List<ScriptBase> appendtoList = appendages.get(d.contentAsID());
				if (appendtoList == null) {
					appendtoList = new LinkedList<ScriptBase>();
					appendages.put(d.contentAsID(), appendtoList);
				}
				appendtoList.add(script);
			}
	}
	
	private <T extends ScriptBase> void addGlobalsFrom(Iterable<T> scripts) {
		for (T script : scripts) {
			addGlobalsFrom(script);
			detectAppendages(script);
			for (IHasIncludes s : script.getIncludes(this, false))
				if (s instanceof ScriptBase)
					((ScriptBase) s).addDependentScript(script);
			/*if (script.usedProjectScripts() != null)
				for (ScriptBase s : script.usedProjectScripts())
					s.addDependentScript(script);*/
		}
	}
	
	/**
	 * Repopulate the quick-access lists ({@link #globalFunctions()}, {@link #staticVariables()}, {@link #declarationMap()}, {@link #appendagesOf(Definition)}) maintained by the index based on {@link #indexedObjects}, {@link #indexedScenarios} and {@link #indexedScripts}.
	 */
	public synchronized void refreshIndex() {
		
		// delete old cache
		if (globalFunctions == null)
			globalFunctions = new LinkedList<Function>();
		if (staticVariables == null)
			staticVariables = new LinkedList<Variable>();
		if (declarationMap == null)
			declarationMap = new HashMap<String, List<Declaration>>();
		if (appendages == null)
			appendages = new HashMap<ID, List<ScriptBase>>();
		globalFunctions.clear();
		staticVariables.clear();
		declarationMap.clear();
		appendages.clear();
		
		// add globals to globals lists
		List<Iterable<? extends ScriptBase>> scriptCollections = new ArrayList<Iterable<? extends ScriptBase>>(3);
		scriptCollections.add(this);
		scriptCollections.add(indexedScripts);
		scriptCollections.add(indexedScenarios);
		
		for (ScriptBase s : allScripts()) {
			s.clearDependentScripts();
		}
		
		for (Iterable<? extends ScriptBase> c : scriptCollections) {
			addGlobalsFrom(c);
		}
		// do some post serialization after globals are known
		for (Iterable<? extends ScriptBase> c : scriptCollections) {
			for (ScriptBase s : c) {
				s.postSerialize(this, this);
			}
		}
		
//		System.out.println("Functions added to cache:");
//		for (C4Function func : globalFunctions)
//			System.out.println("\t"+func.getName());
//		System.out.println("Variables added to cache");
//		for (C4Variable var : staticVariables)
//			System.out.println("\t"+var.getName());
		
	}
	
	/**
	 * Add an {@link Definition} to the index.<br>
	 * {@link #refreshIndex()} will need to be called manually after this.
	 * @param definition The {@link Definition} to add. Attempts to add {@link Definition}s with no id will be ignored.
	 */
	public void addDefinition(Definition definition) {
		if (definition.id() == null)
			return;
		List<Definition> alreadyDefinedObjects = indexedObjects.get(definition.id());
		if (alreadyDefinedObjects == null) {
			alreadyDefinedObjects = new LinkedList<Definition>();
			indexedObjects.put(definition.id(), alreadyDefinedObjects);
		} else {
			if (alreadyDefinedObjects.contains(definition))
				return;
		}
		alreadyDefinedObjects.add(definition);
	}
	
	/**
	 * Remove the script from the index.
	 * @param script Some script. Can be a {@link Definition}, a {@link Scenario} or some other {@link ScriptBase} object managed by this index.
	 */
	public void removeScript(ScriptBase script) {
		if (script instanceof Definition) {
			removeDefinition((Definition)script);
		} else {
			if (indexedScripts.remove(script))
				scriptRemoved(script);
		}
	}

	/**
	 * Remove a {@link Definition} from this index.<br>
	 * {@link #refreshIndex()} will need to be called manually after this.
	 * No attempts are made to remove session properties from affected resources (see {@link ClonkCore#FOLDER_DEFINITION_REFERENCE_ID})
	 * @param definition The {@link Definition} to remove from the index
	 */
	public void removeDefinition(Definition definition) {
		if (definition instanceof Scenario) {
			removeScenario((Scenario)definition);
			return;
		}
		if (definition.id() == null)
			return;
		List<Definition> alreadyDefinedObjects = indexedObjects.get(definition.id());
		if (alreadyDefinedObjects != null) {
			if (alreadyDefinedObjects.remove(definition)) {
				if (alreadyDefinedObjects.size() == 0) { // if there are no more objects with this C4ID
					indexedObjects.remove(definition.id());
				}
				scriptRemoved(definition);
			}
		}
	}

	/**
	 * Remove a {@link Scenario} from the index.
	 * @param scenario The {@link Scenario} to remove
	 */
	public void removeScenario(Scenario scenario) {
		if (indexedScenarios.remove(scenario)) {
			scriptRemoved(scenario);
		}
	}
	
	private void scriptRemoved(ScriptBase script) {
		for (ScriptBase s : allScripts())
			s.scriptRemovedFromIndex(script);
	}
	
	/**
	 * Add some {@link ScriptBase} to the index. If the script is a {@link Definition}, {@link #addDefinition(Definition)} will be called internally.
	 * @param script The script to add to the index
	 */
	public void addScript(ScriptBase script) {
		if (script instanceof Scenario) {
			if (!indexedScenarios.contains(script))
				indexedScenarios.add((Scenario) script);
		}
		else if (script instanceof Definition) {
			addDefinition((Definition)script);
		}
		else {
			if (!indexedScripts.contains(script))
				indexedScripts.add(script);
		}
	}
	
	/**
	 * Returns true if the index does not manage any scripts or definitions.
	 * @return see above
	 */
	public boolean isEmpty() {
		return indexedObjects.isEmpty() && indexedScripts.isEmpty() && indexedScenarios.isEmpty();
	}
	
	public List<Scenario> indexedScenarios() {
		return Collections.unmodifiableList(indexedScenarios);
	}
	
	public List<ScriptBase> indexedScripts() {
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
		List<Definition> objs = getDefinitionsWithID(id);
		if (objs != null) {
			if (objs instanceof LinkedList<?>) { // due to performance
				return ((LinkedList<Definition>)objs).getLast();
			}
			else {
				return objs.get(objs.size()-1);
			}
		}
		return null;
	}

	public static void addIndexesFromReferencedProjects(List<ClonkIndex> result, ClonkIndex index) {
		if (index instanceof ProjectIndex) {
			ProjectIndex projIndex = (ProjectIndex) index;
			try {
				List<ClonkIndex> newOnes = new LinkedList<ClonkIndex>();
				for (IProject p : projIndex.getProject().getReferencedProjects()) {
					ClonkProjectNature n = ClonkProjectNature.get(p);
					if (n != null && n.getIndex() != null && !result.contains(n.getIndex()))
						newOnes.add(n.getIndex());
				}
				result.addAll(newOnes);
				for (ClonkIndex i : newOnes) {
					addIndexesFromReferencedProjects(result, i);
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
	}
	
	public List<ClonkIndex> relevantIndexes() {
		List<ClonkIndex> result = new ArrayList<ClonkIndex>(10);
		result.add(this);
		addIndexesFromReferencedProjects(result, this);
		return result;
	}

	/**
	 * Of those definitions with a matching id, choose the one nearest to the specified resource.
	 * @param resource The resource
	 * @param id The id
	 * @return The chosen definition.
	 */
	public Definition getDefinitionNearestTo(IResource resource, ID id) {
		Definition best = null;
		for (ClonkIndex index : relevantIndexes()) {
			if (resource != null) {
				List<Definition> objs = index.getDefinitionsWithID(id);
				best = Utilities.pickNearest(objs, resource, null);
			}
			else
				best = index.lastDefinitionWithId(id);
			if (best != null)
				break;
		}
		return best;
	}
	
	/**
	 * Like getLastObjectWithId, but falls back to ClonkCore.getDefault().getExternIndex() if there is no object in this index
	 * @param id
	 * @return The definition with a matching id. Undefined which one if there are more than one
	 */
	public Definition getDefinitionFromEverywhere(ID id) {
		return getDefinitionNearestTo(null, id);
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
	 * @param whatYouWant The class of global object
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
		if (declarations != null) {
			return pivot != null
				? Utilities.pickNearest(declarations, pivot, IS_GLOBAL)
				: declarations.get(0);
		}
		return null;
	}

	/**
	 * Clear the index so it won't manage any objects after this call.
	 */
	public void clear() {
		indexedObjects.clear();
		indexedScripts.clear();
		indexedScenarios.clear();
		indexedProplistDeclarations.clear();
		refreshIndex();
	}
	
	private class ObjectIterator implements Iterator<Definition> {

		private Iterator<List<Definition>> valuesIterator;
		private Iterator<Definition> listIterator;
		
		public ObjectIterator() {
			synchronized (ClonkIndex.this) {
				valuesIterator = indexedObjects.values().iterator();
			}
		}
		
		public boolean hasNext() {
			synchronized (ClonkIndex.this) {
				return (listIterator != null && listIterator.hasNext()) || valuesIterator.hasNext();
			}
		}

		public Definition next() {
			synchronized (ClonkIndex.this) {
				while (listIterator == null || !listIterator.hasNext()) {
					listIterator = null;
					if (!valuesIterator.hasNext())
						return null;
					listIterator = valuesIterator.next().iterator();
				}
				return listIterator.next();
			}
		}

		public void remove() {
			// pff
		}
		
	}

	@Override
	public Iterator<Definition> iterator() {
		return new ObjectIterator();
	}
	
	/**
	 * Return an {@link Iterable} to iterate over all {@link Definition}s managed by this index, but in the case of multiple {@link Definition}s with the same name yielding only the one nearest to pivot.
	 * @param pivot The pivot dictating the perspective of the call
	 * @return An {@link Iterable} to iterate over this presumably large subset of all the {@link Definition}s managed by the index.
	 */
	public Iterable<Definition> objectsIgnoringRemoteDuplicates(final IResource pivot) {
		return new ConvertingIterable<List<Definition>, Definition>(new IConverter<List<Definition>, Definition>() {
			@Override
			public Definition convert(List<Definition> from) {
				return Utilities.pickNearest(from, pivot, null);
			}
		}, indexedObjects.values());
	}
	
	/**
	 * Return all scripts that append themselves to the specified {@link Definition}
	 * @param definition The definition to return 'appendages' of
	 * @return The appendages
	 */
	public List<ScriptBase> appendagesOf(Definition definition) {
		if (appendages == null)
			return null;
		List<ScriptBase> list = appendages.get(definition.id());
		if (list != null) {
			return Collections.unmodifiableList(list); 
		}
		return null;
	}
	
	@Override
	public Engine getEngine() {
		return ClonkCore.getDefault().getActiveEngine();
	}
	
	@Override
	public ClonkIndex getIndex() {
		return this;
	}
	
	/**
	 * Mark the index as dirty. Not implemented in ClonkIndex.
	 * @param dirty Whether it's dirty or not.
	 */
	public void setDirty(boolean dirty) {}
	
	/**
	 * Return dirty status.
	 * @return Dirty status.
	 */
	public boolean isDirty() {return false;}
	
	/**
	 * Load an index from a file.
	 * @param <T> ClonkIndex type requested.
	 * @param indexClass The class to instantiate
	 * @param indexFile File to load the index from
	 * @param fallbackFileLocation Secondary file location to use if the first one does not exist
	 * @return The loaded index or null if loading the index failed for any reason.
	 */
	public static <T extends ClonkIndex> T load(Class<T> indexClass, File indexFile, File fallbackFileLocation) {
		boolean oldLocation = false;
		if (!indexFile.exists()) {
			// fall back to old indexdata file
			indexFile = fallbackFileLocation;
			if (indexFile == null || !indexFile.exists()) {
				return null;
			}
			else
				oldLocation = true;
		}
		try {
			InputStream in = new FileInputStream(indexFile);
			T index;
			try {
				ObjectInputStream objStream = new ClonkIndexInputStream(in);
				try {
					index = indexClass.cast(objStream.readObject());
				} finally {
					objStream.close();
				}
			} finally {
				in.close();
			}
			if (oldLocation) {
				// old location: mark as dirty so it will be saved in the new location when shutting down
				// also remove old file
				indexFile.delete();
				index.setDirty(true);
			}
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
	public ScriptBase findScriptByPath(String path) {
		return null;
	}
	
	/**
	 * Return associated project. Returns null in base implementation. See {@link ProjectIndex#getProject()}.
	 * @return The project
	 */
	public IProject getProject() {return null;}
	
	@Override
	public boolean equals(Object obj) {
		return obj == this || (obj instanceof ClonkIndex && ((ClonkIndex)obj).getProject() == this.getProject());
	}
	
	/**
	 * Implementation whose merits can rightfully be argued. Will return the hashcode of the index's project's name, if there is some associated project, or call the super implementation instead.
	 */
	@Override
	public int hashCode() {
		if (getProject() != null)
			return getProject().getName().hashCode(); // project name should be unique
		else
			return super.hashCode();
	}
	
	/**
	 * It's runnable and it takes an index as the only argument!
	 * @author madeen
	 *
	 */
	public interface r {void run(ClonkIndex index);}
	
	/**
	 * Call some runnable ({@link r}) for all indexes yielded by {@link #relevantIndexes()}
	 * @param r The runnable
	 */
	public void forAllRelevantIndexes(ClonkIndex.r r) {
		for (ClonkIndex index : relevantIndexes())
			r.run(index);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Declaration> T getLatestVersion(T from) {
		try {
			if (from instanceof ScriptBase)
				return (T) Utilities.getScriptForResource(from.getResource());
			else if (from instanceof Structure && from.getResource() instanceof IFile)
				return (T) Structure.pinned(from.getResource(), false, false);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return null;
	};

}
