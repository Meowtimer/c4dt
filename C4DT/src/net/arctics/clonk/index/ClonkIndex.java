package net.arctics.clonk.index;

import java.io.Serializable;

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
import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.C4ID;
import net.arctics.clonk.parser.c4script.C4Directive;
import net.arctics.clonk.parser.c4script.C4Function;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.c4script.C4Variable;
import net.arctics.clonk.parser.c4script.C4Directive.C4DirectiveType;
import net.arctics.clonk.parser.c4script.C4Function.C4FunctionScope;
import net.arctics.clonk.parser.c4script.C4Variable.C4VariableScope;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.resource.ExternalLib;
import net.arctics.clonk.util.IHasRelatedResource;
import net.arctics.clonk.util.IPredicate;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

public class ClonkIndex implements Serializable, Iterable<C4Object> {
	
	private static final long serialVersionUID = 1L;

	// not so nice to have it as an instance variable :/
	private transient IPredicate<C4Declaration> isGlobalPredicate;

	private Map<C4ID, List<C4Object>> indexedObjects = new HashMap<C4ID, List<C4Object>>();
	private List<C4ScriptBase> indexedScripts = new LinkedList<C4ScriptBase>(); 
	private List<C4Scenario> indexedScenarios = new LinkedList<C4Scenario>();
	
	private transient List<C4Function> globalFunctions = new LinkedList<C4Function>();
	private transient List<C4Variable> staticVariables = new LinkedList<C4Variable>();
	private transient Map<String, List<C4Declaration>> declarationMap = new HashMap<String, List<C4Declaration>>();
	private transient Map<C4ID, List<C4ScriptBase>> appendages = new HashMap<C4ID, List<C4ScriptBase>>();
	
	public int numUniqueIds() {
		return indexedObjects.size();
	}
	
	public List<C4Object> getObjects(C4ID id) {
		if (indexedObjects == null)
			return null;
		List<C4Object> l = indexedObjects.get(id);
		return l == null ? null : Collections.unmodifiableList(l);
	}
	
	public void postSerialize() throws CoreException {
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
						if (intern.getObjectFolder() != null && intern.getObjectFolder().equals(folder))
							return intern;
					}
				}
			}
			// also try scenarios
			for (C4Scenario s : indexedScenarios) {
				if (s.getObjectFolder() != null && s.getObjectFolder().equals(folder))
					return s;
			}
			//e.printStackTrace();
			return null;
		}
	}
	
	public C4ScriptBase getScript(IFile file) {
		C4ScriptBase result = Utilities.getScriptForFile(file);
		if (result == null) {
			for (C4ScriptBase s : this.indexedScripts)
				if (s.getResource().equals(file))
					return s;
		}
		return result;
	}

	private void addToFieldMap(C4Declaration field) {
		List<C4Declaration> list = declarationMap.get(field.getName());
		if (list == null) {
			list = new LinkedList<C4Declaration>();
			declarationMap.put(field.getName(), list);
		}
		list.add(field);
	}
	
	private void addGlobalsFrom(C4ScriptBase script) {
		for(C4Function func : script.functions()) {
			if (func.getVisibility() == C4FunctionScope.FUNC_GLOBAL) {
				globalFunctions.add(func);
			}
			addToFieldMap(func);
		}
		for(C4Variable var : script.variables()) {
			if (var.getScope() == C4VariableScope.VAR_STATIC || var.getScope() == C4VariableScope.VAR_CONST) {
				staticVariables.add(var);
			}
			addToFieldMap(var);
		}
	}
	
	private void detectAppendages(C4ScriptBase script) {
		for (C4Directive d : script.directives())
			if (d.getType() == C4DirectiveType.APPENDTO) {
				List<C4ScriptBase> appendtoList = appendages.get(d.contentAsID());
				if (appendtoList == null) {
					appendtoList = new LinkedList<C4ScriptBase>();
					appendages.put(d.contentAsID(), appendtoList);
				}
				appendtoList.add(script);
			}
	}
	
	private <T extends C4ScriptBase> void addGlobalsFrom(Iterable<T> scripts) {
		for (T script : scripts) {
			script.postSerialize(null); // for good measure
			addGlobalsFrom(script);
			detectAppendages(script);
		}
	}
	
	public synchronized void refreshCache() {
		// delete old cache
		
		if (globalFunctions == null)
			globalFunctions = new LinkedList<C4Function>();
		if (staticVariables == null)
			staticVariables = new LinkedList<C4Variable>();
		if (declarationMap == null)
			declarationMap = new HashMap<String, List<C4Declaration>>();
		if (appendages == null)
			appendages = new HashMap<C4ID, List<C4ScriptBase>>();
		globalFunctions.clear();
		staticVariables.clear();
		declarationMap.clear();
		appendages.clear();
		
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
	 * Remove the script from the index
	 * @param script script which maybe a standalone-script, a scenario or an object
	 */
	public void removeScript(C4ScriptBase script) {
		if (script instanceof C4Object)
			removeObject((C4Object)script);
		else
			indexedScripts.remove(script);
	}
	
	/**
	 * Removes this object from the index.<br>
	 * The object may still exist in IContainer.sessionProperty<br>
	 * Take care of the global function and static variable cache. You have to call <tt>refreshCache()</tt> after modifying the index.
	 * @param obj
	 */
	public void removeObject(C4Object obj) {
		if (obj instanceof C4Scenario) {
			removeScenario((C4Scenario)obj);
			return;
		}
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
	
	public void removeScenario(C4Scenario scenario) {
		this.indexedScenarios.remove(scenario);
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
	
	public Map<String, List<C4Declaration>> getDeclarationMap() {
		return Collections.unmodifiableMap(declarationMap);
	}

	public C4Object getLastObjectWithId(C4ID id) {
		List<C4Object> objs = getObjects(id);
		if (objs != null) {
			if (objs instanceof LinkedList<?>) { // due to performance
				return ((LinkedList<C4Object>)objs).getLast();
			}
			else {
				return objs.get(objs.size()-1);
			}
		}
		return null;
	}

	public static <T extends IHasRelatedResource> T pickNearest(IResource resource, Collection<T> fromList) {
		return Utilities.pickNearest(resource, fromList, null);
	}
	
	public boolean acceptsFromExternalLib(ExternalLib lib) {
		return true;
	}
	
	public boolean acceptsDeclaration(C4Declaration declaration) {
		C4ScriptBase script = declaration.getScript();
		if (script instanceof IExternalScript)
			return acceptsFromExternalLib(((IExternalScript)script).getExternalLib());
		return true;
	}
	
	public C4Object getExternalObject(C4ID id) {
		List<C4Object> obj = ClonkCore.getDefault().getExternIndex().getObjects(id);
		if (obj != null) {
			for (C4Object o : obj) {
				C4ObjectExtern eo = (C4ObjectExtern) o;
				if (acceptsFromExternalLib(eo.getExternalLib()))
					return eo;
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
				// TODO Auto-generated catch block
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

	public C4Object getObjectNearestTo(IResource resource, C4ID id) {
		C4Object best = null;
		for (ClonkIndex index : relevantIndexes()) {
			if (resource != null) {
				List<C4Object> objs = index.getObjects(id);
				best = pickNearest(resource, objs);
			}
			else {
				best = index.getLastObjectWithId(id);
			}
			if (best != null)
				break;
		}
		if (best == null && this != ClonkCore.getDefault().getExternIndex())
			best = getExternalObject(id);
		return best;
	}
	
	/**
	 * Like getLastObjectWithId, but falls back to ClonkCore.getDefault().getExternIndex() if there is no object in this index
	 * @param id
	 * @return
	 */
	public C4Object getObjectFromEverywhere(C4ID id) {
		return getObjectNearestTo(null, id);
	}

	public <T extends C4Declaration> Iterable<T> declarationsWithName(String name, final Class<T> fieldClass) {
		List<C4Declaration> nonFinalList = this.declarationMap.get(name);
		if (nonFinalList == null)
			nonFinalList = new LinkedList<C4Declaration>();
		final List<C4Declaration> list = nonFinalList;
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
			if (func.getName().equals(functionName) && acceptsDeclaration(func))
				return func;
		}
		return null;
	}
	
	public C4Variable findGlobalVariable(String variableName) {
		if (staticVariables == null)
			return null;
		for (C4Variable var : staticVariables) {
			if (var.getName().equals(variableName) && acceptsDeclaration(var))
				return var;
		}
		return null;
	}
	
	public C4Declaration findGlobalDeclaration(String fieldName) {
		C4Function f = findGlobalFunction(fieldName);
		if (f != null)
			return f;
		return findGlobalVariable(fieldName);
	}

	private IPredicate<C4Declaration> isGlobalPredicate() {
		if (isGlobalPredicate == null) {
			isGlobalPredicate = new IPredicate<C4Declaration>() {
				public boolean test(C4Declaration item) {
					return item.isGlobal() && acceptsDeclaration(item);
				}
			};
		}
		return isGlobalPredicate;
	}
	
	public C4Declaration findGlobalDeclaration(String declName, IResource pivot) {
		if (pivot == null)
			return findGlobalDeclaration(declName);
		List<C4Declaration> declarations = declarationMap.get(declName);
		if (declarations != null) {
			return Utilities.pickNearest(pivot, declarations, isGlobalPredicate());
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
	
	public List<C4ScriptBase> appendagesOf(C4Object object) {
		if (appendages == null)
			return null;
		List<C4ScriptBase> list = appendages.get(object.getId());
		if (list != null) {
			return Collections.unmodifiableList(list); 
		}
		return null;
	}
	
	public Iterable<C4ScriptBase> dependentScripts(final C4ScriptBase base) {
		return new Iterable<C4ScriptBase>() {
			@Override
            public Iterator<C4ScriptBase> iterator() {
	            return new Iterator<C4ScriptBase>() {
	            	private C4Object baseObject = base instanceof C4Object ? (C4Object)base : null;
	            	private boolean hasGlobals = base.containsGlobals();
	            	private int stage = 0;
	            	private Iterator<? extends C4ScriptBase> currentIterator = getIndexedScripts().iterator();
	            	private C4ScriptBase currentScript;
	            	private HashSet<C4ScriptBase> alreadyReturned = new HashSet<C4ScriptBase>();

	            	private Iterator<? extends C4ScriptBase> getIterator() {
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
	            		C4ScriptBase s = null;
	            		Outer: for (Iterator<? extends C4ScriptBase> it = getIterator(); it != null; it = getIterator()) {
	            			do {
	            				s = it.next();
	            				if (s == base || alreadyReturned.contains(s))
	            					continue;
	            				if (hasGlobals) {
	            					break Outer;
	            				}
	            				if (baseObject != null) {
	            					if (s.includes(baseObject))
	            						break Outer;
	            				}
	            				List<C4ScriptBase> appendages;
	            				if (s instanceof C4Object && (appendages = appendagesOf((C4Object)s)) != null && appendages.contains(base)) {
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
                    public C4ScriptBase next() {
						return currentScript;
                    }
					@Override
                    public void remove() {}	            	
	            };
            }
		};
	}
	
	public C4Engine getEngine() {
		return ClonkCore.getDefault().getActiveEngine();
	}
	
	public void setDirty(boolean dirty) {}
	public boolean isDirty() {return false;}

}
