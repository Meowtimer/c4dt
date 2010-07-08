package net.arctics.clonk.parser;

import net.arctics.clonk.ClonkCore;

/**
 * Exception thrown when a parsing error occurs.
 */
public class ParsingException extends Exception {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	public ParsingException(String msg) {
		super(msg);
	}
}