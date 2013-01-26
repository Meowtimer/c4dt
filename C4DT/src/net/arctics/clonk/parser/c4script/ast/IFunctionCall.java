package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.c4script.ProblemReportingContext;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.Variable;

/**
 * Interface implemented by expressions representing a function call.
 * @author madeen
 *
 */
public interface IFunctionCall {
	ASTNode[] params();
	Function quasiCalledFunction(ProblemReportingContext context);
	int parmsStart();
	int parmsEnd();
	int indexOfParm(ASTNode parm);
	IType concreteParameterType(Variable parameter, ProblemReportingContext context);
}
