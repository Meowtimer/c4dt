package net.arctics.clonk.parser;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.C4Function.C4FunctionScope;
import net.arctics.clonk.parser.C4Variable.C4VariableScope;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

public class ClonkIndex implements IC4ObjectListener {
	
	private Map<C4ID,List<C4Object>> projectObjects;
	
	private IProject project;
	private List<C4Function> globalFunctions;
	private List<C4Variable> staticVariables;
	
	public ClonkIndex(IProject project) {
		this.project = project;
	}
	
	public List<C4Object> getObjects(C4ID id) {
		return getIndexedObjects().get(id);
	}
	
	/**
	 * You can use IContainer.getSessionProperty(ClonkCore.C4OBJECT_PROPERTY_ID) instead of this
	 * @param folder
	 * @return
	 */
	public C4Object getObject(IContainer folder) {
		try {
			return (C4Object) folder.getSessionProperty(ClonkCore.C4OBJECT_PROPERTY_ID);
		} catch (CoreException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public List<C4Function> getGlobalFunctions() {
		if (globalFunctions == null) {
			globalFunctions = new LinkedList<C4Function>();
			
			refreshCache();
//			for(List<C4Object> objects : getIndexedObjects().values()) {
//				for(C4Object obj : objects)
//					addToGlobalFunctionCache(obj);
//			}
		}
		return globalFunctions;
	}
	
	public List<C4Variable> getStaticVariables() {
		if (staticVariables == null) {
			staticVariables = new LinkedList<C4Variable>();
			
			refreshCache();
//			for(List<C4Object> objects : getIndexedObjects().values()) {
//				for(C4Object obj : objects)
//					addToStaticVariableCache(obj);
//			}
		}
		return staticVariables;
	}
	
	public void refreshCache() {
		globalFunctions.clear();
		staticVariables.clear();
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
			projectObjects.put(obj.getId(), alreadyDefinedObjects);
		}
		alreadyDefinedObjects.add(obj);
		
//		if (globalFunctions != null) {
//			addToGlobalFunctionCache(obj);
//		}
//		if (staticVariables != null) {
//			addToStaticVariableCache(obj);
//		}
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
				projectObjects.remove(obj.getId());
			}
		}
		
//		if (globalFunctions != null) {
//			removeFromGlobalFunctionCache(obj);
//		}
//		if (staticVariables != null) {
//			removeFromStaticVariableCache(obj);
//		}
	}
	
	/**
	 * Returns true if there are no objects in this index.
	 * @return
	 */
	public boolean isEmpty() {
		if (getIndexedObjects() != null) {
			return projectObjects.isEmpty();
		}
		return true;
	}
	
	private void addToGlobalFunctionCache(C4Object obj) {
		if (obj.definedFunctions != null) {
			for(C4Function func : obj.definedFunctions) {
				if (func.getVisibility() == C4FunctionScope.FUNC_GLOBAL) {
					globalFunctions.add(func);
				}
			}
		}
	}

	private void removeFromGlobalFunctionCache(C4Object obj) {
		if (obj.definedFunctions != null) {
			for(C4Function func : obj.definedFunctions) {
				if (func.getVisibility() == C4FunctionScope.FUNC_GLOBAL) {
					globalFunctions.remove(func);
				}
			}
		}
	}
	
	private void addToStaticVariableCache(C4Object obj) {
		if (obj.definedVariables != null) {
			for(C4Variable var : obj.definedVariables) {
				if (var.getScope() == C4VariableScope.VAR_STATIC) {
					staticVariables.add(var);
				}
			}
		}
	}

	private void removeFromStaticVariableCache(C4Object obj) {
		if (obj.definedVariables != null) {
			for(C4Variable var : obj.definedVariables) {
				if (var.getScope() == C4VariableScope.VAR_STATIC) {
					staticVariables.remove(var);
				}
			}
		}
	}
	
	private Map<C4ID, List<C4Object>> getIndexedObjects() {
		if (projectObjects == null) loadIndexData();
		return projectObjects;
	}
	
	private void loadIndexData() {
		IFile index = project.getFile("indexdata.xml");
		projectObjects = new HashMap<C4ID, List<C4Object>>();
		if (!index.exists()) {
			try {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				XMLEncoder encoder = new XMLEncoder(out);
				encoder.close();
				index.create(new ByteArrayInputStream(out.toByteArray()), IResource.DERIVED | IResource.HIDDEN, null);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		else {
			try {
				java.beans.XMLDecoder decoder = new XMLDecoder(new BufferedInputStream(index.getContents()));
				while (true) {
					Object obj = decoder.readObject();
					if (obj instanceof C4Object) {
						addObject((C4Object)obj);
					}
					else {
						System.out.println("Read unknown object from indexdata.xml: " + obj.getClass().getName());
					}
				}
			} catch(ArrayIndexOutOfBoundsException e) {
				// finished
			} catch (CoreException e) {
				e.printStackTrace();
			}
			refreshCache();
		}
	}

	public void fieldAdded(C4Object obj, C4Field field) {
		// TODO Auto-generated method stub
		
	}

	public void fieldChanged(C4Object obj, C4Field field) {
		// TODO Auto-generated method stub
		
	}

	public void fieldRemoved(C4Object obj, C4Field field) {
		// TODO Auto-generated method stub
		
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
}
