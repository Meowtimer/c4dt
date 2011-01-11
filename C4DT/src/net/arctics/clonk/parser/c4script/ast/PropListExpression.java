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
import net.arctics.clonk.parser.c4script.ProplistDeclaration;
import net.arctics.clonk.parser.inireader.IniData.IniConfiguration;
import net.arctics.clonk.ui.editors.c4script.IPostSerializable;
import net.arctics.clonk.util.ArrayUtil;

public class PropListExpression extends Value implements IType, IPostSerializable<C4Declaration>, IHasSubDeclarations {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	
	private ProplistDeclaration definedDeclaration;
	
	public ProplistDeclaration getDefinedDeclaration() {
		return definedDeclaration;
	}
	
	public List<C4Variable> getComponents() {
		return definedDeclaration.getComponents();
	}
	
	public PropListExpression(ProplistDeclaration declaration) {
		this.definedDeclaration = declaration;
		assignParentToSubElements();
	}
	@Override
	public void doPrint(ExprWriter output, int depth) {
		output.append('{');
		List<C4Variable> components = getComponents();
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
				output.append('\n'); Conf.printIndent(output, depth-2);
			}
		}
		output.append('}');
	}
	@Override
	protected IType obtainType(C4ScriptParser parser) {
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
		List<C4Variable> components = getComponents();
		ExprElm[] result = new ExprElm[components.size()];
		for (int i = 0; i < result.length; i++)
			result[i] = components.get(i).getInitializationExpression();
		return result;
	}
	@Override
	public void setSubElements(ExprElm[] elms) {
		List<C4Variable> components = getComponents();
		for (int i = 0; i < Math.min(elms.length, components.size()); i++) {
			components.get(i).setInitializationExpression(elms[i]);
		}
	}
	@Override
	public boolean isConstant() {
		// whoohoo, proplist expressions can be constant if all components are constant
		for (C4Variable component : getComponents()) {
			if (!component.getInitializationExpression().isConstant())
				return false;
		}
		return true;
	}
	
	@Override
	public Object evaluateAtParseTime(C4ScriptBase context) {
		List<C4Variable> components = getComponents();
		Map<String, Object> map = new HashMap<String, Object>(components.size());
		for (C4Variable component : components) {
			map.put(component.getName(), component.getInitializationExpression().evaluateAtParseTime(context));
		}
		return map;
	}
	
	public IniConfiguration guessedConfiguration(C4ScriptParser context) {
		if (context.getCurrentVariableBeingDeclared() != null) {
			return context.getContainer().getEngine().getIniConfigurations().getConfigurationFor(context.getCurrentVariableBeingDeclared().getName()+".txt");
		} else {
			return null;
		}
	}
	
	public C4Variable findComponent(String declarationName) {
		for (C4Variable v : getComponents()) {
			if (v.getName().equals(declarationName)) {
				return v;
			}
		}
		return null;
	}
	
	// awkward silence...
	
	
	
	
	// it's also a type \o/
	// and it would have been nice to make it also a declaration, but there is no IDeclaration.. oh well

	@Override
	public Iterator<IType> iterator() {
		return ArrayUtil.arrayIterable(C4Type.PROPLIST, this).iterator();
	}

	@Override
	public boolean canBeAssignedFrom(IType other) {
		return C4Type.PROPLIST.canBeAssignedFrom(other);
	}

	@Override
	public String typeName(boolean special) {
		return C4Type.PROPLIST.typeName(special);
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
	public void postSerialize(C4Declaration parent) {
		definedDeclaration.postSerialize(parent);
	}
	@Override
	public Iterable<? extends C4Declaration> allSubDeclarations(int mask) {
		return definedDeclaration.allSubDeclarations(mask);
	}
	
	@Override
	public String getName() {
		return "Proplist Expression 0009247331";
	}

}