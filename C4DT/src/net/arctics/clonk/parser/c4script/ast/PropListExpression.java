package net.arctics.clonk.parser.c4script.ast;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.DeclarationObtainmentContext;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.ProplistDeclaration;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.ast.evaluate.IEvaluationContext;
import net.arctics.clonk.parser.inireader.IniData.IniConfiguration;

public class PropListExpression extends Value {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	
	private ProplistDeclaration definedDeclaration;
	
	public ProplistDeclaration definedDeclaration() {
		return definedDeclaration;
	}
	
	public List<Variable> components() {
		return definedDeclaration.getComponents();
	}
	
	public PropListExpression(ProplistDeclaration declaration) {
		this.definedDeclaration = declaration;
		assignParentToSubElements();
	}
	@Override
	public void doPrint(ExprWriter output, int depth) {
		output.append('{');
		List<Variable> components = components();
		for (int i = 0; i < components.size(); i++) {
			Variable component = components.get(i);
			output.append('\n');
			Conf.printIndent(output, depth-1);
			output.append(component.name());
			output.append(": "); //$NON-NLS-1$
			component.getInitializationExpression().print(output, depth+1);
			if (i < components.size()-1) {
				output.append(',');
			} else {
				output.append('\n'); Conf.printIndent(output, depth-2);
			}
		}
		output.append('}');
	}
	@Override
	protected IType obtainType(DeclarationObtainmentContext parser) {
		return definedDeclaration;
	}
	@Override
	public boolean isModifiable(C4ScriptParser parser) {
		return false;
	}
	@Override
	public boolean isValidInSequence(ExprElm predecessor, C4ScriptParser parser) {
		return predecessor == null;
	}
	@Override
	public ExprElm[] subElements() {
		if (definedDeclaration == null)
			return EMPTY_EXPR_ARRAY;
		List<Variable> components = components();
		ExprElm[] result = new ExprElm[components.size()];
		for (int i = 0; i < result.length; i++)
			result[i] = components.get(i).getInitializationExpression();
		return result;
	}
	@Override
	public void setSubElements(ExprElm[] elms) {
		if (definedDeclaration == null)
			return;
		List<Variable> components = components();
		for (int i = 0; i < Math.min(elms.length, components.size()); i++) {
			components.get(i).setInitializationExpression(elms[i]);
		}
	}
	@Override
	public boolean isConstant() {
		// whoohoo, proplist expressions can be constant if all components are constant
		for (Variable component : components()) {
			if (!component.getInitializationExpression().isConstant())
				return false;
		}
		return true;
	}
	
	@Override
	public Object evaluateAtParseTime(IEvaluationContext context) {
		List<Variable> components = components();
		Map<String, Object> map = new HashMap<String, Object>(components.size());
		for (Variable component : components) {
			map.put(component.name(), component.getInitializationExpression().evaluateAtParseTime(context));
		}
		return map;
	}
	
	public IniConfiguration guessedConfiguration(C4ScriptParser context) {
		if (context.getCurrentVariable() != null) {
			return context.container().engine().iniConfigurations().configurationFor(context.getCurrentVariable().name()+".txt"); //$NON-NLS-1$
		} else {
			return null;
		}
	}
	
	private Declaration associatedDeclaration;
	public Declaration associatedDeclaration() {
		return associatedDeclaration;
	}
	@Override
	public void setAssociatedDeclaration(Declaration declaration) {
		associatedDeclaration = declaration;
	}
	
	public ExprElm value(String key) {
		Variable keyVar = definedDeclaration.findComponent(key);
		return keyVar != null ? keyVar.getInitializationExpression() : null;
	} 
	
	@SuppressWarnings("unchecked")
	public <T> T valueEvaluated(String key, Class<T> cls) {
		ExprElm e = value(key);
		if (e != null) {
			Object eval = e.evaluateAtParseTime(definedDeclaration.getParentDeclarationOfType(IEvaluationContext.class));
			return eval != null && cls.isAssignableFrom(eval.getClass()) ? (T)eval : null;
		} else
			return null;
	}
	
	@Override
	public void reportErrors(C4ScriptParser parser) throws ParsingException {
		super.reportErrors(parser);
		if (!parser.container().engine().currentSettings().proplistsSupported)
			parser.errorWithCode(ParserErrorCode.NotSupported, this, C4ScriptParser.NO_THROW, Messages.PropListExpression_ProplistsFeature);
	}
	
	@Override
	public PropListExpression clone() {
		// when calling super.clone(), the sub elements obtained from definedDeclaration will be cloned
		// and then reassigned to the original ProplistDeclaration which is not desired so temporarily
		// set definedDeclaration to null to avoid this.
		ProplistDeclaration saved = this.definedDeclaration;
		this.definedDeclaration = null;
		try {
			// regular copying of attributes with no sub element cloning taking place
			PropListExpression e = (PropListExpression) super.clone();
			try {
				// clone the ProplistDeclaration, also cloning sub variables. This will automatically
				// lead to getSubElements also returning cloned initialization expressions.
				e.definedDeclaration = saved.clone();
			} catch (CloneNotSupportedException e1) {
				e1.printStackTrace();
			}
			return e;
		} finally {
			// restore state of original expression which is not supposed to be altered by calling clone()
			this.definedDeclaration = saved;
		}
	}
	
	@Override
	public void setParent(ExprElm parent) {
		super.setParent(parent);
	}
	
}