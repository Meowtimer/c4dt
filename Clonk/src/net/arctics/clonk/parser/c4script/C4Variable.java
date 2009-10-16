package net.arctics.clonk.parser.c4script;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.C4Object;
import net.arctics.clonk.index.C4ObjectIntern;
import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.C4ID;
import net.arctics.clonk.parser.C4Structure;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.ExprElm;
import net.arctics.clonk.resource.ClonkProjectNature;

/**
 * Represents a variable.
 * @author ZokRadonh
 *
 */
public class C4Variable extends C4Declaration implements Serializable, ITypedDeclaration, IHasUserDescription {

	private static final long serialVersionUID = -2350345359769750230L;
	
	/**
	 * scope (local, static or function-local)
	 */
	private C4VariableScope scope;
	
	/**
	 * type of variable
	 */
	private C4Type type;
	
	/**
	 * mostly null - only set when type=object
	 */
	private transient WeakReference<C4Object> objectType;
	
	/**
	 * persistent data used
	 */
	private C4ID objectID;
	
	/**
	 * descriptive text meant for the user
	 */
	private String description;
	
	/**
	 * array&
	 */
	private boolean byRef;
	
	/**
	 * explicit type, not to be changed by weird type inference
	 */
	private boolean typeLocked;
	
	/**
	 * variable object used as the special 'this' object
	 */
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
		objectType = null;
		description = "";
		type = null;
	}
	
	public C4Variable(String name, C4Type type, String desc, C4VariableScope scope) {
		this.name = name;
		this.type = type;
		this.description = desc;
		this.scope = scope;
		objectType = null;
	}
	
	@Override
	public C4Declaration latestVersion() {
		if (parentDeclaration instanceof C4Structure)
			return ((C4Structure)parentDeclaration).findDeclaration(getName(), C4Variable.class);
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
	public void forceType(C4Type type) {
		// -.-;
//		if (type == C4Type.DWORD) formerly DWORD
//			type = C4Type.INT;
		this.type = type;
	}
	
	public void forceType(C4Type type, boolean typeLocked) {
		forceType(type);
		this.typeLocked = typeLocked;
	}
	
	public void setType(C4Type type) {
		if (typeLocked)
			return;
		forceType(type);
	}

	/**
	 * @return the expectedContent
	 */
	public C4Object getObjectType() {
		return objectType != null ? objectType.get() : null;
	}

	/**
	 * @param objType the object type to set
	 */
	public void setObjectType(C4Object objType) {
		this.objectType = objType != null ? new WeakReference<C4Object>(objType) : null;
		this.objectID = objType != null ? objType.getId() : null;
	}

	public C4ID getObjectID() {
		return objectID;
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
	 * @param scope the scope to set
	 */
	public void setScope(C4VariableScope scope) {
		this.scope = scope;
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
			if (scopeString.equals(Keywords.GlobalNamed + " " + Keywords.Const)) return C4VariableScope.VAR_CONST;
			//if (C4VariableScope.valueOf(scopeString) != null) return C4VariableScope.valueOf(scopeString);
			else return null;
		}
		
		public String toKeyword() {
			switch (this) {
			case VAR_CONST:
				return Keywords.GlobalNamed + " " + Keywords.Const;
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
	public String getInfoText() {
		StringBuilder builder = new StringBuilder();
		builder.append((getType() == C4Type.UNKNOWN ? C4Type.ANY : getType()) .toString());
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
		ITypedDeclaration.Default.inferTypeFromAssignment(this, val, context);
	}
	
	public void expectedToBeOfType(C4Type t) {
		// engine objects should not be altered
		if (!typeLocked && getScript() != ClonkCore.getDefault().getEngineObject())
			ITypedDeclaration.Default.expectedToBeOfType(this, t);
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

	
	@Override
	public boolean isGlobal() {
		return scope == C4VariableScope.VAR_STATIC || scope == C4VariableScope.VAR_CONST;
	}

	public boolean isAt(int offset) {
		return offset >= getLocation().getStart() && offset <= getLocation().getEnd();
	}

	public boolean isTypeLocked() {
		return typeLocked;
	}
	
	private void ensureTypeLockedIfPredefined(C4Declaration declaration) {
		if (!typeLocked && declaration == ClonkCore.getDefault().getEngineObject())
			typeLocked = true;
	}
	
	@Override
	public void setParentDeclaration(C4Declaration declaration) {
		super.setParentDeclaration(declaration);
		ensureTypeLockedIfPredefined(declaration);
	}
	
	@Override
	public void postSerialize(C4Declaration parent) {
		super.postSerialize(parent);
		ensureTypeLockedIfPredefined(parent);
		if (objectID != null && parent instanceof C4ScriptBase) {
			C4ScriptBase script = (C4ScriptBase) parent;
			if (script.getResource() != null && script.getIndex() != null)
				setObjectType(script.getIndex().getObjectNearestTo(parent.getResource(), objectID));
		}
	}
	
}
