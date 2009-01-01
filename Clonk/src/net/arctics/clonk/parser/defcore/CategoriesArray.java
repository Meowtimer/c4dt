package net.arctics.clonk.parser.defcore;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.C4Variable;

public class CategoriesArray extends DefCoreOption {

	private final List<C4Variable> constants = new ArrayList<C4Variable>(4);
	private int summedValue = -1;
	
	public CategoriesArray(String name) {
		super(name);
	}
	
	public void add(C4Variable var) {
		constants.add(var);
	}
	
	public String getStringRepresentation() {
		StringBuilder builder = new StringBuilder(constants.size() * 10); // C4D_Back|
		ListIterator<C4Variable> it = constants.listIterator();
		while (it.hasNext()) {
			C4Variable var = it.next();
			builder.append(var.getName());
			if (it.hasNext()) builder.append('|');
		}
		return builder.toString();
	}

	public List<C4Variable> getConstants() {
		return constants;
	}

	@Override
	public void setInput(String input) throws DefCoreParserException {
		String[] parts = input.split("|");
		if (parts.length == 1) {
			try {
				int categories = Integer.parseInt(parts[0]);
				summedValue = categories;
			} catch(NumberFormatException e) {
				summedValue = -1;
			}
		}
		else {
			for(int i = 0; i < parts.length;i++) {
				add(ClonkCore.ENGINE_OBJECT.findVariable(parts[i]));
			}
		}
	}
	
	/**
	 * Only set when the defcore value is the binary sum.
	 * @return the sum or <tt>-1<tt> if categories are defined through concatenation of constant names
	 */
	public int getSummedValue() {
		return summedValue;
	}

}
