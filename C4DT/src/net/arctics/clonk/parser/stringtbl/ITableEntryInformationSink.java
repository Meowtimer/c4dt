package net.arctics.clonk.parser.stringtbl;

public interface ITableEntryInformationSink {
	void addTblEntry(String name, String value, int start, int end);
}