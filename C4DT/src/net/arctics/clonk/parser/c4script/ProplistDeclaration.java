package net.arctics.clonk.parser.c4script;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.util.ArrayUtil;

public class ProplistDeclaration extends Structure implements IType {

	private static final long serialVersionUID = 1L;
	
	/**
	 * Each assignment in a proplist declaration is represented by a C4Variable object.
	 */
	private List<Variable> components;
	
	/**
	 * Whether the declaration was "explicit" {blub=<blub>...} or
	 * by assigning values separately (effect.var1 = ...; ...)
	 */
	private boolean adHoc;
	
	public boolean isAdHoc() {
		return adHoc;
	}
	
	public List<Variable> getComponents() {
		return components;
	}
	
	public ProplistDeclaration() {
		super();
		setName(String.format("%s {...}", PrimitiveType.PROPLIST.toString()));
	}

	public ProplistDeclaration(List<Variable> components) {
		this();
		this.components = components;
	}
	
	public static ProplistDeclaration adHocDeclaration() {
		ProplistDeclaration result = new ProplistDeclaration();
		result.adHoc = true;
		result.components = new LinkedList<Variable>();
		return result;
	}
	
	public Variable addComponent(Variable variable) {
		Variable found = findComponent(variable.getName());
		if (found != null) {
			return found;
		} else {
			components.add(variable);
			return variable;
		}
	}
	
	public Variable findComponent(String declarationName) {
		for (Variable v : components) {
			if (v.getName().equals(declarationName)) {
				return v;
			}
		}
		return null;
	}

	@Override
	public Declaration findLocalDeclaration(String declarationName, Class<? extends Declaration> declarationClass) {
		for (Variable v : getComponents()) {
			if (v.getName().equals(declarationName)) {
				return v;
			}
		}
		return null;
	}
	
	@Override
	public Iterable<? extends Declaration> allSubDeclarations(int mask) {
		if ((mask & VARIABLES) != 0) {
			return components;
		}
		else
			return NO_SUB_DECLARATIONS;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Declaration> T getLatestVersion(T from) {
		if (Variable.class.isAssignableFrom(from.getClass())) {
			return (T) findComponent(from.getName());
		} else {
			return super.getLatestVersion(from);
		}
	};
	
	@Override
	public Iterator<IType> iterator() {
		return ArrayUtil.arrayIterable(PrimitiveType.PROPLIST, this).iterator();
	}

	@Override
	public boolean canBeAssignedFrom(IType other) {
		return PrimitiveType.PROPLIST.canBeAssignedFrom(other);
	}

	@Override
	public String typeName(boolean special) {
		return PrimitiveType.PROPLIST.typeName(special);
	}

	@Override
	public boolean intersects(IType typeSet) {
		return PrimitiveType.PROPLIST.intersects(typeSet);
	}

	@Override
	public boolean containsType(IType type) {
		return PrimitiveType.PROPLIST.containsType(type);
	}

	@Override
	public boolean containsAnyTypeOf(IType... types) {
		return PrimitiveType.PROPLIST.containsAnyTypeOf(types);
	}

	@Override
	public int specificness() {
		return PrimitiveType.PROPLIST.specificness()+1;
	}

	@Override
	public IType staticType() {
		return PrimitiveType.PROPLIST;
	}
	
	@Override
	public String getName() {
		return "Proplist Expression 0009247331";
	}

}
