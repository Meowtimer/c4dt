package net.arctics.clonk.parser.inireader;

import java.util.Collection;

import net.arctics.clonk.parser.inireader.IniData.IniDataEntry;
import net.arctics.clonk.util.IHasChildren;
import net.arctics.clonk.util.IHasChildrenWithContext;
import net.arctics.clonk.util.IHasContext;
import net.arctics.clonk.util.INode;
import net.arctics.clonk.util.ITreeNode;

public class ComplexIniEntry extends IniEntry implements IHasChildren, IHasContext  {
	
	private static final long serialVersionUID = 1L;
	
	private Object extendedValue;
	private IniDataEntry entryConfig;

	protected ComplexIniEntry(int pos, int endPos, String key, String value) {
		super(pos,endPos, key,value);
	}
	
	public ComplexIniEntry(int pos, int endPos, String key, Object value) {
		super(pos,endPos, key,null);
		extendedValue = value;
	}
	
	public Object getExtendedValue() {
		return extendedValue;
	}
	
	public IniDataEntry getEntryConfig() {
		return entryConfig;
	}
	
	public static ComplexIniEntry adaptFrom(IniEntry entry, Object extendedValue, IniDataEntry config, boolean createErrorMarkers) {
		ComplexIniEntry cmpl = new ComplexIniEntry(entry.getStartPos(), entry.getEndPos(), entry.getKey(), entry.getValue());
		cmpl.entryConfig = config;
		cmpl.extendedValue = extendedValue;
		cmpl.setParentDeclaration(entry.getParentDeclaration());
		if (createErrorMarkers && extendedValue instanceof IComplainingIniEntry)
			((IComplainingIniEntry)extendedValue).complain(cmpl);
		return cmpl;
	}
	
	public IniUnit getIniUnit() {
		return this.getTopLevelParentDeclarationOfType(IniUnit.class);
	}
	
	@Override
	public String getValue() {
		return extendedValue.toString();
	}
	
	@Override
	public void setValue(String value) {
		if (extendedValue instanceof IIniEntry) {
			try {
				((IIniEntry)extendedValue).setInput(value, entryConfig);
			} catch (IniParserException e) {
				e.printStackTrace();
			}
		} else {
			if (extendedValue instanceof String)
				extendedValue = value;
		}
	}

	public Object[] getChildren() {
		if (extendedValue instanceof IHasChildrenWithContext)
			return ((IHasChildrenWithContext)extendedValue).getChildren(this);
		else if (extendedValue instanceof IHasChildren)
			return ((IHasChildren)extendedValue).getChildren();
		return null;
	}

	@Override
	public Collection<? extends INode> getChildCollection() {
		if (extendedValue instanceof ITreeNode) {
			return ((ITreeNode) extendedValue).getChildCollection();
		}
		return null;
	}
	
	public boolean hasChildren() {
		return
			(extendedValue instanceof IHasChildren && ((IHasChildren)extendedValue).hasChildren()) ||
			(extendedValue instanceof IHasChildrenWithContext && ((IHasChildrenWithContext)extendedValue).hasChildren());
	}

	public Object context() {
		return this; // is it's own context; over-abstraction is awesome -.-
	}
	
}
