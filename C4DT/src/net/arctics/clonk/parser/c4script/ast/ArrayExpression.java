package net.arctics.clonk.parser.c4script.ast;

import java.util.ArrayList;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ExprElm;
import net.arctics.clonk.parser.c4script.ArrayType;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.DeclarationObtainmentContext;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.ast.evaluate.IEvaluationContext;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.IConverter;

public class ArrayExpression extends ExprElmWithSubElementsArray {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	public ArrayExpression(ExprElm... elms) {
		super(elms);
	}

	@Override
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
	public IType unresolvedType(final DeclarationObtainmentContext context) {
		return new ArrayType(
			null,
			ArrayUtil.map(elements, IType.class, new IConverter<ExprElm, IType>() {
				@Override
				public IType convert(ExprElm from) {
					return from != null ? from.type(context) : PrimitiveType.UNKNOWN;
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
	public boolean isConstant() {
		for (ExprElm e : subElements())
			if (e != null && !e.isConstant())
				return false;
		return true;
	}
	
	@Override
	public Object evaluate(IEvaluationContext context) throws ControlFlowException {
		ArrayList<Object> elm = new ArrayList<Object>(elements.length);
		for (int i = 0; i < elements.length; i++)
			elm.set(i, elements[i] != null ? elements[i].evaluate(context) : null);
		return elm;
	}

}