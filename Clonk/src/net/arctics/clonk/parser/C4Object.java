package net.arctics.clonk.parser;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.Utilities;
import net.arctics.clonk.parser.C4Directive.C4DirectiveType;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;

public abstract class C4Object extends C4Field {

	static public class FindFieldInfo {
		private ClonkIndex index;
		private int recursion;
		private Class<? extends C4Field> fieldClass;
		private C4Function context;
		/**
		 * @param indexer the indexer to be passed to the info
		 */
		public FindFieldInfo(ClonkIndex clonkIndex) {
			super();
			index = clonkIndex;
		}
		public FindFieldInfo(ClonkIndex clonkIndex, C4Function ctx) {
			this(clonkIndex);
			setContext(ctx);
		}
		public Class<? extends C4Field> getFieldClass() {
			return fieldClass;
		}
		public void setFieldClass(Class<?extends C4Field> fieldClass) {
			this.fieldClass = fieldClass;
		}
		public void setContext(C4Function ctx) {
			context = ctx;
		}
		public C4Function getContext() {
			return context;
		}
		
	}
	
	protected C4ID id;
	
	protected List<C4Function> definedFunctions = new LinkedList<C4Function>();
	protected List<C4Variable> definedVariables = new ArrayList<C4Variable>(); // default capacity of 10 is ok
	protected List<C4Directive> definedDirectives = new ArrayList<C4Directive>(4); // mostly 4 are enough
	
//	private List<IC4ObjectListener> changeListeners = new LinkedList<IC4ObjectListener>();
	
	/**
	 * Creates a new C4Object and assigns it to <code>container</code>
	 * @param id C4ID (e.g. CLNK)
	 * @param name intern name
	 * @param container ObjectName.c4d resource
	 */
	protected C4Object(C4ID id, String name) {
		this.id = id;
		this.name = name;
	}
	
	public int strictLevel() {
		int level = 0;
		for (C4Directive d : this.definedDirectives) {
			if (d.getType() == C4DirectiveType.STRICT) {
				try {
					level = Math.max(level, Integer.parseInt(d.getContent()));
				}
				catch (NumberFormatException e) {
					level = 1;
				}
			}
		}
		return level;
	}
	
	public C4Object[] getIncludes(ClonkIndex index) {
		List<C4Object> result = new ArrayList<C4Object>();
		for (C4Directive d : definedDirectives) {
			if (d.getType() == C4DirectiveType.INCLUDE) {
				C4Object obj = index.getLastObjectWithId(C4ID.getID(d.getContent()));
				if (obj != null)
					result.add(obj);
			}
		}
		return result.toArray(new C4Object[]{}); // lolz?
	}
	
	public C4Field findField(String name) {
		return findField(name, new FindFieldInfo(Utilities.getProject(this).getIndexedData()));
	}
	
	public C4Field findField(String name, FindFieldInfo info) {
		
		// local variable?
		if (info.recursion == 0) {
			if (info.getContext() != null) {
				C4Field v = info.getContext().findVar(name);
				if (v != null)
					return v;
			}
		}
		
		// this object?
		if (info.getFieldClass() == null || info.getFieldClass() == C4Object.class) {
			if (id != null && id.getName().equals(name))
				return this;
		}
		
		// a function defined in this object
		if (info.getFieldClass() == null || info.getFieldClass() == C4Function.class) {
			for (C4Function f : definedFunctions) {
				if (f.getName().equals(name))
					return f;
			}
		}
		// a variable
		if (info.getFieldClass() == null || info.getFieldClass() == C4Variable.class) {
			for (C4Variable v : definedVariables) {
				if (v.getName().equals(name))
					return v;
			}
		}
		
		// search in included definitions
		info.recursion++;
		for (C4Object o : getIncludes(info.index)) {
			C4Field result = o.findField(name, info);
			if (result != null)
				return result;
		}
		info.recursion--;
		
		// finally look if it's something global
		if (info.recursion == 0 && this != ClonkCore.ENGINE_OBJECT) { // .-.
			C4Field f;
			// global stuff defined in project
			f = info.index.findGlobalField(name);
			// engine function
			if (f == null)
				f = ClonkCore.ENGINE_OBJECT.findField(name, info);
			
			if (f == null && Utilities.looksLikeID(name)) {
				f = info.index.getLastObjectWithId(C4ID.getID(name));
			}

			if (f != null && (info.fieldClass == null || info.fieldClass.isAssignableFrom(f.getClass())))
				return f;
		}
		return null;
	}
	
	public void addField(C4Field field) {
		field.setObject(this);
		if (field instanceof C4Function) {
			definedFunctions.add((C4Function)field);
//			for(IC4ObjectListener listener : changeListeners) {
//				listener.fieldAdded(this, field);
//			}
		}
		else if (field instanceof C4Variable) {
			definedVariables.add((C4Variable)field);
//			for(IC4ObjectListener listener : changeListeners) {
//				listener.fieldAdded(this, field);
//			}
		}
	}
	
	public void removeField(C4Field field) {
		if (field.getObject() != this) field.setObject(this);
		if (field instanceof C4Function) {
			definedFunctions.remove((C4Function)field);
//			for(IC4ObjectListener listener : changeListeners) {
//				listener.fieldRemoved(this, field);
//			}
		}
		else if (field instanceof C4Variable) {
			definedVariables.remove((C4Variable)field);
//			for(IC4ObjectListener listener : changeListeners) {
//				listener.fieldRemoved(this, field);
//			}
		}
	}
	
	public void clearFields() {
		while (definedFunctions.size() > 0)
			removeField(definedFunctions.get(definedFunctions.size()-1));
		while (definedVariables.size() > 0)
			removeField(definedVariables.get(definedVariables.size()-1));
	}

//	/**
//	 * @param objectFolder the objectFolder to set
//	 */
//	public void setObjectFolder(IContainer objectFolder) {
//		this.objectFolder = objectFolder;
//	}
	
	/**
	 * @deprecated if you use this function, you are not allowed to add or remove items
	 * @return the definedFunctions
	 */
	public List<C4Function> getDefinedFunctions() {
		return definedFunctions;
	}
	/**
	 * @deprecated never use this function
	 * @param definedFunctions the definedFunctions to set
	 */
	public void setDefinedFunctions(List<C4Function> definedFunctions) {
		this.definedFunctions = definedFunctions;
	}
	/**
	 * @deprecated if you use this function, you are not allowed to add or remove items
	 * @return the definedVariables
	 */
	public List<C4Variable> getDefinedVariables() {
		return definedVariables;
	}
	/**
	 * @deprecated never use this function
	 * @param definedVariables the definedVariables to set
	 */
	public void setDefinedVariables(List<C4Variable> definedVariables) {
		this.definedVariables = definedVariables;
	}
	/**
	 * @deprecated if you use this function, you are not allowed to add or remove items
	 * @return the definedDirectives
	 */
	public List<C4Directive> getDefinedDirectives() {
		return definedDirectives;
	}
	/**
	 * @deprecated never use this function
	 * @param definedDirectives the definedDirectives to set
	 */
	public void setDefinedDirectives(List<C4Directive> definedDirectives) {
		this.definedDirectives = definedDirectives;
	}
	
	/**
	 * The id of this object. (e.g. CLNK)
	 * @return the id
	 */
	public C4ID getId() {
		return id;
	}
	
	/**
	 * Sets the id property of this object.
	 * This method does not change resources.
	 * @param newId
	 */
	public void setId(C4ID newId) {
		id = newId;
	}
	
	public void setName(String name) {
		this.name = name;
	}

//	public void addListener(IC4ObjectListener listener) {
//		changeListeners.add(listener);
//	}
//	
//	public void removeListener(IC4ObjectListener listener) {
//		changeListeners.remove(listener);
//	}
	
	public abstract Object getScript();

	public C4Function findFunction(String functionName, FindFieldInfo info) {
		info.setFieldClass(C4Function.class);
		return (C4Function) findField(functionName, info);
	}
	
	public C4Variable findVariable(String varName, FindFieldInfo info) {
		info.setFieldClass(C4Variable.class);
		return (C4Variable) findField(varName, info);
	}

	public C4Function funcAt(IRegion region) {
		// from name to end of body should be enough... ?
		for (C4Function f : definedFunctions) {
			if (f.getLocation().getOffset() <= region.getOffset() && region.getOffset()+region.getLength() <= f.getBody().getOffset()+f.getBody().getLength())
				return f;
		}
		return null;
	}
	
	// OMG, IRegion <-> ITextSelection
	public C4Function funcAt(ITextSelection region) {
		// from name to end of body should be enough... ?
		for (C4Function f : definedFunctions) {
			if (f.getLocation().getOffset() <= region.getOffset() && region.getOffset()+region.getLength() <= f.getBody().getOffset()+f.getBody().getLength())
				return f;
		}
		return null;
	}
	
	public static C4Object objectCorrespondingTo(IContainer folder) {
//		try {
			return (Utilities.getIndex(folder) != null) ? Utilities.getIndex(folder).getObject(folder) : null;
//			return (folder != null) ? (C4Object)folder.getSessionProperty(ClonkCore.C4OBJECT_PROPERTY_ID) : null;
//		} catch (CoreException e) {
//			return null;
//		}
	}
	
//	public String getText(Object element) {
//		if (element instanceof C4Function)
//			return ((C4Function)element).getName();
//		return element.toString();
//	}
	
}
