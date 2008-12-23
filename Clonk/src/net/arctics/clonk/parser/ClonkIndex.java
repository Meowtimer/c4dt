package net.arctics.clonk.parser;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.C4Function.C4FunctionScope;
import net.arctics.clonk.parser.C4Variable.C4VariableScope;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.CoreException;

public class ClonkIndex implements Serializable, Iterable<C4Object> {
	
	private static final long serialVersionUID = 1L;

	private Map<C4ID,List<C4Object>> indexedObjects;
	private List<C4ScriptBase> indexedScripts;
	private List<C4Scenario> indexedScenarios;
	
	private transient List<C4Function> globalFunctions;
	private transient List<C4Variable> staticVariables;
	
	public List<C4Object> getObjects(C4ID id) {
		return getIndexedObjects().get(id);
	}
	
	public void fixReferencesAfterSerialization() throws CoreException {
		refreshCache();
	}
	
	/**
	 * You should use IContainer.getSessionProperty(ClonkCore.C4OBJECT_PROPERTY_ID) instead of this
	 * @param folder
	 * @return
	 */
	public C4ObjectIntern getObject(IContainer folder) {
		try {
			// fetch from session cache
			if (folder.getSessionProperty(ClonkCore.C4OBJECT_PROPERTY_ID) != null)
				return (C4ObjectIntern) folder.getSessionProperty(ClonkCore.C4OBJECT_PROPERTY_ID);
			
			// create session cache
			if (folder.getPersistentProperty(ClonkCore.FOLDER_C4ID_PROPERTY_ID) == null) return null;
			List<C4Object> objects = getObjects(C4ID.getID(folder.getPersistentProperty(ClonkCore.FOLDER_C4ID_PROPERTY_ID)));
			if (objects != null) {
				for(C4Object obj : objects) {
					if ((obj instanceof C4ObjectIntern)) {
						if (((C4ObjectIntern)obj).relativePath.equalsIgnoreCase(folder.getProjectRelativePath().toPortableString())) {
							folder.setSessionProperty(ClonkCore.C4OBJECT_PROPERTY_ID, obj);
							return (C4ObjectIntern) obj;
						}
					}
				}
			}
			return null;
		} catch (CoreException e) {
			// likely due to getSessionProperty being called on non-existent resources
			for (List<C4Object> list : getIndexedObjects().values()) {
				for (C4Object obj : list) {
					if (obj instanceof C4ObjectIntern) {
						C4ObjectIntern intern = (C4ObjectIntern)obj;
						if (intern.getObjectFolder().equals(folder))
							return intern;
					}
				}
			}
			//e.printStackTrace();
			return null;
		}
	}
	
	public List<C4Function> getGlobalFunctions() {
		if (globalFunctions == null) {
			globalFunctions = new LinkedList<C4Function>();
			
			refreshCache();
		}
		return globalFunctions;
	}
	
	public List<C4Variable> getStaticVariables() {
		if (staticVariables == null) {
			staticVariables = new LinkedList<C4Variable>();
			refreshCache();
		}
		return staticVariables;
	}

	private void addGlobalsFrom(C4ScriptBase script) {
		for(C4Function func : script.definedFunctions) {
			if (func.getVisibility() == C4FunctionScope.FUNC_GLOBAL) {
				globalFunctions.add(func);
			}
		}
		for(C4Variable var : script.definedVariables) {
			if (var.getScope() == C4VariableScope.VAR_STATIC || var.getScope() == C4VariableScope.VAR_CONST) {
				staticVariables.add(var);
			}
		}
	}
	
	public void refreshCache() {
		// delete old cache
		if (globalFunctions != null) globalFunctions.clear();
		else globalFunctions = new LinkedList<C4Function>();
		if (staticVariables != null) staticVariables.clear();
		else staticVariables = new LinkedList<C4Variable>();
		
		// save cachable items
		for(List<C4Object> objects : getIndexedObjects().values()) {
			for(C4Object obj : objects) {
				addGlobalsFrom(obj);
			}
		}
		for (C4ScriptBase script : getIndexedScripts()) {
			addGlobalsFrom(script);
		}
		for (C4Scenario scen : getIndexedScenarios()) {
			addGlobalsFrom(scen);
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
	public void addObject(C4Object obj) {
		if (obj.getId() == null)
			return;
		List<C4Object> alreadyDefinedObjects = getIndexedObjects().get(obj.getId());
		if (alreadyDefinedObjects == null) {
			alreadyDefinedObjects = new LinkedList<C4Object>();
			indexedObjects.put(obj.getId(), alreadyDefinedObjects);
		} else {
			if (alreadyDefinedObjects.contains(obj))
				return;
		}
		alreadyDefinedObjects.add(obj);
	}
	
	/**
	 * Removes this object from index.<br>
	 * The object may still exist in IContainer.sessionProperty<br>
	 * Take care of the global function and static variable cache. You have to call <tt>refreshCache()</tt> after modifying the index.
	 * @param obj
	 */
	public void removeObject(C4Object obj) {
		if (obj.getId() == null)
			return;
		List<C4Object> alreadyDefinedObjects = getIndexedObjects().get(obj.getId());
		if (alreadyDefinedObjects != null) {
			alreadyDefinedObjects.remove(obj);
			if (alreadyDefinedObjects.size() == 0) { // if there are no more objects with this C4ID
				indexedObjects.remove(obj.getId());
			}
		}
	}
	
	public void addScript(C4ScriptBase script) {
		if (script instanceof C4Scenario) {
			if (!getIndexedScenarios().contains(script))
				getIndexedScenarios().add((C4Scenario) script);
		}
		else if (script instanceof C4Object) {
			addObject((C4Object)script);
		}
		else {
			if (!getIndexedScripts().contains(script))
				getIndexedScripts().add(script);
		}
	}
	
	public void removeScript(C4ScriptBase script) {
		if (script instanceof C4Object)
			removeObject((C4Object)script);
		else
			getIndexedScripts().remove(script);
	}
	
	/**
	 * Returns true if there are no objects in this index.
	 * @return
	 */
	public boolean isEmpty() {
		if (getIndexedObjects() != null) {
			return indexedObjects.isEmpty();
		}
		return true;
	}
	
	public Map<C4ID, List<C4Object>> getIndexedObjects() {
		if (indexedObjects == null) {
			indexedObjects = new HashMap<C4ID, List<C4Object>>();
		}
		return indexedObjects;
	}
	
	public List<C4Scenario> getIndexedScenarios() {
		if (indexedScenarios == null) {
			indexedScenarios = new LinkedList<C4Scenario>();
		}
		return indexedScenarios;
	}
	
	public List<C4ScriptBase> getIndexedScripts() {
		if (indexedScripts == null) {
			indexedScripts = new LinkedList<C4ScriptBase>();
		}
		return indexedScripts;
	}

	public C4Object getLastObjectWithId(C4ID id) {
		List<C4Object> objs = getObjects(id);
		if (objs != null) {
			if (objs instanceof LinkedList) { // due to performance
				return ((LinkedList<C4Object>)objs).getLast();
			}
			else {
				return objs.get(objs.size()-1);
			}
		}
		return null;
	}
	
	/**
	 * Like getLastObjectWithId, but falls back to ClonkCore.EXTERN_INDEX if there is no object in this index
	 * @param id
	 * @return
	 */
	public C4Object getObjectFromEverywhere(C4ID id) {
		C4Object result = getLastObjectWithId(id);
		if (result == null && this != ClonkCore.EXTERN_INDEX)
			result = ClonkCore.EXTERN_INDEX.getLastObjectWithId(id);
		return result;
	}
	
	public C4Object getObjectWithIDPreferringInterns(C4ID id) {
		List<C4Object> objs = getObjects(id);
		if (objs != null) {
			C4Object best = null;
			for (C4Object obj : objs) {
				if (best == null || obj instanceof C4ObjectIntern)
					best = obj;
			}
			return best;
		}
		return null;
	}
	
	public C4Function findGlobalFunction(String functionName) {
		for (C4Function func : getGlobalFunctions()) {
			if (func.getName().equals(functionName))
				return func;
		}
		return null;
	}
	
	public C4Field findGlobalField(String fieldName) {
		C4Function f = findGlobalFunction(fieldName);
		if (f != null)
			return f;
		for (C4Variable var : getStaticVariables()) {
			if (var.getName().equals(fieldName))
				return var;
		}
		return null;
	}

	public void clear() {
		getIndexedObjects().clear();
		getIndexedScripts().clear();
		getIndexedScenarios().clear();
		refreshCache();
	}
	
	private class ObjectIterator implements Iterator<C4Object> {

		private Iterator<List<C4Object>> valuesIterator;
		private Iterator<C4Object> listIterator;
		
		public ObjectIterator() {
			valuesIterator = indexedObjects.values().iterator();
		}
		
		public boolean hasNext() {
			return (listIterator != null && listIterator.hasNext()) || valuesIterator.hasNext();
		}

		public C4Object next() {
			while (listIterator == null || !listIterator.hasNext()) {
				listIterator = null;
				if (!valuesIterator.hasNext())
					return null;
				listIterator = valuesIterator.next().iterator();
			}
			return listIterator.next();
		}

		public void remove() {
			// pff
		}
		
	}

	public Iterator<C4Object> iterator() {
		return new ObjectIterator();
	}

}
