package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.DeclarationObtainmentContext;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.FunctionType;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.PrimitiveType;

/**
 * Call the sequence
 * @author madeen
 *
 */
public class CallExpr extends Value implements IFunctionCall {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	
	private ExprElm[] params;
	
	public CallExpr(ExprElm[] params) {
		this.params = params;
		assignParentToSubElements();
	}
	
	@Override
	public ExprElm[] params() {
		return params;
	}
	
	@Override
	public ExprElm[] subElements() {
		return params;
	}
	
	@Override
	public void setSubElements(ExprElm[] elms) {
		this.params = elms;
	}
	
	@Override
	public boolean isValidInSequence(ExprElm predecessor, C4ScriptParser context) {
		return predecessor != null;
	}
	
	@Override
	protected IType obtainType(DeclarationObtainmentContext context) {
		IType type = predecessorInSequence().obtainType(context);
		if (type instanceof FunctionType)
			return ((FunctionType)type).prototype().returnType();
		else
			return PrimitiveType.ANY;
	}
	
	@Override
	public void doPrint(ExprWriter output, int depth) {
		CallDeclaration.printParmString(output, params, depth);
	}
	
	@Override
	public boolean hasSideEffects() {
		return true;
	}

	@Override
	public Function function(DeclarationObtainmentContext context) {
		for (IType type : predecessorInSequence().obtainType(context))
			if (type instanceof FunctionType)
				return ((FunctionType)type).prototype();
		return null;
	}

	@Override
	public int parmsStart() {
		return this.start()+1;
	}
	
	@Override
	public int parmsEnd() {
		return this.end()-1;
	}
	
	@Override
	public int indexOfParm(ExprElm parm) {
		for (int i = 0; i < params.length; i++)
			if (params[i] == parm)
				return i;
		return -1;
	}
	
}
