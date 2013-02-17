package net.arctics.clonk.parser.c4script;

import java.util.Arrays;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.c4script.ast.AccessVar;
import net.arctics.clonk.parser.c4script.ast.BinaryOp;
import net.arctics.clonk.parser.c4script.ast.FunctionBody;
import net.arctics.clonk.parser.c4script.ast.SimpleStatement;

public class InitializationFunction extends Function {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private final Variable variable;
	public InitializationFunction(Variable variable) {
		this.variable = variable;
	}
	public Variable variable() { return variable; }
	@Override
	public void storeBody(ASTNode body, String source) {
		super.storeBody(new FunctionBody(this, Arrays.asList(
			(ASTNode)new SimpleStatement(new BinaryOp(Operator.Assign, new AccessVar(variable), body)))
		), source);
	}
}
