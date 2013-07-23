package net.arctics.clonk.c4script.typing;

import java.util.HashSet;
import java.util.Set;

import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.EntityRegion;
import net.arctics.clonk.ast.ExpressionLocator;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.index.IIndexEntity;

/**
 * A type annotation in a {@link Script}.
 * @author madeen
 *
 */
public final class TypeAnnotation extends ASTNode {
	private static final long serialVersionUID = 1L;
	private ITypeable typeable;
	private IType type;
	/** The typeable element this annotation is targeted at */
	public ITypeable target() {return typeable;}
	/** Set the target of this annotation. */
	public void setTarget(ITypeable typeable) {
		if (this.typeable != null && this.typeable != typeable)
			throw new IllegalArgumentException();
		else
			this.typeable = typeable;
	}
	/** The type this annotation refers to. */
	public IType type() {return type;}
	/** Set the type of this annotation. */
	public void setType(IType type) {this.type = type;}
	public TypeAnnotation(int start, int end) {super(start, end);}
	@Override
	public EntityRegion entityAt(int offset, ExpressionLocator<?> locator) {
		if (type == null)
			return null;
		final Set<IIndexEntity> entities = new HashSet<>();
		for (final IType t : type)
			if (t instanceof IIndexEntity)
				entities.add((IIndexEntity)t);
		return entities.size() > 0 ? new EntityRegion(entities, absolute()) : null;
	}
}