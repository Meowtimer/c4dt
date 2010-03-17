package net.arctics.clonk.util;

import java.io.OutputStream;
import java.net.URL;

public interface IStorageLocation {
	String getName();
	OutputStream getOutputStream(URL storageURL);
	URL getURL(String entryName, boolean create);
}
