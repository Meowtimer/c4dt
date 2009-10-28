package net.arctics.clonk.index;

import net.arctics.clonk.parser.C4ID;

/**
 * Container for engine functions and constants.
 * @author Madeen
 *
 */
public class C4Engine extends C4ObjectExtern {

    public C4Engine(String name) {
	    super(C4ID.getID(name), name, null, null);
    }

	private static final long serialVersionUID = 1L;

}
