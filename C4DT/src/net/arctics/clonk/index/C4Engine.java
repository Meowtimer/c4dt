package net.arctics.clonk.index;

import net.arctics.clonk.parser.C4ID;

/**
 * Container for engine functions and constants.
 * @author Madeen
 *
 */
public class C4Engine extends C4ObjectExtern {

    public C4Engine(String name) {
	    super(null, name, null, null);
    }
    
    @Override
    public void setName(String name) {
        super.setName(name);
        // sync node name
        nodeName = name;
        id = C4ID.getID(name);
    }
    
    @Override
    public void setId(C4ID newId) {
        // ignore
    }

	private static final long serialVersionUID = 1L;

}
