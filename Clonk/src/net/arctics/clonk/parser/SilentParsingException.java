package net.arctics.clonk.parser;

/**
 * Special parsing exception thrown when the error is not supposed to be shown to the user (in form of error markers in the Errors view for example).
 * Used when calling the parser internally to support content assistance and similar things.
 */
public class SilentParsingException extends ParsingException {

	private static final long serialVersionUID = 1L;
	
	public SilentParsingException(String msg) {
		super(msg);
	}
	
}