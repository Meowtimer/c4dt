package net.arctics.clonk.ui.editors.actions.c4script;

import java.util.HashSet;
import java.util.Set;

import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.IHasIncludes;
import net.arctics.clonk.parser.c4script.IHasSubDeclarations;
import net.arctics.clonk.parser.c4script.ScriptBase;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.ui.editors.c4script.C4ScriptEditor;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;

public class LookupDeclarationHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchPart part = HandlerUtil.getActivePart(event);
		if (part instanceof C4ScriptEditor) {
			ScriptBase script = ((C4ScriptEditor)part).scriptBeingEdited();
			if (script != null) {
				Set<IHasIncludes> scripts = new HashSet<IHasIncludes>();
				script.gatherIncludes(scripts, true);
				Set<Declaration> declarations = new HashSet<Declaration>();
				for (IHasIncludes s : scripts)
					if (s instanceof ScriptBase)
						for (Declaration d : ((ScriptBase)s).allSubDeclarations(IHasSubDeclarations.DIRECT_SUBDECLARATIONS))
							declarations.add(d);
				DeclarationChooser chooser = new DeclarationChooser(HandlerUtil.getActiveShell(event), declarations);
				chooser.setInitialPattern(".*");
				chooser.run();
			}
		}
		else {
			ClonkProjectNature nat = ClonkProjectNature.get(part);
			if (nat != null && nat.getIndex() != null)
				new DeclarationChooser(HandlerUtil.getActiveShell(event), nat.getIndex()).run();
		}
		return null;
	}

}
