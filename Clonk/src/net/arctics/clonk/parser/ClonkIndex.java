package net.arctics.clonk.parser;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.C4Function.C4FunctionScope;
import net.arctics.clonk.parser.C4Variable.C4VariableScope;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.CoreException;

public class ClonkIndex implements Serializable {
	
	private static final long serialVersionUID = 1L;

	private Map<C4ID,List<C4Object>> indexedObjects;
	
	private transient List<C4Function> globalFunctions;
	private transient List<C4Variable> staticVariables;
	
	public List<C4Object> getObjects(C4ID id) {
		return getIndexedObjects().get(id);
	}
	
	public void fixReferencesAfterSerialization() throws CoreException {
	}
	
	/**
	 * You should use IContainer.getSessionProperty(ClonkCore.C4OBJECT_PROPERTY_ID) instead of this
	 * @param folder
	 * @return
	 */
	public C4Object getObject(IContainer folder) {
		try {
			// fetch from session cache
			if (folder.getSessionProperty(ClonkCore.C4OBJECT_PROPERTY_ID) != null)
				return (C4Object) folder.getSessionProperty(ClonkCore.C4OBJECT_PROPERTY_ID);
			
			// create session cache
			if (folder.getPersistentProperty(ClonkCore.FOLDER_C4ID_PROPERTY_ID) == null) return null;
			List<C4Object> objects = getObjects(C4ID.getID(folder.getPersistentProperty(ClonkCore.FOLDER_C4ID_PROPERTY_ID)));
			if (objects != null) {
				for(C4Object obj : objects) {
					if ((obj instanceof C4ObjectIntern)) {
						if (((C4ObjectIntern)obj).relativePath.equalsIgnoreCase(folder.getProjectRelativePath().toPortableString())) {
							folder.setSessionProperty(ClonkCore.C4OBJECT_PROPERTY_ID, obj);
							return obj;
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
						if (intern.getObjectFolder() == folder)
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
	
	public void refreshCache() {
		// delete old cache
		if (globalFunctions != null) globalFunctions.clear();
		else globalFunctions = new LinkedList<C4Function>();
		if (staticVariables != null) staticVariables.clear();
		else staticVariables = new LinkedList<C4Variable>();
		
		// save cachable items
		for(List<C4Object> objects : getIndexedObjects().values()) {
			for(C4Object obj : objects) {
				for(C4Function func : obj.definedFunctions) {
					if (func.getVisibility() == C4FunctionScope.FUNC_GLOBAL) {
						globalFunctions.add(func);
					}
				}
				for(C4Variable var : obj.definedVariables) {
					if (var.getScope() == C4VariableScope.VAR_STATIC) {
						staticVariables.add(var);
					}
				}
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
	 * Adds an C4Object to the index.<br>
	 * Take care of the global function and static variable cache. You have to call <tt>refreshCache()</tt> after modifying the index.
	 * @param obj
	 */
	public void addObject(C4Object obj) {
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
		List<C4Object> alreadyDefinedObjects = getIndexedObjects().get(obj.getId());
		if (alreadyDefinedObjects != null) {
			alreadyDefinedObjects.remove(obj);
			if (alreadyDefinedObjects.size() == 0) { // if there are no more objects with this C4ID
				indexedObjects.remove(obj.getId());
			}
		}
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
	
	public C4Field findGlobalField(String fieldName) {
		for (C4Function func : getGlobalFunctions()) {
			if (func.getName().equals(fieldName))
				return func;
		}
		for (C4Variable var : getStaticVariables()) {
			if (var.getName().equals(fieldName))
				return var;
		}
		return null;
	}

	public void clear() {
		getIndexedObjects().clear();
		refreshCache();
	}

}
