package net.arctics.clonk.parser;

import java.io.Serializable;
import java.util.regex.Matcher;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.c4script.Function;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

public class SourceLocation implements IRegion, Serializable, Cloneable {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	
	private int start, end;
	public SourceLocation(int start,int end) {
		this.start = start;
		this.end = end;
	}
	public SourceLocation(Matcher matcher) {
		start = matcher.start();
		end = matcher.end();
	}
	public SourceLocation(IRegion region, Function relative) {
		start = relative.getBody().getStart()+region.getOffset();
		end = relative.getBody().getStart()+region.getOffset()+region.getLength();
	}
	public SourceLocation(int offset, IRegion relativeLocation) {
		start = offset+relativeLocation.getOffset();
		end = offset+relativeLocation.getOffset()+relativeLocation.getLength();
	}
	
	/**
	 * @param start the start to set
	 */
	public void setStart(int start) {
		this.start = start;
	}
	/**
	 * @return the start
	 */
	public int getStart() {
		return start;
	}
	/**
	 * @param end the end to set
	 */
	public void setEnd(int end) {
		this.end = end;
	}
	/**
	 * @return the end
	 */
	public int getEnd() {
		return end;
	}
	@Override
	public int getLength() {
		return getEnd()-getStart();
	}
	@Override
	public int getOffset() {
		return getStart();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SourceLocation) {
			SourceLocation cmp = (SourceLocation) obj;
			return (cmp.getStart() == start && cmp.getEnd() == end);
		}
		else
			return false;
	}
	
	public String getString(IDocument document) throws BadLocationException {
		return document.get(start, end-start);
	}
	
	@Override
	public String toString() {
		return "("+getStart()+", "+getEnd()+")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	// http://stackoverflow.com/questions/113511/hash-code-implementation -.-
	@Override
	public int hashCode() {
		return (start ^ start >>> 32) * 37 + (end ^ end >>> 32) * 37;
	}
	
	public SourceLocation offset(int o) {
		return new SourceLocation(o+start, o+end);
	}
	
	@Override
	public SourceLocation clone() throws CloneNotSupportedException {
		return new SourceLocation(start, end);
	}

}
