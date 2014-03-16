package net.arctics.clonk.command.ui.actions;

import net.arctics.clonk.command.Command;
import net.arctics.clonk.command.ExecutableScript;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.PlatformUI;

public class InvokeCommandHandler extends AbstractHandler {

	/*private static class InputDialogWithHistory extends InputDialog {

		private static Stack<String> backStack = new Stack<String>();
		private static Stack<String> forwardStack = new Stack<String>();

		public InputDialogWithHistory(Shell parentShell, String dialogTitle,
				String dialogMessage, String initialValue,
				IInputValidator validator) {
			super(parentShell, dialogTitle, dialogMessage, initialValue, validator);
			getText().addKeyListener(new KeyListener() {
				@Override
				public void keyPressed(KeyEvent e) {
					// TODO Auto-generated method stub

				}
				@Override
				public void keyReleased(KeyEvent e) {
					if (e.keyCode == SWT.KeyUp) {
						if (!backStack.isEmpty()) {
							String text = backStack.pop();
							forwardStack.push
							getText().setText(text);
						}
					}
				}
			});
		}

	}*/

	@Override
    public Object execute(final ExecutionEvent event) throws ExecutionException {
		 final InputDialog inputDialog = new InputDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
		    	Messages.InvokeCommandAction_InvokeCommand, Messages.InvokeCommandAction_SpecifyCommand, "", null); //$NON-NLS-1$
		    switch (inputDialog.open()) {
		    case Window.OK:
		    	final ExecutableScript script = Command.executableScriptFromCommand(inputDialog.getValue());
		    	script.main().invoke(script.main().new Invocation(new Object[0], null, null));
		    }
	    return null;
    }

}
