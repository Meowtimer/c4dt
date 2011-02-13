package net.arctics.clonk.util;

import java.io.File;
import java.io.OutputStream;
import java.net.URL;
import java.util.Enumeration;

public interface IStorageLocation {
	String getName();
	OutputStream getOutputStream(URL storageURL);
	URL getURL(String entryPath, boolean create);
	Enumeration<URL> getURLs(String containerPath, boolean recurse);
	File toFolder();
}
