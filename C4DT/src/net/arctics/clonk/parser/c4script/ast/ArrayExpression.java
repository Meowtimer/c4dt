package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.c4script.ArrayType;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.DeclarationObtainmentContext;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.ast.evaluate.IEvaluationContext;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.IConverter;

public class ArrayExpression extends ExprElmWithSubElementsArray {

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
	protected IType obtainType(final DeclarationObtainmentContext context) {
		return new ArrayType(
			null,
			ArrayUtil.map(elements, IType.class, new IConverter<ExprElm, IType>() {
				@Override
				public IType convert(ExprElm from) {
					return from != null ? from.getType(context) : PrimitiveType.UNKNOWN;
				}
			})
		);
	}

	@Override
	public boolean isValidInSequence(ExprElm predecessor, C4ScriptParser context) {
		return predecessor == null;
	}

	@Override
	public boolean isModifiable(C4ScriptParser context) {
		return false;
	}
	
	@Override
	public Object evaluate(IEvaluationContext context) {
		return ArrayUtil.map(elements, Object.class, Conf.EVALUATE_EXPR);
	}

}