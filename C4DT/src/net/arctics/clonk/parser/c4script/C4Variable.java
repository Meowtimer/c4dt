package net.arctics.clonk.parser.c4script;

import java.io.Serializable;
import java.security.InvalidParameterException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.text.IRegion;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.C4Engine;
import net.arctics.clonk.index.C4Object;
import net.arctics.clonk.index.C4ObjectIntern;
import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.C4ID;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.TypeExpectancyMode;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.ui.editors.c4script.IPostSerializable;

/**
 * Represents a variable.
 * @author ZokRadonh
 *
 */
public class C4Variable extends C4Declaration implements Serializable, ITypedDeclaration, IHasUserDescription {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	
	/**
	 * Scope (local, static or function-local)
	 */
	private C4VariableScope scope;
	
	/**
	 * Type of the variable.
	 */
	private IType type;
	
	/**
	 * Descriptive text meant for the user
	 */
	private String description;
	
	/**
	 * Explicit type, not to be changed by weird type inference
	 */
	private transient boolean typeLocked;
	
	/**
	 * Initialize expression for locals; not constant so saving value is not sufficient
	 */
	private Object initializationExpression;
	
	/**
	 * Whether the variable was used in some expression
	 */
	private boolean used;
	
	/**
	 * Variable object used as the special 'this' object.
	 */
	public static final C4Variable THIS = new C4Variable("this", C4Type.OBJECT, Messages.This_Description); //$NON-NLS-1$
	
	private C4Variable(String name, C4Type type, String desc) {
		this(name, type, desc, C4VariableScope.VAR);
		typeLocked = true;
	}
	
	public C4Variable(String name, IType type) {
		this.name = name;
		forceType(type);
	}
	
	public C4Variable(String name, C4VariableScope scope) {
		this.name = name;
		this.scope = scope;
		description = ""; //$NON-NLS-1$
		type = C4Type.UNKNOWN;
	}
	
	public C4Variable(String name, C4Type type, String desc, C4VariableScope scope) {
		this.name = name;
		this.type = type;
		this.description = desc;
		this.scope = scope;
	}

	public C4Variable() {
		name = ""; //$NON-NLS-1$
		scope = C4VariableScope.VAR;
	}
	
	public C4Variable(String name, String scope) {
		this(name,C4VariableScope.makeScope(scope));
	}

	public C4Variable(String name, ExprElm expr, C4ScriptParser context) {
		this(name, expr.getType(context));
		scope = C4VariableScope.VAR;
		setInitializationExpression(expr);
	}

	/**
	 * @return the type
	 */
	public IType getType() {
		if (type == null)
			type = C4Type.UNKNOWN;
		return type;
	}
	
	/**
	 * @param type the type to set
	 */
	public void forceType(IType type) {
		if (type == null)
			type = C4Type.UNKNOWN;
		C4ScriptBase script = getScript();
		if (script != null && script.getIndex() != null) {
			type = SerializableType.serializableTypeFrom(type, script.getIndex());
		}
		this.type = type;
	}
	
	public void forceType(IType type, boolean typeLocked) {
		forceType(type);
		this.typeLocked = typeLocked;
	}
	
	public void setType(IType type) {
		if (typeLocked)
			return;
		forceType(type);
	}

	/**
	 * @return the expectedContent
	 */
	public C4Object getObjectType() {
		return type instanceof SerializableType ? ((SerializableType)type).getObject() : null;
	}

	public C4ID getObjectID() {
		C4Object obj = getObjectType();
		return obj != null ? obj.getId() : null;
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
		return isEngineDeclaration() ? getEngine().descriptionFor(this) : description;
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

	public boolean isUsed() {
		return used;
	}
	
	public void setUsed(boolean used) {
		this.used = used;
	}
	
	/**
	 * The scope of a variable
	 * @author ZokRadonh
	 *
	 */
	public enum C4VariableScope implements Serializable {
		STATIC,
		LOCAL,
		VAR,
		CONST;
		
		public static C4VariableScope makeScope(String scopeString) {
			if (scopeString.equals(Keywords.VarNamed)) return C4VariableScope.VAR;
			if (scopeString.equals(Keywords.LocalNamed)) return C4VariableScope.LOCAL;
			if (scopeString.equals(Keywords.GlobalNamed)) return C4VariableScope.STATIC;
			if (scopeString.equals(Keywords.GlobalNamed + " " + Keywords.Const)) return C4VariableScope.CONST; //$NON-NLS-1$
			//if (C4VariableScope.valueOf(scopeString) != null) return C4VariableScope.valueOf(scopeString);
			else return null;
		}
		
		public String toKeyword() {
			switch (this) {
			case CONST:
				return Keywords.GlobalNamed + " " + Keywords.Const; //$NON-NLS-1$
			case STATIC:
				return Keywords.GlobalNamed;
			case LOCAL:
				return Keywords.LocalNamed;
			case VAR:
				return Keywords.VarNamed;
			default:
				return null;
			}
		}
	}
	
	public int sortCategory() {
		return (scope != null ? scope : C4VariableScope.VAR).ordinal();
	}

	private static String htmlerize(String text) {
		return text.
			replace("<", "&lt;"). //$NON-NLS-1$ //$NON-NLS-2$
			replace(">", "&gt;"). //$NON-NLS-1$ //$NON-NLS-2$
			replace("\n", " "). //$NON-NLS-1$ //$NON-NLS-2$
			replace("\t", " "); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	@Override
	public String getInfoText() {
		IType t = getType(); //getObjectType() != null ? getObjectType() : getType();
		String format = Messages.C4Variable_InfoTextFormatOverall;
		String valueFormat = scope == C4VariableScope.CONST
			? Messages.C4Variable_InfoTextFormatConstValue
			: Messages.C4Variable_InfoTextFormatDefaultValue;
		String descriptionFormat = Messages.C4Variable_InfoTextFormatUserDescription;
		return String.format(format,
			htmlerize((t == C4Type.UNKNOWN ? C4Type.ANY : t).typeName(false)),
			getName(),
			initializationExpression != null
				? String.format(valueFormat, htmlerize(initializationExpression.toString()))
				: "", //$NON-NLS-1$
			getUserDescription() != null && getUserDescription().length() > 0
				? String.format(descriptionFormat, getUserDescription())
				: "" //$NON-NLS-1$
		);
	}
	
	public void expectedToBeOfType(IType t, TypeExpectancyMode mode) {
		// engine objects should not be altered
		if (!typeLocked && !(getScript() instanceof C4Engine))
			ITypedDeclaration.Default.expectedToBeOfType(this, t);
	}

	public Object getDefaultValue() {
		return initializationExpression instanceof ExprElm ? null : initializationExpression;
	}

	public void setConstValue(Object constValue) {
		if (C4Type.typeFrom(constValue) == C4Type.ANY)
			throw new InvalidParameterException("constValue must be of primitive type recognized by C4Type"); //$NON-NLS-1$
		this.initializationExpression = constValue;
	}
	
	public ExprElm getInitializationExpression() {
		return initializationExpression instanceof ExprElm ? (ExprElm)initializationExpression : null;
	}
	
	public IRegion getInitializationExpressionLocation() {
		if (initializationExpression instanceof ExprElm) {
			return ((ExprElm)initializationExpression);
		} else {
			return null; // const value not sufficient
		}
	}
	
	public Object evaluateInitializationExpression(C4ScriptBase context) {
		ExprElm e = getInitializationExpression();
		if (e != null) {
			return e.evaluateAtParseTime(context);
		}
		return getDefaultValue();
	}
	
	public void setInitializationExpression(ExprElm initializationExpression) {
		this.initializationExpression = initializationExpression;
		if (initializationExpression != null) {
			initializationExpression.setAssociatedDeclaration(this);
		}
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
		return scope == C4VariableScope.STATIC || scope == C4VariableScope.CONST;
	}

	public boolean isAt(int offset) {
		return offset >= getLocation().getStart() && offset <= getLocation().getEnd();
	}

	public boolean isTypeLocked() {
		return typeLocked;
	}
	
	private void ensureTypeLockedIfPredefined(C4Declaration declaration) {
		if (!typeLocked && declaration instanceof C4Engine)
			typeLocked = true;
	}
	
	@Override
	public void setParentDeclaration(C4Declaration declaration) {
		super.setParentDeclaration(declaration);
		ensureTypeLockedIfPredefined(declaration);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void postSerialize(C4Declaration parent) {
		super.postSerialize(parent);
		ensureTypeLockedIfPredefined(parent);
		if (type instanceof SerializableType && parent instanceof C4ScriptBase) {
			((SerializableType)type).restoreType((C4ScriptBase) parent);
		}
		if (initializationExpression instanceof IPostSerializable) {
			((IPostSerializable<IPostSerializable<?>>)initializationExpression).postSerialize(this);
		}
	}

	/**
	 * Returns whether this variable is an actual explicitly declared parameter and not some crazy hack thingie like '...'
	 * @return look above and feel relieved that redundancy is lifted from you
	 */
	public boolean isActualParm() {
		return !getName().equals("..."); //$NON-NLS-1$
	}
	
	@Override
	public void sourceCodeRepresentation(StringBuilder builder, Object cookie) {
		builder.append(getScope().toKeyword());
		builder.append(" "); //$NON-NLS-1$
		builder.append(getName());
		builder.append(";"); //$NON-NLS-1$
	}
	
	@Override
	public Iterable<? extends C4Declaration> allSubDeclarations() {
		if (initializationExpression instanceof IHasSubDeclarations) {
			return ((IHasSubDeclarations)initializationExpression).allSubDeclarations();
		} else {
			return super.allSubDeclarations();
		}
	}
	
	public C4Function getFunction() {
		return getTopLevelParentDeclarationOfType(C4Function.class);
	}
	
	@Override
	public boolean typeIsInvariant() {
		return scope == C4VariableScope.CONST || typeLocked;
	}
	
}
