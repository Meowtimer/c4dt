package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.parser.ID;
import net.arctics.clonk.parser.DeclarationRegion;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.DeclarationObtainmentContext;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.IType;

public final class IDLiteral extends Literal<ID> {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	public IDLiteral(ID literal) {
		super(literal);
	}

	public ID idValue() {
		return getLiteral();
	}

	@Override
	public void doPrint(ExprWriter output, int depth) {
		output.append(idValue().getName());
	}

	@Override
	protected IType obtainType(DeclarationObtainmentContext context) {
		Definition obj = context.getContainer().nearestDefinitionWithId(idValue());
		return obj != null ? obj.getObjectType() : PrimitiveType.ID;
	}

	@Override
	public DeclarationRegion declarationAt(int offset, C4ScriptParser parser) {
		return new DeclarationRegion(parser.getContainer().nearestDefinitionWithId(idValue()), region(0));
	}

}