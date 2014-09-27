package net.arctics.clonk.c4script;

import net.arctics.clonk.Core;

/**
 * Variable which has been created because of some assignment like this.asd = 123;
 * This class exists to differentiate between regular locals and locals added in this way which is necessary
 * because locals created via assignment should be accessible without qualification and that way shadow global identifiers.
 * @author madeen
 *
 */
public class AssignmentVariable extends Variable {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	public AssignmentVariable(Scope scope, String name) {
		super(scope, name);
	}
}
