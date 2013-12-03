package net.arctics.clonk.util;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.MessageConsole;

public abstract class Console {
	private final static MessageConsole clonkConsole = withName("Clonk");
	public static MessageConsole clonkConsole() { return clonkConsole; }
	private static MessageConsole withName(final String name) {
		final ConsolePlugin plugin = ConsolePlugin.getDefault();
		final IConsoleManager conMan = plugin.getConsoleManager();
		final IConsole[] existing = conMan.getConsoles();
		for (int i = 0; i < existing.length; i++)
			if (name.equals(existing[i].getName()))
				return (MessageConsole) existing[i];
		//no console found, so create a new one
		final MessageConsole console = new MessageConsole(name, null);
		conMan.addConsoles(new IConsole[] {console});
		return console;
	}
	public static void display() {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				final String id = IConsoleConstants.ID_CONSOLE_VIEW;

				// show console
				try {
					final IConsoleView view = (IConsoleView) page.showView(id);
					view.display(clonkConsole());
				} catch (final PartInitException e) {
					e.printStackTrace();
				}
			}
		});
	}
}
