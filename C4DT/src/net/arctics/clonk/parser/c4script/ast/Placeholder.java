package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.DeclarationRegion;
import net.arctics.clonk.parser.NameValueAssignment;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.stringtbl.StringTbl;

public class Placeholder extends ExprElm {
	/**
	 * 
	 */
	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	private String entryName;

	public Placeholder(String entryName) {
		this.entryName = entryName;
	}

	public String getEntryName() {
		return entryName;
	}
	@Override
	public void doPrint(ExprWriter builder, int depth) {
		builder.append('$');
		builder.append(entryName);
		builder.append('$');
	}
	@Override
	public DeclarationRegion declarationAt(int offset, C4ScriptParser parser) {
		StringTbl stringTbl = parser.getContainer().getStringTblForLanguagePref();
		if (stringTbl != null) {
			NameValueAssignment entry = stringTbl.getMap().get(entryName);
			if (entry != null)
				return new DeclarationRegion(entry, this);
		}
		return super.declarationAt(offset, parser);
	}
}