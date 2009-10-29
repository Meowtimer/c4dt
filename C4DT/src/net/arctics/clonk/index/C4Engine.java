package net.arctics.clonk.index;

import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.C4ID;

/**
 * Container for engine functions and constants.
 * @author Madeen
 *
 */
public class C4Engine extends C4ObjectExtern {

	private static final long serialVersionUID = 1L;
	
	private CachedEngineFuncs cachedFuncs;
	
    public CachedEngineFuncs getCachedFuncs() {
		return cachedFuncs;
	}

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
    
    public void modified() {
    	cachedFuncs = new CachedEngineFuncs(this);
    }
    
    @Override
    public void postSerialize(C4Declaration parent) {
    	super.postSerialize(parent);
    	modified();
    }

}
