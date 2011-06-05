package net.arctics.clonk.parser.c4script;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.util.Utilities;

/**
 * A proplist (might be either an object or a {@link Definition} or whatever) 
 * @author madeen
 *
 */
public class ConstrainedProplist implements IType, IHasConstraint {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	private ScriptBase constraintScript;
	private ConstraintKind constraintKind;
	private transient Iterable<IType> iterable;
	
	/**
	 * Instances of this type can be {@link Definition}s
	 */
	private boolean isType = true;
	
	/**
	 * Instances of this type can be objects
	 */
	private boolean isObject = true;
	
	/**
	 * The script an instance of this type is known to include.
	 * @return
	 */
	@Override
	public ScriptBase constraintScript() {
		return constraintScript;
	}
	
	/**
	 * Return the {@link #constraintScript()} cast to {@link Definition}.
	 * @return The cast {@link #constraintScript()} or null if the constraint is not a {@link Definition}
	 */
	public ScriptBase constraintDefinition() {
		return Utilities.as(constraintScript, Definition.class);
	}
	
	/**
	 * Constraint kind.
	 */
	@Override
	public ConstraintKind constraintKind() {
		return constraintKind;
	}
	
	public ConstrainedProplist(ScriptBase obligatoryInclude, ConstraintKind constraintKind) {
		super();
		this.constraintScript = obligatoryInclude;
		this.constraintKind = constraintKind;
	}
	
	public ConstrainedProplist(ScriptBase obligatoryInclude, ConstraintKind constraintKind, boolean isType, boolean isObject) {
		this(obligatoryInclude, constraintKind);
		this.isType = isType;
		this.isObject = isObject;
	}

	@Override
	public synchronized Iterator<IType> iterator() {
		if (iterable == null) {
			List<IType> types = new ArrayList<IType>(4);
			types.add(PrimitiveType.PROPLIST);
			if (isObject)
				types.add(PrimitiveType.OBJECT);
			if (isType)
				types.add(PrimitiveType.ID);
			types.add(this);
			iterable = types;
		}
		return iterable.iterator();
	}

	@Override
	public boolean canBeAssignedFrom(IType other) {
		return
			(isObject && other == PrimitiveType.OBJECT) ||
			other == PrimitiveType.ANY ||
			other == PrimitiveType.UNKNOWN ||
			other == PrimitiveType.PROPLIST ||
			(isType && other == PrimitiveType.ID) ||
			other instanceof ScriptBase ||
			other instanceof ConstrainedProplist;
	}

	@Override
	public String typeName(boolean special) {
		if (constraintScript == null)
			return IType.ERRONEOUS_TYPE;
		String formatString;
		switch (constraintKind) {
		case CallerType:
			formatString = isType ? Messages.ConstrainedProplist_CurrentType : Messages.ConstrainedProplist_ObjectOfCurrentType;
			break;
		case Exact:
			formatString = isType ? Messages.ConstrainedProplist_ExactType : "'%s'"; //$NON-NLS-1$
			break;
		case Includes:
			formatString = Messages.ConstrainedProplist_Including;
			break;
		default:
			return IType.ERRONEOUS_TYPE;
		}
		return String.format(formatString, constraintScript instanceof IType ? ((IType)constraintScript).typeName(false) : constraintScript.toString());
	}
	
	public static ConstrainedProplist get(ScriptBase script, ConstraintKind kind) {
			return (kind == ConstraintKind.Exact) && script instanceof Definition
				? ((Definition)script).getObjectType()
				: new ConstrainedProplist(script, kind);
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
		if (type == PrimitiveType.PROPLIST)
			return true;
		if (isObject && type == PrimitiveType.OBJECT)
			return true;
		if (isType && type == PrimitiveType.ID)
			return true;
		if (type instanceof ScriptBase)
			return ((ScriptBase)type).includes(constraintScript);
		if (type instanceof ConstrainedProplist)
			return ((ConstrainedProplist)type).constraintScript().includes(constraintScript);
		return false;
	}

	@Override
	public boolean containsAnyTypeOf(IType... types) {
		return IType.Default.containsAnyTypeOf(this, types);
	}

	@Override
	public int specificness() {
		int spec = PrimitiveType.OBJECT.specificness();
		spec++;
		if (constraintKind == ConstraintKind.Exact)
			spec++;
		return spec;
	}

	@Override
	public IType staticType() {
		return PrimitiveType.OBJECT;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ConstrainedProplist) {
			ConstrainedProplist cobj = (ConstrainedProplist) obj;
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
