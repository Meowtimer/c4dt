package net.arctics.clonk.parser.c4script.ast;

import java.util.HashMap;
import java.util.Map;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.C4Type;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.util.Pair;

public class PropListExpression extends Value {
	/**
	 * 
	 */
	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	private Pair<String, ExprElm>[] components;
	public PropListExpression(Pair<String, ExprElm>[] components) {
		this.components = components;
	}
	@Override
	public void doPrint(ExprWriter output, int depth) {
		output.append('{');
		for (int i = 0; i < components.length; i++) {
			Pair<String, ExprElm> component = components[i];
			output.append('\n');
			Conf.printIndent(output, depth-1);
			output.append(component.getFirst());
			output.append(": "); //$NON-NLS-1$
			component.getSecond().print(output, depth+1);
			if (i < components.length-1) {
				output.append(',');
			} else {
				output.append('\n'); Conf.printIndent(output, depth-2); output.append('}');
			}
		}
	}
	@Override
	public IType getType(C4ScriptParser parser) {
		return C4Type.PROPLIST;
	}
	@Override
	public boolean modifiable(C4ScriptParser parser) {
		return false;
	}
	@Override
	public boolean isValidInSequence(ExprElm predecessor, C4ScriptParser parser) {
		return predecessor == null;
	}
	@Override
	public ExprElm[] getSubElements() {
		ExprElm[] result = new ExprElm[components.length];
		for (int i = 0; i < result.length; i++)
			result[i] = components[i].getSecond();
		return result;
	}
	@Override
	public void setSubElements(ExprElm[] elms) {
		for (int i = 0; i < Math.min(elms.length, components.length); i++) {
			components[i].setSecond(elms[i]);
		}
	}
	@Override
	public boolean isConstant() {
		// whoohoo, proplist expressions can be constant if all components are constant
		for (Pair<String, ExprElm> component : components) {
			if (!component.getSecond().isConstant())
				return false;
		}
		return true;
	}
	
	@Override
	public Object evaluateAtParseTime(C4ScriptBase context) {
		Map<String, Object> map = new HashMap<String, Object>(components.length);
		for (Pair<String, ExprElm> component : components) {
			map.put(component.getFirst(), component.getSecond().evaluateAtParseTime(context));
		}
		return map;
	}
}