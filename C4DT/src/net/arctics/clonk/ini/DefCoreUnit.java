package net.arctics.clonk.ini;

import net.arctics.clonk.Core;
import net.arctics.clonk.builder.ClonkBuilder;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.ast.IDLiteral;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.ID;

public class DefCoreUnit extends IniUnit {
	public static final String FILE_NAME = "DefCore.txt";
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	@Override
	protected String configurationName() { return FILE_NAME; } //$NON-NLS-1$
	public DefCoreUnit(final Object input) { super(input); }
	@Override
	public boolean requiresScriptReparse() { return true; /* i guess */ }
	public ID definitionID() {
		final IniEntry entry = entryInSection("DefCore", "id"); //$NON-NLS-1$ //$NON-NLS-2$
		if (entry != null && entry.value() instanceof IDLiteral)
			return ((IDLiteral)entry.value()).idValue();
		else
			return ID.NULL;
	}
	@Override
	public String name() {
		final IniEntry entry = entryInSection("DefCore", "Name"); //$NON-NLS-1$ //$NON-NLS-2$
		return entry instanceof IniEntry ? (String)entry.value() : defaultName();
	}
	@Override
	public void commitTo(final Script script, final ClonkBuilder builder) {
		super.commitTo(script, builder);
		if (script instanceof Definition)
			((Definition)script).setDefCoreFile(file());
	}
	@Override
	public String defaultName() {
		final ID id = definitionID();
		return id != ID.NULL ? id.stringValue() : super.defaultName();
	}
}
