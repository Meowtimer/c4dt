package net.arctics.clonk.parser.inireader;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.ProjectDefinition;
import net.arctics.clonk.parser.ParserErrorCode;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;

public class FuncRefEntry extends NamedReference implements IComplainingIniEntryValue {

	@Override
	public void complain(ComplexIniEntry context) {
		if (toString().equals("None") || toString().equals("")) // special null value //$NON-NLS-1$ //$NON-NLS-2$
			return;
		IniUnit iniUnit = context.getIniUnit();
		IFile f = iniUnit.getIniFile();
		if (f != null) {
			Definition obj = ProjectDefinition.definitionCorrespondingToFolder(f.getParent());
			if (obj != null && obj.findFunction(this.toString()) == null)
				iniUnit.markerAtValue(ClonkCore.MARKER_C4SCRIPT_ERROR, ParserErrorCode.UndeclaredIdentifier, context, IMarker.SEVERITY_ERROR, toString());
		}
	}

}
