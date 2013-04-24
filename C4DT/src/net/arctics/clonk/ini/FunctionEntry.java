package net.arctics.clonk.ini;

import net.arctics.clonk.Problem;
import net.arctics.clonk.ProblemException;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.parser.Markers;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;

public class FunctionEntry extends NamedReference implements ISelfValidatingIniEntryValue {

	@Override
	public void validate(Markers markers, ComplexIniEntry context) throws ProblemException {
		if (toString().equals("None") || toString().equals("")) // special null value //$NON-NLS-1$ //$NON-NLS-2$
			return;
		IniUnit iniUnit = context.iniUnit();
		IFile f = iniUnit.file();
		if (f != null) {
			Definition obj = Definition.definitionCorrespondingToFolder(f.getParent());
			if (obj != null && obj.findFunction(this.toString()) == null)
				markers.marker(context.parentOfType(IniUnit.class).parser(), Problem.UndeclaredIdentifier, context, context.start(), context.end(), Markers.ABSOLUTE_MARKER_LOCATION|Markers.NO_THROW, IMarker.SEVERITY_ERROR, toString());
		}
	}

}
