package net.arctics.clonk.parser.c4script;

import java.io.Serializable;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.IHasSubDeclarations;
import net.arctics.clonk.index.IIndexEntity;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.PropListExpression;
import net.arctics.clonk.parser.c4script.ast.TypeChoice;
import net.arctics.clonk.parser.c4script.ast.TypingJudgementMode;
import net.arctics.clonk.parser.c4script.ast.evaluate.IEvaluationContext;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.util.IHasUserDescription;
import net.arctics.clonk.util.IPredicate;
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
	private transient boolean staticallyTyped;
	
	/**
	 * Initialize expression for locals; not constant so saving value is not sufficient
	 */
	private ExprElm initializationExpression;
	
	/**
	 * Whether the variable was used in some expression
	 */
	private boolean used;
	
	/**
	 * Variable object used as the special 'this' object.
	 */
	public static final Variable THIS = new Variable("this", PrimitiveType.OBJECT, Messages.This_Description); //$NON-NLS-1$
	
	private Variable(String name, IType type, String desc) {
		this(name, type, desc, Scope.VAR);
		staticallyTyped = true;
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
		this(name, expr.type(context));
		scope = Scope.VAR;
		setInitializationExpression(expr);
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
	
	/**
	 * @param type the type to set
	 */
	@Override
	public void forceType(IType type) {
		if (type == null)
			type = PrimitiveType.UNKNOWN;
		if (this.scope == Scope.PARAMETER)
			type = TypeChoice.remove(type, new IPredicate<IType>() {
				@Override
				public boolean test(IType item) {
					return item instanceof ParameterType && ((ParameterType)item).parameter() == Variable.this;
				}
			});
		this.type = type;
	}
	
	public void forceType(IType type, boolean typeLocked) {
		forceType(type);
		this.staticallyTyped = typeLocked;
	}
	
	public void lockType() {
		staticallyTyped = true;
	}
	
	public void assignType(IType type) {
		assignType(type, false);
	}
	
	@Override
	public void assignType(IType type, boolean _static) {
		if (!staticallyTyped || _static) {
			forceType(type);
			staticallyTyped = _static;
		}
	}

	/**
	 * @return the scope
	 */
	public Scope scope() {
		return scope;
	}
	
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
	public String userDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	@Override
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
		CONST,
		PARAMETER;
		
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

		public final boolean isLocal() {
			switch (this) {
			case PARAMETER: case VAR:
				return true;
			default:
				return false;
			}
		}
	}
	
	@Override
	public int sortCategory() {
		return (scope != null ? scope : Scope.VAR).ordinal();
	}
	
	@Override
	public String infoText(IIndexEntity context) {
		IType t = TypeUtil.resolve(type(), context, this);
		String format = Messages.C4Variable_InfoTextFormatOverall;
		String valueFormat = scope == Scope.CONST
			? Messages.C4Variable_InfoTextFormatConstValue
			: Messages.C4Variable_InfoTextFormatDefaultValue;
		String descriptionFormat = Messages.C4Variable_InfoTextFormatUserDescription;
		return String.format(format,
			StringUtil.htmlerize((t == PrimitiveType.UNKNOWN ? PrimitiveType.ANY : t).typeName(true)),
				name(),
				initializationExpression != null
					? String.format(valueFormat, StringUtil.htmlerize(initializationExpression.toString()))
					: "", //$NON-NLS-1$
				obtainUserDescription() != null && obtainUserDescription().length() > 0
					? String.format(descriptionFormat, obtainUserDescription())
					: "" //$NON-NLS-1$
		);
	}
	
	@Override
	public String displayString(IIndexEntity context) {
		return this.name();
	}
	
	@Override
	public void expectedToBeOfType(IType t, TypingJudgementMode mode) {
		// engine objects should not be altered
		if (!staticallyTyped && !(script() instanceof Engine))
			ITypeable.Default.expectedToBeOfType(this, t);
	}
	
	public ExprElm initializationExpression() {
		return initializationExpression;
	}
	
	public IRegion initializationExpressionLocation() {
		if (initializationExpression instanceof ExprElm)
			return initializationExpression;
		else
			return null; // const value not sufficient
	}
	
	public Object evaluateInitializationExpression(IEvaluationContext context) {
		ExprElm e = initializationExpression();
		if (e != null)
			return e.evaluateAtParseTime(context);
		else
			return null;
	}
	
	public void setInitializationExpression(ExprElm initializationExpression) {
		this.initializationExpression = initializationExpression;
		if (initializationExpression != null)
			initializationExpression.setAssociatedDeclaration(this);
	}

	@Override
	public boolean isGlobal() {
		return scope == Scope.STATIC || scope == Scope.CONST;
	}
	
	public boolean isAt(int offset) {
		return location() != null && offset >= location().start() && offset <= location().end();
	}
	
	private void ensureTypeLockedIfPredefined(Declaration declaration) {
		if (!staticallyTyped && declaration instanceof Engine)
			staticallyTyped = true;
	}
	
	@Override
	public void setParentDeclaration(Declaration declaration) {
		super.setParentDeclaration(declaration);
		ensureTypeLockedIfPredefined(declaration);
	}
	
	@Override
	public void postLoad(Declaration parent, Index root) {
		super.postLoad(parent, root);
		ensureTypeLockedIfPredefined(parent);
		if (initializationExpression != null)
			initializationExpression.postLoad(null, declarationObtainmentContext());
		if (initializationExpression instanceof PropListExpression)
			((PropListExpression)initializationExpression).definedDeclaration().postLoad(this, root);
	}

	/**
	 * Returns whether this variable is an actual explicitly declared parameter and not some crazy hack thingie like '...'
	 * @return look above and feel relieved that redundancy is lifted from you
	 */
	public boolean isActualParm() {
		return !name().equals("..."); //$NON-NLS-1$
	}
	
	@Override
	public void sourceCodeRepresentation(StringBuilder builder, Object cookie) {
		builder.append(scope().toKeyword());
		builder.append(" "); //$NON-NLS-1$
		builder.append(name());
		builder.append(";"); //$NON-NLS-1$
	}
	
	@Override
	public Iterable<? extends Declaration> subDeclarations(Index contextIndex, int mask) {
		if (initializationExpression instanceof IHasSubDeclarations)
			return ((IHasSubDeclarations)initializationExpression).subDeclarations(contextIndex, mask);
		else
			return super.subDeclarations(contextIndex, mask);
	}
	
	/**
	 * Return the function this variable was declared in. This also applies for variables declared inside proplist expressions inside functions.
	 * @return The function or null if there is no function in the parent chain.
	 */
	@Override
	public Function function() {
		return topLevelParentDeclarationOfType(Function.class);
	}
	
	@Override
	public boolean staticallyTyped() {
		return staticallyTyped || isEngineDeclaration();
	}
	
	/**
	 * Return the parameter index of this variable if it is a function parameter.
	 * @return Return the parameter index or -1 if the variable is not a function parameter.
	 */
	public int parameterIndex() {
		if (parentDeclaration instanceof Function) {
			int i = 0;
			for (Variable v : ((Function)parentDeclaration).parameters())
				if (v == this)
					return i;
				else
					i++;
		}
		return -1;
	}

	@Override
	public Object valueForVariable(String varName) {
		return script().findLocalVariable(varName, true);
	}

	@Override
	public Object[] arguments() {
		return new Object[0];
	}

	@Override
	public int codeFragmentOffset() {
		// for some reason, initialization expression locations are stored absolutely
		return 0; // return initializationExpression != null ? initializationExpression.getExprStart() : 0;
	}

	@Override
	public void reportOriginForExpression(ExprElm expression, IRegion location, IFile file) {
		// wow
	}
	
	@Override
	public Variable clone() {
		Variable clone = new Variable();
		clone.description = this.description;
		clone.initializationExpression = this.initializationExpression != null ? this.initializationExpression.clone() : null;
		clone.location = this.location.clone();
		clone.name = this.name;
		clone.scope = this.scope;
		clone.type = this.type;
		return clone;
	}

	public void initializeFromAssignment(ExprElm referee, ExprElm expression, DeclarationObtainmentContext context) {
		IType type = expression.type(context);
		expectedToBeOfType(type, TypingJudgementMode.Expect);
		setLocation(context.absoluteSourceLocationFromExpr(referee));
		forceType(type);
		setInitializationExpression(expression);
	}
	
	@Override
	public boolean isLocal() { return scope.isLocal(); }
	
	@Override
	public Object[] occurenceScope(ClonkProjectNature project) {
		if (parentDeclaration instanceof Function)
			return new Object[] {parentDeclaration};
		else
			return super.occurenceScope(project);
	}
	
	@Override
	public ExprElm code() {
		return initializationExpression();
	}
	
	public IType parameterType() {
		return scope == Scope.PARAMETER && type() == PrimitiveType.UNKNOWN
			? new ParameterType(this)
			: type(); 
	}
	
}
