package net.arctics.clonk.c4script.typing;

import net.arctics.clonk.ast.SourceLocation;
import net.arctics.clonk.c4script.Script;

/**
 * A type annotation in a {@link Script}.
 * @author madeen
 *
 */
public final class TypeAnnotation extends SourceLocation {
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
}