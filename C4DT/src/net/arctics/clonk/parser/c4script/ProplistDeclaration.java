package net.arctics.clonk.parser.c4script;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.C4Structure;
import net.arctics.clonk.util.ArrayUtil;

public class ProplistDeclaration extends C4Structure implements IType {

	private static final long serialVersionUID = 1L;
	
	/**
	 * Each assignment in a proplist declaration is represented by a C4Variable object.
	 */
	private List<C4Variable> components;
	
	/**
	 * Whether the declaration was "explicit" {blub=<blub>...} or
	 * by assigning values separately (effect.var1 = ...; ...)
	 */
	private boolean adHoc;
	
	public boolean isAdHoc() {
		return adHoc;
	}
	
	public List<C4Variable> getComponents() {
		return components;
	}
	
	public ProplistDeclaration() {
		super();
		setName("proplist {...}");
	}

	public ProplistDeclaration(List<C4Variable> components) {
		this();
		this.components = components;
	}
	
	public static ProplistDeclaration adHocDeclaration() {
		ProplistDeclaration result = new ProplistDeclaration();
		result.adHoc = true;
		result.components = new LinkedList<C4Variable>();
		return result;
	}
	
	public C4Variable addComponent(C4Variable variable) {
		C4Variable found = findComponent(variable.getName());
		if (found != null) {
			//found.setLocation(variable.getLocation());
			return found;
		} else {
			components.add(variable);
			return variable;
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

	@Override
	public C4Declaration findLocalDeclaration(String declarationName, Class<? extends C4Declaration> declarationClass) {
		for (C4Variable v : getComponents()) {
			if (v.getName().equals(declarationName)) {
				return v;
			}
		}
		return null;
	}
	
	@Override
	public Iterable<? extends C4Declaration> allSubDeclarations(int mask) {
		if ((mask & VARIABLES) != 0)
			return components;
		else
			return NO_SUB_DECLARATIONS;
	}
	
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

	@Override
	public IType serializableVersion(ClonkIndex indexToBeSerialized) {
		if (getScript() != null && getScript().getIndex() == indexToBeSerialized) {
			return this;
		} else {
			return C4Type.PROPLIST;
		}
	}
	
	@Override
	public String getName() {
		return "Proplist Expression 0009247331";
	}

}
