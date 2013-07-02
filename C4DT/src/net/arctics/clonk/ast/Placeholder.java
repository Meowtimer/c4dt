package net.arctics.clonk.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.stringtbl.StringTbl;

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
	public EntityRegion entityAt(int offset, ExpressionLocator<?> locator) {
		final StringTbl stringTbl = parent(Script.class).localStringTblMatchingLanguagePref();
		if (stringTbl != null) {
			final NameValueAssignment entry = stringTbl.map().get(entryName);
			if (entry != null)
				return new EntityRegion(entry, this);
		}
		return super.entityAt(offset, locator);
	}

	@Override
	public boolean hasSideEffects() {
		return true; // let's just assume that
	}

}