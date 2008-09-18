package net.arctics.clonk.parser;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class C4Function extends C4Field implements Serializable {

	private static final long serialVersionUID = 3848213897251037684L;
	private C4FunctionScope visibility; 
	private List<C4Variable> localVars;
	private C4Object parentObject;
	private List<C4Variable> parameter;
	private C4Type returnType;
	private String description;
	private boolean isCallback;
	private SourceLocation body;
	
	/**
	 * Do NOT use this constructor! Its for engine-functions only.
	 * @param name
	 * @param type
	 * @param desc
	 * @param pars
	 */
	public C4Function(String name, String type, String desc, C4Variable[] pars) {
		this.name = name;
		returnType = C4Type.makeType(type);
		description = desc;
		parameter = new ArrayList<C4Variable>(pars.length);
		for(C4Variable var : pars) {
			parameter.add(var);
		}
		parentObject = null; // since engine function only
		visibility = C4FunctionScope.FUNC_GLOBAL;
		localVars = null;
	}
	
	public C4Function() {
	}
	
	public C4Function(String name, C4Object parent, C4FunctionScope scope) {
		this.name = name;
		parentObject = parent;
		visibility = scope;
		parameter = new ArrayList<C4Variable>();
		localVars = new ArrayList<C4Variable>();
	}
	
	public C4Function(String name, C4Object parent, String scope) {
		this(name,parent,C4FunctionScope.makeScope(scope));
	}
	
	/**
	 * @return the localVars
	 */
	public List<C4Variable> getLocalVars() {
		return localVars;
	}

	/**
	 * @param localVars the localVars to set
	 */
	public void setLocalVars(List<C4Variable> localVars) {
		this.localVars = localVars;
	}

	/**
	 * @return the parameter
	 */
	public List<C4Variable> getParameter() {
		return parameter;
	}

	/**
	 * @param parameter the parameter to set
	 */
	public void setParameter(List<C4Variable> parameter) {
		this.parameter = parameter;
	}

	/**
	 * @return the returnType
	 */
	public C4Type getReturnType() {
		return returnType;
	}

	/**
	 * @param returnType the returnType to set
	 */
	public void setReturnType(C4Type returnType) {
		this.returnType = returnType;
	}

	/**
	 * @return the visibility
	 */
	public C4FunctionScope getVisibility() {
		return visibility;
	}

	/**
	 * @return the parentObject
	 */
	public C4Object getParentObject() {
		return parentObject;
	}
	
	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @param visibility the visibility to set
	 */
	public void setVisibility(C4FunctionScope visibility) {
		this.visibility = visibility;
	}

	/**
	 * @param parentObject the parentObject to set
	 */
	public void setParentObject(C4Object parentObject) {
		this.parentObject = parentObject;
	}

	/**
	 * The scope of a function.
	 * @author ZokRadonh
	 *
	 */
	public enum C4FunctionScope implements Serializable {
		FUNC_GLOBAL,
		FUNC_PUBLIC,
		FUNC_PROTECTED,
		FUNC_PRIVATE;
		
		public static C4FunctionScope makeScope(String scopeString) {
			if (scopeString == null) return C4FunctionScope.FUNC_PUBLIC;
			if (scopeString.equalsIgnoreCase("public")) return C4FunctionScope.FUNC_PUBLIC;
			if (scopeString.equalsIgnoreCase("protected")) return C4FunctionScope.FUNC_PROTECTED;
			if (scopeString.equalsIgnoreCase("private")) return C4FunctionScope.FUNC_PRIVATE;
			if (scopeString.equalsIgnoreCase("global")) return C4FunctionScope.FUNC_GLOBAL;
			return C4FunctionScope.FUNC_PUBLIC;
		}
	}
	
	/**
	 * Generates a function string in the form of
	 * function(int parName1, int parName2)
	 * if withFuncName is true, else
	 * int parName1, int parName2
	 * 
	 * @param withFuncName
	 * @return
	 */
	public String getLongParameterString(boolean withFuncName) {
		StringBuilder string = new StringBuilder();
		if (withFuncName) {
			string.append(getName());
			string.append("(");
		}
		if (getParameter().size() > 0) {
			for(C4Variable par : getParameter()) {
				if (par.getType() != C4Type.UNKNOWN && par.getType() != null) {
					string.append(par.getType().toString());
					string.append(' ');
					string.append(par.getName());
					string.append(',');
					string.append(' ');
				}
			}
			if (string.length() > 0) {
				if (string.charAt(string.length() - 1) == ' ') {
					string.delete(string.length() - 2,string.length());
				}
			}
		}
		if (withFuncName) string.append(")");
		return string.toString();
	}

	/**
	 * @param isCallback the isCallback to set
	 */
	public void setCallback(boolean isCallback) {
		this.isCallback = isCallback;
	}

	/**
	 * @return the isCallback
	 */
	public boolean isCallback() {
		return isCallback;
	}

	/**
	 * @param body the body to set
	 */
	public void setBody(SourceLocation body) {
		this.body = body;
	}

	/**
	 * @return the body
	 */
	public SourceLocation getBody() {
		return body;
	}
	
	public int sortCategory() {
		return C4Variable.C4VariableScope.values().length + visibility.ordinal();
	}
	
}
