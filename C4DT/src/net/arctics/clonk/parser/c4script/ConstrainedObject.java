package net.arctics.clonk.parser.c4script;

import java.util.Iterator;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.C4Object;
import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.util.ArrayUtil;

/**
 * An object that is known to be of an object type that includes a certain script.
 * @author madeen
 *
 */
public class ConstrainedObject implements IType, IHasConstraint {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	private C4ScriptBase constraintScript;
	private ConstraintKind constraintKind;
	private transient Iterable<IType> iterable;
	
	/**
	 * The script that must be included.
	 * @return
	 */
	@Override
	public C4ScriptBase constraintScript() {
		return constraintScript;
	}
	
	@Override
	public ConstraintKind constraintKind() {
		return constraintKind;
	}
	
	public ConstrainedObject(C4ScriptBase obligatoryInclude, ConstraintKind constraintKind) {
		super();
		this.constraintScript = obligatoryInclude;
		this.constraintKind = constraintKind;
	}

	@Override
	public Iterator<IType> iterator() {
		if (iterable == null)
			iterable = ArrayUtil.arrayIterable(C4Type.OBJECT, C4Type.PROPLIST, constraintScript instanceof IType ? (IType)constraintScript : null);
		return iterable.iterator();
	}

	@Override
	public boolean canBeAssignedFrom(IType other) {
		if (other == C4Type.OBJECT)
			return true;
		C4ScriptBase script = null;
		if (other instanceof C4ScriptBase)
			script = (C4ScriptBase)other;
		if (other instanceof ConstrainedObject)
			script = ((ConstrainedObject)other).constraintScript;
		return script != null && script.includes(constraintScript);
	}

	@Override
	public String typeName(boolean special) {
		if (constraintScript == null)
			return IType.ERRONEOUS_TYPE;
		String formatString;
		switch (constraintKind) {
		case CallerType:
			formatString = Messages.ConstrainedObject_ObjectOfCurrentType;
			break;
		case Exact:
			formatString = "'%s'"; //$NON-NLS-1$
			break;
		case Includes:
			formatString = Messages.ConstrainedObject_ObjectIncluding;
			break;
		default:
			return IType.ERRONEOUS_TYPE;
		}
		return String.format(formatString, constraintScript instanceof IType ? ((IType)constraintScript).typeName(false) : constraintScript.toString());
	}
	
	@Override
	public String toString() {
		return typeName(false);
	}

	@Override
	public boolean intersects(IType typeSet) {
		for (IType t : typeSet) {
			if (canBeAssignedFrom(t)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean containsType(IType type) {
		return type == C4Type.PROPLIST || canBeAssignedFrom(type);
	}

	@Override
	public boolean containsAnyTypeOf(IType... types) {
		return IType.Default.containsAnyTypeOf(this, types);
	}

	@Override
	public int specificness() {
		if (constraintScript instanceof C4Object)
			return ((C4Object)constraintScript).specificness()+1;
		else
			return C4Type.OBJECT.specificness();
	}

	@Override
	public IType staticType() {
		return C4Type.OBJECT;
	}

	@Override
	public IType serializableVersion(ClonkIndex indexToBeSerialized) {
		if (constraintScript.getIndex() == indexToBeSerialized) {
			return this;
		} else {
			return C4Type.OBJECT;
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ConstrainedObject) {
			ConstrainedObject cobj = (ConstrainedObject) obj;
			return cobj.constraintKind == this.constraintKind && cobj.constraintScript == this.constraintScript;
		}
		return false;
	}

}
