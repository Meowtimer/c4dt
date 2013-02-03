package net.arctics.clonk.index;

import java.io.Serializable;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.Variable;

public class EngineVariable extends Variable implements IReplacedWhenSaved {
	protected static class Ticket implements Serializable, ISerializationResolvable {
		private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
		private final String name;
		public Ticket(String name) {
			super();
			this.name = name;
		}
		@Override
		public Object resolve(Index index) {
			return index.engine().findVariable(name);
		}
	}
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	public EngineVariable(String name, IType type) { super(name, type); }
	public EngineVariable(String name, Scope scope) { super(name, scope); }
	public EngineVariable(String name, IType type, String desc, Scope scope) { super(name, type, desc, scope); }
	public EngineVariable() { super(); }
	public EngineVariable(String name, String scope) { super(name, scope); }
	@Override
	public Object saveReplacement() { return new Ticket(name()); }
}