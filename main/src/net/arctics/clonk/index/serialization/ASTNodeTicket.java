package net.arctics.clonk.index.serialization;

import java.io.Serializable;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ast.IASTVisitor;
import net.arctics.clonk.ast.TraversalContinuation;
import net.arctics.clonk.c4script.IHasCode;
import net.arctics.clonk.index.IDeserializationResolvable;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.IndexEntity;

public class ASTNodeTicket implements IDeserializationResolvable, Serializable, IASTVisitor<Object> {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private final Declaration owner;
	private final String textRepresentation;
	private final int depth;
	private transient ASTNode found;
	private static int depth(ASTNode elm) {
		int depth;
		for (depth = 1, elm = elm.parent(); elm != null; elm = elm.parent(), depth++) {
			;
		}
		return depth;
	}
	public ASTNodeTicket(final Declaration owner, final ASTNode elm) {
		this.owner = owner;
		this.textRepresentation = elm.toString();
		this.depth = depth(elm);
	}
	@Override
	public Object resolve(final Index index, final IndexEntity deserializee) {
		if (owner instanceof IHasCode) {
			if (owner instanceof IndexEntity) {
				((IndexEntity) owner).requireLoaded();
			}
			final ASTNode code = ((IHasCode) owner).code();
			if (code != null) {
				code.traverse(this, null);
			}
			return found;
		}
		return null;
	}
	@Override
	public TraversalContinuation visitNode(final ASTNode expression, final Object context) {
		final int ed = depth(expression);
		if (ed == depth && textRepresentation.equals(expression.toString())) {
			found = expression;
			return TraversalContinuation.Cancel;
		}
		else if (ed > depth) {
			return TraversalContinuation.SkipSubElements;
		} else {
			return TraversalContinuation.Continue;
		}
	}
}