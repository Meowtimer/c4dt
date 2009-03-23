package net.arctics.clonk.parser.inireader;

import net.arctics.clonk.parser.inireader.IniData.IniDataEntry;
import net.arctics.clonk.util.IHasChildren;

public class ComplexIniEntry extends IniEntry implements IHasChildren  {
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
	
	@Override
	public String getValue() {
		return extendedValue.toString();
	}
	
	@Override
	public void setValue(String value) {
		if (extendedValue instanceof IEntryCreateable) {
			try {
				((IEntryCreateable)extendedValue).setInput(value);
			} catch (IniParserException e) {
				e.printStackTrace();
			}
		} else {
			if (extendedValue instanceof String)
				extendedValue = value;
		}
	}

	public Object[] getChildren() {
		if (extendedValue instanceof IHasChildren)
			return ((IHasChildren)extendedValue).getChildren();
		return null;
	}

	public boolean hasChildren() {
		return
			extendedValue instanceof IHasChildren && ((IHasChildren)extendedValue).hasChildren();
	}
	
}
