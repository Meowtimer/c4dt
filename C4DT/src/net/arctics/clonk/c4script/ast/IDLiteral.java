package net.arctics.clonk.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodePrinter;
import net.arctics.clonk.ast.EntityRegion;
import net.arctics.clonk.ast.ExpressionLocator;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.ID;

public final class IDLiteral extends Literal<ID> {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private final ID literal;
	public IDLiteral(final ID literal) { this.literal = literal; }
	@Override
	public ID literal() { return literal; }
	public ID idValue() { return literal; }
	@Override
	public void doPrint(final ASTNodePrinter output, final int depth) { output.append(idValue().stringValue()); }
	@Override
	public EntityRegion entityAt(final int offset, final ExpressionLocator<?> locator) { return new EntityRegion(definition(), region(0)); }
	public Definition definition() {
		final Script script = parent(Script.class);
		return script != null && idValue() != null ? script.nearestDefinitionWithId(idValue()) : null;
	}
	@Override
	public boolean allowsSequenceSuccessor(final ASTNode successor) { return true; }
}