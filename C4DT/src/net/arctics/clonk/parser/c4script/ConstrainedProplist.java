package net.arctics.clonk.parser.c4script;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.IHasSubDeclarations;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.IHasIncludes;
import net.arctics.clonk.parser.c4script.ast.TypeChoice;
import net.arctics.clonk.util.Utilities;

/**
 * A proplist (might be either an object or a {@link Definition} or whatever) 
 * @author madeen
 *
 */
public class ConstrainedProplist implements IRefinedPrimitiveType, IHasConstraint, IHasSubDeclarations, IHasIncludes {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	private final IHasIncludes constraint;
	private final ConstraintKind constraintKind;
	private transient Iterable<IType> iterable;
	
	/**
	 * Instances of this type can be {@link Definition}s
	 */
	private boolean definition = true;
	
	/**
	 * Instances of this type can be objects
	 */
	private boolean object = true;
	
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
	public Script constraintDefinition() {
		return Utilities.as(constraint, Definition.class);
	}
	
	/**
	 * Constraint kind.
	 */
	@Override
	public ConstraintKind constraintKind() {
		return constraintKind;
	}
	
	public ConstrainedProplist(IHasIncludes constraint, ConstraintKind constraintKind, boolean isType, boolean isObject) {
		this.constraint = constraint;
		this.constraintKind = constraintKind;
		this.definition = isType;
		this.object = isObject;
	}
	
	public static ConstrainedProplist definition(IHasIncludes constraint, ConstraintKind constraintKind) {
		return new ConstrainedProplist(constraint, constraintKind, true, false);
	}
	
	public static ConstrainedProplist object(IHasIncludes constraint, ConstraintKind constraintKind) {
		if (constraintKind == ConstraintKind.Exact && constraint instanceof Definition)
			return ((Definition)constraint).objectType();
		else
			return new ConstrainedProplist(constraint, constraintKind, false, true);
	}

	@Override
	public synchronized Iterator<IType> iterator() {
		if (iterable == null) {
			List<IType> types = new ArrayList<IType>(4);
			types.add(PrimitiveType.PROPLIST);
			if (object)
				types.add(PrimitiveType.OBJECT);
			if (definition)
				types.add(PrimitiveType.ID);
			types.add(constraint);
			iterable = types;
		}
		return iterable.iterator();
	}

	@Override
	public boolean canBeAssignedFrom(IType other) {
		if (object)
			if (other instanceof PrimitiveType)
				switch ((PrimitiveType)other) {
				case ANY: case UNKNOWN: case PROPLIST: case OBJECT:
					return true;
				default:
					return false;
				}
			else if (other.canBeAssignedFrom(PrimitiveType.PROPLIST))
				return true;
		if (definition)
			if (other == PrimitiveType.ID || other instanceof Script || other instanceof ConstrainedProplist)
				return true;
		return false;
	}

	@Override
	public String typeName(boolean special) {
		IType simpleType = primitiveType();
		if (constraint == null)
			return simpleType.typeName(special);
		if (!special)
			if (simpleType == PrimitiveType.ID)
				return simpleType.typeName(false);
			else if (simpleType == PrimitiveType.OBJECT)
				return (constraint instanceof Definition ? constraint : simpleType).typeName(false);
		String formatString;
		switch (constraintKind) {
		case CallerType:
			formatString = String.format(definition ? Messages.ConstrainedProplist_CurrentType : Messages.ConstrainedProplist_ObjectOfCurrentType, constraint.name());
			break;
		case Exact:
			formatString = definition ? Messages.ConstrainedProplist_ExactType : "'%s'"; //$NON-NLS-1$
			break;
		case Includes:
			formatString = Messages.ConstrainedProplist_Including;
			break;
		default:
			return IType.ERRONEOUS_TYPE;
		}
		return String.format(formatString, constraint instanceof IType ? ((IType)constraint).typeName(special) : constraint.toString());
	}
	
	@Override
	public String toString() { return typeName(true); }

	@Override
	public IType simpleType() {
		if (definition && !object)
			return PrimitiveType.ID;
		else if (!definition && object)
			return PrimitiveType.OBJECT;
		else if (definition && object)
			return TypeChoice.make(PrimitiveType.ID, PrimitiveType.OBJECT);
		else
			return PrimitiveType.ANY;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
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
			if (callerType == null)
				return ConstrainedProplist.object(constraint, ConstraintKind.Includes);
			else if (callerType != constraint() || context.script() != constraint())
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
	public Iterable<? extends Declaration> subDeclarations(Index contextIndex, int mask) {
		return constraint.subDeclarations(contextIndex, mask);
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
	public String name() {
		return typeName(true);
	}

	@Override
	public Collection<? extends IHasIncludes> includes(Index contextIndex, IHasIncludes origin, int options) {
		return constraint.includes(contextIndex, origin, options);
	}

	@Override
	public boolean doesInclude(Index contextIndex, IHasIncludes other) {
		return constraint.doesInclude(contextIndex, other);
	}

	@Override
	public boolean gatherIncludes(Index contextIndex, IHasIncludes origin, List<IHasIncludes> set, int options) {
		return constraint.gatherIncludes(contextIndex, origin, set, options);
	}

	@Override
	public PrimitiveType primitiveType() {
		return PrimitiveType.PROPLIST;
	}

}
