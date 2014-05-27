package net.arctics.clonk.c4group;


/**
 * Interface used to filter files in {@link C4Group}s so they won't be loaded into memory
 */
@FunctionalInterface
public interface C4GroupHeaderFilterBase {
	int READINTOMEMORY = 1;
	boolean accepts(C4GroupEntryHeader header, C4Group context);
	default void processGroupItem(final C4GroupItem item) {}
	default void notifyAboutCreatedItem(final C4GroupEntryHeader header, final C4GroupItem item) {}
	default int flagsForEntry(final C4GroupFile entry) { return 0; }
}