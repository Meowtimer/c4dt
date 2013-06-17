package net.arctics.clonk.parser;

import static net.arctics.clonk.util.ArrayUtil.concat;
import static net.arctics.clonk.util.Utilities.as;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.arctics.clonk.Core;
import net.arctics.clonk.Problem;
import net.arctics.clonk.ProblemException;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ast.IASTPositionProvider;
import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.Marker;
import net.arctics.clonk.c4script.typing.PrimitiveType;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.ProjectIndex;
import net.arctics.clonk.parser.IMarkerListener.Decision;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class Markers implements Iterable<Marker> {
	public static final String MARKER_PROBLEM = "c4dtProblem"; //$NON-NLS-1$
	public static final String MARKER_EXPECTEDTYPE = "c4dtExpectedType";

	public Markers() {}
	public Markers(IMarkerListener listener) { this(); this.listener = listener; }
	public Markers(boolean enabled) { this.enabled = enabled; }

	/**
	 * Whether to not create any error markers at all - set if script is contained in linked group
	 */
	private boolean enabled = true;
	private final Set<Problem> disabledErrors = new HashSet<Problem>();
	private IMarkerListener listener;
	private Marker first, last;
	protected List<IMarker> captured;

	public synchronized Marker clear() {
		final Marker r = first;
		first = last = null;
		return r;
	}

	public void setListener(IMarkerListener markerListener) { this.listener = markerListener; }
	public IMarkerListener listener() { return listener; }

	public Marker last() { return last; }

	public void discardFrom(Marker start) {
		if (start == null) {
			clear();
			return;
		}
		if (start.prev != null)
			start.prev.next = null;
		else
			first = null;
		last = start.prev;
	}

	public synchronized void deploy() {
		if (Core.instance().runsHeadless())
			return;
		for (Marker deploy = clear(); deploy != null; deploy = deploy.next)
			deploy(deploy);
		if (captured != null)
			try {
				for (final IMarker m : captured)
					m.delete();
			} catch (final CoreException e) {
				e.printStackTrace();
			} finally {
				captured = null;
			}
	}

	private IMarker findCaptured(int start, int end) {
		if (captured == null)
			return null;
		for (final Iterator<IMarker> it = captured.iterator(); it.hasNext();) {
			final IMarker c = it.next();
			if (c.getAttribute(IMarker.CHAR_START, -1) == start && c.getAttribute(IMarker.CHAR_END, -1) == end) {
				it.remove();
				return c;
			}
		}
		return null;
	}

	private IMarker deploy(Marker marker) {
		final IFile file = marker.scriptFile;
		final Declaration declarationAssociatedWithFile = marker.container;
		if (file == null)
			return null;
		try {
			String[] attributes = new String[] {IMarker.SEVERITY, IMarker.TRANSIENT, IMarker.MESSAGE, IMarker.LOCATION, MARKER_PROBLEM};
			Object[] attributeValues = new Object[] {marker.severity, false, marker.code.makeErrorString(marker.args),
				declarationAssociatedWithFile != null ? declarationAssociatedWithFile.toString() : null, marker.code.ordinal()};
			IMarker m = findCaptured(marker.start, marker.end);
			if (m == null) {
				m = file.createMarker(Core.MARKER_C4SCRIPT_ERROR);
				attributes = concat(attributes, IMarker.CHAR_START, IMarker.CHAR_END);
				attributeValues = concat(attributeValues, marker.start, marker.end);
			}
			if (marker.code == Problem.IncompatibleTypes) {
				attributes = concat(MARKER_EXPECTEDTYPE, attributes);
				attributeValues = concat(marker.args[0], attributeValues);
			}
			m.setAttributes(
				attributes,
				attributeValues
			);
			return m;
		} catch (final CoreException e) {
			e.printStackTrace();
		}
		return null;
	}

	public synchronized boolean add(Marker e) {
		if (last == null) {
			first = last = e;
			e.prev = e.next = null;
		}
		else {
			e.prev = last;
			e.next = null;
			last.next = e;
			last = e;
		}
		return true;
	}

	public void take(Markers others) {
		final Marker f = others.clear();
		synchronized (this) {
			for (Marker m = f, n; m != null; m = n) {
				n = m.next;
				if (errorEnabled(m.code))
					this.add(m);
			}
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
	 * @throws ProblemException
	 */
	public void marker(IASTPositionProvider positionProvider, Problem code, ASTNode node, int markerStart, int markerEnd, int flags, int severity, Object... args) throws ProblemException {
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
			throw new ProblemException(problem);
	}

	public void warning(IASTPositionProvider positionProvider, Problem code, ASTNode node, int errorStart, int errorEnd, int flags, Object... args) {
		try {
			marker(positionProvider, code, node, errorStart, errorEnd, flags|Markers.NO_THROW, IMarker.SEVERITY_WARNING, args);
		} catch (final ProblemException e) {
			// won't happen
		}
	}
	public void warning(IASTPositionProvider positionProvider, Problem code, ASTNode node, IRegion region, int flags, Object... args) {
		warning(positionProvider, code, node, region.getOffset(), region.getOffset()+region.getLength(), flags, args);
	}
	public void error(IASTPositionProvider positionProvider, Problem code, ASTNode node, IRegion errorRegion, int flags, Object... args) throws ProblemException {
		error(positionProvider, code, node, errorRegion.getOffset(), errorRegion.getOffset()+errorRegion.getLength(), flags, args);
	}
	public void error(IASTPositionProvider positionProvider, Problem code, ASTNode node, int errorStart, int errorEnd, int flags, Object... args) throws ProblemException {
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
	@Override
	public Iterator<Marker> iterator() {
		return new Iterator<Marker>() {
			private Marker current;
			@Override
			public void remove() {
				throw new NotImplementedException();
			}
			@Override
			public Marker next() { return current; }
			@Override
			public boolean hasNext() {
				if (current == null)
					current = first;
				else
					current = current.next;
				return current != null;
			}
		};
	}

	public synchronized int size() {
		int count = 0;
		for (@SuppressWarnings("unused") final Marker m : this)
			count++;
		return count;
	}

	public static void clearMarkers(IResource resource) {
		if (resource == null)
			return;
		try {
			resource.deleteMarkers(Core.MARKER_C4SCRIPT_ERROR, true, IResource.DEPTH_ONE);
			resource.deleteMarkers(IMarker.TASK, true, IResource.DEPTH_ONE);
		} catch (final CoreException e1) {
			e1.printStackTrace();
		}
	}

	public void captureExistingMarkers(IResource resource) {
		if (resource == null)
			return;
		try {
			captured = new ArrayList<>(Arrays.asList(resource.findMarkers(Core.MARKER_C4SCRIPT_ERROR, true, IResource.DEPTH_ONE)));
			resource.deleteMarkers(IMarker.TASK, true, IResource.DEPTH_ONE);
		} catch (final CoreException e1) {
			e1.printStackTrace();
		}
	}
}