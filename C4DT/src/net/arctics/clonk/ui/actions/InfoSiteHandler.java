package net.arctics.clonk.ui.actions;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.PartInitException;

import net.arctics.clonk.ui.editors.EntityHyperlink;

public class InfoSiteHandler extends AbstractHandler {
	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		try {
			EntityHyperlink.openURL(new URL("http://www.deenosaurier.de/c4dt/"));
		} catch (PartInitException | MalformedURLException e) {
			e.printStackTrace();
		}
		return null;
	}
}
