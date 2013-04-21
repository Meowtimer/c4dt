package net.arctics.clonk.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.c4script.Variable.Scope;
import net.arctics.clonk.c4script.ast.evaluate.EvaluationContextProxy;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.IEvaluationContext;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IRegion;

/**
 * Variable name expression element.
 * @author madeen
 *
 */
public class AccessVar extends AccessDeclaration {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	public AccessVar(String varName) {
		super(varName);
	}

	public AccessVar(Declaration declaration) {
		this(declaration.name());
		this.declaration = declaration;
	}
	
	public static AccessVar temp(Declaration declaration, ASTNode parent) {
		final AccessVar r = new AccessVar(declaration);
		r.setParent(parent);
		return r;
	}

	@Override
	public boolean isValidInSequence(ASTNode predecessor) {
		return
			// either null or
			predecessor == null ||
			// normally, a check would be performed whether the MemberOperator uses '.' instead of '->'
			// but in order to avoid the case where writing obj->StartOfFuncName is interpreted as
			// two not-properly-finished statements (obj->; and StartOfFuncName;) and then having
			// the StartOfFuncName; statement be replaced by the function call which will then have no
			// parameter information since the function only exists in the definition of obj that is not
			// consulted in that case, the '.' rule is enforced not here but in reportErrors (!!1)
			// and anyways, everything is different now that function values are possible yadda
			predecessor instanceof MemberOperator;
	}

	private static Definition definitionProxiedBy(Declaration var) {
		if (var instanceof Definition.ProxyVar)
			return ((Definition.ProxyVar)var).definition();
		else
			return null;
	}

	public Definition proxiedDefinition() {
		return definitionProxiedBy(declaration());
	}

	@Override
	public Object evaluateStatic(final IEvaluationContext context) {
		Definition obj;
		if (declaration instanceof Variable) {
			final Variable var = (Variable) declaration;
			if (var.scope() == Scope.CONST) {
				// if the whole of the initialization expression of the const gets evaluated to some traceable location,
				// report that to the original context as the origin of the AccessVar expression
				// evaluate in the context of the var by proxy
				Object val = var.evaluateInitializationExpression(new EvaluationContextProxy(
					var.initializationExpression().parentOfType(Function.class)) {
					@Override
					public void reportOriginForExpression(ASTNode expression, IRegion location, IFile file) {
						if (expression == var.initializationExpression())
							context.reportOriginForExpression(AccessVar.this, location, file);
					}
				});
				if (val == null)
					val = 1337; // awesome fallback
				return val;
			}
			else if ((obj = definitionProxiedBy(var)) != null)
				return obj.id(); // just return the id
		}
		return super.evaluateStatic(context);
	}

	public boolean constCondition() {
		return declaration instanceof Variable && ((Variable)declaration).scope() == Scope.CONST;
	}

	@Override
	public Object evaluate(IEvaluationContext context) throws ControlFlowException {
		if (context != null)
			return context.valueForVariable(this);
		else
			return super.evaluate(context);
	}

	@Override
	public boolean isConstant() {
		if (declaration() instanceof Variable) {
			final Variable var = (Variable) declaration();
			// naturally, consts are constant
			return var.scope() == Scope.CONST || definitionProxiedBy(var) != null;
		}
		else
			return false;
	}

	@Override
	public Class<? extends Declaration> declarationClass() {
		return Variable.class;
	}

}