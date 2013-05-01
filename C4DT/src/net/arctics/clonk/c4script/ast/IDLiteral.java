package net.arctics.clonk.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodePrinter;
import net.arctics.clonk.ast.EntityRegion;
import net.arctics.clonk.ast.IEntityLocator;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.ID;

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
	public void doPrint(ASTNodePrinter output, int depth) {
		output.append(idValue().stringValue());
	}

	@Override
	public EntityRegion entityAt(int offset, IEntityLocator locator) {
		return new EntityRegion(definition(), region(0));
	}
	
	public Definition definition() {
		return parentOfType(Script.class).nearestDefinitionWithId(idValue());
	}

	@Override
	public boolean allowsSequenceSuccessor(ASTNode successor) { return true; }

}