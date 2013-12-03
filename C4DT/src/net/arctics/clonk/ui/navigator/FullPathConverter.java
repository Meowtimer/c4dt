package net.arctics.clonk.ui.navigator;

import java.io.File;

import net.arctics.clonk.util.IConverter;

import org.eclipse.swt.widgets.FileDialog;

public final class FullPathConverter implements IConverter<String, File> {
	private final FileDialog fileDialog;

	public FullPathConverter(final FileDialog fileDialog) {
		this.fileDialog = fileDialog;
	}

	@Override
	public File convert(final String fileName) {
		return new File(fileDialog.getFilterPath()+"/"+fileName); //$NON-NLS-1$
	}
}