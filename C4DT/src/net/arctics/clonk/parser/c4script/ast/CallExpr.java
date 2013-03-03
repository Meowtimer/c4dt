package net.arctics.clonk.parser.c4script.ast;

import static net.arctics.clonk.util.Utilities.defaulting;
import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.ASTNodePrinter;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.FunctionType;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.ProblemReportingContext;
import net.arctics.clonk.parser.c4script.Variable;

/**
 * Call the sequence
 * @author madeen
 *
 */
public class CallExpr extends Tuple implements IFunctionCall {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	public CallExpr(ASTNode[] params) { super(params); }
	@Override
	public ASTNode[] params() { return subElements(); }
	@Override
	public boolean isValidInSequence(ASTNode predecessor) { return predecessor != null; }
	@Override
	public void doPrint(ASTNodePrinter output, int depth) { CallDeclaration.printParmString(output, subElements(), depth); }
	@Override
	public boolean hasSideEffects() { return true; }
	@Override
	public int parmsStart() { return this.start()+1; }
	@Override
	public int parmsEnd() { return this.end()-1; }
	@Override
	public Function quasiCalledFunction(ProblemReportingContext context) {
		for (IType type : defaulting(predecessorInSequence().inferredType(), PrimitiveType.UNKNOWN))
			if (type instanceof FunctionType)
				return ((FunctionType)type).prototype();
		return null;
	}
	@Override
	public int indexOfParm(ASTNode parm) {
		for (int i = 0; i < elements.length; i++)
			if (elements[i] == parm)
				return i;
		return -1;
	}
	@Override
	public IType concreteParameterType(Variable parameter, ProblemReportingContext context) {
		Function f = quasiCalledFunction(context);
		if (f != null) {
			int ndx = f.parameters().indexOf(parameter);
			if (ndx != -1 && ndx < elements.length)
				return elements[ndx].inferredType();
		}
		return PrimitiveType.UNKNOWN;
	}
}
