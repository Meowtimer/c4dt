package net.arctics.clonk.parser;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.C4Function.C4FunctionScope;
import net.arctics.clonk.parser.C4Variable.C4VariableScope;
import net.arctics.clonk.preferences.PreferenceConstants;
import net.arctics.clonk.resource.c4group.C4Group;
import net.arctics.clonk.resource.c4group.C4GroupItem;
import net.arctics.clonk.resource.c4group.InvalidDataException;
import net.arctics.clonk.resource.c4group.C4Group.C4GroupType;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.util.IPropertyChangeListener;

public class ClonkIndex implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Map<C4ID,List<C4Object>> projectObjects;
	
	private transient IProject project;
	private List<C4Function> globalFunctions;
	private List<C4Variable> staticVariables;
	
	public ClonkIndex(IProject project) {
		this.project = project;
	}
	
	public void setProject(IProject proj) {
		project = proj;
	}
	
	public List<C4Object> getObjects(C4ID id) {
		return getIndexedObjects().get(id);
	}
	
	public void fixReferences() throws CoreException {
		for (List<C4Object> list : getIndexedObjects().values()) {
			for (C4Object obj : list) {
				obj.fixReferencesAfterSerialization();
				if (obj instanceof C4ObjectIntern) {
					C4ObjectIntern objIntern = (C4ObjectIntern)obj;
					Path path = new Path(objIntern.relativePath);
					IPath projectPath = path.removeFirstSegments(1);
					IResource res = project.findMember(projectPath);
					if (res instanceof IContainer)
						((C4ObjectIntern)obj).setCorrespondingFolder((IContainer)res);
				}
			}
		}
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
		if (projectObjects == null) {
			projectObjects = new HashMap<C4ID, List<C4Object>>();
		}
	//	if (projectObjects == null) loadIndexData();
		return projectObjects;
	}
	
//	private static final String indexDataFile = "indexdata.xml";
//	
//	public void saveIndexData() {
//		final IFile index = project.getFile(indexDataFile);
//		ByteArrayOutputStream out = new ByteArrayOutputStream();
//		try {
//			ObjectOutputStream objStream = new ObjectOutputStream(out);
//			objStream.writeObject(this);
//			objStream.close();
//			ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
//			if (index.exists()) {
//				index.setContents(in, true, false, null);
//			} else {
//				index.create(in, true, null);
//			}
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (CoreException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
//	
//	public void loadIndexData() {
//		final IFile index = project.getFile(indexDataFile);
//		try {
//			InputStream in = index.getContents();
//			ObjectInputStream objStream = new ObjectInputStream(in);
//	//		objStream.readObject();
//		} catch (CoreException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
	
//	public void saveIndexData() {
//		final IFile index = project.getFile("indexdata.xml");
//		ByteArrayOutputStream out = new ByteArrayOutputStream();
//		XMLEncoder encoder = new XMLEncoder(out);
//		
//		encoder.setExceptionListener(new ExceptionListener() {
//			public void exceptionThrown(Exception e) {
//				e.printStackTrace();
//			}
//		});
//		
//		try {
//			BeanInfo info = Introspector.getBeanInfo(C4ObjectIntern.class);
//	        for (PropertyDescriptor desc : info.getPropertyDescriptors())
//	            if (desc.getName().equals("objectFolder"))
//	               desc.setValue("transient", Boolean.TRUE);
//		} catch (IntrospectionException e1) {
//			e1.printStackTrace();
//		}
//		
//		encoder.setPersistenceDelegate(C4ObjectIntern.class, new DefaultPersistenceDelegate() {
//			@Override
//			protected Expression instantiate(Object oldInstance, Encoder out) {
//				C4ObjectIntern intern = (C4ObjectIntern) oldInstance;
//				if (intern.relativePath == null) {
//					System.out.println(intern.getName() + intern.getId().getName());
//				}
//				return new Expression(oldInstance,C4ObjectIntern.class, "fromSerialize", new Object[] { intern.getId(), intern.getName(), intern.relativePath });
//			}
//		});
//
//		encoder.setPersistenceDelegate(C4ID.class, new DefaultPersistenceDelegate() {
//			@Override
//			protected Expression instantiate(Object oldInstance, Encoder out) {
//				return new Expression(oldInstance, C4ID.class, "getID", new Object[] { ((C4ID)oldInstance).getName() });
//			}
//		});
//		
//		encoder.setPersistenceDelegate(C4Type.class, new DefaultPersistenceDelegate() {
//			@Override
//			protected Expression instantiate(Object oldInstance, Encoder out) {
//				return new Expression(oldInstance, C4Type.class, "makeType", new Object[] { ((C4Type)oldInstance).toString() });
//			}
//		});
//		
//		encoder.setPersistenceDelegate(SourceLocation.class, new DefaultPersistenceDelegate() {
//			@Override
//			protected Expression instantiate(Object oldInstance, Encoder out) {
//				return new Expression(oldInstance, SourceLocation.class, "new", new Object[] { ((SourceLocation)oldInstance).getStart(), ((SourceLocation)oldInstance).getStart() });
//			}
//		});
//		
//		encoder.setPersistenceDelegate(C4FunctionScope.class, new DefaultPersistenceDelegate() {
//			@Override
//			protected Expression instantiate(Object oldInstance, Encoder out) {
//				return new Expression(oldInstance, C4FunctionScope.class, "valueOf", new Object[] { ((C4FunctionScope)oldInstance).toString() });
//			}
//		});
//		
//		encoder.setPersistenceDelegate(C4VariableScope.class, new DefaultPersistenceDelegate() {
//			@Override
//			protected Expression instantiate(Object oldInstance, Encoder out) {
//				return new Expression(oldInstance, C4VariableScope.class, "valueOf", new Object[] { ((C4VariableScope)oldInstance).toString() });
//			}
//		});
//		
//		encoder.setPersistenceDelegate(C4DirectiveType.class, new DefaultPersistenceDelegate() {
//			@Override
//			protected Expression instantiate(Object oldInstance, Encoder out) {
//				return new Expression(oldInstance, C4DirectiveType.class, "valueOf", new Object[] { ((C4DirectiveType)oldInstance).toString() });
//			}
//		});
//		
//		encoder.setPersistenceDelegate(C4Directive.class, new DefaultPersistenceDelegate() {
//			@Override
//			protected Expression instantiate(Object oldInstance, Encoder out) {
//				return new Expression(oldInstance, C4Directive.class, "new", new Object[] { ((C4Directive)oldInstance).getType(), ((C4Directive)oldInstance).getContent() });
//			}
//		});
//		
//		for (List<C4Object> objects : getIndexedObjects().values()) {
//			for(C4Object obj : objects) {
//				encoder.writeObject(obj);
//			}
//		}
//		
//		encoder.close();
//
//		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
//		
//		try {
//			if (!index.exists()) {
//				index.create(in, IResource.DERIVED | IResource.HIDDEN, null);
//			}
//			else {
//				index.setContents(in, true, false, null);
//			}
//		} catch (CoreException e) {
//			e.printStackTrace();
//		}
//
//	}
	
	private void loadIndexData() {
		
		long start = System.currentTimeMillis();
		
		IFile index = project.getFile("indexdata.xml");
		projectObjects = new HashMap<C4ID, List<C4Object>>();
		if (!index.exists()) {
			try {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
//				ObjectOutputStream objOutput = new ObjectOutputStream(out);
//				objOutput.close();
				XMLEncoder encoder = new XMLEncoder(out);
				encoder.close();
				index.create(new ByteArrayInputStream(out.toByteArray()), IResource.DERIVED | IResource.HIDDEN, null);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		else {
			try {
//				ObjectInputStream decoder = new ObjectInputStream(new BufferedInputStream(index.getContents()));
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
		
		long end = System.currentTimeMillis();
		long span = end - start;
		System.out.println(String.format("Time to read persistent build data: %d",span));
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
