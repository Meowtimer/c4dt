package net.arctics.clonk.parser.c4script;

import java.util.Iterator;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.C4Object;
import net.arctics.clonk.util.Utilities;

/**
 * Type that does not denote a concrete object but a type
 *
 */
public class C4ObjectType implements IType {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	
	private C4Object type;

	public C4ObjectType(C4Object type) {
		this.type = type;
	}
	
	public C4Object getType() {
		return type;
	}
	
	@Override
	public Iterator<IType> iterator() {
		return Utilities.arrayIterable(C4Type.ID, C4Type.PROPLIST, type).iterator();
	}

	@Override
	public boolean canBeAssignedFrom(IType other) {
		return C4Type.ID.canBeAssignedFrom(other);
	}

	@Override
	public String typeName(boolean special) {
		return String.format("Type %s", type.typeName(special));
	}
	
	@Override
	public String toString() {
		return typeName(false);
	}

	@Override
	public boolean intersects(IType typeSet) {
		for (IType t : typeSet)
			if (t.canBeAssignedFrom(C4Type.ID))
				return true;
		return false;
	}

	@Override
	public boolean containsType(IType type) {
		return type == C4Type.ID || type == this.type || type == C4Type.PROPLIST;
	}
	
	@Override
	public boolean containsAnyTypeOf(IType... types) {
		return IType.Default.containsAnyTypeOf(this, types);
	}

	@Override
	public int specificness() {
		return C4Type.ID.specificness()+1;
	}

	@Override
	public IType staticType() {
		return C4Type.ID;
	}

}
