package net.arctics.clonk.parser;

import static net.arctics.clonk.util.Utilities.as;

import java.util.HashMap;
import java.util.Map;

import net.arctics.clonk.parser.c4script.ast.ASTComparisonDelegate;
import net.arctics.clonk.parser.c4script.ast.Block;
import net.arctics.clonk.parser.c4script.ast.MatchingPlaceholder;
import net.arctics.clonk.parser.c4script.ast.MatchingPlaceholder.Multiplicity;
import net.arctics.clonk.util.ArrayUtil;

public class ASTNodeMatcher extends ASTComparisonDelegate {
	public Map<String, Object> result;
	@Override
	public boolean ignoreClassDifference() {
		if (left instanceof MatchingPlaceholder) {
			MatchingPlaceholder mp = (MatchingPlaceholder)left;
			if (mp.satisfiedBy(right) && mp.multiplicity() == Multiplicity.One)
				return true;
		}
		else if (left instanceof Block && right instanceof Block)
			return true;
		return false;
	}
	@Override
	public boolean consume(ASTNode consumer, ASTNode extra) {
		MatchingPlaceholder mp = as(consumer, MatchingPlaceholder.class);
		if (mp != null && mp.multiplicity() != Multiplicity.One)
			if (mp.satisfiedBy(extra)) {
				ASTNode[] mpSubElements = mp.subElements();
				if (mpSubElements.length > 0) {
					ASTNode oldLeft = left; left = mp;
					ASTNode oldRight = right; right = extra;
					boolean result = compareSubElements(mpSubElements, extra.subElements()) != null;
					left = oldLeft; right = oldRight;
					return result;
				} else
					return true;
			}
		return false;
	}
	@Override
	public void applyLeftToRightMapping(ASTNode[] leftSubElements, ASTNode[][] leftToRightMapping) {
		if (left instanceof MatchingPlaceholder)
			addToResult(right, (MatchingPlaceholder)left);
		for (int i = 0; i < leftSubElements.length; i++) {
			ASTNode left = leftSubElements[i];
			if (left instanceof MatchingPlaceholder)
				if (leftToRightMapping[i] != null)
					for (int r = 0; r < leftToRightMapping[i].length; r++)
						addToResult(leftToRightMapping[i][r], (MatchingPlaceholder)left);
				else
					System.out.println("wat");
		}
	}
	private void addToResult(ASTNode extra, MatchingPlaceholder mp) {
		if (result == null)
			result = new HashMap<String, Object>();
		Object existing = result.get(mp.entryName());
		if (existing instanceof ASTNode)
			existing = new ASTNode[] {(ASTNode)existing, extra};
		else if (existing instanceof ASTNode[])
			existing = ArrayUtil.concat((ASTNode[])existing, extra);
		else
			existing = extra;
		result.put(mp.entryName(), existing);
	}
	@Override
	public boolean ignoreLeftSubElement(ASTNode leftNode) {
		return leftNode instanceof MatchingPlaceholder && ((MatchingPlaceholder)leftNode).multiplicity() == Multiplicity.Multiple;
	}
	@Override
	public boolean ignoreSubElementDifference(ASTNode left, ASTNode right) {
		MatchingPlaceholder mp = as(left, MatchingPlaceholder.class);
		return mp != null && mp.multiplicity() == Multiplicity.One && mp.subElements().length == 0;
	};
}