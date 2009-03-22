package net.arctics.clonk.parser.inireader;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import net.arctics.clonk.parser.C4ID;
import net.arctics.clonk.util.Pair;

public class IDArray implements IEntryCreateable {
	private final List<Pair<C4ID,Integer>> components = new ArrayList<Pair<C4ID,Integer>>();
	
	public IDArray(String value) throws IniParserException {
		setInput(value);
	}
	
	public IDArray() {
	}
	
	public void add(C4ID id, int num) {
		components.add(new Pair<C4ID, Integer>(id,num));
	}

	public List<Pair<C4ID, Integer>> getComponents() {
		return components;
	}
	
	public String toString() {
		StringBuilder builder = new StringBuilder(components.size() * 7); // MYID=1;
		ListIterator<Pair<C4ID,Integer>> it = components.listIterator();
		while (it.hasNext()) {
			Pair<C4ID,Integer> pair = it.next();
			builder.append(pair.getFirst().getName());
			builder.append('=');
			builder.append(pair.getSecond());
			if (it.hasNext()) builder.append(';');
		}
		return builder.toString();
	}

	public void setInput(String input) throws IniParserException {
		// CLNK=1;STIN=10;
		components.clear();
		String[] parts = input.split(";");
		for(String part : parts) {
			if (part.contains("=")) {
				String[] idAndCount = part.split("=");
				try {
					components.add(new Pair<C4ID, Integer>(C4ID.getID(idAndCount[0].trim()),Integer.parseInt(idAndCount[1].trim())));
				}
				catch(NumberFormatException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
