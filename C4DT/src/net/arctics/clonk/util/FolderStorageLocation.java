package net.arctics.clonk.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;


import org.eclipse.core.runtime.AssertionFailedException;
import org.eclipse.core.runtime.IPath;

public abstract class FolderStorageLocation implements IStorageLocation {
	protected final String engineName;

	public FolderStorageLocation(String engineName) {
		this.engineName = engineName;
	}

	@Override
	public String getName() {
		return engineName;
	}

	@Override
	public URL getURL(String entryName, boolean create) {
		try {
			File file = getFile(entryName);
			try {
				if (create) {
					try {
						file.getParentFile().mkdirs();
						file.createNewFile();
					} catch (IOException e) {
						e.printStackTrace();
						return null;
					}
				}
				return file.exists() ? file.toURI().toURL() : null;
			} catch (MalformedURLException e) {
				e.printStackTrace();
				return null;
			}
		} catch (AssertionFailedException assertionFail) {
			// happens when invoking getURL without having initialized the workspace (headless utilities)
			return null;
		}
	}

	protected abstract IPath getStorageLocationForEngine(String engineName);

	private File getFile(String entryName) {
		IPath path = getStorageLocationForEngine(engineName);
		File file = path.append(entryName).toFile();
		return file;
	}

	@Override
	public OutputStream getOutputStream(URL storageURL) {
		try {
			return new FileOutputStream(new File(storageURL.toURI()));
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}

	private static void addFilesFrom(File folder, String containerPath, List<URL> list, boolean recurse) {
		for (File f : folder.listFiles()) {
			try {
				PathUtil.addURLIfNotDuplicate(containerPath, f.toURI().toURL(), list);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			if (recurse && f.isDirectory()) {
				addFilesFrom(f, containerPath, list, true);
			}
		}
	}
	
	@Override
	public void getURLsOfContainer(String containerPath, boolean recurse, List<URL> listToAddTo) {
		final File folder = getFile(containerPath);
		containerPath = getName() + "/" + containerPath;
		if (folder == null || !folder.exists())
			return;
		addFilesFrom(folder, containerPath, listToAddTo, recurse);
	}
}