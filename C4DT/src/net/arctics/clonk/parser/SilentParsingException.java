package net.arctics.clonk.parser;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.c4script.C4ScriptParser;

/**
 * Special parsing exception thrown when the error is not supposed to be shown to the user (in form of error markers in the Errors view for example).
 * Used when calling the parser internally to support content assistance and similar things.
 */
public class SilentParsingException extends ParsingException {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	
	private final Reason reason;
	
	public enum Reason {
		SilenceRequested,
		Cancellation
	}
	
	public SilentParsingException(Reason reason, String msg, C4ScriptParser parser) {
		super(msg, parser);
		this.reason = reason;
	}
	
	public Reason getReason() {
		return reason;
	}
	
}