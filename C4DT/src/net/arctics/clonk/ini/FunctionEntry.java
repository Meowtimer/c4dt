package net.arctics.clonk.ini;

import net.arctics.clonk.Core;
import net.arctics.clonk.Problem;
import net.arctics.clonk.ProblemException;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.parser.Markers;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;

public class FunctionEntry extends NamedReference implements ISelfValidatingIniEntryValue {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	@Override
	public void validate(Markers markers, IniEntry context) throws ProblemException {
		if (toString().equals("None") || toString().equals("")) // special null value //$NON-NLS-1$ //$NON-NLS-2$
			return;
		final IniUnit iniUnit = context.unit();
		final IFile f = iniUnit.file();
		if (f != null) {
			final Definition obj = Definition.definitionCorrespondingToFolder(f.getParent());
			if (obj != null && obj.findFunction(this.toString()) == null)
				markers.marker(new IniUnitParser(context.parentOfType(IniUnit.class)), Problem.UndeclaredIdentifier, context, context.start(), context.end(), Markers.ABSOLUTE_MARKER_LOCATION|Markers.NO_THROW, IMarker.SEVERITY_ERROR, toString());
		}
	}

}
