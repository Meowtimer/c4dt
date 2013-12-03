package net.arctics.clonk.ast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.arctics.clonk.Core;

public class Sequence extends ASTNodeWithSubElementsArray {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	public Sequence(final ASTNode[] elms, final int num) { this(Arrays.copyOf(elms, num)); }

	public Sequence(final ASTNode... elms) {
		super(elms);
		ASTNode prev = null;
		for (final ASTNode e : elements) {
			if (e != null)
				e.setPredecessor(prev);
			prev = e;
		}
	}
	public Sequence(final List<ASTNode> elms) {
		this(elms.toArray(new ASTNode[elms.size()]));
	}
	@Override
	public void doPrint(final ASTNodePrinter output, final int depth) {
		for (final ASTNode e : elements)
			e.print(output, depth);
	}
	public Sequence subSequenceUpTo(final ASTNode elm) {
		final List<ASTNode> list = new ArrayList<ASTNode>(elements.length);
		for (final ASTNode e : elements)
			if (e == elm)
				break;
			else
				list.add(e);
		if (list.size() > 0) {
			final Sequence s = new Sequence(list);
			s.setParent(parent());
			return s;
		} else
			return null;
	}
	public Sequence subSequenceIncluding(final ASTNode elm) {
		final List<ASTNode> list = new ArrayList<ASTNode>(elements.length);
		for (final ASTNode e : elements) {
			list.add(e);
			if (e == elm)
				break;
		}
		if (list.size() > 0) {
			final Sequence s = new Sequence(list);
			s.setParent(parent());
			return s;
		} else
			return null;
	}
	public ASTNode successorOfSubElement(final ASTNode element) {
		for (int i = 0; i < elements.length; i++)
			if (elements[i] == element)
				return i+1 < elements.length ? elements[i+1] : null;
		return null;
	}
	@Override
	public Object evaluate(final IEvaluationContext context) throws ControlFlowException {
		return lastElement().evaluate(context);
	}
	@Override
	public void postLoad(final ASTNode parent) {
		super.postLoad(parent);
		ASTNode prev = null;
		for (final ASTNode e : subElements()) {
			if (e != null)
				e.setPredecessor(prev);
			prev = e;
		}
	}
}