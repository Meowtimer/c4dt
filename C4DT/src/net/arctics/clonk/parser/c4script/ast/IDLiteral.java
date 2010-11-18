package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.C4Object;
import net.arctics.clonk.parser.C4ID;
import net.arctics.clonk.parser.DeclarationRegion;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.C4Type;
import net.arctics.clonk.parser.c4script.IType;

public final class IDLiteral extends Literal<C4ID> {
	/**
	 * 
	 */
	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	public IDLiteral(C4ID literal) {
		super(literal);
	}

	public C4ID idValue() {
		return getLiteral();
	}

	@Override
	public void doPrint(ExprWriter output, int depth) {
		output.append(idValue().getName());
	}

	@Override
	public IType getType(C4ScriptParser context) {
		C4Object obj = context.getContainer().getNearestObjectWithId(idValue());
		return obj != null ? obj.getObjectType() : C4Type.ID;
	}

	@Override
	public DeclarationRegion declarationAt(int offset, C4ScriptParser parser) {
		return new DeclarationRegion(parser.getContainer().getNearestObjectWithId(idValue()), region(0));
	}

}