package net.arctics.clonk.parser.c4script.ast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.ASTNodePrinter;
import net.arctics.clonk.parser.IEvaluationContext;
import net.arctics.clonk.parser.c4script.C4ScriptParser;

public class Sequence extends ASTNodeWithSubElementsArray {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	public Sequence(ASTNode[] elms, int num) { this(Arrays.copyOf(elms, num)); }

	public Sequence(ASTNode... elms) {
		super(elms);
		ASTNode prev = null;
		for (ASTNode e : elements) {
			if (e != null)
				e.setPredecessorInSequence(prev);
			prev = e;
		}
	}
	public Sequence(List<ASTNode> elms) {
		this(elms.toArray(new ASTNode[elms.size()]));
	}
	@Override
	public void doPrint(ASTNodePrinter output, int depth) {
		for (ASTNode e : elements)
			e.print(output, depth+1);
	}
	public Statement[] splitIntoValidSubStatements(C4ScriptParser parser) {
		List<ASTNode> currentSequenceExpressions = new LinkedList<ASTNode>();
		List<Statement> result = new ArrayList<Statement>(elements.length);
		ASTNode p = null;
		for (ASTNode e : elements) {
			if (!e.isValidInSequence(p)) {
				result.add(SimpleStatement.wrapExpression(new Sequence(currentSequenceExpressions)));
				currentSequenceExpressions.clear();
			}
			currentSequenceExpressions.add(e);
			p = e;
		}
		if (result.size() == 0)
			return new Statement[] {SimpleStatement.wrapExpression(this)};
		else {
			result.add(SimpleStatement.wrapExpression(new Sequence(currentSequenceExpressions)));
			return result.toArray(new Statement[result.size()]);
		}
	}
	public Sequence subSequenceUpTo(ASTNode elm) {
		List<ASTNode> list = new ArrayList<ASTNode>(elements.length);
		for (ASTNode e : elements)
			if (e == elm)
				break;
			else
				list.add(e);
		if (list.size() > 0) {
			Sequence s = new Sequence(list);
			s.setParent(parent());
			return s;
		} else
			return null;
	}
	public Sequence subSequenceIncluding(ASTNode elm) {
		List<ASTNode> list = new ArrayList<ASTNode>(elements.length);
		for (ASTNode e : elements) {
			list.add(e);
			if (e == elm)
				break;
		}
		if (list.size() > 0) {
			Sequence s = new Sequence(list);
			s.setParent(parent());
			return s;
		} else
			return null;
	}
	public ASTNode successorOfSubElement(ASTNode element) {
		for (int i = 0; i < elements.length; i++)
			if (elements[i] == element)
				return i+1 < elements.length ? elements[i+1] : null;
		return null;
	}
	@Override
	public Object evaluate(IEvaluationContext context) throws ControlFlowException {
		return lastElement().evaluate(context);
	}
}