package net.arctics.clonk.util;

import java.net.URL;
import java.util.List;

public abstract class PathUtil {
	public static String chopPath(String containerPath, String path) {
		int ndx = path.lastIndexOf(containerPath);
		return ndx != -1
			? path.substring(ndx+containerPath.length())
			: null;
	}
	
	public static void addURLIfNotDuplicate(String containerPathIncludingEngine, URL url, List<URL> urls) {
		String truncatedPath = chopPath(containerPathIncludingEngine, url.getPath());
		assert(truncatedPath != null);
		for (URL oldURL : urls) {
			String chopped = chopPath(containerPathIncludingEngine, oldURL.getPath());
			if (chopped != null && chopped.equals(truncatedPath)) {
				return;
			}
		}
		urls.add(url);
	}
}
