package net.arctics.clonk.parser;

import java.io.Serializable;
import java.util.regex.Matcher;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.c4script.Function;

import org.eclipse.jface.text.IRegion;

public class SourceLocation implements IRegion, Serializable, Cloneable, Comparable<SourceLocation> {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	public static final SourceLocation ZERO = new SourceLocation(0, 0);
	
	protected int start, end;
	
	public SourceLocation() {}
	
	public SourceLocation(int start,int end) {
		this.start = start;
		this.end = end;
	}
	public SourceLocation(Matcher matcher) {
		start = matcher.start();
		end = matcher.end();
	}
	public SourceLocation(IRegion region, Function relative) {
		start = relative.bodyLocation().start()+region.getOffset();
		end = relative.bodyLocation().start()+region.getOffset()+region.getLength();
	}
	public SourceLocation(int offset, IRegion relativeLocation) {
		start = offset+relativeLocation.getOffset();
		end = offset+relativeLocation.getOffset()+relativeLocation.getLength();
	}
	public SourceLocation(String stringRepresentation) {
		int comma = stringRepresentation.indexOf(",");
		start = Integer.parseInt(stringRepresentation.substring(1, comma));
		end = Integer.parseInt(stringRepresentation.substring(comma+2, stringRepresentation.length()-1));
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
	public int start() {
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
	public int end() {
		return end;
	}
	@Override
	public int getLength() {
		return end-start;
	}
	@Override
	public int getOffset() {
		return start;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SourceLocation) {
			SourceLocation cmp = (SourceLocation) obj;
			return (cmp.start() == start && cmp.end() == end);
		}
		else
			return false;
	}
	
	@Override
	public String toString() {
		return "("+start+", "+end+")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
	public SourceLocation clone() {
		try {
			return (SourceLocation)super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public SourceLocation relativeTo(IRegion other) {
		return new SourceLocation(this.start-other.getOffset(), this.end-other.getOffset());
	}

	@Override
	public int compareTo(SourceLocation o) {
		return start - o.start;
	}
	
	public SourceLocation add(IRegion other) {
		return new SourceLocation(start+other.getOffset(), start+other.getOffset()+other.getLength());
	}
	
	public boolean containsOffset(int offset) {
		return offset >= start && offset <= end;
	}

}
