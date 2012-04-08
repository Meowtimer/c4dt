package net.arctics.clonk.parser.inireader;

public abstract class IniEntryValueBase implements IIniEntryValue {

	@Override
	public Object evaluate(Object context) {
		return toString();
	}

}
