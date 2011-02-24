package net.arctics.clonk.parser.playercontrols;

import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.Variable.C4VariableScope;
import net.arctics.clonk.parser.inireader.IniEntry;
import net.arctics.clonk.parser.inireader.IniItem;
import net.arctics.clonk.parser.inireader.IniSection;
import net.arctics.clonk.parser.inireader.IniUnitWithNamedSections;

public class PlayerControlsUnit extends IniUnitWithNamedSections {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	
	private List<Variable> controlVariables = new LinkedList<Variable>();
	
	@Override
	protected String getConfigurationName() {
		return "PlayerControls.txt"; //$NON-NLS-1$
	}
	
	public List<Variable> getControlVariables() {
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
	
	@Override
	protected void endParsing() {
		IniSection controlsDefsSection = sectionWithName("ControlDefs");
		if (controlsDefsSection != null) {
			for (IniItem item : controlsDefsSection.getSubItemList()) {
				if (item instanceof IniSection) {
					IniSection section = (IniSection) item;
					if (section.getName().equals("ControlDef")) { //$NON-NLS-1$
						IniItem identifierEntry = section.getSubItem("Identifier"); //$NON-NLS-1$
						if (identifierEntry instanceof IniEntry) {
							IniEntry e = (IniEntry) identifierEntry;
							String ident = e.getValue();
							Variable var = new Variable("CON_" + ident, PrimitiveType.INT); //$NON-NLS-1$
							var.setScope(C4VariableScope.CONST);
							var.setParentDeclaration(this);
							var.setLocation(e.getLocation());
							controlVariables.add(var);
						}
					}
				}
			}
		}
		super.endParsing();
	}
	
	@Override
	public String sectionNameEntryName(IniSection section) {
		if (section.getParentSection() != null) {
			IniSection psec = section.getParentSection();
			if (psec.getName().equals("ControlDefs"))
				return "Identifier";
			else if (psec.getName().equals("ControlSets"))
				return "Name";
			else if (psec.getName().equals("ControlSet"))
				return "Control";
		}
		return super.sectionNameEntryName(section);
	}
	
	@Override
	public Declaration findLocalDeclaration(String declarationName, Class<? extends Declaration> declarationClass) {
		if (declarationClass == Variable.class && declarationName.startsWith("CON_")) { //$NON-NLS-1$
			for (Variable var : getControlVariables())
				if (var.getName().equals(declarationName))
					return var;
		}
		return super.findLocalDeclaration(declarationName, declarationClass);
	}

}
