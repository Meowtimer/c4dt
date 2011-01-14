/**
 * 
 */
package net.arctics.clonk.resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import net.arctics.clonk.parser.ID;
import net.arctics.clonk.parser.c4script.TypeSet;
import net.arctics.clonk.parser.c4script.ImportedObject;

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
		Class<?> cls = obj.getClass();
		if (cls == ID.class) {
			//System.out.println(obj.toString());
			return ((ID)obj).internalize();
		}
		else if (cls == TypeSet.class) {
			return ((TypeSet)obj).internalize();
		}
		else if (cls == ImportedObject.class) {
			return ((ImportedObject)obj).resolve();
		}
		return super.resolveObject(obj);
	}
	
}