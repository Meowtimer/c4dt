package net.arctics.clonk.parser;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.Utilities;
import net.arctics.clonk.parser.C4Function.C4FunctionScope;
import net.arctics.clonk.parser.C4Variable.C4VariableScope;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

public class ClonkIndex implements Serializable, Iterable<C4Object> {
	
	private static final long serialVersionUID = 1L;

	private Map<C4ID,List<C4Object>> indexedObjects = new HashMap<C4ID, List<C4Object>>();
	private List<C4ScriptBase> indexedScripts = new LinkedList<C4ScriptBase>(); 
	private List<C4Scenario> indexedScenarios = new LinkedList<C4Scenario>();
	
	private transient List<C4Function> globalFunctions = new LinkedList<C4Function>();
	private transient List<C4Variable> staticVariables = new LinkedList<C4Variable>();
	private transient Map<String, List<C4Field>> fieldMap = new HashMap<String, List<C4Field>>();
	
	public int numUniqueIds() {
		return indexedObjects.size();
	}
	
	public List<C4Object> getObjects(C4ID id) {
		if (indexedObjects == null)
			return null;
		List<C4Object> l = indexedObjects.get(id);
		return l == null ? null : Collections.unmodifiableList(l);
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
			for (List<C4Object> list : indexedObjects.values()) {
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

	private void addToFieldMap(C4Field field) {
		List<C4Field> list = fieldMap.get(field.getName());
		if (list == null) {
			list = new LinkedList<C4Field>();
			fieldMap.put(field.getName(), list);
		}
		list.add(field);
	}
	
	private void addGlobalsFrom(C4ScriptBase script) {
		for(C4Function func : script.definedFunctions) {
			if (func.getVisibility() == C4FunctionScope.FUNC_GLOBAL) {
				globalFunctions.add(func);
			}
			addToFieldMap(func);
		}
		for(C4Variable var : script.definedVariables) {
			if (var.getScope() == C4VariableScope.VAR_STATIC || var.getScope() == C4VariableScope.VAR_CONST) {
				staticVariables.add(var);
			}
			addToFieldMap(var);
		}
	}
	
	private <T extends C4ScriptBase> void addGlobalsFrom(Iterable<T> scripts) {
		for (T script : scripts) {
			addGlobalsFrom(script);
		}
	}
	
	public synchronized void refreshCache() {
		// delete old cache
		
		if (globalFunctions == null)
			globalFunctions = new LinkedList<C4Function>();
		if (staticVariables == null)
			staticVariables = new LinkedList<C4Variable>();
		if (fieldMap == null)
			fieldMap = new HashMap<String, List<C4Field>>();
		globalFunctions.clear();
		staticVariables.clear();
		fieldMap.clear();
		
		// save cachable items
		addGlobalsFrom(this);
		addGlobalsFrom(indexedScripts);
		addGlobalsFrom(indexedScenarios);
		
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
		List<C4Object> alreadyDefinedObjects = indexedObjects.get(obj.getId());
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
		List<C4Object> alreadyDefinedObjects = indexedObjects.get(obj.getId());
		if (alreadyDefinedObjects != null) {
			alreadyDefinedObjects.remove(obj);
			if (alreadyDefinedObjects.size() == 0) { // if there are no more objects with this C4ID
				indexedObjects.remove(obj.getId());
			}
		}
	}
	
	public void addScript(C4ScriptBase script) {
		if (script instanceof C4Scenario) {
			if (!indexedScenarios.contains(script))
				indexedScenarios.add((C4Scenario) script);
		}
		else if (script instanceof C4Object) {
			addObject((C4Object)script);
		}
		else {
			if (!indexedScripts.contains(script))
				indexedScripts.add(script);
		}
	}
	
	public void removeScript(C4ScriptBase script) {
		if (script instanceof C4Object)
			removeObject((C4Object)script);
		else
			indexedScripts.remove(script);
	}
	
	/**
	 * Returns true if there are no objects in this index.
	 * @return
	 */
	public boolean isEmpty() {
		return indexedObjects.isEmpty();
	}
	
	public List<C4Scenario> getIndexedScenarios() {
		return Collections.unmodifiableList(indexedScenarios);
	}
	
	public List<C4ScriptBase> getIndexedScripts() {
		return Collections.unmodifiableList(indexedScripts);
	}
	
	public List<C4Function> getGlobalFunctions() {
		return Collections.unmodifiableList(globalFunctions);
	}
	
	public List<C4Variable> getStaticVariables() {
		return Collections.unmodifiableList(staticVariables);
	}
	
	public Map<String, List<C4Field>> getFieldMap() {
		return Collections.unmodifiableMap(fieldMap);
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

	public static <T extends IRelatedResource> T pickNearest(IResource resource, Collection<T> fromList) {
		int bestDist = 1000;
		T best = null;
		if (fromList != null) {
			for (T o : fromList) {
				int newDist;
				if (o instanceof C4ObjectIntern)
					newDist = Utilities.distanceToCommonContainer(resource, ((C4ObjectIntern)o).getObjectFolder());
				else
					newDist = 100;
				if (best == null || newDist < bestDist) {
					best = o;
					bestDist = newDist;
				}
			}
		}
		return best;
	}
	
	public C4Object getObjectNearestTo(IResource resource, C4ID id) {
		if (resource == null)
			return getObjectFromEverywhere(id);
		List<C4Object> objs = getObjects(id);
		C4Object best = pickNearest(resource, objs);
		if (best == null && this != ClonkCore.getDefault().EXTERN_INDEX)
			best = ClonkCore.getDefault().EXTERN_INDEX.getLastObjectWithId(id);
		return best;
	}
	
	/**
	 * Like getLastObjectWithId, but falls back to ClonkCore.getDefault().EXTERN_INDEX if there is no object in this index
	 * @param id
	 * @return
	 */
	public C4Object getObjectFromEverywhere(C4ID id) {
		C4Object result = getLastObjectWithId(id);
		if (result == null && this != ClonkCore.getDefault().EXTERN_INDEX)
			result = ClonkCore.getDefault().EXTERN_INDEX.getLastObjectWithId(id);
		return result;
	}

	public <T extends C4Field> Iterable<T> fieldsWithName(String name, final Class<T> fieldClass) {
		List<C4Field> nonFinalList = this.fieldMap.get(name);
		if (nonFinalList == null)
			nonFinalList = new LinkedList<C4Field>();
		final List<C4Field> list = nonFinalList;
		return new Iterable<T>() {
			public Iterator<T> iterator() {
				return new Iterator<T>() {

					private int index = 0;

					public boolean hasNext() {
						for (; index < list.size(); index++)
							if (fieldClass.isAssignableFrom(list.get(index).getClass()))
								break;
						return index < list.size();
					}

					@SuppressWarnings("unchecked")
					public T next() {
						return (T) list.get(index++);
					}

					public void remove() {
						// not supported
					}

				};
			}
		};
	}

	public C4Function findGlobalFunction(String functionName) {
		for (C4Function func : globalFunctions) {
			if (func.getName().equals(functionName))
				return func;
		}
		return null;
	}
	
	public C4Variable findGlobalVariable(String variableName) {
		if (staticVariables == null)
			return null;
		for (C4Variable var : staticVariables) {
			if (var.getName().equals(variableName))
				return var;
		}
		return null;
	}
	
	public C4Field findGlobalField(String fieldName) {
		C4Function f = findGlobalFunction(fieldName);
		if (f != null)
			return f;
		return findGlobalVariable(fieldName);
	}
	
	public C4Field findGlobalField(String fieldName, IResource pivot) {
		if (pivot == null)
			return findGlobalField(fieldName);
		List<C4Field> fields = fieldMap.get(fieldName);
		if (fields != null) {
			return pickNearest(pivot, fields);
		}
		return null;
	}

	public void clear() {
		indexedObjects.clear();
		indexedScripts.clear();
		indexedScenarios.clear();
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
	
	public Iterable<C4Object> objectsIgnoringRemoteDuplicates(final IResource pivot) {
		return new Iterable<C4Object>() {
			public Iterator<C4Object> iterator() {
				final Iterator<List<C4Object>> listIterator = indexedObjects.values().iterator();
				return new Iterator<C4Object>() {
					
					public boolean hasNext() {
						return listIterator.hasNext();
					}

					public C4Object next() {
						List<C4Object> nextList = listIterator.next();
						return pickNearest(pivot, nextList);
					}

					public void remove() {
						// ...
					}
					
				};
			}
		};
	}

}
