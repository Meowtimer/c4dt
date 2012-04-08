package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.DeclarationObtainmentContext;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.IType;

public class ArraySliceExpression extends ArrayElementExpression {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private ExprElm argument2;
	
	public ArraySliceExpression(ExprElm lo, ExprElm hi) {
		super(lo);
		this.argument2 = hi;
	}
	
	public ExprElm lo() {
		return argument;
	}
	
	public ExprElm hi() {
		return argument2;
	}
	
	@Override
	public void doPrint(ExprWriter output, int depth) {
		output.append("["); //$NON-NLS-1$
		if (argument != null)
			argument.print(output, depth+1);
		output.append(":"); //$NON-NLS-1$
		if (argument2 != null)
			argument2.print(output, depth+1);
		output.append("]"); //$NON-NLS-1$
	}
	
	@Override
	public ExprElm[] subElements() {
		return new ExprElm[] {argument, argument2};
	}
	@Override
	public void setSubElements(ExprElm[] subElements) {
		argument  = subElements[0];
		argument2 = subElements[1];
	}
	
	@Override
	public void reportErrors(C4ScriptParser parser) throws ParsingException {
		super.reportErrors(parser);
		if (argument2 != null)
			argument2.reportErrors(parser);
	}
	
	@Override
	public boolean isModifiable(C4ScriptParser context) {
		return false;
	}
	
	@Override
	protected IType obtainType(DeclarationObtainmentContext context) {
		return PrimitiveType.ARRAY;
	}
	
}