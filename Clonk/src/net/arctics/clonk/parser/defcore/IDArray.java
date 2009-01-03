package net.arctics.clonk.parser.defcore;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import net.arctics.clonk.parser.C4ID;
import net.arctics.clonk.parser.Pair;

public class IDArray extends DefCoreOption {
	private final List<Pair<C4ID,Integer>> components = new ArrayList<Pair<C4ID,Integer>>();
	
	public IDArray(String name) {
		super(name);
	}
	
	public void add(C4ID id, int num) {
		components.add(new Pair<C4ID, Integer>(id,num));
	}

	public List<Pair<C4ID, Integer>> getComponents() {
		return components;
	}

	public String getStringRepresentation() {
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

	@Override
	public void setInput(String input) throws DefCoreParserException {
		// TODO Auto-generated method stub
		
	}
}
