package net.arctics.clonk.util;

import static net.arctics.clonk.util.Utilities.eq;

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
		final boolean duplicate = truncatedPath != null && urls.stream()
			.map(oldURL -> chopPath(containerPathIncludingEngine, oldURL.getPath()))
			.anyMatch(chopped -> eq(chopped, truncatedPath));
		if (!duplicate)
			urls.add(url);
	}
}
