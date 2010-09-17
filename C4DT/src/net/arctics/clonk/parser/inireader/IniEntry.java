package net.arctics.clonk.parser.inireader;

import java.io.IOException;
import java.io.Writer;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.NameValueAssignment;
import net.arctics.clonk.util.Utilities;

public class IniEntry extends NameValueAssignment implements IniItem {
	
	public IniEntry(int pos, int endPos, String k, String v) {
		super(pos, endPos, k, v);
	}
	
	public Object getValueObject() {
		return getValue();
	}

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	@Override
	public void writeTextRepresentation(Writer writer, int indentation) throws IOException {
		writer.append(Utilities.multiply("\t", indentation));
		writer.append(toString());
	}

	@Override
	public void validate() {
	}
	
	@Override
	public int sortCategory() {
		return 0;
	}
	
}