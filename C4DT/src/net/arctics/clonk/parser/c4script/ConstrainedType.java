package net.arctics.clonk.parser.c4script;

import java.io.Serializable;
import java.util.Iterator;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.C4Object;
import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.Utilities;

/**
 * Type that does not denote a concrete object but a type
 *
 */
public class ConstrainedType implements IType, IHasConstraint, Serializable {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	
	private C4ScriptBase constraintScript;
	private ConstraintKind constraintKind;

	public ConstrainedType(C4ScriptBase script, ConstraintKind kind) {
		this.constraintScript = script;
		this.constraintKind = kind;
	}
	
	@Override
	public C4ScriptBase constraintScript() {
		return constraintScript;
	}
	
	@Override
	public ConstraintKind constraintKind() {
		return constraintKind;
	}
	
	@Override
	public Iterator<IType> iterator() {
		return ArrayUtil.arrayIterable(C4Type.ID, C4Type.PROPLIST, constraintScript instanceof IType ? (IType)constraintScript : null).iterator();
	}

	@Override
	public boolean canBeAssignedFrom(IType other) {
		return C4Type.ID.canBeAssignedFrom(other);
	}

	@Override
	public String typeName(boolean special) {
		switch (constraintKind) {
		case Includes:
			return String.format("<Definition including %s>", constraintScriptToString(special));
		case CallerType:
			return "<Current type>";
		case Exact:
			return String.format("<Type %s>", constraintScriptToString(special));
		default:
			return "<Fail>";
		}
	}

	private String constraintScriptToString(boolean special) {
		return constraintScript instanceof IType ? ((IType)constraintScript).typeName(special) : constraintScript.toString();
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
		return type == C4Type.ID || type == this.constraintScript || type == C4Type.PROPLIST;
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
	
	@Override
	public IType serializableVersion(ClonkIndex indexToBeSerialized) {
		if (constraintScript.getIndex() == indexToBeSerialized)
			return this;
		else
			return C4Type.ID;
	}

	public C4Object getObjectType() {
		return Utilities.as(constraintScript, C4Object.class);
	}
	
	public static ConstrainedType get(C4ScriptBase script, ConstraintKind kind) {
		return (kind == ConstraintKind.Exact) && script instanceof C4Object
			? ((C4Object)script).getObjectType()
			: new ConstrainedType(script, kind);
	}

}
