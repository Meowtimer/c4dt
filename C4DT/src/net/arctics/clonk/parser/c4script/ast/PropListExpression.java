package net.arctics.clonk.parser.c4script.ast;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.EntityRegion;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.DeclarationObtainmentContext;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.ProplistDeclaration;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.ast.evaluate.IEvaluationContext;
import net.arctics.clonk.parser.inireader.IniData.IniConfiguration;

public class PropListExpression extends ExprElm {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	
	private ProplistDeclaration definedDeclaration;
	
	public ProplistDeclaration definedDeclaration() {
		return definedDeclaration;
	}
	
	public List<Variable> components() {
		return definedDeclaration.components();
	}
	
	public PropListExpression(ProplistDeclaration declaration) {
		this.definedDeclaration = declaration;
		assignParentToSubElements();
	}
	@Override
	public void doPrint(ExprWriter output, int depth) {
		output.append('{');
		List<Variable> components = components();
		final boolean singleLine = output.flag(ExprWriter.SINGLE_LINE);
		for (int i = 0; i < components.size(); i++) {
			Variable component = components.get(i);
			if (singleLine) { if (i > 0) output.append(' '); }
			else output.append('\n');
			Conf.printIndent(output, depth+1);
			output.append(component.name());
			output.append(": "); //$NON-NLS-1$
			if (component.initializationExpression() instanceof PropListExpression)
				Conf.blockPrelude(output, depth+1);
			component.initializationExpression().print(output, depth+1);
			if (i < components.size()-1)
				output.append(',');
			else {
				if (!singleLine)
					output.append('\n');
				Conf.printIndent(output, depth);
			}
		}
		output.append('}');
	}
	@Override
	public IType unresolvedType(DeclarationObtainmentContext parser) {
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
			result[i] = components.get(i).initializationExpression();
		return result;
	}
	@Override
	public void setSubElements(ExprElm[] elms) {
		if (definedDeclaration == null)
			return;
		List<Variable> components = components();
		for (int i = 0; i < Math.min(elms.length, components.size()); i++)
			components.get(i).setInitializationExpression(elms[i]);
	}
	@Override
	public boolean isConstant() {
		// whoohoo, proplist expressions can be constant if all components are constant
		for (Variable component : components())
			if (!component.initializationExpression().isConstant())
				return false;
		return true;
	}
	
	@Override
	public Object evaluateAtParseTime(IEvaluationContext context) {
		List<Variable> components = components();
		Map<String, Object> map = new HashMap<String, Object>(components.size());
		for (Variable component : components)
			map.put(component.name(), component.initializationExpression().evaluateAtParseTime(context));
		return map;
	}
	
	public IniConfiguration guessedConfiguration(C4ScriptParser context) {
		if (context.currentVariable() != null)
			return context.script().engine().iniConfigurations().configurationFor(context.currentVariable().name()+".txt"); //$NON-NLS-1$
		else
			return null;
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
		return keyVar != null ? keyVar.initializationExpression() : null;
	} 
	
	@SuppressWarnings("unchecked")
	public <T> T valueEvaluated(String key, Class<T> cls) {
		ExprElm e = value(key);
		if (e != null) {
			Object eval = e.evaluateAtParseTime(definedDeclaration.firstParentDeclarationOfType(IEvaluationContext.class));
			return eval != null && cls.isAssignableFrom(eval.getClass()) ? (T)eval : null;
		} else
			return null;
	}
	
	@Override
	public void reportProblems(C4ScriptParser parser) throws ParsingException {
		super.reportProblems(parser);
		if (!parser.script().engine().settings().supportsProplists)
			parser.error(ParserErrorCode.NotSupported, this, C4ScriptParser.NO_THROW, Messages.PropListExpression_ProplistsFeature);
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
	
	@Override
	public EntityRegion declarationAt(int offset, C4ScriptParser parser) {
		int absolute = parser.absoluteSourceLocation(start()+offset, 0).start();
		for (Variable v : this.components())
			if (v.isAt(absolute))
				return new EntityRegion(v, v.location().relativeTo(parser.absoluteSourceLocation(0, 0)));
		return super.declarationAt(offset, parser);
	}
	
}