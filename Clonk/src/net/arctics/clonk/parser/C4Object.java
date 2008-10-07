package net.arctics.clonk.parser;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.C4Directive.C4DirectiveType;

public abstract class C4Object extends C4Field  {

	static public class FindFieldInfo {
		private ClonkIndex index;
		private int recursion;
		/**
		 * @param indexer the indexer to be passed to the info
		 */
		public FindFieldInfo(ClonkIndex clonkIndex) {
			super();
			index = clonkIndex;
		}
		
	}
	
	protected C4ID id;
	
	protected List<C4Function> definedFunctions = new LinkedList<C4Function>();
	protected List<C4Variable> definedVariables = new ArrayList<C4Variable>(); // default capacity of 10 is ok
	protected List<C4Directive> definedDirectives = new ArrayList<C4Directive>(4); // mostly 4 are enough
	
	private List<IC4ObjectListener> changeListeners = new LinkedList<IC4ObjectListener>();
	
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
	
	public C4Object[] getIncludes(ClonkIndex index) {
		List<C4Object> result = new ArrayList<C4Object>();
		for (C4Directive d : definedDirectives) {
			if (d.getType() == C4DirectiveType.INCLUDE) {
				List<C4Object> objs = index.getObjects(C4ID.getID(d.getContent()));
				if (objs != null) {
					if (objs instanceof LinkedList) { // due to performance
						result.add(((LinkedList<C4Object>)objs).getLast());
					}
					else {
						result.add(objs.get(objs.size()-1));
					}
				}
			}
		}
		return result.toArray(new C4Object[]{}); // lolz?
	}
	
	public C4Field findField(String name, FindFieldInfo info) {
		if (id.getName().equals(name))
			return this;
		for (C4Function f : definedFunctions) {
			if (f.getName().equals(name))
				return f;
		}
		for (C4Variable v : definedVariables) {
			if (v.getName().equals(name))
				return v;
		}
		info.recursion++;
		for (C4Object o : getIncludes(info.index)) {
			C4Field result = o.findField(name, info);
			if (result != null)
				return result;
		}
		info.recursion--;
		if (info.recursion == 0) {
			for (C4Function f : ClonkCore.ENGINE_OBJECT.definedFunctions) {
				if (f.getName().equals(name))
					return f;
			}
		}
		return null;
	}
	
	public void addField(C4Field field) {
		field.setObject(this);
		if (field instanceof C4Function) {
			definedFunctions.add((C4Function)field);
			for(IC4ObjectListener listener : changeListeners) {
				listener.fieldAdded(this, field);
			}
		}
		else if (field instanceof C4Variable) {
			definedVariables.add((C4Variable)field);
			for(IC4ObjectListener listener : changeListeners) {
				listener.fieldAdded(this, field);
			}
		}
	}
	
	public void removeField(C4Field field) {
		if (field.getObject() != this) field.setObject(this);
		if (field instanceof C4Function) {
			definedFunctions.remove((C4Function)field);
			for(IC4ObjectListener listener : changeListeners) {
				listener.fieldRemoved(this, field);
			}
		}
		else if (field instanceof C4Variable) {
			definedVariables.remove((C4Variable)field);
			for(IC4ObjectListener listener : changeListeners) {
				listener.fieldRemoved(this, field);
			}
		}
	}
	
	public void clearFields() {
		for(C4Function func : definedFunctions) {
			removeField(func);
		}
		for(C4Variable var : definedVariables) {
			removeField(var);
		}
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

	public void addListener(IC4ObjectListener listener) {
		changeListeners.add(listener);
	}
	
	public void removeListener(IC4ObjectListener listener) {
		changeListeners.remove(listener);
	}
	
	public abstract Object getScript(); 
	
//	public String getText(Object element) {
//		if (element instanceof C4Function)
//			return ((C4Function)element).getName();
//		return element.toString();
//	}
	
}
