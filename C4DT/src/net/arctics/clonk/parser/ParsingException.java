package net.arctics.clonk.parser;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.c4script.C4ScriptParser;

/**
 * Exception thrown when a parsing error occurs.
 */
public class ParsingException extends Exception {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	private C4ScriptParser parser;
	
	public C4ScriptParser parser() { return parser; }
	
	public ParsingException(String msg) {
		super(msg);
	}
}