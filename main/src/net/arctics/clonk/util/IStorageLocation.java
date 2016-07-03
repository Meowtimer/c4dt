package net.arctics.clonk.util;

import java.io.File;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Interface for any kind of storage location for named data units.
 * Can refer to jar bundle entries or files in folders.
 * @author madeen
 *
 */
public interface IStorageLocation {
	/**
	 * Return the name of this storage location. If the storage location is associated with an engine, the engine's name and this name will be identical.
	 * @return The name
	 */
	String name();
	/**
	 * Create a writable stream for the passed URL which is required to have been obtained from this storage location.
	 * @param storageURL the URL to create an output stream for
	 * @return the output stream or null if wrong URL or the storage location is readonly.
	 */
	OutputStream outputStreamForURL(URL storageURL);
	/**
	 * Return storage-location specific URL for the passed path, creating the data entry the path refers to if necessary and requested.
	 * @param entryPath The path of the data entry, relative to the storage location.
	 * @param create Create data entry if not already existing.
	 * @return The URL for the path or null if it didn't exist and couldn't be created or if the path was just unsuitable.
	 */
	URL locatorForEntry(String entryPath, boolean create);
	/**
	 * Return URLs of data entries contained in the given container path.
	 * @param containerPath the path of the container
	 * @param recurse Whether to also return URLs for data entries in sub-containers.
	 * @param listToAddTo Add results to this list, attempting to avoid duplicates
	 */
	void collectURLsOfContainer(String containerPath, boolean recurse, List<URL> listToAddTo);
	/**
	 * Return this storage location as a folder. Will only work with storage locations that are actually backed by a folder.
	 * @return The folder or null if read-only/not folder backed/whatever
	 */
	File toFolder();

	/**
	 * Outbound {@link #collectURLsOfContainer(String, boolean, List)}
	 * @param containerPath Container path
	 * @param recurse Whether to recurse
	 * @return List with contents supplied by {@link #collectURLsOfContainer(String, boolean, List)}
	 */
	default List<URL> locatorsOfContainer(String containerPath, boolean recurse) {
		final ArrayList<URL> result = new ArrayList<>();
		collectURLsOfContainer(containerPath, recurse, result);
		return result;
	}
}
