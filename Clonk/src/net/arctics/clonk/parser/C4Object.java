package net.arctics.clonk.parser;

import java.util.ArrayList;
import java.util.List;
import java.lang.ref.WeakReference;
import org.eclipse.core.resources.IResource;

public class C4Object {
	private String name;
	private C4ID id;
	private String fullName;
	private boolean rooted;
	
	private List<C4Function> definedFunctions = new ArrayList<C4Function>();
	private List<C4Variable> definedVariables = new ArrayList<C4Variable>();
	private List<C4Directive> definedDirectives = new ArrayList<C4Directive>();
	
	private IResource script;
	
	public IResource getScript() {
		return script;
	}
	
	public void setScript(IResource s) {
		script = s;
	}
	
//	private WeakReference<IResource> script;
//	
//	public IResource getScript() {
//		return script == null ? null : script.get();
//	}
//	
//	public void setScript(IResource value) {
//		if (getScript() != value) {
//			script = new WeakReference<IResource>(value);
//		}
//	}
	
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
	 * If present, the name declared in Names.txt, otherwise the filename without .c4d
	 * @return the name
	 */
	public String getName() {
		return name;
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
	
}
