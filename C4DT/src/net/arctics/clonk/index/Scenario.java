package net.arctics.clonk.index;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ID;

import org.eclipse.core.resources.IContainer;

/**
 * A scenario. 
 * @author madeen
 *
 */
public class Scenario extends Definition {

	public Scenario(Index index, String name, IContainer container) {
		super(index, ID.NULL, name, container);
	}
	
	public static Scenario get(IContainer folder) {
		Definition obj = definitionCorrespondingToFolder(folder);
		return obj instanceof Scenario ? (Scenario)obj : null;
	}

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

}
