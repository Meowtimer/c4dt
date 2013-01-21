package net.arctics.clonk.parser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.IMarkerListener.Decision;
import net.arctics.clonk.parser.c4script.Marker;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

public class Markers extends LinkedList<Marker> {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	
	/**
	 * Whether to not create any error markers at all - set if script is contained in linked group
	 */
	protected boolean allErrorsDisabled;
	private final Set<ParserErrorCode> disabledErrors = new HashSet<ParserErrorCode>();
	
	private IMarkerListener listener;
	
	public void setListener(IMarkerListener markerListener) { this.listener = markerListener; }
	public IMarkerListener listener() { return listener; }

	public void deploy() {
		if (Core.instance().runsHeadless())
			return;
		final List<Marker> markersToDeploy;
		synchronized (this) {
			markersToDeploy = new ArrayList<Marker>(this);
			this.clear();
		}
		for (Marker m : markersToDeploy)
			m.deploy();
	}
	
	@Override
	public boolean add(Marker e) {
		synchronized (this) {
			return super.add(e);
		}
	}
	
	public static final int NO_THROW = 1;
	public static final int ABSOLUTE_MARKER_LOCATION = 2;
	
	/**
	 * Create a code marker.
	 * @param positionProvider AST position provider
	 * @param code The error code
	 * @param markerStart Start of the marker (relative to function body)
	 * @param markerEnd End of the marker (relative to function body)
	 * @param noThrow true means that no exception will be thrown after creating the marker.
	 * @param severity IMarker severity value
	 * @param args Format arguments used when creating the marker message with the message from the error code as the format.
	 * @throws ParsingException
	 */
	public void marker(IASTPositionProvider positionProvider, ParserErrorCode code, int markerStart, int markerEnd, int flags, int severity, Object... args) throws ParsingException {
		if (listener != null) {
			if ((flags & ABSOLUTE_MARKER_LOCATION) == 0) {
				markerStart += positionProvider.fragmentOFfset();
				markerEnd += positionProvider.fragmentOFfset();
			}
			if (listener.markerEncountered(this, positionProvider, code, markerStart, markerEnd, flags, severity, args) == Decision.DropCharges)
				return;
		}
		
		if (!errorEnabled(code))
			return;
		if ((flags & ABSOLUTE_MARKER_LOCATION) == 0) {
			int offs = positionProvider.sectionOffset();
			markerStart += offs;
			markerEnd += offs;
		}
		String problem = code.makeErrorString(args);
		add(new Marker(positionProvider, code, markerStart, markerEnd, severity, args));
		if ((flags & NO_THROW) == 0 && severity >= IMarker.SEVERITY_ERROR)
			throw new ParsingException(problem);
	}
	/**
	 * Get error enabled status.
	 * @param error The error to check the enabled status of
	 * @return Return whether the error is enabled. 
	 */
	public boolean errorEnabled(ParserErrorCode error) {
		return !(allErrorsDisabled || disabledErrors.contains(error));
	}
	
	/**
	 * Convert a region relative to the body offset of the current function to a script-absolute region.
	 * @param flags 
	 * @param region The region to convert
	 * @return The relative region or the passed region, if there is no current function.
	 */
	public IRegion convertRelativeRegionToAbsolute(IASTPositionProvider positionProvider, int flags, IRegion region) {
		int offset = positionProvider.sectionOffset();
		if (offset == 0 || (flags & ABSOLUTE_MARKER_LOCATION) == 0)
			return region;
		else
			return new Region(offset+region.getOffset(), region.getLength());
	}
	
	public IMarker todo(IASTPositionProvider positionProvider, String todoText, int markerStart, int markerEnd, int priority) {
		IFile file = positionProvider.file();
		if (file != null)
			try {
				IMarker marker = file.createMarker(IMarker.TASK);
				marker.setAttribute(IMarker.CHAR_START, markerStart+positionProvider.sectionOffset());
				marker.setAttribute(IMarker.CHAR_END, markerEnd+positionProvider.sectionOffset());
				marker.setAttribute(IMarker.MESSAGE, todoText);
				Declaration declaration = positionProvider.node().parentOfType(Declaration.class);
				marker.setAttribute(IMarker.LOCATION, declaration != null ? declaration.qualifiedName() : ""); //$NON-NLS-1$
				marker.setAttribute(IMarker.PRIORITY, priority);
				return marker;
			} catch (CoreException e) {
				e.printStackTrace();
				return null;
			}
		else
			return null;
	}
}