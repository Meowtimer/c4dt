package net.arctics.clonk.parser;

import java.util.ArrayList;
import java.util.List;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.C4Directive.C4DirectiveType;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.*;

public class C4Object extends C4Field {
	
	static public class FindFieldInfo {
		private ClonkIndexer indexer;
		private int recursion;
		/**
		 * @param indexer the indexer to be passed to the info
		 */
		public FindFieldInfo(ClonkIndexer indexer) {
			super();
			this.indexer = indexer;
		}
		
	}
	
	private C4ID id;
	private String fullName;
	private boolean rooted;
	
	private List<C4Function> definedFunctions = new ArrayList<C4Function>();
	private List<C4Variable> definedVariables = new ArrayList<C4Variable>();
	private List<C4Directive> definedDirectives = new ArrayList<C4Directive>();
	
	private IResource script;
	
	public C4Object[] getIncludes(ClonkIndexer indexer) {
		List<C4Object> result = new ArrayList<C4Object>();
		for (C4Directive d : definedDirectives) {
			if (d.getType() == C4DirectiveType.INCLUDE) {
				C4Object obj = indexer.getObjects().get(C4ID.getID(d.getContent()));
				if (obj != null)
					result.add(obj);
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
		for (C4Object o : getIncludes(info.indexer)) {
			C4Field result = o.findField(name, info);
			if (result != null)
				return result;
		}
		info.recursion--;
		if (info.recursion == 0) {
			for (C4Function f : ClonkCore.ENGINE_FUNCTIONS) {
				if (f.getName().equals(name))
					return f;
			}
		}
		return null;
	}
	
	public void addField(C4Field field) {
		field.setObject(this);
		if (field instanceof C4Function)
			definedFunctions.add((C4Function)field);
		else if (field instanceof C4Variable)
			definedVariables.add((C4Variable)field);
	}
	
	public IResource getScript() {
		return script;
	}
	
	public void setScript(IResource s) {
		script = s;
	}
	
	public C4Object(C4ID id, String name, boolean isRooted) {
		this.id = id;
		this.name = name;
		rooted = isRooted;
	}
	
	protected String parseName() {
		return "";
	}
	
	protected void setName(String newName) {
		name = newName;
	}
	
	/**
	 * @return the definedFunctions
	 */
	public List<C4Function> getDefinedFunctions() {
		return definedFunctions;
	}
	/**
	 * @param definedFunctions the definedFunctions to set
	 */
	public void setDefinedFunctions(List<C4Function> definedFunctions) {
		this.definedFunctions = definedFunctions;
	}
	/**
	 * @return the definedVariables
	 */
	public List<C4Variable> getDefinedVariables() {
		return definedVariables;
	}
	/**
	 * @param definedVariables the definedVariables to set
	 */
	public void setDefinedVariables(List<C4Variable> definedVariables) {
		this.definedVariables = definedVariables;
	}
	/**
	 * @return the definedDirectives
	 */
	public List<C4Directive> getDefinedDirectives() {
		return definedDirectives;
	}
	/**
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
	 * @return the rooted
	 */
	public boolean isRooted() {
		return rooted;
	}

	/**
	 * Not implemented yet
	 * @return the fullName
	 */
	public String getFullName() {
		return fullName;
	}

	/**
	 * @param fullName the fullName to set
	 */
	public void setFullName(String fullName) {
		this.fullName = fullName;
	}
	
	public String getText(Object element) {
		if (element instanceof C4Function)
			return ((C4Function)element).getName();
		return element.toString();
	}
	
}
