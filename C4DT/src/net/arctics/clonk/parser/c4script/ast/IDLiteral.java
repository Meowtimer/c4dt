package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.ASTNodePrinter;
import net.arctics.clonk.parser.EntityRegion;
import net.arctics.clonk.parser.ID;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.ProblemReportingContext;

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
	public EntityRegion entityAt(int offset, ProblemReportingContext context) {
		return new EntityRegion(definition(context), region(0));
	}
	
	public Definition definition(ProblemReportingContext context) {
		return context.script().nearestDefinitionWithId(idValue());
	}

	@Override
	public boolean allowsSequenceSuccessor(C4ScriptParser context, ASTNode successor) { return true; }

}