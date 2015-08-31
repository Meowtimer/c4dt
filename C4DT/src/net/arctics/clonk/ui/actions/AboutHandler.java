package net.arctics.clonk.ui.actions;

import java.text.DateFormat;
import java.util.Calendar;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Version;

import net.arctics.clonk.Core;

public class AboutHandler extends AbstractHandler {

	public String convertVersionStringToReadableInfo(final Version version) {
		String timestamp = version.getQualifier();
		try {
			final int year = Integer.parseInt(timestamp.substring(0, 4));
			final int month = Integer.parseInt(timestamp.substring(4, 6)) - 1;
			final int day = Integer.parseInt(timestamp.substring(6, 8));
			final int hour = Integer.parseInt(timestamp.substring(8, 10));
			final int minute = Integer.parseInt(timestamp.substring(10));
			final Calendar c = Calendar.getInstance();
			c.set(year, month, day, hour, minute);
			timestamp = DateFormat.getDateInstance().format(c.getTime());
		} catch (final NumberFormatException e) {
			// not a timestamp - ignore
		}
		return String.format(Messages.AboutHandler_BuildTimeInfoFormat, version.getMajor(), version.getMinor(), version.getMicro(), timestamp); 
	}
	
	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final String version = convertVersionStringToReadableInfo(Core.instance().getBundle().getVersion());
		final String message = String.format(Messages.AboutHandler_InfoTemplate, version);
		MessageDialog.openInformation(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), Messages.AboutHandler_Title, message);
		return null;
	}

}
