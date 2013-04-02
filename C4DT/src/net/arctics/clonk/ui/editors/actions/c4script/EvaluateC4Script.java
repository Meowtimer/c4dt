package net.arctics.clonk.ui.editors.actions.c4script;

import static net.arctics.clonk.util.Utilities.defaulting;

import java.util.Arrays;
import java.util.Collection;
import java.util.ResourceBundle;

import net.arctics.clonk.Core;
import net.arctics.clonk.command.Command;
import net.arctics.clonk.command.ExecutableScript;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.ui.editors.actions.ClonkTextEditorAction;
import net.arctics.clonk.ui.editors.actions.ClonkTextEditorAction.CommandId;
import net.arctics.clonk.ui.editors.c4script.C4ScriptEditor;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

@CommandId(id="ui.editors.actions.EvaluateC4Script")
public class EvaluateC4Script extends ClonkTextEditorAction {
	public EvaluateC4Script(ResourceBundle bundle, String prefix, ITextEditor editor) {
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
						public Collection<Script> includes(Index index, Object origin, int options) {
							return Arrays.asList(Command.COMMAND_BASESCRIPT, editor.script());
						}
					};
					final Function main = script.findFunction("Main");
					final Object r = main.invoke(main.new FunctionInvocation(new Object[0], null, this));
					final String evaluated = defaulting(r, "<No result>").toString();
					Display.getDefault().asyncExec(new Runnable() {
						@Override
						public void run() {
							MessageDialog.openInformation(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
								"Evaluation", evaluated);
						}
					});
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
