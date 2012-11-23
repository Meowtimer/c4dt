package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.PrimitiveType;

public abstract class TypeInfo implements ITypeInfo, Cloneable {

	protected IType type = PrimitiveType.UNKNOWN;

	@Override
	public IType type() {
		return type;
	}

	@Override
	public void storeType(IType type) {
		this.type = type;
	}

	@Override
	public boolean hint(IType hint) {
		if (type == PrimitiveType.UNKNOWN)
			storeType(hint);
		else if (type == PrimitiveType.ANY)
			type = TypeUnification.unify(type, hint);
		return true;
	}
	
	@Override
	public void apply(boolean soft, C4ScriptParser parser) {}
	
	@Override
	public void merge(ITypeInfo other) {
		if (type() == PrimitiveType.UNKNOWN)
			// unknown before so now it is assumed to be of this type
			storeType(type);
		else if (!type().equals(other.type()))
			// assignments of multiple types - construct type set
			storeType(TypeUnification.unify(type(), other.type()));
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
	@Override
	public String toString() {
		return "type: " + type(); //$NON-NLS-1$
	}
	
	@Override
	public boolean local() {
		return false;
	}

}
