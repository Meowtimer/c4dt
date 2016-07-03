package net.arctics.clonk.c4script.ast;

import net.arctics.clonk.ast.ASTNode;

/**
 * Interface implemented by expressions representing a function call.
 * @author madeen
 *
 */
public interface IFunctionCall {
	ASTNode[] params();
	int parmsStart();
	int parmsEnd();
	int indexOfParm(ASTNode parm);
}
