package net.arctics.clonk.parser.c4script;

import java.util.Iterator;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.util.ArrayUtil;

/**
 * An object that is known to be of an object type that includes a certain script.
 * @author madeen
 *
 */
public class ConstrainedObject implements IType, IHasConstraint {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	private ScriptBase constraintScript;
	private ConstraintKind constraintKind;
	private transient Iterable<IType> iterable;
	
	/**
	 * The script that must be included.
	 * @return
	 */
	@Override
	public ScriptBase constraintScript() {
		return constraintScript;
	}
	
	@Override
	public ConstraintKind constraintKind() {
		return constraintKind;
	}
	
	public ConstrainedObject(ScriptBase obligatoryInclude, ConstraintKind constraintKind) {
		super();
		this.constraintScript = obligatoryInclude;
		this.constraintKind = constraintKind;
	}

	@Override
	public Iterator<IType> iterator() {
		if (iterable == null)
			iterable = ArrayUtil.arrayIterable(PrimitiveType.OBJECT, PrimitiveType.PROPLIST, this);
		return iterable.iterator();
	}

	@Override
	public boolean canBeAssignedFrom(IType other) {
		if (other == PrimitiveType.OBJECT || other == PrimitiveType.ANY || other == PrimitiveType.UNKNOWN || other == PrimitiveType.PROPLIST)
			return true;
		ScriptBase script = null;
		if (other instanceof ScriptBase)
			script = (ScriptBase)other;
		if (other instanceof ConstrainedObject)
			script = ((ConstrainedObject)other).constraintScript;
		return script != null && constraintScript.canBeAssignedFrom(script);
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
			if (t == PrimitiveType.PROPLIST)
				return true;
			if (canBeAssignedFrom(t))
				return true;
		}
		return false;
	}

	@Override
	public boolean containsType(IType type) {
		return type == PrimitiveType.PROPLIST || type == PrimitiveType.OBJECT || canBeAssignedFrom(type);
	}

	@Override
	public boolean containsAnyTypeOf(IType... types) {
		return IType.Default.containsAnyTypeOf(this, types);
	}

	@Override
	public int specificness() {
		return PrimitiveType.OBJECT.specificness()+1;
	}

	@Override
	public IType staticType() {
		return PrimitiveType.OBJECT;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ConstrainedObject) {
			ConstrainedObject cobj = (ConstrainedObject) obj;
			return cobj.constraintKind == this.constraintKind && cobj.constraintScript == this.constraintScript;
		}
		return false;
	}
	
	@Override
	public void setTypeDescription(String description) {}
	
	@Override
	public IType resolve(DeclarationObtainmentContext context, IType callerType) {
		switch (constraintKind()) {
		case CallerType:
			if (callerType != constraintScript())
				return callerType;
			else
				break;
		case Exact:
			return constraintScript();
		case Includes:
			break;
		}
		return this;
	}

}
