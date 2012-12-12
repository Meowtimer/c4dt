package net.arctics.clonk.parser.inireader;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.parser.ParserErrorCode;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;

public class FunctionEntry extends NamedReference implements IComplainingIniEntryValue {

	@Override
	public void complain(ComplexIniEntry context) {
		if (toString().equals("None") || toString().equals("")) // special null value //$NON-NLS-1$ //$NON-NLS-2$
			return;
		IniUnit iniUnit = context.iniUnit();
		IFile f = iniUnit.file();
		if (f != null) {
			Definition obj = Definition.definitionCorrespondingToFolder(f.getParent());
			if (obj != null && obj.findFunction(this.toString()) == null)
				iniUnit.markerAtValue(Core.MARKER_C4SCRIPT_ERROR, ParserErrorCode.UndeclaredIdentifier, context, IMarker.SEVERITY_ERROR, toString());
		}
	}

}
