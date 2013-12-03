package net.arctics.clonk.c4script.typing;

import static net.arctics.clonk.util.ArrayUtil.concat;
import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.defaulting;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.EntityRegion;
import net.arctics.clonk.ast.ExpressionLocator;
import net.arctics.clonk.ast.Placeholder;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.index.IIndexEntity;

/**
 * A type annotation in a {@link Script}.
 * @author madeen
 *
 */
public final class TypeAnnotation extends ASTNode implements IType {
	private static final long serialVersionUID = 1L;
	private ITypeable target;
	private IType type;
	private TypeAnnotation[] subAnnotations;
	public TypeAnnotation(final int start, final int end, final IType type) {
		super(start, end);
		this.type = type;
	}
	/** The typeable element this annotation is targeted at */
	public ITypeable target() {return target;}
	/** Set the target of this annotation. */
	public void setTarget(final ITypeable typeable) {
		if (this.target != null && this.target != typeable)
			throw new IllegalArgumentException();
		else
			this.target = typeable;
	}
	/** The type this annotation refers to. */
	public IType type() {return type;}
	/** Set the type of this annotation. */
	public void setType(final IType type) {
		this.type = type;
		assignParentToSubElements();
	}
	@Override
	public ASTNode[] subElements() { return concat(as(type, Placeholder.class), defaulting(subAnnotations, EMPTY_EXPR_ARRAY)); }
	@Override
	public void setSubElements(final ASTNode[] elms) {
		type = (IType)elms[0];
		subAnnotations = elms.length > 1 ? Arrays.copyOfRange(elms, 1, elms.length, TypeAnnotation[].class) : null;
		assignParentToSubElements();
	}
	public void setSubAnnotations(final TypeAnnotation[] subAnnotations) {
		this.subAnnotations = subAnnotations;
		assignParentToSubElements();
	}
	@Override
	public EntityRegion entityAt(final int offset, final ExpressionLocator<?> locator) {
		if (type == null)
			return null;
		final Set<IIndexEntity> entities = new HashSet<>();
		for (final IType t : type)
			if (t instanceof IIndexEntity)
				entities.add((IIndexEntity)t);
		return entities.size() > 0 ? new EntityRegion(entities, this) : null;
	}
	@Override
	public String printed() { return type.typeName(true); }
	@Override
	public String toString() { return printed(); }
	@Override
	public boolean equalAttributes(final ASTNode other) { return super.equalAttributes(other); }
	@Override
	public Iterator<IType> iterator() { return null; }
	@Override
	public String typeName(final boolean special) { return type.typeName(special); }
	@Override
	public IType simpleType() { return type.simpleType(); }
}