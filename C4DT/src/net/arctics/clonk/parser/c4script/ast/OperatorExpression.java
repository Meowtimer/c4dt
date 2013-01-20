package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.DeclarationObtainmentContext;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.Operator;

public class OperatorExpression extends ASTNode {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private final Operator operator;

	@Override
	public IType unresolvedType(DeclarationObtainmentContext context) {
		return operator.resultType();
	}

	public OperatorExpression(Operator operator) {
		super();
		this.operator = operator;
	}

	public Operator operator() {
		return operator;
	}

	@Override
	public boolean hasSideEffects() {
		return operator().modifiesArgument() || super.hasSideEffects();
	}

	@Override
	public boolean isModifiable(C4ScriptParser context) {
		return operator().returnsRef();
	}
	
	@Override
	public boolean equalAttributes(ASTNode other) {
		if (!super.equalAttributes(other))
			return false;
		if (operator != ((OperatorExpression)other).operator)
			return false;
		return true;
	}

}