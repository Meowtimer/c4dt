package net.arctics.clonk;

import net.arctics.clonk.c4script.C4ScriptParser;

/**
 * Exception thrown when a {@link Problem} comes up.
 */
public class ProblemException extends Exception {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	private C4ScriptParser parser;
	
	public C4ScriptParser parser() { return parser; }
	
	public ProblemException(String msg) {
		super(msg);
	}
}