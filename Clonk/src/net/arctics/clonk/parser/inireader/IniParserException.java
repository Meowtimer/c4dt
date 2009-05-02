package net.arctics.clonk.parser.inireader;

public class IniParserException extends Exception {
	
	private static final long serialVersionUID = 1044904198922273368L;
	private int severity;
	private int offset;
	private int endOffset;
	private Exception innerException;
	
	public IniParserException(int severity, String message) {
		this(severity,message,0,0);
	}
	
	public IniParserException(int severity, String message, int offset, int endOffset) {
		super(message);
		this.severity = severity;
		this.offset = offset;
		this.endOffset = endOffset;
	}
	
	public int getSeverity() {
		return severity;
	}

	public int getOffset() {
		return offset;
	}

	public int getEndOffset() {
		return endOffset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	public void setEndOffset(int endOffset) {
		this.endOffset = endOffset;
	}

	public Exception getInnerException() {
		return innerException;
	}

	public void setInnerException(Exception innerException) {
		this.innerException = innerException;
	}
}