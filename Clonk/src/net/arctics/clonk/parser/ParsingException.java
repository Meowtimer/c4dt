package net.arctics.clonk.parser;

/**
 * Exception thrown when a parsing error occurs.
 */
public class ParsingException extends Exception {

	private static final long serialVersionUID = 1L;

	public ParsingException(String msg) {
		super(msg);
	}
}