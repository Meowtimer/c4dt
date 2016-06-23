package net.arctics.clonk.c4script.ast;

import static java.lang.String.format;

import net.arctics.clonk.ProblemException;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodePrinter;
import net.arctics.clonk.ast.MatchingPlaceholder;
import net.arctics.clonk.c4script.Operator;

public class CombinedMatchingPlaceholder extends MatchingPlaceholder {
	private static final long serialVersionUID = 1L;
	private final MatchingPlaceholder left, right;
	private final Operator operator;

	public CombinedMatchingPlaceholder(final MatchingPlaceholder left, final MatchingPlaceholder right, final Operator operator) throws ProblemException {
		super();
		this.left = left;
		this.right = right;
		this.operator = operator;
		if (left.entryName().equals(right.entryName())) {
			this.entryName = left.entryName();
		} else {
			throw new IllegalArgumentException(
				format("Combined matching placeholder requires both operands to have same placeholder name (%s <> %s)",
					left.entryName(), right.entryName())
			);
		}
	}

	@Override
	public Multiplicity multiplicity() {
		final Integer absl = left.multiplicity().absolute();
		final Integer absr = right.multiplicity().absolute();
		return makeMultiplicity(Math.min(absl, absr));
	}

	@Override
	public boolean satisfiedBy(final ASTNode element) {
		switch (operator) {
		case And: case BitAnd:
			return left.satisfiedBy(element) && right.satisfiedBy(element);
		case Or: case BitOr:
			return left.satisfiedBy(element) || right.satisfiedBy(element);
		default:
			return false;
		}
	}

	@Override
	public void doPrint(final ASTNodePrinter output, final int depth) {
		left.print(output, depth);
		output.append(' ');
		output.append(operator.operatorName());
		output.append(' ');
		right.print(output, depth);
	}
}
