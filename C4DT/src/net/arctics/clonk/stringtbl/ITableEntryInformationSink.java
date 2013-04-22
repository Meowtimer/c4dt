package net.arctics.clonk.stringtbl;

public interface ITableEntryInformationSink {
	void addTblEntry(String name, String value, int start, int end);
}