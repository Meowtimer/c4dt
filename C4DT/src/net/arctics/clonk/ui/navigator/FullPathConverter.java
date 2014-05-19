package net.arctics.clonk.ui.navigator;

import java.io.File;
import java.util.function.Function;

import org.eclipse.swt.widgets.FileDialog;

public final class FullPathConverter implements Function<String, File> {
	private final FileDialog fileDialog;
	public FullPathConverter(final FileDialog fileDialog) {
		this.fileDialog = fileDialog;
	}
	@Override
	public File apply(final String fileName) {
		return new File(fileDialog.getFilterPath()+"/"+fileName); //$NON-NLS-1$
	}
}