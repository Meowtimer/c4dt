package net.arctics.clonk.parser.defcore;

/**
 * 
 * @author ZokRadonh
 *
 */
public class DefCoreParserException extends Exception {

	private static final long serialVersionUID = -5687352146082560705L;
	
	private int severity;
	
	public DefCoreParserException(int severity, String message) {
		super(message);
		this.severity = severity;
	}
	
	public int getSeverity() {
		return severity;
	}
}
