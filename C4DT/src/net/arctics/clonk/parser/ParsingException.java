package net.arctics.clonk.parser;

import net.arctics.clonk.Core;

/**
 * Exception thrown when a parsing error occurs.
 */
public class ParsingException extends Exception {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	public ParsingException(String msg) {
		super(msg);
	}
}