package net.arctics.clonk.parser.defcore;

import org.eclipse.core.resources.IMarker;

import net.arctics.clonk.parser.C4ID;

public class DefCoreC4ID extends DefCoreOption {

	public C4ID id;
	
	public DefCoreC4ID(String name) {
		super(name);
	}
	
	public DefCoreC4ID(String name, C4ID id) {
		super(name);
		this.id = id;
	}
	
	@Override
	public String getStringRepresentation() {
		return id.getName();
	}

	@Override
	public void setInput(String input) throws DefCoreParserException {
		if (!C4ID.isValidC4ID(input)) {
			throw new DefCoreParserException(IMarker.SEVERITY_ERROR,"Expected a C4ID value.");
		}
		id = C4ID.getID(input);
	}

}
