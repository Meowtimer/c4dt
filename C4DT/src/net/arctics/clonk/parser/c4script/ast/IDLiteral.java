package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.parser.EntityRegion;
import net.arctics.clonk.parser.ID;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.DeclarationObtainmentContext;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.PrimitiveType;

public final class IDLiteral extends Literal<ID> {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private final ID literal;
	
	public IDLiteral(ID literal) {
		this.literal = literal;
	}
	
	@Override
	public ID literal() {
		return literal;
	}

	public ID idValue() {
		return literal;
	}

	@Override
	public void doPrint(ExprWriter output, int depth) {
		output.append(idValue().stringValue());
	}

	@Override
	public IType unresolvedType(DeclarationObtainmentContext context) {
		Definition obj = context.script().nearestDefinitionWithId(idValue());
		return obj != null ? obj.objectType() : PrimitiveType.ID;
	}

	@Override
	public EntityRegion entityAt(int offset, C4ScriptParser parser) {
		return new EntityRegion(parser.script().nearestDefinitionWithId(idValue()), region(0));
	}
	
	@Override
	public boolean allowsSequenceSuccessor(C4ScriptParser context, ExprElm successor) { return true; }

}