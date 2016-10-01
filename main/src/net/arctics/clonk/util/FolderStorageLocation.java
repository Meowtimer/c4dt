package net.arctics.clonk.util;

import static net.arctics.clonk.util.Utilities.attempt;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.eclipse.core.runtime.AssertionFailedException;
import org.eclipse.core.runtime.IPath;

public abstract class FolderStorageLocation implements IStorageLocation {
	protected final String engineName;

	public FolderStorageLocation(final String engineName) {
		this.engineName = engineName;
	}

	@Override
	public String name() {
		return engineName;
	}

	@Override
	public URL locatorForEntry(final String entryName, final boolean create) {
		try {
			final File file = fileForEntry(entryName);
			if (file == null) {
				return null;
			}
			try {
				if (create) {
					try {
						file.getParentFile().mkdirs();
						file.createNewFile();
					} catch (final IOException e) {
						e.printStackTrace();
						return null;
					}
				}
				return file.exists() ? file.toURI().toURL() : null;
			} catch (final MalformedURLException e) {
				e.printStackTrace();
				return null;
			}
		} catch (final AssertionFailedException assertionFail) {
			// happens when invoking getURL without having initialized the workspace (headless utilities)
			return null;
		}
	}

	protected abstract IPath storageLocationForEngine(String engineName);

	private File fileForEntry(final String entryName) {
		final IPath path = storageLocationForEngine(engineName);
		final File file = path != null ? path.append(entryName).toFile() : null;
		return file;
	}

	@Override
	public OutputStream outputStreamForURL(final URL storageURL) {
		return attempt(() -> new FileOutputStream(new File(storageURL.toURI())),
			Exception.class, Exception::printStackTrace
		);
	}

	private static void addFilesFrom(final File folder, final String containerPath, final List<URL> list, final boolean recurse) {
		for (final File f : folder.listFiles()) {
			try {
				PathUtil.addURLIfNotDuplicate(containerPath, f.toURI().toURL(), list);
			} catch (final MalformedURLException e) {
				e.printStackTrace();
			}
			if (recurse && f.isDirectory()) {
				addFilesFrom(f, containerPath, list, true);
			}
		}
	}

	@Override
	public void collectURLsOfContainer(String containerPath, final boolean recurse, final List<URL> listToAddTo) {
		final File folder = fileForEntry(containerPath);
		if (folder == null || !folder.exists()) {
			return;
		}
		containerPath = name() + "/" + containerPath;
		addFilesFrom(folder, containerPath, listToAddTo, recurse);
	}

	@Override
	public File toFolder() {
		final IPath p = storageLocationForEngine(engineName);
		return p != null ? new File(p.toOSString()) : null;
	}
}