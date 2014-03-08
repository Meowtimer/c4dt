package net.arctics.clonk.c4script;

import static net.arctics.clonk.util.Utilities.as;

import java.io.Serializable;
import java.util.List;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodePrinter;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ast.IASTSection;
import net.arctics.clonk.ast.IEvaluationContext;
import net.arctics.clonk.c4script.ast.AccessVar;
import net.arctics.clonk.c4script.ast.Comment;
import net.arctics.clonk.c4script.ast.PropListExpression;
import net.arctics.clonk.c4script.ast.evaluate.IVariable;
import net.arctics.clonk.c4script.typing.IType;
import net.arctics.clonk.c4script.typing.ITypeable;
import net.arctics.clonk.c4script.typing.PrimitiveType;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.IIndexEntity;
import net.arctics.clonk.index.IVariableFactory;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.util.IHasUserDescription;
import net.arctics.clonk.util.StringUtil;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IRegion;

/**
 * Represents a variable.
 * @author ZokRadonh
 *
 */
public class Variable extends Declaration implements Serializable, ITypeable, IHasUserDescription, IEvaluationContext, Cloneable, IHasCode {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

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
	private boolean typePinned;

	/**
	 * Initialize expression for locals; not constant so saving value is not sufficient
	 */
	private ASTNode initializationExpression;

	/**
	 * Whether the variable was used in some expression
	 */
	private boolean used;

	/**
	 * Variable object used as the special 'this' object.
	 */
	public static final Variable THIS = new Variable("this", PrimitiveType.OBJECT, Messages.This_Description); //$NON-NLS-1$

	private Variable(final String name) { this.name = name; }

	private Variable(final String name, final IType type, final String desc) {
		this(name);
		this.type = type;
		this.description = desc;
		this.scope = Scope.VAR;
		this.typePinned = true;
	}

	public Variable(final String name, final ASTNode initialization) {
		this(name);
		initializationExpression = initialization;
	}

	public Variable(final String name, final IType type) {
		this(name);
		forceType(type);
	}

	public Variable(final String name, final Scope scope) {
		this(name);
		this.scope = scope;
		type = PrimitiveType.UNKNOWN;
	}

	public Variable() {
		this(""); //$NON-NLS-1$
		scope = Scope.VAR;
	}

	/**
	 * @return the type
	 */
	@Override
	public IType type() {
		if (type == null)
			type = PrimitiveType.UNKNOWN;
		return type;
	}

	public IType type(final Script script) {
		IType type = null;
		switch (scope()) {
		case LOCAL:
			if (script != null)
				type = script.typings().variableTypes.get(name());
			break;
		case PARAMETER:
			if (script != null) {
				final Function f = parent(Function.class);
				final Function.Typing t = script.typings().functionTypings.get(f.name());
				final int ndx = this.parameterIndex();
				if (t != null && t.parameterTypes.length > ndx)
					type = t.parameterTypes[ndx];
			}
			break;
		default:
			break;
		}
		return type != null ? type : type();
	}

	/**
	 * @param type the type to set
	 */
	@Override
	public void forceType(IType type) {
		if (type == null)
			type = PrimitiveType.UNKNOWN;
		this.type = type;
	}

	public void forceType(final IType type, final boolean typeLocked) {
		forceType(type);
		this.typePinned = typeLocked;
	}

	public void pinType() { typePinned = true; }
	public void assignType(final IType type) { assignType(type, false); }

	@Override
	public void assignType(final IType type, final boolean pin) {
		if (!typePinned || pin) {
			forceType(type);
			typePinned = pin;
		}
	}

	/**
	 * @return the scope
	 */
	public Scope scope() { return scope; }

	/**
	 * @return the description
	 */
	@Override
	public String obtainUserDescription() {
		if (isEngineDeclaration())
			return engine().obtainDescription(this);
		else
			return description;
	}

	@Override
	public String userDescription() { return description; }
	/**
	 * @param description the description to set
	 */
	@Override
	public void setUserDescription(final String description) { this.description = description; }
	/**
	 * @param scope the scope to set
	 */
	public void setScope(final Scope scope) { this.scope = scope; }
	public boolean isUsed() { return used; }
	public void setUsed(final boolean used) { this.used = used; }

	/**
	 * The scope of a variable
	 * @author ZokRadonh
	 */
	public enum Scope implements Serializable {
		STATIC,
		LOCAL,
		VAR,
		CONST,
		PARAMETER;

		public static Scope makeScope(final String scopeString) {
			switch (scopeString) {
			case Keywords.VarNamed:
				return Scope.VAR;
			case Keywords.LocalNamed:
				return Scope.LOCAL;
			case Keywords.GlobalNamed:
				return Scope.STATIC;
			case Keywords.GlobalNamed + " " + Keywords.Const:
				return Scope.CONST;
			default:
				return null;
			}
		}

		public String toKeyword() {
			switch (this) {
			case CONST:
				return Keywords.GlobalNamed + " " + Keywords.Const; //$NON-NLS-1$
			case STATIC:
				return Keywords.GlobalNamed;
			case LOCAL:
				return Keywords.LocalNamed;
			default:
				return Keywords.VarNamed;
			}
		}

		public boolean isLocal() {
			switch (this) {
			case PARAMETER: case VAR:
				return true;
			default:
				return false;
			}
		}
	}

	@Override
	public int sortCategory() { return (scope != null ? scope : Scope.VAR).ordinal(); }

	@Override
	public String infoText(final IIndexEntity context) {
		return infoText(type(as(context, Script.class)));
	}

	public String infoText(final IType type) {
		final String format = Messages.C4Variable_InfoTextFormatOverall;
		final String valueFormat = scope == Scope.CONST
			? Messages.C4Variable_InfoTextFormatConstValue
			: Messages.C4Variable_InfoTextFormatDefaultValue;
		final String descriptionFormat = Messages.C4Variable_InfoTextFormatUserDescription;
		return String.format(format,
			StringUtil.htmlerize((type == PrimitiveType.UNKNOWN ? PrimitiveType.ANY : type).typeName(true)),
				name(),
				initializationExpression != null
					? String.format(valueFormat, initializationExpression.toString())
					: "", //$NON-NLS-1$
				obtainUserDescription() != null && obtainUserDescription().length() > 0
					? String.format(descriptionFormat, obtainUserDescription())
					: "" //$NON-NLS-1$
		);
	}

	@Override
	public String displayString(final IIndexEntity context) { return this.name(); }

	public ASTNode initializationExpression() { return initializationExpression; }

	public IRegion initializationExpressionLocation() {
		if (initializationExpression instanceof ASTNode)
			return initializationExpression;
		else
			return null; // const value not sufficient
	}

	public Object evaluateInitializationExpression(final IEvaluationContext context) {
		final ASTNode e = initializationExpression();
		if (e != null)
			return e.evaluateStatic(context);
		else
			return null;
	}

	public void setInitializationExpression(final ASTNode initializationExpression) {
		this.initializationExpression = initializationExpression;
	}

	@Override
	public boolean isGlobal() { return scope == Scope.STATIC || scope == Scope.CONST; }

	private void ensureTypeLockedIfPredefined(final ASTNode declaration) {
		if (!typePinned && declaration instanceof Engine)
			typePinned = true;
	}

	@Override
	public void setParent(final ASTNode declaration) {
		super.setParent(declaration);
		ensureTypeLockedIfPredefined(declaration);
	}

	@Override
	public void postLoad(final Declaration parent, final Index root) {
		super.postLoad(parent, root);
		ensureTypeLockedIfPredefined(parent);
		if (initializationExpression != null)
			initializationExpression.postLoad(this);
		if (initializationExpression instanceof PropListExpression)
			((PropListExpression)initializationExpression).definedDeclaration().postLoad(this, root);
	}

	/**
	 * Returns whether this variable is an actual explicitly declared parameter and not some crazy hack thingie like '...'
	 * @return look above and feel relieved that redundancy is lifted from you
	 */
	public boolean isActualParm() { return !name().equals("..."); } //$NON-NLS-1$
	@Override
	public IASTSection section() { return null; /* variables are always absolute because of the reasons */ }

	@Override
	public void doPrint(final ASTNodePrinter output, final int depth) {
		output.append(scope().toKeyword());
		output.append(" "); //$NON-NLS-1$
		output.append(name());
		final ASTNode init = initializationExpression();
		if (init != null) {
			output.append(" = ");
			init.print(output, depth);
		}
		output.append(";"); //$NON-NLS-1$
		Comment.printUserDescription(output, depth, userDescription(), false);
	}

	@Override
	public List<? extends Declaration> subDeclarations(final Index contextIndex, final int mask) {
		if (initializationExpression instanceof Declaration)
			return ((Declaration)initializationExpression).subDeclarations(contextIndex, mask);
		else
			return super.subDeclarations(contextIndex, mask);
	}

	/**
	 * Return the function this variable was declared in. This also applies for variables declared inside proplist expressions inside functions.
	 * @return The function or null if there is no function in the parent chain.
	 */
	@Override
	public Function function() { return topLevelParent(Function.class); }
	@Override
	public boolean staticallyTyped() { return typePinned || isEngineDeclaration(); }
	@Override
	public Object self() { return null; }
	@Override
	public IVariable variable(final AccessVar access, final Object obj) { return script().variable(access, null); }
	@Override
	public Object[] arguments() { return new Object[0]; }
	@Override
	public int codeFragmentOffset() { return 0; }
	@Override
	public void reportOriginForExpression(final ASTNode expression, final IRegion location, final IFile file) {}
	@Override
	public ASTNode code() { return initializationExpression(); }
	@Override
	public ASTNode[] subElements() { return super.subElements(); }
	@Override
	public boolean isLocal() { return scope.isLocal(); }

	/**
	 * Return the parameter index of this variable if it is a function parameter.
	 * @return Return the parameter index or -1 if the variable is not a function parameter.
	 */
	public int parameterIndex() {
		if (parent instanceof Function) {
			int i = 0;
			for (final Variable v : ((Function)parent).parameters())
				if (v == this)
					return i;
				else
					i++;
		}
		return -1;
	}

	@Override
	public Variable clone() {
		final Variable clone = new Variable();
		clone.description = this.description;
		clone.initializationExpression = this.initializationExpression != null ? this.initializationExpression.clone() : null;
		clone.setLocation(this);
		clone.name = this.name;
		clone.scope = this.scope;
		clone.type = this.type;
		return clone;
	}

	@Override
	public Object[] occurenceScope(final Iterable<Index> indexes) {
		if (parent instanceof Function)
			return new Object[] {parent};
		else
			return super.occurenceScope(indexes);
	}

	public static final IVariableFactory DEFAULT_VARIABLE_FACTORY = new IVariableFactory() {
		@Override
		public Variable newVariable(final String varName, final Scope scope) {
			return new Variable(varName, scope);
		}
	};

}
