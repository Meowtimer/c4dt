package net.arctics.clonk.parser;

import static net.arctics.clonk.util.ArrayUtil.concat;
import static net.arctics.clonk.util.Utilities.as;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ast.IASTPositionProvider;
import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.Marker;
import net.arctics.clonk.c4script.PrimitiveType;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.ProjectIndex;
import net.arctics.clonk.parser.IMarkerListener.Decision;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

public class Markers extends LinkedList<Marker> {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	public static final String MARKER_PROBLEM = "c4dtProblem"; //$NON-NLS-1$
	public static final String MARKER_EXPECTEDTYPE = "c4dtExpectedType";

	public Markers() {}
	public Markers(IMarkerListener listener) { this(); this.listener = listener; }

	/**
	 * Whether to not create any error markers at all - set if script is contained in linked group
	 */
	private boolean enabled = true;
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
		for (final Marker m : markersToDeploy)
			deploy(m);
	}

	public IMarker deploy(Marker marker) {
		final IFile file = marker.scriptFile;
		final Declaration declarationAssociatedWithFile = marker.container;
		if (file == null)
			return null;
		try {
			final IMarker deployed = file.createMarker(Core.MARKER_C4SCRIPT_ERROR);
			String[] attributes = new String[] {IMarker.SEVERITY, IMarker.TRANSIENT, IMarker.MESSAGE, IMarker.CHAR_START, IMarker.CHAR_END, IMarker.LOCATION, MARKER_PROBLEM};
			Object[] attributeValues = new Object[] {marker.severity, false, marker.code.makeErrorString(marker.args), marker.start, marker.end,
				declarationAssociatedWithFile != null ? declarationAssociatedWithFile.toString() : null, marker.code.ordinal()};
			if (marker.code == Problem.IncompatibleTypes) {
				attributes = concat(MARKER_EXPECTEDTYPE, attributes);
				attributeValues = concat(marker.args[0], attributeValues);
			}
			deployed.setAttributes(
				attributes,
				attributeValues
			);
			return deployed;
		} catch (final CoreException e) {
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
			if ((flags & ABSOLUTE_MARKER_LOCATION) != 0) {
				markerStart += positionProvider.fragmentOffset();
				markerEnd += positionProvider.fragmentOffset();
			}
			synchronized (listener) {
				final IMarkerListener saved = listener;
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
			final Function f = node.parentOfType(Function.class);
			if (f != null) {
				final int offs = f.bodyLocation().start();
				markerStart += offs;
				markerEnd += offs;
			}
		}
		final String problem = code.makeErrorString(args);
		add(new Marker(positionProvider, code, node, markerStart, markerEnd, severity, args));
		if ((flags & NO_THROW) == 0 && severity >= IMarker.SEVERITY_ERROR)
			throw new ParsingException(problem);
	}

	public void warning(IASTPositionProvider positionProvider, Problem code, ASTNode node, int errorStart, int errorEnd, int flags, Object... args) {
		try {
			marker(positionProvider, code, node, errorStart, errorEnd, flags|Markers.NO_THROW, IMarker.SEVERITY_WARNING, args);
		} catch (final ParsingException e) {
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
		return enabled && !disabledErrors.contains(error);
	}

	/**
	 * Convert a region relative to the body offset of the current function to a script-absolute region.
	 * @param flags
	 * @param region The region to convert
	 * @return The relative region or the passed region, if there is no current function.
	 */
	public IRegion convertRelativeRegionToAbsolute(ASTNode node, int flags, IRegion region) {
		final int offset = node != null ? node.sectionOffset() : 0;
		if (offset == 0 || (flags & ABSOLUTE_MARKER_LOCATION) == 0)
			return region;
		else
			return new Region(offset+region.getOffset(), region.getLength());
	}

	public IMarker todo(IFile file, ASTNode node, String todoText, int markerStart, int markerEnd, int priority) {
		if (file != null)
			try {
				final Declaration declaration = node.parentOfType(Declaration.class);
				final Function f = as(declaration, Function.class);
				final int bodyOffset = f != null ? f.bodyLocation().start() : 0;
				final IMarker marker = file.createMarker(IMarker.TASK);
				marker.setAttributes(
					new String[] {IMarker.CHAR_START, IMarker.CHAR_END, IMarker.MESSAGE, IMarker.LOCATION, IMarker.PRIORITY},
					new Object[] {markerStart+bodyOffset, markerEnd+bodyOffset, todoText, declaration != null ? declaration.qualifiedName() : "", priority}
				);
				return marker;
			} catch (final CoreException e) {
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
	
	public void enabled(boolean value) { enabled = value; }
	public boolean enabled() { return enabled; }
	
	public boolean enableError(Problem error, boolean doEnable) {
		final boolean result = errorEnabled(error);
		if (doEnable)
			disabledErrors.remove(error);
		else
			disabledErrors.add(error);
		return result;
	}
	public void applyProjectSettings(Index index) {
		disabledErrors.clear();
		if (index instanceof ProjectIndex) {
			final ProjectIndex projIndex = (ProjectIndex) index;
			final ClonkProjectNature nature = projIndex.nature();
			if (nature != null)
				enableErrors(nature.settings().disabledErrorsSet(), false);
		}
	}

	public static Problem problem(IMarker marker) {
		try {
			return Problem.values()[marker.getAttribute(MARKER_PROBLEM, -1)];
		} catch (final ArrayIndexOutOfBoundsException e) {
			return null;
		}
	}

	public static PrimitiveType expectedType(IMarker marker) {
		final String attr = marker.getAttribute(MARKER_EXPECTEDTYPE, null);
		return attr != null ? PrimitiveType.fromString(attr) : PrimitiveType.ANY;
	}
}