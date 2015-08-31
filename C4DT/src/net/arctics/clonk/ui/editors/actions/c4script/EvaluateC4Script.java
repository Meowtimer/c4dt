package net.arctics.clonk.ui.editors.actions.c4script;

import static net.arctics.clonk.util.Utilities.defaulting;

import java.util.Collection;
import java.util.ResourceBundle;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import net.arctics.clonk.Core;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.command.Command;
import net.arctics.clonk.command.ExecutableScript;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.ui.editors.actions.ClonkTextEditorAction;
import net.arctics.clonk.ui.editors.actions.ClonkTextEditorAction.CommandId;
import net.arctics.clonk.ui.editors.c4script.C4ScriptEditor;

@CommandId(id="ui.editors.actions.EvaluateC4Script")
public class EvaluateC4Script extends ClonkTextEditorAction {
	public EvaluateC4Script(final ResourceBundle bundle, final String prefix, final ITextEditor editor) {
		super(bundle, prefix, editor);
	}
	@Override
	public void run() {
		try {
			final Thread thread = new Thread() {
				@Override
				public void run() {
					final C4ScriptEditor editor = (C4ScriptEditor)getTextEditor();
					final String code = ((ITextSelection)getTextEditor().getSelectionProvider().getSelection()).getText();
					final Script script = new ExecutableScript("Eval", String.format("func Main() { %s; }", code), editor.script().index()) {
						private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
						@Override
						public boolean gatherIncludes(final Index contextIndex, final Script origin, final Collection<Script> set, final int options) {
							set.add(Command.BASE);
							set.add(editor.script());
							return super.gatherIncludes(contextIndex, origin, set, options);
						}
					};
					final Function main = script.findFunction("Main");
					final Object r = main.invoke(main.new Invocation(new Object[0], null, this));
					final String evaluated = defaulting(r, "<No result>").toString();
					Display.getDefault().asyncExec(() -> MessageDialog.openInformation(
						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
						"Evaluation", evaluated)
					);
				}
			};
			thread.run();
			thread.join(3000);
		} catch (final Exception e) {
			e.printStackTrace();
			MessageDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Oops", "Something went wrong");
		}
	}
}
