package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.parser.ID;
import net.arctics.clonk.parser.EntityRegion;
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
		output.append(idValue().stringValue());
	}

	@Override
	protected IType obtainType(DeclarationObtainmentContext context) {
		Definition obj = context.containingScript().nearestDefinitionWithId(idValue());
		return obj != null ? obj.objectType() : PrimitiveType.ID;
	}

	@Override
	public EntityRegion declarationAt(int offset, C4ScriptParser parser) {
		return new EntityRegion(parser.containingScript().nearestDefinitionWithId(idValue()), region(0));
	}

}