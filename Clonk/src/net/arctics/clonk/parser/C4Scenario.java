package net.arctics.clonk.parser;

import org.eclipse.core.resources.IContainer;

// kind of a hack; but scenarios also have scripts...
public class C4Scenario extends C4ObjectIntern {

	public C4Scenario(C4ID id, String name, IContainer container) {
		super(id, name, container);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
