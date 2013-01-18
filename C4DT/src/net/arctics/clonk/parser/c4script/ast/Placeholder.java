package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ASTNodePrinter;
import net.arctics.clonk.parser.EntityRegion;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.NameValueAssignment;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.stringtbl.StringTbl;

public class Placeholder extends ASTNode {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	protected String entryName;

	public Placeholder(String entryName) {
		this.entryName = entryName;
	}
	@Override
	public boolean isValidAtEndOfSequence(C4ScriptParser context) {
		return true;
	}
	@Override
	public boolean isValidInSequence(ASTNode predecessor, C4ScriptParser context) {
		return true;
	}

	public String entryName() {
		return entryName;
	}
	@Override
	public void doPrint(ASTNodePrinter builder, int depth) {
		builder.append('$');
		builder.append(entryName);
		builder.append('$');
	}
	@Override
	public EntityRegion entityAt(int offset, C4ScriptParser parser) {
		StringTbl stringTbl = parser.script().localStringTblMatchingLanguagePref();
		if (stringTbl != null) {
			NameValueAssignment entry = stringTbl.map().get(entryName);
			if (entry != null)
				return new EntityRegion(entry, this);
		}
		return super.entityAt(offset, parser);
	}
	
	@Override
	public void reportProblems(C4ScriptParser parser) throws ParsingException {
		StringTbl.reportMissingStringTblEntries(parser, new EntityRegion(null, this, entryName));
	}
	
	@Override
	public boolean hasSideEffects() {
		return true; // let's just assume that
	}
	
}