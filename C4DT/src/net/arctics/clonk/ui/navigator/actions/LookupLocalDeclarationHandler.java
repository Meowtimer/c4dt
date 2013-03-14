package net.arctics.clonk.ui.navigator.actions;

import java.util.HashSet;
import java.util.Set;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.IHasIncludes.GatherIncludesOptions;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.ui.editors.actions.c4script.EntityChooser;
import net.arctics.clonk.ui.editors.c4script.C4ScriptEditor;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;

public class LookupLocalDeclarationHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final IWorkbenchPart part = HandlerUtil.getActivePart(event);
		if (part instanceof C4ScriptEditor) {
			final Script script = ((C4ScriptEditor)part).script();
			if (script != null) {
				final Set<Script> scripts = new HashSet<Script>();
				script.gatherIncludes(script.index(), script, scripts, GatherIncludesOptions.Recursive);
				final Set<Declaration> declarations = new HashSet<Declaration>();
				for (final Script s : scripts)
					if (s instanceof Script)
						for (final Declaration d : s.subDeclarations(s.index(), net.arctics.clonk.parser.DeclMask.ALL))
							declarations.add(d);
				final EntityChooser chooser = new EntityChooser(
					Platform.getResourceString(Core.instance().getBundle(), "%LookupLocalDeclaration_Name"),
					HandlerUtil.getActiveShell(event), declarations
				);
				chooser.setInitialPattern(".*");
				chooser.run();
			}
		}
		return null;
	}

}
