package net.arctics.clonk.ui.editors.c4script;

import java.util.List;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.Declaration.DeclarationLocation;
import net.arctics.clonk.ui.editors.ClonkHyperlink;
import net.arctics.clonk.ui.editors.ClonkTextEditor;
import net.arctics.clonk.ui.editors.actions.c4script.DeclarationChooser;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.window.Window;

public class ClonkMultipleDeclarationsHyperlink extends ClonkHyperlink {
	
	private List<Declaration> proposedDeclarations;

	public ClonkMultipleDeclarationsHyperlink(IRegion identRegion,
			List<Declaration> proposedDeclarations) {
		super(identRegion, null);
		this.proposedDeclarations = proposedDeclarations;
	}
	
	@Override
	public void open() {
		if (this.proposedDeclarations.size() == 1) {
			target = proposedDeclarations.get(0);
			super.open();
		}
		else
			chooseDeclarations();
	}

	private boolean chooseDeclarations() {
		DeclarationChooser chooser = new DeclarationChooser(ClonkCore.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell(), this.proposedDeclarations);
		switch (chooser.open()) {
		case Window.OK:
			boolean b = true;
			for (DeclarationLocation d : chooser.getSelectedDeclarationLocations()) {
				ClonkTextEditor.openDeclarationLocation(d, b);
				b = false;
				return true;
			}
		}
		return false;
	}

}
