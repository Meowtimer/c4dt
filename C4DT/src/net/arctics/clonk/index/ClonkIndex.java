package net.arctics.clonk.index;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.ref.WeakReference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.ID;
import net.arctics.clonk.parser.c4script.AdhocVariable;
import net.arctics.clonk.parser.c4script.Directive;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.ScriptBase;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.Directive.DirectiveType;
import net.arctics.clonk.parser.c4script.Function.C4FunctionScope;
import net.arctics.clonk.parser.c4script.Variable.Scope;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.resource.ClonkIndexInputStream;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.CompoundIterable;
import net.arctics.clonk.util.FilteredIterable;
import net.arctics.clonk.util.IHasRelatedResource;
import net.arctics.clonk.util.IPredicate;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

public class ClonkIndex extends Declaration implements Serializable, Iterable<Definition> {
	
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
	
	protected transient List<Function> globalFunctions = new LinkedList<Function>();
	protected transient List<Variable> staticVariables = new LinkedList<Variable>();
	protected transient Map<String, List<Declaration>> declarationMap = new HashMap<String, List<Declaration>>();
	protected transient Map<ID, List<ScriptBase>> appendages = new HashMap<ID, List<ScriptBase>>();
	
	public int numUniqueIds() {
		return indexedObjects.size();
	}
	
	public List<Definition> getObjects(ID id) {
		if (indexedObjects == null)
			return null;
		List<Definition> l = indexedObjects.get(id);
		return l == null ? null : Collections.unmodifiableList(l);
	}
	
	public void postSerialize() throws CoreException {
		restoreWeakAdhocVarRefs();
		refreshIndex();
	}

	private void restoreWeakAdhocVarRefs() {
		if (_serializedAdhocVariables != null) {
			adhocVariables = new HashMap<String, WeakReference<AdhocVariable>>();
			for (AdhocVariable v : _serializedAdhocVariables.values()) {
				adhocVariables.put(v.getName(), new WeakReference<AdhocVariable>(v));
			}
			_serializedAdhocVariables.clear();
			_serializedAdhocVariables = null;
		} else
			adhocVariables = null;
	}
	
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
		putAdhocVarsIntoSerializableMap();
	}

	private void putAdhocVarsIntoSerializableMap() {
		_serializedAdhocVariables = null;
		for (AdhocVariable var : adhocVariables()) {
			if (_serializedAdhocVariables == null)
				_serializedAdhocVariables = new HashMap<String, AdhocVariable>();
			_serializedAdhocVariables.put(var.getName(), var);
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
			if (folder.getSessionProperty(ClonkCore.C4OBJECT_PROPERTY_ID) != null)
				return (ProjectDefinition) folder.getSessionProperty(ClonkCore.C4OBJECT_PROPERTY_ID);
			
			// create session cache
			if (folder.getPersistentProperty(ClonkCore.FOLDER_C4ID_PROPERTY_ID) == null) return null;
			List<Definition> objects = getObjects(ID.getID(folder.getPersistentProperty(ClonkCore.FOLDER_C4ID_PROPERTY_ID)));
			if (objects != null) {
				for(Definition obj : objects) {
					if ((obj instanceof ProjectDefinition)) {
						if (((ProjectDefinition)obj).relativePath.equalsIgnoreCase(folder.getProjectRelativePath().toPortableString())) {
							folder.setSessionProperty(ClonkCore.C4OBJECT_PROPERTY_ID, obj);
							return (ProjectDefinition) obj;
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
						if (intern.getObjectFolder() != null && intern.getObjectFolder().equals(folder))
							return intern;
					}
				}
			}
			// also try scenarios
			for (Scenario s : indexedScenarios) {
				if (s.getObjectFolder() != null && s.getObjectFolder().equals(folder))
					return s;
			}
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
			addToDeclarationMap(func);
		}
		for (Variable var : script.variables()) {
			if (var.getScope() == Scope.STATIC || var.getScope() == Scope.CONST) {
				staticVariables.add(var);
			}
			addToDeclarationMap(var);
		}
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
		}
	}
	
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
		for (Iterable<? extends ScriptBase> c : scriptCollections) {
			addGlobalsFrom(c);
		}
		// do some post serialization after globals are known
		for (Iterable<? extends ScriptBase> c : scriptCollections) {
			for (ScriptBase s : c) {
				s.postSerialize(this, this);
			}
		}
		for (AdhocVariable var : adhocVariables()) {
			var.postSerialize(this, this);
		}
		
//		System.out.println("Functions added to cache:");
//		for (C4Function func : globalFunctions)
//			System.out.println("\t"+func.getName());
//		System.out.println("Variables added to cache");
//		for (C4Variable var : staticVariables)
//			System.out.println("\t"+var.getName());
		
	}
	
	/**
	 * Adds an C4Object to the index.<br>
	 * Take care of the global function and static variable cache. You have to call <tt>refreshCache()</tt> after modifying the index.
	 * @param obj
	 */
	public void addObject(Definition obj) {
		if (obj.getId() == null)
			return;
		List<Definition> alreadyDefinedObjects = indexedObjects.get(obj.getId());
		if (alreadyDefinedObjects == null) {
			alreadyDefinedObjects = new LinkedList<Definition>();
			indexedObjects.put(obj.getId(), alreadyDefinedObjects);
		} else {
			if (alreadyDefinedObjects.contains(obj))
				return;
		}
		alreadyDefinedObjects.add(obj);
	}
	
	/**
	 * Remove the script from the index
	 * @param script script which may be a standalone-script, a scenario or an object
	 */
	public void removeScript(ScriptBase script) {
		if (script instanceof Definition) {
			removeObject((Definition)script);
		} else {
			if (indexedScripts.remove(script))
				scriptRemoved(script);
		}
	}

	/**
	 * Removes this object from the index.<br>
	 * The object may still exist in IContainer.sessionProperty<br>
	 * Take care of the global function and static variable cache. You have to call <tt>refreshCache()</tt> after modifying the index.
	 * @param obj
	 */
	public void removeObject(Definition obj) {
		if (obj instanceof Scenario) {
			removeScenario((Scenario)obj);
			return;
		}
		if (obj.getId() == null)
			return;
		List<Definition> alreadyDefinedObjects = indexedObjects.get(obj.getId());
		if (alreadyDefinedObjects != null) {
			if (alreadyDefinedObjects.remove(obj)) {
				if (alreadyDefinedObjects.size() == 0) { // if there are no more objects with this C4ID
					indexedObjects.remove(obj.getId());
				}
				scriptRemoved(obj);
			}
		}
	}

	public void removeScenario(Scenario scenario) {
		if (indexedScenarios.remove(scenario)) {
			scriptRemoved(scenario);
		}
	}
	
	private void scriptRemoved(ScriptBase script) {
		for (ScriptBase s : allScripts())
			s.scriptRemovedFromIndex(script);
	}
	
	public void addScript(ScriptBase script) {
		if (script instanceof Scenario) {
			if (!indexedScenarios.contains(script))
				indexedScenarios.add((Scenario) script);
		}
		else if (script instanceof Definition) {
			addObject((Definition)script);
		}
		else {
			if (!indexedScripts.contains(script))
				indexedScripts.add(script);
		}
	}
	
	/**
	 * Returns true if there are no objects in this index.
	 * @return see above
	 */
	public boolean isEmpty() {
		return indexedObjects.isEmpty() && indexedScripts.isEmpty() && indexedScenarios.isEmpty();
	}
	
	public List<Scenario> getIndexedScenarios() {
		return Collections.unmodifiableList(indexedScenarios);
	}
	
	public List<ScriptBase> getIndexedScripts() {
		return Collections.unmodifiableList(indexedScripts);
	}
	
	public List<Function> getGlobalFunctions() {
		return Collections.unmodifiableList(globalFunctions);
	}
	
	public List<Variable> getStaticVariables() {
		return Collections.unmodifiableList(staticVariables);
	}
	
	public Map<String, List<Declaration>> getDeclarationMap() {
		return Collections.unmodifiableMap(declarationMap);
	}

	public Definition getLastObjectWithId(ID id) {
		List<Definition> objs = getObjects(id);
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

	public static <T extends IHasRelatedResource> T pickNearest(Collection<T> fromList, IResource resource) {
		return Utilities.pickNearest(fromList, resource, null);
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

	public Definition getObjectNearestTo(IResource resource, ID id) {
		Definition best = null;
		for (ClonkIndex index : relevantIndexes()) {
			if (resource != null) {
				List<Definition> objs = index.getObjects(id);
				best = pickNearest(objs, resource);
			}
			else {
				best = index.getLastObjectWithId(id);
			}
			if (best != null)
				break;
		}
		return best;
	}
	
	/**
	 * Like getLastObjectWithId, but falls back to ClonkCore.getDefault().getExternIndex() if there is no object in this index
	 * @param id
	 * @return
	 */
	public Definition getObjectFromEverywhere(ID id) {
		return getObjectNearestTo(null, id);
	}

	public <T extends Declaration> Iterable<T> declarationsWithName(String name, final Class<T> fieldClass) {
		List<Declaration> list = this.declarationMap.get(name);
		if (list == null)
			list = new LinkedList<Declaration>();
		return ArrayUtil.filteredIterable(list, fieldClass);
	}
	
	public Function findGlobalFunction(String functionName) {
		for (Function func : globalFunctions) {
			if (func.getName().equals(functionName))
				return func;
		}
		return null;
	}
	
	public Variable findGlobalVariable(String variableName) {
		if (staticVariables == null)
			return null;
		for (Variable var : staticVariables) {
			if (var.getName().equals(variableName))
				return var;
		}
		return null;
	}
	
	public Declaration findGlobalDeclaration(String fieldName) {
		Function f = findGlobalFunction(fieldName);
		if (f != null)
			return f;
		return findGlobalVariable(fieldName);
	}
	
	public Declaration findGlobalDeclaration(String declName, IResource pivot) {
		if (pivot == null)
			return findGlobalDeclaration(declName);
		List<Declaration> declarations = declarationMap.get(declName);
		if (declarations != null) {
			return Utilities.pickNearest(declarations, pivot, IS_GLOBAL);
		}
		return null;
	}

	public void clear() {
		indexedObjects.clear();
		indexedScripts.clear();
		indexedScenarios.clear();
		if (adhocVariables != null)
			adhocVariables.clear();
		if (_serializedAdhocVariables != null) {
			_serializedAdhocVariables.clear();
			_serializedAdhocVariables = null;
		}
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

	public Iterator<Definition> iterator() {
		return new ObjectIterator();
	}
	
	public Iterable<Definition> objectsIgnoringRemoteDuplicates(final IResource pivot) {
		return new Iterable<Definition>() {
			public Iterator<Definition> iterator() {
				final Iterator<List<Definition>> listIterator = indexedObjects.values().iterator();
				return new Iterator<Definition>() {
					
					public boolean hasNext() {
						return listIterator.hasNext();
					}

					public Definition next() {
						List<Definition> nextList = listIterator.next();
						return pickNearest(nextList, pivot);
					}

					public void remove() {
						// ...
					}
					
				};
			}
		};
	}
	
	public List<ScriptBase> appendagesOf(Definition object) {
		if (appendages == null)
			return null;
		List<ScriptBase> list = appendages.get(object.getId());
		if (list != null) {
			return Collections.unmodifiableList(list); 
		}
		return null;
	}
	
	public Iterable<ScriptBase> dependentScripts(final ScriptBase base) {
		return new Iterable<ScriptBase>() {
			@Override
            public Iterator<ScriptBase> iterator() {
	            return new Iterator<ScriptBase>() {
	            	private Definition baseObject = base instanceof Definition ? (Definition)base : null;
	            	private boolean hasGlobals = base.containsGlobals();
	            	private int stage = 0;
	            	private Iterator<? extends ScriptBase> currentIterator = getIndexedScripts().iterator();
	            	private ScriptBase currentScript;
	            	private HashSet<ScriptBase> alreadyReturned = new HashSet<ScriptBase>();

	            	private Iterator<? extends ScriptBase> getIterator() {
	            		while (!currentIterator.hasNext()) {
            				switch (++stage) {
            				case 1:
            					currentIterator = getIndexedScenarios().iterator();
            					break;
            				case 2:
            					currentIterator = ClonkIndex.this.iterator();
            					break; 
            				default:
            					return null;
            				}
            			}
	            		return currentIterator;
	            	}
	            	
	            	@Override
	            	public boolean hasNext() {
	            		ScriptBase s = null;
	            		Outer: for (Iterator<? extends ScriptBase> it = getIterator(); it != null; it = getIterator()) {
	            			do {
	            				s = it.next();
	            				if (s == base || alreadyReturned.contains(s))
	            					continue;
	            				if (hasGlobals) {
	            					if (s.usedProjectScript(base))
	            						break Outer;
	            				}
	            				if (baseObject != null) {
	            					if (s.includes(baseObject))
	            						break Outer;
	            				}
	            				List<ScriptBase> appendages;
	            				if (s instanceof Definition && (appendages = appendagesOf((Definition)s)) != null && appendages.contains(base)) {
	            					break Outer;
	            				}
	            			} while (it.hasNext());
	            			s = null;
	            		}
	            		if (s != null) {
	            			alreadyReturned.add(s);
	            			currentScript = s;
	            			return true;
	            		}
	            		else
	            			return false;
	            	}
					@Override
                    public ScriptBase next() {
						return currentScript;
                    }
					@Override
                    public void remove() {}	            	
	            };
            }
		};
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
	 * Finds a script by its path. This may be a path to an actual file or in the case of ExternIndex objects a path of an external object
	 * @param path the path
	 * @return the script or null if not found
	 */
	public ScriptBase findScriptByPath(String path) {
		return null;
	}
	
	public IProject getProject() {return null;}
	
	/**
	 * Variables being declared by assignment.
	 */
	private transient HashMap<String, WeakReference<AdhocVariable>> adhocVariables;
	
	/**
	 * Hack: Before serialization, put adhoc variables not churned by GC into this map.<br/>
	 * After serialization, put back into map with weak references.
	 */
	private HashMap<String, AdhocVariable> _serializedAdhocVariables;
	
	/**
	 * Return an Iterable that can be used to iterate over adhoc variables.
	 * @return The Iterable
	 */
	public Iterable<AdhocVariable> adhocVariables() {
		if (adhocVariables == null)
			return ArrayUtil.arrayIterable();
		else {
			return new FilteredIterable<AdhocVariable, Map.Entry<String, WeakReference<AdhocVariable>>>(AdhocVariable.class, adhocVariables.entrySet(), true) {
				@Override
				protected boolean stillValid(Map.Entry<String, WeakReference<AdhocVariable>> item) {
					return item.getValue().get() != null; 
				}
				@Override
				protected AdhocVariable map(Map.Entry<String, WeakReference<AdhocVariable>> original) {
					return original.getValue().get();
				}
			};
		}
	}
	
	/**
	 * Add a new adhoc-variable
	 * @param name The name of the variable
	 * @param file The file the assignment took place in
	 * @param declaration The declaration the assignment took place in
	 * @param assignedExpression The expression assigned to the adhoc-variable 
	 * @return Either a newly-created AdhocVariable or an existing one with the passed name.
	 */
	public AdhocVariable addAdhocVariable(String name, IFile file, Declaration declaration, ExprElm assignedExpression) {
		if (adhocVariables == null)
			adhocVariables = new HashMap<String, WeakReference<AdhocVariable>>();
		WeakReference<AdhocVariable> ref = adhocVariables.get(name);
		AdhocVariable var = ref != null ? ref.get() : null;
		if (var == null) {
			var = new AdhocVariable(this, name, Scope.VAR);
			adhocVariables.put(name, new WeakReference<AdhocVariable>(var));
		}
		var.addAssignmentLocation(file, declaration, assignedExpression);
		return var;
	}

}
