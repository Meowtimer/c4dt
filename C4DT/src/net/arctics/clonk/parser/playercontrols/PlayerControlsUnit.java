package net.arctics.clonk.parser.playercontrols;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IFile;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.c4script.C4Type;
import net.arctics.clonk.parser.c4script.C4Variable;
import net.arctics.clonk.parser.c4script.C4Variable.C4VariableScope;
import net.arctics.clonk.parser.inireader.IniEntry;
import net.arctics.clonk.parser.inireader.IniSection;
import net.arctics.clonk.parser.inireader.IniUnit;
import net.arctics.clonk.parser.inireader.IniData.IniConfiguration;

public class PlayerControlsUnit extends IniUnit {

	private static final long serialVersionUID = 1L;
	private static final IniConfiguration configuration = ClonkCore.getDefault().iniConfigurations.getConfigurationFor("PlayerControls.txt");
	
	private List<C4Variable> controlVariables = new LinkedList<C4Variable>();
	
	public List<C4Variable> getControlVariables() {
		return controlVariables;
	}

	public PlayerControlsUnit(IFile file) {
		super(file);
	}
	
	public PlayerControlsUnit(String text) {
		super(text);
	}
	
	public PlayerControlsUnit(InputStream stream) {
		super(stream);
	}
	
	@Override
	public IniConfiguration getConfiguration() {
		return configuration;
	}
	
	@Override
	public void parse(boolean modifyMarkers) {
		controlVariables.clear();
		super.parse(modifyMarkers);
		for (IniSection section : getSections()) {
			if (section.getName().equals("ControlDef")) {
				IniEntry e = section.getEntry("Identifier");
				if (e != null) {
					String ident = e.getValue();
					C4Variable var = new C4Variable("CON_" + ident, C4Type.INT);
					var.setScope(C4VariableScope.VAR_CONST);
					var.setParentDeclaration(this);
					var.setLocation(e.getLocation());
					controlVariables.add(var);
				}
			}
		}
	}
	
	@Override
	public C4Declaration findDeclaration(String declarationName, Class<? extends C4Declaration> declarationClass) {
		if (declarationClass == C4Variable.class && declarationName.startsWith("CON_")) {
			for (C4Variable var : getControlVariables())
				if (var.getName().equals(declarationName))
					return var;
		}
		return super.findDeclaration(declarationName, declarationClass);
	}

}
