/**
 * 
 */
package net.arctics.clonk.resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import net.arctics.clonk.parser.C4ID;

public class InputStreamRespectingUniqueIDs extends ObjectInputStream {

	public InputStreamRespectingUniqueIDs(InputStream input)
			throws IOException {
		super(input);
		enableResolveObject(true);
	}

	@Override
	protected Object resolveObject(Object obj) throws IOException {
		if (obj instanceof C4ID) {
			//System.out.println(obj.toString());
			return ((C4ID)obj).makeSpecial();
		}
		// TODO Auto-generated method stub
		return super.resolveObject(obj);
	}
	
}