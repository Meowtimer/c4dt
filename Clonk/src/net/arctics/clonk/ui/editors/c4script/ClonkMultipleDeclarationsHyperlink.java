package net.arctics.clonk.ui.editors.c4script;

import java.util.List;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.ui.editors.ClonkHyperlink;
import net.arctics.clonk.ui.editors.ClonkTextEditor;
import net.arctics.clonk.ui.editors.actions.c4script.DeclarationChooser;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.window.Window;

public class ClonkMultipleDeclarationsHyperlink extends ClonkHyperlink {
	
	private List<C4Declaration> proposedDeclarations;

	public ClonkMultipleDeclarationsHyperlink(IRegion identRegion,
			List<C4Declaration> proposedDeclarations) {
		super(identRegion, null);
		this.proposedDeclarations = proposedDeclarations;
	}
	
	@Override
	public void open() {
		chooseDeclarations();
	}

	private boolean chooseDeclarations() {
		DeclarationChooser chooser = new DeclarationChooser(ClonkCore.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell(), this.proposedDeclarations);
		switch (chooser.open()) {
		case Window.OK:
			for (C4Declaration d : chooser.getSelectedDeclarations()) {
				ClonkTextEditor.openDeclaration(d);
				return true;
			}
		}
		return false;
	}

}
