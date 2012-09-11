package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.parser.c4script.DeclarationObtainmentContext;
import net.arctics.clonk.parser.c4script.Function;

/**
 * Interface implemented by expressions representing a function call.
 * @author madeen
 *
 */
public interface IFunctionCall {
	ExprElm[] params();
	Function quasiCalledFunction(DeclarationObtainmentContext context);
	int parmsStart();
	int parmsEnd();
	int indexOfParm(ExprElm parm);
}
