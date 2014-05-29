package net.arctics.clonk.ini;
import net.arctics.clonk.Core;
public class IniParserException extends Exception {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private final int severity;
	private int offset;
	private int endOffset;
	private final Exception innerException;
	public IniParserException(final int severity, final String message) {
		this(severity,message, 0, 0, null);
	}
	public IniParserException(final int severity, final String message, final Exception innerException) {
		this(severity, message, 0, 0, innerException);
	}
	public IniParserException(final int severity, final String message, final int offset, final int endOffset, final Exception innerException) {
		super(message);
		this.severity = severity;
		this.offset = offset;
		this.endOffset = endOffset;
		this.innerException = innerException;
	}
	public int severity() { return severity; }
	public int offset() { return offset; }
	public int endOffset() { return endOffset; }
	public Exception innerException() { return innerException; }
	public void offsets(int start, int end) {
		this.offset = start;
		this.endOffset = end;
	}
}