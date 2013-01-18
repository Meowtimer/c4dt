package net.arctics.clonk.parser.inireader;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.NameValueAssignment;
import net.arctics.clonk.parser.c4script.ast.ASTNodePrinter;
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
	public void validate() {
	}
	
	@Override
	public int sortCategory() {
		return 0;
	}
	
	@Override
	public boolean isTransient() {
		return false;
	}
	
}