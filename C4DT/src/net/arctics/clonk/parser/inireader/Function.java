package net.arctics.clonk.parser.inireader;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.C4Object;
import net.arctics.clonk.index.C4ObjectIntern;
import net.arctics.clonk.parser.ParserErrorCode;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;

public class Function extends NamedReference implements IComplainingIniEntryValue {

	@Override
	public void complain(ComplexIniEntry context) {
		if (toString().equals("None") || toString().equals("")) // special null value
			return;
		IniUnit iniUnit = context.getIniUnit();
		IFile f = iniUnit.getIniFile();
		if (f != null) {
			C4Object obj = C4ObjectIntern.objectCorrespondingTo(f.getParent());
			if (obj != null && obj.findFunction(this.toString()) == null)
				iniUnit.markerAtValue(ClonkCore.MARKER_C4SCRIPT_ERROR, ParserErrorCode.UndeclaredIdentifier, context, IMarker.SEVERITY_ERROR, toString());
		}
	}

}
