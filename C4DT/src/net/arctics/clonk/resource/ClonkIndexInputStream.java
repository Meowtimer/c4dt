/**
 * 
 */
package net.arctics.clonk.resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import net.arctics.clonk.parser.IInternalizable;

/**
 * Enforces that some objects won't be duplicated, like ids and typesets
 * @author madeen
 *
 */
public class ClonkIndexInputStream extends ObjectInputStream {

	public ClonkIndexInputStream(InputStream input) throws IOException {
		super(input);
		enableResolveObject(true);
	}

	@Override
	protected Object resolveObject(Object obj) throws IOException {
		if (obj instanceof IInternalizable)
			return ((IInternalizable)obj).internalize();
		else
			return super.resolveObject(obj);
	}
	
}