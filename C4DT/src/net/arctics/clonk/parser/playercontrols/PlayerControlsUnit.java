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

public class PlayerControlsUnit extends IniUnit {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	
	private List<C4Variable> controlVariables = new LinkedList<C4Variable>();
	
	@Override
	protected String getConfigurationName() {
		return "PlayerControls.txt"; //$NON-NLS-1$
	}
	
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
	public void parse(boolean modifyMarkers) {
		controlVariables.clear();
		super.parse(modifyMarkers);
		for (IniSection section : getSections()) {
			if (section.getName().equals("ControlDef")) { //$NON-NLS-1$
				IniEntry e = section.getEntry("Identifier"); //$NON-NLS-1$
				if (e != null) {
					String ident = e.getValue();
					C4Variable var = new C4Variable("CON_" + ident, C4Type.INT); //$NON-NLS-1$
					var.setScope(C4VariableScope.CONST);
					var.setParentDeclaration(this);
					var.setLocation(e.getLocation());
					controlVariables.add(var);
				}
			}
		}
	}
	
	@Override
	public C4Declaration findDeclaration(String declarationName, Class<? extends C4Declaration> declarationClass) {
		if (declarationClass == C4Variable.class && declarationName.startsWith("CON_")) { //$NON-NLS-1$
			for (C4Variable var : getControlVariables())
				if (var.getName().equals(declarationName))
					return var;
		}
		return super.findDeclaration(declarationName, declarationClass);
	}

}
