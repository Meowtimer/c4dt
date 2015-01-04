package net.arctics.clonk.util;

import static java.util.Arrays.stream;

import java.io.File;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;

public class AggregateStorageLocation implements IStorageLocation {

	private final String name;
	private final IStorageLocation[] locations;

	public AggregateStorageLocation(String name, IStorageLocation... parts) {
		this.name = name;
		this.locations = parts;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public OutputStream outputStreamForURL(URL storageURL) {
		return stream(locations)
			.map(l -> l.outputStreamForURL(storageURL))
			.filter(s -> s != null)
			.findFirst().orElse(null);
	}

	@Override
	public URL locatorForEntry(String entryPath, boolean create) {
		return stream(locations)
			.map(l -> l.locatorForEntry(entryPath, create))
			.filter(l -> l != null)
			.findFirst().orElse(null);
	}

	@Override
	public void collectURLsOfContainer(String containerPath, boolean recurse, List<URL> listToAddTo) {
		stream(locations)
			.forEach(l -> l.collectURLsOfContainer(containerPath, recurse, listToAddTo));
	}

	@Override
	public File toFolder() {
		return stream(locations).map(IStorageLocation::toFolder).findFirst().orElse(null);
	}

}
