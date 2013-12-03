package net.arctics.clonk.util;

import java.net.URL;
import java.util.List;

public abstract class PathUtil {
	public static String chopPath(final String containerPath, final String path) {
		final int ndx = path.lastIndexOf(containerPath);
		return ndx != -1
			? path.substring(ndx+containerPath.length())
			: null;
	}
	
	public static void addURLIfNotDuplicate(final String containerPathIncludingEngine, final URL url, final List<URL> urls) {
		final String truncatedPath = chopPath(containerPathIncludingEngine, url.getPath());
		assert(truncatedPath != null);
		for (final URL oldURL : urls) {
			final String chopped = chopPath(containerPathIncludingEngine, oldURL.getPath());
			if (chopped != null && chopped.equals(truncatedPath))
				return;
		}
		urls.add(url);
	}
}
