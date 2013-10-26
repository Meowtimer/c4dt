package net.arctics.clonk.c4script;

import java.util.Arrays;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.c4script.ast.AccessVar;
import net.arctics.clonk.c4script.ast.BinaryOp;
import net.arctics.clonk.c4script.ast.FunctionBody;
import net.arctics.clonk.c4script.ast.SimpleStatement;

public class InitializationFunction extends SynthesizedFunction {
	public static final class VarInitializationAccess extends AccessVar {
		private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
		public VarInitializationAccess(Declaration declaration) { super(declaration); }
	}
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private final Variable variable;
	public InitializationFunction(Variable variable) {
		this.variable = variable;
	}
	public Variable variable() { return variable; }
	@Override
	public void storeBody(ASTNode body, String source) {
		if (body == null) {
			// oh well
			super.storeBody(new FunctionBody(this), source);
			return;
		}
		final VarInitializationAccess leftSide = new VarInitializationAccess(variable);
		leftSide.setLocation(variable.start()-bodyLocation().start(), variable.end()-bodyLocation().start());
		super.storeBody(new FunctionBody(this, Arrays.asList(
			(ASTNode)new SimpleStatement(new BinaryOp(Operator.Assign,
				leftSide, body)
			))), source);
	}
}
