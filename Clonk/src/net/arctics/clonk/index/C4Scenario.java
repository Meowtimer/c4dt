package net.arctics.clonk.index;

import net.arctics.clonk.parser.C4ID;

import org.eclipse.core.resources.IContainer;

// kind of a hack; but scenarios also have scripts...
public class C4Scenario extends C4ObjectIntern {

	public C4Scenario(C4ID id, String name, IContainer container) {
		super(id, name, container);
	}
	
	public static C4Scenario scenarioCorrespondingTo(IContainer folder) {
		C4ObjectIntern obj = objectCorrespondingTo(folder);
		return obj instanceof C4Scenario ? (C4Scenario)obj : null;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
