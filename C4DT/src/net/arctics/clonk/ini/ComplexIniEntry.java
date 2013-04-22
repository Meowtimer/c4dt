package net.arctics.clonk.ini;

import java.util.Collection;

import net.arctics.clonk.Core;
import net.arctics.clonk.ini.IniData.IniEntryDefinition;
import net.arctics.clonk.parser.Markers;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.util.IHasChildren;
import net.arctics.clonk.util.IHasChildrenWithContext;
import net.arctics.clonk.util.IHasContext;
import net.arctics.clonk.util.INode;
import net.arctics.clonk.util.ITreeNode;

public class ComplexIniEntry extends IniEntry implements IHasChildren, IHasContext  {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	private Object extendedValue;
	private IniEntryDefinition definition;

	protected ComplexIniEntry(int pos, int endPos, String key, String value) {
		super(pos,endPos, key,value);
	}

	public ComplexIniEntry(int pos, int endPos, String key, Object value) {
		super(pos,endPos, key,null);
		extendedValue = value;
	}

	public Object extendedValue() {
		return extendedValue;
	}

	public void setDefinition(IniEntryDefinition entryConfig) {
		this.definition = entryConfig;
	}

	public IniEntryDefinition definition() {
		return definition;
	}

	public static ComplexIniEntry adaptFrom(IniEntry entry, Object extendedValue, IniEntryDefinition config, boolean createErrorMarkers) {
		final ComplexIniEntry cmpl = new ComplexIniEntry(entry.start(), entry.end(), entry.key(), entry.stringValue());
		cmpl.definition = config;
		cmpl.extendedValue = extendedValue;
		cmpl.setParent(entry.parentDeclaration());
		return cmpl;
	}

	public IniUnit iniUnit() {
		return this.topLevelParentDeclarationOfType(IniUnit.class);
	}

	@Override
	public String stringValue() {
		return extendedValue.toString();
	}

	@Override
	public Object value() {
		return extendedValue;
	}

	@Override
	public Object[] children() {
		if (extendedValue instanceof IHasChildrenWithContext)
			return ((IHasChildrenWithContext)extendedValue).children(this);
		else if (extendedValue instanceof IHasChildren)
			return ((IHasChildren)extendedValue).children();
		return null;
	}

	@Override
	public Collection<? extends INode> childCollection() {
		if (extendedValue instanceof ITreeNode)
			return ((ITreeNode) extendedValue).childCollection();
		return null;
	}

	@Override
	public boolean hasChildren() {
		return
			(extendedValue instanceof IHasChildren && ((IHasChildren)extendedValue).hasChildren()) ||
			(extendedValue instanceof IHasChildrenWithContext && ((IHasChildrenWithContext)extendedValue).hasChildren());
	}

	@Override
	public Object context() {
		return this; // is it's own context; over-abstraction is awesome -.-
	}

	@Override
	public void validate(Markers markers) throws ParsingException {
		if (extendedValue() instanceof ISelfValidatingIniEntryValue)
			((ISelfValidatingIniEntryValue)extendedValue()).validate(markers, this);
	}

	@Override
	public boolean isTransient() {
		return value() instanceof IniEntryValueBase && ((IniEntryValueBase)value()).isEmpty();
	}

}
