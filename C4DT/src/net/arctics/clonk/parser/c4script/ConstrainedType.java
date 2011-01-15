package net.arctics.clonk.parser.c4script;

import java.io.Serializable;
import java.util.Iterator;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.Utilities;

/**
 * Type that does not denote a concrete definition but is constrained to include some script.
 *
 */
public class ConstrainedType implements IType, IHasConstraint, Serializable {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	
	private ScriptBase constraintScript;
	private ConstraintKind constraintKind;

	public ConstrainedType(ScriptBase script, ConstraintKind kind) {
		this.constraintScript = script;
		this.constraintKind = kind;
	}
	
	@Override
	public ScriptBase constraintScript() {
		return constraintScript;
	}
	
	@Override
	public ConstraintKind constraintKind() {
		return constraintKind;
	}
	
	@Override
	public Iterator<IType> iterator() {
		return ArrayUtil.arrayIterable(PrimitiveType.ID, PrimitiveType.PROPLIST, constraintScript instanceof IType ? (IType)constraintScript : null).iterator();
	}

	@Override
	public boolean canBeAssignedFrom(IType other) {
		return PrimitiveType.ID.canBeAssignedFrom(other);
	}

	@Override
	public String typeName(boolean special) {
		if (constraintScript == null)
			return IType.ERRONEOUS_TYPE;
		switch (constraintKind) {
		case Includes:
			return String.format(Messages.ConstrainedType_DefinitionIncluding, constraintScriptToString(special));
		case CallerType:
			return Messages.ConstrainedType_CurrentType;
		case Exact:
			return String.format(Messages.ConstrainedType_ExactType, constraintScriptToString(special));
		default:
			return IType.ERRONEOUS_TYPE;
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
			if (t.canBeAssignedFrom(PrimitiveType.ID))
				return true;
		return false;
	}

	@Override
	public boolean containsType(IType type) {
		return type == PrimitiveType.ID || type == this.constraintScript || type == PrimitiveType.PROPLIST;
	}
	
	@Override
	public boolean containsAnyTypeOf(IType... types) {
		return IType.Default.containsAnyTypeOf(this, types);
	}

	@Override
	public int specificness() {
		return PrimitiveType.ID.specificness()+1;
	}

	@Override
	public IType staticType() {
		return PrimitiveType.ID;
	}

	public Definition getObjectType() {
		return Utilities.as(constraintScript, Definition.class);
	}
	
	public static ConstrainedType get(ScriptBase script, ConstraintKind kind) {
		return (kind == ConstraintKind.Exact) && script instanceof Definition
			? ((Definition)script).getObjectType()
			: new ConstrainedType(script, kind);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ConstrainedType) {
			ConstrainedType cobj = (ConstrainedType) obj;
			return cobj.constraintKind == this.constraintKind && cobj.constraintScript == this.constraintScript;
		}
		return false;
	}

}
