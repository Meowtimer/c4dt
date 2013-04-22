package net.arctics.clonk.ini;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNodePrinter;
import net.arctics.clonk.ast.NameValueAssignment;
import net.arctics.clonk.parser.Markers;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.util.StringUtil;

public class IniEntry extends NameValueAssignment implements IniItem {

	public IniEntry(int pos, int endPos, String k, String v) {
		super(pos, endPos, k, v);
	}

	public Object value() {
		return stringValue();
	}

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	@Override
	public void doPrint(ASTNodePrinter writer, int indentation) {
		writer.append(StringUtil.multiply("\t", indentation));
		writer.append(toString());
	}

	@Override
	public void validate(Markers markers) throws ParsingException {}

	@Override
	public int sortCategory() {
		return 0;
	}

	@Override
	public boolean isTransient() {
		return false;
	}

}