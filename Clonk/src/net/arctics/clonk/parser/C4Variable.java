package net.arctics.clonk.parser;

import java.io.Serializable;

/**
 * Represents a variable.
 * @author ZokRadonh
 *
 */
public class C4Variable extends C4Field implements Serializable {

	private static final long serialVersionUID = -2350345359769750230L;
	private C4VariableScope scope;
	private C4Type type;
	private C4Object expectedContent; // mostly null - only set when type=object
	private String description;
	
	/**
	 * Do NOT use this constructor! Its for engine-function-parameter only.
	 * @param name
	 * @param type
	 * @param desc
	 */
	public C4Variable(String name, String type, String desc) {
		this.name = name;
		this.type = C4Type.makeType(type);
		description = desc;
		scope = C4VariableScope.VAR_VAR;
		expectedContent = null;
	}
	
	public C4Variable(String name, C4VariableScope scope) {
		this.name = name;
		this.scope = scope;
		expectedContent = null;
		description = "";
		type = null;
	}
	
	public C4Variable() {
		
	}
	
	public C4Variable(String name, String scope) {
		this(name,C4VariableScope.makeScope(scope));
	}

	/**
	 * @return the type
	 */
	public C4Type getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(C4Type type) {
		this.type = type;
	}

	/**
	 * @return the expectedContent
	 */
	public C4Object getExpectedContent() {
		return expectedContent;
	}

	/**
	 * @param expectedContent the expectedContent to set
	 */
	public void setExpectedContent(C4Object expectedContent) {
		this.expectedContent = expectedContent;
	}

	/**
	 * @return the scope
	 */
	public C4VariableScope getScope() {
		return scope;
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
	 * @param scope the scope to set
	 */
	public void setScope(C4VariableScope scope) {
		this.scope = scope;
	}
	
	/**
	 * generates a string describing the variable (including name and type)
	 */
	public String getAdditionalProposalInfo() {
		return getName() + "  (" + (getType() != null ? getType().toString() : "any") + ")" + (description != null && description.length() > 0 ? (": " + description) : "");
	}

	/**
	 * The scope of a variable
	 * @author ZokRadonh
	 *
	 */
	public enum C4VariableScope implements Serializable {
		VAR_STATIC,
		VAR_LOCAL,
		VAR_VAR,
		VAR_CONST;
		
		public static C4VariableScope makeScope(String scopeString) {
			if (scopeString.equalsIgnoreCase("var")) return C4VariableScope.VAR_VAR;
			if (scopeString.equalsIgnoreCase("local")) return C4VariableScope.VAR_LOCAL;
			if (scopeString.equalsIgnoreCase("static")) return C4VariableScope.VAR_STATIC;
			if (scopeString.equalsIgnoreCase("static const")) return C4VariableScope.VAR_CONST;
			else return null;
		}
	}
	
	public int sortCategory() {
		return scope.ordinal();
	}
	
}
