package net.arctics.clonk.parser.playercontrols;

import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.Variable.Scope;
import net.arctics.clonk.parser.inireader.IniEntry;
import net.arctics.clonk.parser.inireader.IniItem;
import net.arctics.clonk.parser.inireader.IniSection;
import net.arctics.clonk.parser.inireader.IniUnitWithNamedSections;

public class PlayerControlsUnit extends IniUnitWithNamedSections {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	
	private final List<Variable> controlVariables = new LinkedList<Variable>();
	
	@Override
	protected String configurationName() {
		return "PlayerControls.txt"; //$NON-NLS-1$
	}
	
	public List<Variable> controlVariables() {
		return controlVariables;
	}

	public PlayerControlsUnit(Object input) {
		super(input);
	}
	
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
		IniSection controlsDefsSection = sectionWithName("ControlDefs", false);
		if (controlsDefsSection != null)
			for (IniItem item : controlsDefsSection.subItemList())
				if (item instanceof IniSection) {
					IniSection section = (IniSection) item;
					if (section.name().equals("ControlDef")) { //$NON-NLS-1$
						IniItem identifierEntry = section.subItemByKey("Identifier"); //$NON-NLS-1$
						if (identifierEntry instanceof IniEntry) {
							IniEntry e = (IniEntry) identifierEntry;
							String ident = e.stringValue();
							Variable var = new Variable("CON_" + ident, PrimitiveType.INT); //$NON-NLS-1$
							var.setScope(Scope.CONST);
							var.setParentDeclaration(this);
							var.setLocation(e);
							controlVariables.add(var);
						}
					}
				}
		super.endParsing();
	}
	
	@Override
	public String nameOfEntryToTakeSectionNameFrom(IniSection section) {
		if (section != null && section.parentSection() != null) {
			IniSection psec = section.parentSection();
			if (psec.name().equals("ControlDefs"))
				return "Identifier";
			else if (psec.name().equals("ControlSets"))
				return "Name";
			else if (psec.name().equals("ControlSet"))
				return "Control";
		}
		return super.nameOfEntryToTakeSectionNameFrom(section);
	}
	
	@Override
	public Declaration findLocalDeclaration(String declarationName, Class<? extends Declaration> declarationClass) {
		if (declarationClass == Variable.class && declarationName.startsWith("CON_"))
			for (Variable var : controlVariables())
				if (var.name().equals(declarationName))
					return var;
		return super.findLocalDeclaration(declarationName, declarationClass);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends Declaration> T latestVersionOf(T of) {
		T r = super.latestVersionOf(of);
		if (r != null)
			return r;
		if (of instanceof Variable)
			for (Variable c : controlVariables)
				if (of.name().equals(c.name()))
					return (T) c;
		return null;
	};

}
