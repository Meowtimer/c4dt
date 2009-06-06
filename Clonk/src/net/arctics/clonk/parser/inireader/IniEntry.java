package net.arctics.clonk.parser.inireader;

import net.arctics.clonk.parser.NameValueAssignment;

public class IniEntry extends NameValueAssignment {
	
	public IniEntry(int pos, int endPos, String k, String v) {
		super(pos, endPos, k, v);
	}

	private static final long serialVersionUID = 1L;
	
}