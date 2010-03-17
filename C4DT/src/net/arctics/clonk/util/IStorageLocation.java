package net.arctics.clonk.util;

import java.io.OutputStream;
import java.net.URL;
import java.util.Enumeration;

public interface IStorageLocation {
	String getName();
	OutputStream getOutputStream(URL storageURL);
	URL getURL(String entryName, boolean create);
	Enumeration<URL> getURLs(String containerName);
}
