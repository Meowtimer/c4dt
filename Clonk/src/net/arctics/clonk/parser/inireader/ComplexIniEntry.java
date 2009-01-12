package net.arctics.clonk.parser.inireader;

import net.arctics.clonk.parser.inireader.IniData.IniDataEntry;

public class ComplexIniEntry extends IniEntry  {
	private Object extendedValue;
	private IniDataEntry entryConfig;

	protected ComplexIniEntry(int pos, String key, String value) {
		super(pos,key,value);
	}
	
	public ComplexIniEntry(int pos, String key, Object value) {
		super(pos,key,null);
		extendedValue = value;
	}
	
	public Object getExtendedValue() {
		return extendedValue;
	}
	
	public IniDataEntry getEntryConfig() {
		return entryConfig;
	}
	
	public static ComplexIniEntry adaptFrom(IniEntry entry, Object extendedValue, IniDataEntry config) {
		ComplexIniEntry cmpl = new ComplexIniEntry(entry.getStartPos(), entry.getKey(), entry.getValue());
		cmpl.entryConfig = config;
		cmpl.extendedValue = extendedValue;
		return cmpl;
	}
	
}
