package net.arctics.clonk.parser;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.C4ScriptExprTree.ExprElm;
import net.arctics.clonk.parser.C4ScriptParser.Keywords;
import net.arctics.clonk.resource.ClonkProjectNature;

/**
 * Represents a variable.
 * @author ZokRadonh
 *
 */
public class C4Variable extends C4Declaration implements Serializable, ITypedField {

	private static final long serialVersionUID = -2350345359769750230L;
	private C4VariableScope scope;
	private C4Type type;
	private C4Object expectedContent; // mostly null - only set when type=object
	private String description;
	private boolean byRef; // array&
	private boolean typeLocked; // explicit type, not to be changed by assignments
	
	public static final C4Variable THIS = new C4Variable("this", "object", "reference to the object calling the function");
	
	/**
	 * Do NOT use this constructor! Its for engine-function-parameter only.
	 * @param name
	 * @param type
	 * @param desc
	 */
	public C4Variable(String name, String type, String desc) {
		this(name, C4Type.makeType(type), desc, C4VariableScope.VAR_VAR);
	}
	
	public C4Variable(String name, C4Type type) {
		this.name = name;
		this.type = type;
	}
	
	public C4Variable(String name, C4VariableScope scope) {
		this.name = name;
		this.scope = scope;
		expectedContent = null;
		description = "";
		type = null;
	}
	
	public C4Variable(String name, C4Type type, String desc, C4VariableScope scope) {
		this.name = name;
		this.type = type;
		this.description = desc;
		this.scope = scope;
		expectedContent = null;
	}
	
	@Override
	public C4Declaration latestVersion() {
		if (parentDeclaration instanceof C4Structure)
			return ((C4Structure)parentDeclaration).findVariable(getName());
		return super.latestVersion();
	}

	public C4Variable() {
		name = "";
		scope = C4VariableScope.VAR_VAR;
	}
	
	public C4Variable(String name, String scope) {
		this(name,C4VariableScope.makeScope(scope));
	}

	/**
	 * @return the type
	 */
	public C4Type getType() {
		if (type == null)
			type = C4Type.UNKNOWN;
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(C4Type type) {
		this.type = type;
	}
	
	public void setType(C4Type type, boolean typeLocked) {
		setType(type);
		this.typeLocked = typeLocked;
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
	public String getUserDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setUserDescription(String description) {
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
			if (scopeString.equals(Keywords.VarNamed)) return C4VariableScope.VAR_VAR;
			if (scopeString.equals(Keywords.LocalNamed)) return C4VariableScope.VAR_LOCAL;
			if (scopeString.equals(Keywords.GlobalNamed)) return C4VariableScope.VAR_STATIC;
			if (scopeString.equals("static const")) return C4VariableScope.VAR_CONST;
			//if (C4VariableScope.valueOf(scopeString) != null) return C4VariableScope.valueOf(scopeString);
			else return null;
		}
		
		public String toKeyword() {
			switch (this) {
			case VAR_CONST:
				return "static const";
			case VAR_STATIC:
				return Keywords.GlobalNamed;
			case VAR_LOCAL:
				return Keywords.LocalNamed;
			case VAR_VAR:
				return Keywords.VarNamed;
			default:
				return null;
			}
		}
	}
	
	public int sortCategory() {
		if (scope == null) return C4VariableScope.VAR_VAR.ordinal();
		return scope.ordinal();
	}
	
	@Override
	public String getShortInfo() {
		StringBuilder builder = new StringBuilder();
		builder.append(getType().toString());
		builder.append(" ");
		builder.append(getName());
		if (getUserDescription() != null && getUserDescription().length() > 0) {
			builder.append(": ");
			builder.append(getUserDescription());
		}
		return builder.toString();
	}

	public void inferTypeFromAssignment(ExprElm val, C4ScriptParser context) {
		if (typeLocked)
			return;
		ITypedField.Default.inferTypeFromAssignment(this, val, context);
	}
	
	public void expectedToBeOfType(C4Type t) {
		// engine objects should not be altered
		if (!typeLocked && getScript() != ClonkCore.getDefault().ENGINE_OBJECT)
			ITypedField.Default.expectedToBeOfType(this, t);
	}

	public boolean isByRef() {
		return byRef;
	}

	public void setByRef(boolean byRef) {
		this.byRef = byRef;
	}
	
	@Override
	public Object[] occurenceScope(ClonkProjectNature project) {
		if (parentDeclaration instanceof C4Function)
			return new Object[] {parentDeclaration};
		if (!isGlobal() && parentDeclaration instanceof C4ObjectIntern) {
			C4ObjectIntern obj = (C4ObjectIntern) parentDeclaration;
			ClonkIndex index = obj.getIndex();
			Set<Object> result = new HashSet<Object>();
			result.add(obj);
			for (C4Object o : index) {
				if (o.includes(obj)) {
					result.add(o);
				}
			}
			for (C4ScriptBase script : index.getIndexedScripts()) {
				if (script.includes(obj)) {
					result.add(script);
				}
			}
			// scenarios... unlikely
			return result.toArray();
		}
		return super.occurenceScope(project);
	}

	private boolean isGlobal() {
		return scope == C4VariableScope.VAR_STATIC || scope == C4VariableScope.VAR_CONST;
	}
	
}
