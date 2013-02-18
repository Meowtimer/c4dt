package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.ASTNodePrinter;
import net.arctics.clonk.parser.EntityRegion;
import net.arctics.clonk.parser.NameValueAssignment;
import net.arctics.clonk.parser.c4script.ProblemReportingContext;
import net.arctics.clonk.parser.stringtbl.StringTbl;

public class Placeholder extends ASTNode {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	protected String entryName;

	public Placeholder(String entryName) {
		this.entryName = entryName;
	}
	@Override
	public boolean isValidAtEndOfSequence() {
		return true;
	}
	@Override
	public boolean isValidInSequence(ASTNode predecessor) {
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
	public EntityRegion entityAt(int offset, ProblemReportingContext context) {
		StringTbl stringTbl = context.script().localStringTblMatchingLanguagePref();
		if (stringTbl != null) {
			NameValueAssignment entry = stringTbl.map().get(entryName);
			if (entry != null)
				return new EntityRegion(entry, this);
		}
		return super.entityAt(offset, context);
	}

	@Override
	public boolean hasSideEffects() {
		return true; // let's just assume that
	}

}