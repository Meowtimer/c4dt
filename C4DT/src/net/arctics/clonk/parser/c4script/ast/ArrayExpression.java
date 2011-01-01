package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.C4Type;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.ast.evaluate.IEvaluationContext;
import net.arctics.clonk.util.Utilities;

public class ArrayExpression extends Sequence {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	public ArrayExpression(ExprElm... elms) {
		super(elms);
	}

	public void doPrint(ExprWriter output, int depth) {
		output.append("["); //$NON-NLS-1$
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] != null)
				elements[i].print(output, depth+1);
			if (i < elements.length-1)
				output.append(", "); //$NON-NLS-1$
		}
		output.append("]"); //$NON-NLS-1$
	}

	@Override
	public IType getType(C4ScriptParser context) {
		return C4Type.ARRAY;
	}

	@Override
	public boolean isValidInSequence(ExprElm predecessor, C4ScriptParser context) {
		return predecessor == null;
	}

	@Override
	public boolean modifiable(C4ScriptParser context) {
		return false;
	}
	
	@Override
	public Object evaluate(IEvaluationContext context) {
		return Utilities.map(getElements(), Object.class, Conf.EVALUATE_EXPR);
	}

}