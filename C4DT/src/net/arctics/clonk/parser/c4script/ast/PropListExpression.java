package net.arctics.clonk.parser.c4script.ast;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.C4Type;
import net.arctics.clonk.parser.c4script.C4Variable;
import net.arctics.clonk.parser.c4script.IHasSubDeclarations;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.inireader.IniData.IniConfiguration;
import net.arctics.clonk.ui.editors.c4script.IPostSerializable;

public class PropListExpression extends Value implements IType, IPostSerializable<IPostSerializable<?>>, IHasSubDeclarations {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	
	private List<C4Variable> components;
	
	public PropListExpression(List<C4Variable> components) {
		this.components = components;
	}
	@Override
	public void doPrint(ExprWriter output, int depth) {
		output.append('{');
		for (int i = 0; i < components.size(); i++) {
			C4Variable component = components.get(i);
			output.append('\n');
			Conf.printIndent(output, depth-1);
			output.append(component.getName());
			output.append(": "); //$NON-NLS-1$
			component.getInitializationExpression().print(output, depth+1);
			if (i < components.size()-1) {
				output.append(',');
			} else {
				output.append('\n'); Conf.printIndent(output, depth-2); output.append('}');
			}
		}
	}
	@Override
	public IType getType(C4ScriptParser parser) {
		return this; // :D
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
		ExprElm[] result = new ExprElm[components.size()];
		for (int i = 0; i < result.length; i++)
			result[i] = components.get(i).getInitializationExpression();
		return result;
	}
	@Override
	public void setSubElements(ExprElm[] elms) {
		for (int i = 0; i < Math.min(elms.length, components.size()); i++) {
			components.get(i).setInitializationExpression(elms[i]);
		}
	}
	@Override
	public boolean isConstant() {
		// whoohoo, proplist expressions can be constant if all components are constant
		for (C4Variable component : components) {
			if (!component.getInitializationExpression().isConstant())
				return false;
		}
		return true;
	}
	
	@Override
	public Object evaluateAtParseTime(C4ScriptBase context) {
		Map<String, Object> map = new HashMap<String, Object>(components.size());
		for (C4Variable component : components) {
			map.put(component.getName(), component.getInitializationExpression().evaluateAtParseTime(context));
		}
		return map;
	}
	
	public IniConfiguration guessedConfiguration(C4ScriptParser context) {
		if (context.getActiveVariableBeingDeclared() != null) {
			return context.getContainer().getEngine().getIniConfigurations().getConfigurationFor(context.getActiveVariableBeingDeclared().getName()+".txt");
		} else {
			return null;
		}
	}
	
	public C4Variable findComponent(String declarationName) {
		for (C4Variable v : components) {
			if (v.getName().equals(declarationName)) {
				return v;
			}
		}
		return null;
	}
	
	// awkward silence...
	
	
	
	
	// it's also a type \o/

	@Override
	public Iterator<IType> iterator() {
		return C4Type.PROPLIST.iterator();
	}

	@Override
	public boolean canBeAssignedFrom(IType other) {
		return C4Type.PROPLIST.canBeAssignedFrom(other);
	}

	@Override
	public String typeName(boolean special) {
		return "<Known proplist>";
	}

	@Override
	public boolean intersects(IType typeSet) {
		return C4Type.PROPLIST.intersects(typeSet);
	}

	@Override
	public boolean containsType(IType type) {
		return C4Type.PROPLIST.containsType(type);
	}

	@Override
	public boolean containsAnyTypeOf(IType... types) {
		return C4Type.PROPLIST.containsAnyTypeOf(types);
	}

	@Override
	public int specificness() {
		return C4Type.PROPLIST.specificness()+1;
	}

	@Override
	public IType staticType() {
		return C4Type.PROPLIST;
	}
	
	private C4Declaration associatedDeclaration;
	public C4Declaration getAssociatedDeclaration() {
		return associatedDeclaration;
	}
	@Override
	public void setAssociatedDeclaration(C4Declaration declaration) {
		associatedDeclaration = declaration;
	}

	@Override
	public IType serializableVersion(ClonkIndex indexToBeSerialized) {
		if (associatedDeclaration != null && associatedDeclaration.getScript().getIndex() == indexToBeSerialized) {
			return this;
		} else {
			return C4Type.PROPLIST;
		}
	}
	@Override
	public void postSerialize(IPostSerializable<?> parent) {
		for (C4Variable component : components) {
			component.postSerialize(associatedDeclaration);
		}
	}
	@Override
	public Iterable<? extends C4Declaration> allSubDeclarations() {
		return components;
	}

}