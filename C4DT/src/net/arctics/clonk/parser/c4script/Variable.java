package net.arctics.clonk.parser.c4script;

import java.io.Serializable;
import java.security.InvalidParameterException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.text.IRegion;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.ProjectDefinition;
import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.ID;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.PropListExpression;
import net.arctics.clonk.parser.c4script.ast.TypeExpectancyMode;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.ui.editors.c4script.IPostSerializable;
import net.arctics.clonk.util.Utilities;

/**
 * Represents a variable.
 * @author ZokRadonh
 *
 */
public class Variable extends Declaration implements Serializable, ITypedDeclaration, IHasUserDescription {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	
	/**
	 * Scope (local, static or function-local)
	 */
	private Scope scope;
	
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
	public static final Variable THIS = new Variable("this", PrimitiveType.OBJECT, Messages.This_Description); //$NON-NLS-1$
	
	private Variable(String name, PrimitiveType type, String desc) {
		this(name, type, desc, Scope.VAR);
		typeLocked = true;
	}
	
	public Variable(String name, IType type) {
		this.name = name;
		forceType(type);
	}
	
	public Variable(String name, Scope scope) {
		this.name = name;
		this.scope = scope;
		description = ""; //$NON-NLS-1$
		type = PrimitiveType.UNKNOWN;
	}
	
	public Variable(String name, IType type, String desc, Scope scope) {
		this.name = name;
		this.type = type;
		this.description = desc;
		this.scope = scope;
	}

	public Variable() {
		name = ""; //$NON-NLS-1$
		scope = Scope.VAR;
	}
	
	public Variable(String name, String scope) {
		this(name,Scope.makeScope(scope));
	}

	public Variable(String name, ExprElm expr, C4ScriptParser context) {
		this(name, expr.getType(context));
		scope = Scope.VAR;
		setInitializationExpression(expr);
	}

	/**
	 * @return the type
	 */
	public IType getType() {
		if (type == null)
			type = PrimitiveType.UNKNOWN;
		return type;
	}
	
	/**
	 * @param type the type to set
	 */
	public void forceType(IType type) {
		if (type == null)
			type = PrimitiveType.UNKNOWN;
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
	public Definition getObjectType() {
		for (IType t : type) {
			if (t instanceof Definition) {
				return (Definition)t;
			}
		}
		return null;
	}

	public ID getObjectID() {
		Definition obj = getObjectType();
		return obj != null ? obj.getId() : null;
	}

	/**
	 * @return the scope
	 */
	public Scope getScope() {
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
	public void setScope(Scope scope) {
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
	public enum Scope implements Serializable {
		STATIC,
		LOCAL,
		VAR,
		CONST;
		
		public static Scope makeScope(String scopeString) {
			if (scopeString.equals(Keywords.VarNamed)) return Scope.VAR;
			if (scopeString.equals(Keywords.LocalNamed)) return Scope.LOCAL;
			if (scopeString.equals(Keywords.GlobalNamed)) return Scope.STATIC;
			if (scopeString.equals(Keywords.GlobalNamed + " " + Keywords.Const)) return Scope.CONST; //$NON-NLS-1$
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
		return (scope != null ? scope : Scope.VAR).ordinal();
	}
	
	@Override
	public String getInfoText() {
		IType t = getType(); //getObjectType() != null ? getObjectType() : getType();
		String format = Messages.C4Variable_InfoTextFormatOverall;
		String valueFormat = scope == Scope.CONST
			? Messages.C4Variable_InfoTextFormatConstValue
			: Messages.C4Variable_InfoTextFormatDefaultValue;
		String descriptionFormat = Messages.C4Variable_InfoTextFormatUserDescription;
		return String.format(format,
			Utilities.htmlerize((t == PrimitiveType.UNKNOWN ? PrimitiveType.ANY : t).typeName(false)),
			getName(),
			initializationExpression != null
				? String.format(valueFormat, Utilities.htmlerize(initializationExpression.toString()))
				: "", //$NON-NLS-1$
			getUserDescription() != null && getUserDescription().length() > 0
				? String.format(descriptionFormat, getUserDescription())
				: "" //$NON-NLS-1$
		);
	}
	
	public void expectedToBeOfType(IType t, TypeExpectancyMode mode) {
		// engine objects should not be altered
		if (!typeLocked && !(getScript() instanceof Engine))
			ITypedDeclaration.Default.expectedToBeOfType(this, t);
	}

	public Object getDefaultValue() {
		return initializationExpression instanceof ExprElm ? null : initializationExpression;
	}

	public void setConstValue(Object constValue) {
		if (PrimitiveType.typeFrom(constValue) == PrimitiveType.ANY)
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
	
	public Object evaluateInitializationExpression(ScriptBase context) {
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
		if (parentDeclaration instanceof Function)
			return new Object[] {parentDeclaration};
		if (!isGloballyAccessible() && parentDeclaration instanceof ProjectDefinition) {
			ProjectDefinition obj = (ProjectDefinition) parentDeclaration;
			ClonkIndex index = obj.getIndex();
			Set<Object> result = new HashSet<Object>();
			result.add(obj);
			for (Definition o : index) {
				if (o.includes(obj)) {
					result.add(o);
				}
			}
			for (ScriptBase script : index.getIndexedScripts()) {
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
		return scope == Scope.STATIC || scope == Scope.CONST;
	}
	
	/**
	 * Returns whether references to this declaration might exist from everywhere
	 * @return The above
	 */
	public boolean isGloballyAccessible() {
		return scope == Scope.LOCAL || parentDeclaration instanceof ProplistDeclaration || isGlobal();
	}

	public boolean isAt(int offset) {
		return offset >= getLocation().getStart() && offset <= getLocation().getEnd();
	}

	public boolean isTypeLocked() {
		return typeLocked;
	}
	
	private void ensureTypeLockedIfPredefined(Declaration declaration) {
		if (!typeLocked && declaration instanceof Engine)
			typeLocked = true;
	}
	
	@Override
	public void setParentDeclaration(Declaration declaration) {
		super.setParentDeclaration(declaration);
		ensureTypeLockedIfPredefined(declaration);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void postSerialize(Declaration parent, ClonkIndex root) {
		super.postSerialize(parent, root);
		ensureTypeLockedIfPredefined(parent);
		if (initializationExpression instanceof IPostSerializable) {
			((IPostSerializable<ExprElm, DeclarationObtainmentContext>)initializationExpression).postSerialize(null, getDeclarationObtainmentContext());
		}
		if (initializationExpression instanceof PropListExpression) {
			((PropListExpression)initializationExpression).getDefinedDeclaration().postSerialize(this, root);
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
	public Iterable<? extends Declaration> allSubDeclarations(int mask) {
		if (initializationExpression instanceof IHasSubDeclarations) {
			return ((IHasSubDeclarations)initializationExpression).allSubDeclarations(mask);
		} else {
			return super.allSubDeclarations(mask);
		}
	}
	
	/**
	 * Return the function this variable was declared in. This also applies for variables declared inside proplist expressions inside functions.
	 * @return The function or null if there is no function in the parent chain.
	 */
	public Function getFunction() {
		return getTopLevelParentDeclarationOfType(Function.class);
	}
	
	@Override
	public boolean typeIsInvariant() {
		return scope == Scope.CONST || typeLocked;
	}
	
	/**
	 * Return the parameter index of this variable if it is a function parameter.
	 * @return Return the parameter index or -1 if the variable is not a function parameter.
	 */
	public int parameterIndex() {
		if (parentDeclaration instanceof Function) {
			int i = 0;
			for (Variable v : ((Function)parentDeclaration).getParameters())
				if (v == this)
					return i;
				else
					i++;
		}
		return -1;
	}
	
}
