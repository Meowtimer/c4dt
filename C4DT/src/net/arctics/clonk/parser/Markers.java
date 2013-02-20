package net.arctics.clonk.parser;

import static net.arctics.clonk.util.Utilities.as;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.ProjectIndex;
import net.arctics.clonk.parser.IMarkerListener.Decision;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.Marker;
import net.arctics.clonk.resource.ClonkProjectNature;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

public class Markers extends LinkedList<Marker> {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	public static final String MARKER_PROBLEM = "c4dtProblem"; //$NON-NLS-1$

	public Markers() {}
	public Markers(IMarkerListener listener) { this(); this.listener = listener; }

	/**
	 * Whether to not create any error markers at all - set if script is contained in linked group
	 */
	private boolean allErrorsDisabled;
	private final Set<Problem> disabledErrors = new HashSet<Problem>();
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
			deploy(m);
	}

	public IMarker deploy(Marker marker) {
		IFile file = marker.scriptFile;
		Declaration declarationAssociatedWithFile = marker.container;
		if (file == null)
			return null;
		try {
			IMarker marker1 = file.createMarker(Core.MARKER_C4SCRIPT_ERROR);
			marker1.setAttributes(
				new String[] {IMarker.SEVERITY, IMarker.TRANSIENT, IMarker.MESSAGE, IMarker.CHAR_START, IMarker.CHAR_END, IMarker.LOCATION, MARKER_PROBLEM},
				new Object[] {marker.severity, false, marker.code.makeErrorString(marker.args), marker.start, marker.end, declarationAssociatedWithFile != null ? declarationAssociatedWithFile.toString() : null, marker.code.ordinal()}
			);
			return marker1;
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return null;
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
	public void marker(IASTPositionProvider positionProvider, Problem code, ASTNode node, int markerStart, int markerEnd, int flags, int severity, Object... args) throws ParsingException {
		if (!errorEnabled(code))
			return;

		if (listener != null) {
			if ((flags & ABSOLUTE_MARKER_LOCATION) == 0) {
				markerStart += positionProvider.fragmentOffset();
				markerEnd += positionProvider.fragmentOffset();
			}
			synchronized (listener) {
				IMarkerListener saved = listener;
				listener = null;
				try {
					if (saved.markerEncountered(this, positionProvider, code, node, markerStart, markerEnd, flags, severity, args) == Decision.DropCharges)
						return;
				} finally {
					listener = saved;
				}
			}
		}

		if ((flags & ABSOLUTE_MARKER_LOCATION) == 0 && node != null) {
			Function f = node.parentOfType(Function.class);
			if (f != null) {
				int offs = f.bodyLocation().start();
				markerStart += offs;
				markerEnd += offs;
			}
		}
		String problem = code.makeErrorString(args);
		add(new Marker(positionProvider, code, node, markerStart, markerEnd, severity, args));
		if ((flags & NO_THROW) == 0 && severity >= IMarker.SEVERITY_ERROR)
			throw new ParsingException(problem);
	}

	public void warning(IASTPositionProvider positionProvider, Problem code, ASTNode node, int errorStart, int errorEnd, int flags, Object... args) {
		try {
			marker(positionProvider, code, node, errorStart, errorEnd, flags|Markers.NO_THROW, IMarker.SEVERITY_WARNING, args);
		} catch (ParsingException e) {
			// won't happen
		}
	}
	public void warning(IASTPositionProvider positionProvider, Problem code, ASTNode node, IRegion region, int flags, Object... args) {
		warning(positionProvider, code, node, region.getOffset(), region.getOffset()+region.getLength(), flags, args);
	}
	public void error(IASTPositionProvider positionProvider, Problem code, ASTNode node, IRegion errorRegion, int flags, Object... args) throws ParsingException {
		error(positionProvider, code, node, errorRegion.getOffset(), errorRegion.getOffset()+errorRegion.getLength(), flags, args);
	}
	public void error(IASTPositionProvider positionProvider, Problem code, ASTNode node, int errorStart, int errorEnd, int flags, Object... args) throws ParsingException {
		marker(positionProvider, code, node, errorStart, errorEnd, flags, IMarker.SEVERITY_ERROR, args);
	}

	/**
	 * Get error enabled status.
	 * @param error The error to check the enabled status of
	 * @return Return whether the error is enabled.
	 */
	public boolean errorEnabled(Problem error) {
		return !(allErrorsDisabled || disabledErrors.contains(error));
	}

	/**
	 * Convert a region relative to the body offset of the current function to a script-absolute region.
	 * @param flags
	 * @param region The region to convert
	 * @return The relative region or the passed region, if there is no current function.
	 */
	public IRegion convertRelativeRegionToAbsolute(ASTNode node, int flags, IRegion region) {
		int offset = node != null ? node.sectionOffset() : 0;
		if (offset == 0 || (flags & ABSOLUTE_MARKER_LOCATION) == 0)
			return region;
		else
			return new Region(offset+region.getOffset(), region.getLength());
	}

	public IMarker todo(IFile file, ASTNode node, String todoText, int markerStart, int markerEnd, int priority) {
		if (file != null)
			try {
				Declaration declaration = node.parentOfType(Declaration.class);
				Function f = as(declaration, Function.class);
				int bodyOffset = f != null ? f.bodyLocation().start() : 0;
				IMarker marker = file.createMarker(IMarker.TASK);
				marker.setAttribute(IMarker.CHAR_START, markerStart+bodyOffset);
				marker.setAttribute(IMarker.CHAR_END, markerEnd+bodyOffset);
				marker.setAttribute(IMarker.MESSAGE, todoText);
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

	public void enableErrors(Set<Problem> set, boolean doEnable) {
		if (doEnable)
			disabledErrors.removeAll(set);
		else
			disabledErrors.addAll(set);
	}
	public void disableAllErrors(boolean _do) {
		allErrorsDisabled = _do;
	}
	public boolean enableError(Problem error, boolean doEnable) {
		boolean result = errorEnabled(error);
		if (doEnable)
			disabledErrors.remove(error);
		else
			disabledErrors.add(error);
		return result;
	}
	public void applyProjectSettings(Index index) {
		disabledErrors.clear();
		if (index instanceof ProjectIndex) {
			ProjectIndex projIndex = (ProjectIndex) index;
			ClonkProjectNature nature = projIndex.nature();
			if (nature != null)
				enableErrors(nature.settings().disabledErrorsSet(), false);
		}
	}

	public static Problem problem(IMarker marker) {
		try {
			return Problem.values()[marker.getAttribute(MARKER_PROBLEM, -1)];
		} catch (ArrayIndexOutOfBoundsException e) {
			return null;
		}
	}
}