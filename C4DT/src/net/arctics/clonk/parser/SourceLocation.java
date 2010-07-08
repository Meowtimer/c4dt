package net.arctics.clonk.parser;

import java.io.Serializable;
import java.util.regex.Matcher;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.c4script.C4Function;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

public class SourceLocation implements IRegion, Serializable {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	
	private int start, end;
	public SourceLocation(int start,int end) {
		this.setStart(start);
		this.setEnd(end);
	}
	public SourceLocation(Matcher matcher) {
		this.setStart(matcher.start());
		this.setEnd(matcher.end());
	}
	public SourceLocation(IRegion region, C4Function relative) {
		this.setStart(relative.getBody().getStart()+region.getOffset());
		this.setEnd(relative.getBody().getStart()+region.getOffset()+region.getLength());
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
	public int getLength() {
		return getEnd()-getStart();
	}
	public int getOffset() {
		return getStart();
	}
	
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (!(obj instanceof SourceLocation)) return false;
		SourceLocation cmp = (SourceLocation) obj;
		return (cmp.getStart() == start && cmp.getEnd() == end);
	}
	
	public String getString(IDocument document) throws BadLocationException {
		return document.get(start, end-start);
	}
	
	@Override
	public String toString() {
		return "("+getStart()+", "+getEnd()+")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
	
	// TODO: hashcode() implementation
}
