/**
 * 
 */
package net.arctics.clonk.resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import net.arctics.clonk.parser.C4ID;

/**
 * Enforces that there is only one instance of C4ID with the same value
 * @author madeen
 *
 */
public class InputStreamRespectingUniqueIDs extends ObjectInputStream {

	public InputStreamRespectingUniqueIDs(InputStream input)
			throws IOException {
		super(input);
		enableResolveObject(true);
	}

	@Override
	protected Object resolveObject(Object obj) throws IOException {
		if (obj.getClass() == C4ID.class) {
			//System.out.println(obj.toString());
			return ((C4ID)obj).makeSpecial();
		}
		return super.resolveObject(obj);
	}
	
}