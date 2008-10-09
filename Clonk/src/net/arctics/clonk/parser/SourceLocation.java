package net.arctics.clonk.parser;

import java.util.regex.Matcher;

import org.eclipse.jface.text.IRegion;

public class SourceLocation implements IRegion {
	private int start, end;
	public SourceLocation(int start,int end) {
		this.setStart(start);
		this.setEnd(end);
	}
	public SourceLocation(Matcher matcher) {
		this.setStart(matcher.start());
		this.setEnd(matcher.end());
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
	
	// TODO: hashcode() implementation
}
