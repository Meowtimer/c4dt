package net.arctics.clonk.ini;

import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.c4script.Variable.Scope;
import net.arctics.clonk.c4script.typing.PrimitiveType;
import net.arctics.clonk.index.StructureVariable;

public class PlayerControlsUnit extends IniUnitWithNamedSections {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private final List<Variable> controlVariables = new LinkedList<Variable>();
	@Override
	protected String configurationName() { return "PlayerControls.txt"; } //$NON-NLS-1$
	public List<Variable> controlVariables() { return controlVariables; }
	public PlayerControlsUnit(final Object input) { super(input); }
	@Override
	public void startParsing() {
		super.startParsing();
		controlVariables.clear();
	}
	/**
	 * Creates global const variables for definitions in the PlayerControls.txt file so they can be used in scripts.
	 */
	@Override
	protected void endParsing() {
		final IniSection controlsDefsSection = sectionWithName("ControlDefs", false, null);
		if (controlsDefsSection != null)
			for (final IniItem item : controlsDefsSection.items())
				if (item instanceof IniSection) {
					final IniSection section = (IniSection) item;
					if (section.name().equals("ControlDef")) { //$NON-NLS-1$
						final IniItem identifierEntry = section.item("Identifier"); //$NON-NLS-1$
						if (identifierEntry instanceof IniEntry) {
							final IniEntry e = (IniEntry) identifierEntry;
							final String ident = e.stringValue();
							final Variable var = new StructureVariable("CON_" + ident, PrimitiveType.INT); //$NON-NLS-1$
							var.setScope(Scope.CONST);
							var.setParent(this);
							var.setLocation(e);
							controlVariables.add(var);
						}
					}
				}
		super.endParsing();
	}
	@Override
	public String nameEntryName(final IniSection section) {
		if (section != null && section.parentSection() != null) {
			final IniSection psec = section.parentSection();
			switch (psec.name()) {
			case "ControlDefs":
				return "Identifier";
			case "ControlSets":
				return "Name";
			case "ControlSet":
				return "Control";
			}
		}
		return super.nameEntryName(section);
	}
	@Override
	public Declaration findLocalDeclaration(final String declarationName, final Class<? extends Declaration> declarationClass) {
		if (declarationClass == Variable.class && declarationName.startsWith("CON_"))
			for (final Variable var : controlVariables())
				if (var.name().equals(declarationName))
					return var;
		return super.findLocalDeclaration(declarationName, declarationClass);
	}
	@SuppressWarnings("unchecked")
	@Override
	public <T extends Declaration> T latestVersionOf(final T of) {
		final T r = super.latestVersionOf(of);
		if (r != null)
			return r;
		if (of instanceof Variable)
			for (final Variable c : controlVariables)
				if (of.name().equals(c.name()))
					return (T) c;
		return null;
	};
}
