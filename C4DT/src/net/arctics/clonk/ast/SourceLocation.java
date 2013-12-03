package net.arctics.clonk.ast;

import java.io.Serializable;
import java.util.regex.Matcher;

import net.arctics.clonk.Core;

import org.eclipse.jface.text.IRegion;

public class SourceLocation implements IRegion, Serializable, Cloneable, Comparable<SourceLocation> {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	public static final SourceLocation ZERO = new SourceLocation(0, 0);

	protected int start, end;

	public SourceLocation() {}

	public SourceLocation(final int start,final int end) {
		this.start = start;
		this.end = end;
	}
	public SourceLocation(final Matcher matcher) {
		start = matcher.start();
		end = matcher.end();
	}
	public SourceLocation(final int offset, final IRegion relativeLocation) {
		start = offset+relativeLocation.getOffset();
		end = offset+relativeLocation.getOffset()+relativeLocation.getLength();
	}
	public SourceLocation(final String stringRepresentation) {
		final int comma = stringRepresentation.indexOf(",");
		start = Integer.parseInt(stringRepresentation.substring(1, comma));
		end = Integer.parseInt(stringRepresentation.substring(comma+2, stringRepresentation.length()-1));
	}

	public void setStart(final int start) { this.start = start; }
	public int start() { return start; }
	public void setEnd(final int end) { this.end = end; }
	public int end() { return end; }
	@Override
	public int getLength() { return end-start; }
	@Override
	public int getOffset() { return start; }

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof SourceLocation) {
			final SourceLocation cmp = (SourceLocation) obj;
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
		return start * 37 + end * 37;
	}

	public SourceLocation offset(final int o) {
		return new SourceLocation(o+start, o+end);
	}

	@Override
	public SourceLocation clone() {
		try {
			return (SourceLocation)super.clone();
		} catch (final CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}

	public SourceLocation relativeTo(final IRegion other) {
		return new SourceLocation(this.start-other.getOffset(), this.end-other.getOffset());
	}

	@Override
	public int compareTo(final SourceLocation o) {
		return start - o.start;
	}

	public SourceLocation add(final IRegion other) {
		return new SourceLocation(start+other.getOffset(), start+other.getOffset()+other.getLength());
	}

	public boolean containsOffset(final int offset) {
		return offset >= start && offset <= end;
	}

	public boolean sameLocation(final SourceLocation other) {
		return other != null && this.start == other.start && this.end == other.end;
	}

	public boolean isAt(final int offset) {
		return offset >= start && offset <= end;
	}

}
