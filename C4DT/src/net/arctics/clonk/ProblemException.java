package net.arctics.clonk;

import net.arctics.clonk.c4script.ScriptParser;

/**
 * Exception thrown when a {@link Problem} comes up.
 */
public class ProblemException extends Exception {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	private ScriptParser parser;
	
	public ScriptParser parser() { return parser; }
	
	public ProblemException(final String msg) {
		super(msg);
	}
}