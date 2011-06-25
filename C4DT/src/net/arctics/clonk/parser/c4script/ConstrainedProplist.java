package net.arctics.clonk.parser.c4script;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.IHasIncludes;
import net.arctics.clonk.util.Utilities;

/**
 * A proplist (might be either an object or a {@link Definition} or whatever) 
 * @author madeen
 *
 */
public class ConstrainedProplist implements IType, IHasConstraint, IHasSubDeclarations, IHasIncludes {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	private IHasIncludes constraint;
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
	public IHasIncludes constraint() {
		return constraint;
	}
	
	/**
	 * Return the {@link #constraint()} cast to {@link Definition}.
	 * @return The cast {@link #constraint()} or null if the constraint is not a {@link Definition}
	 */
	public ScriptBase constraintDefinition() {
		return Utilities.as(constraint, Definition.class);
	}
	
	/**
	 * Constraint kind.
	 */
	@Override
	public ConstraintKind constraintKind() {
		return constraintKind;
	}
	
	public ConstrainedProplist(IHasIncludes obligatoryInclude, ConstraintKind constraintKind) {
		super();
		this.constraint = obligatoryInclude;
		this.constraintKind = constraintKind;
	}
	
	public ConstrainedProplist(IHasIncludes obligatoryInclude, ConstraintKind constraintKind, boolean isType, boolean isObject) {
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
			other.canBeAssignedFrom(PrimitiveType.PROPLIST) ||
			(isType && other == PrimitiveType.ID) ||
			other instanceof ScriptBase ||
			other instanceof ConstrainedProplist;
	}

	@Override
	public String typeName(boolean special) {
		if (constraint == null)
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
		return String.format(formatString, constraint instanceof IType ? ((IType)constraint).typeName(false) : constraint.toString());
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
			return ((ScriptBase)type).includes(constraint);
		if (type instanceof ConstrainedProplist)
			return ((ConstrainedProplist)type).constraint().includes(constraint);
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
			return cobj.constraintKind == this.constraintKind && cobj.constraint != null && cobj.constraint.equals(this.constraint);
		}
		return false;
	}
	
	@Override
	public void setTypeDescription(String description) {}
	
	@Override
	public IType resolve(DeclarationObtainmentContext context, IType callerType) {
		switch (constraintKind()) {
		case CallerType:
			if (callerType != constraint())
				return callerType;
			else
				break;
		case Exact:
			return constraint();
		case Includes:
			break;
		}
		return this;
	}

	@Override
	public Iterable<? extends Declaration> allSubDeclarations(int mask) {
		return constraint.allSubDeclarations(mask);
	}

	@Override
	public Function findFunction(String functionName) {
		return constraint.findFunction(functionName);
	}

	@Override
	public Declaration findDeclaration(String name, FindDeclarationInfo info) {
		return constraint.findDeclaration(name, info);
	}

	@Override
	public String getName() {
		return typeName(true);
	}

	@Override
	public Collection<? extends IHasIncludes> getIncludes(Index index, boolean recursive) {
		return constraint.getIncludes(index, recursive);
	}

	@Override
	public boolean includes(IHasIncludes other) {
		return constraint.includes(other);
	}

	@Override
	public boolean gatherIncludes(Set<IHasIncludes> set, Index index, boolean recursive) {
		return constraint.gatherIncludes(set, index, recursive);
	}

}
