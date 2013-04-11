package net.arctics.clonk.index;

import java.io.Serializable;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.IType;

public class EngineFunction extends Function implements IReplacedWhenSaved {
	private static class Ticket implements Serializable, IDeserializationResolvable {
		private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
		private final String name;
		public Ticket(String name) {
			super();
			this.name = name;
		}
		@Override
		public Object resolve(Index index) {
			return index.engine().findFunction(name);
		}
	}
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	public EngineFunction(String name, IType returnType) { super(name, returnType); }
	public EngineFunction(String name, FunctionScope scope) { super(name, scope); }
	public EngineFunction() { super(); }
	@Override
	public Function inheritedFunction() { return null; }
	@Override
	public String obtainUserDescription() { return engine().obtainDescription(this); }
	@Override
	public boolean staticallyTyped() { return true; }
	@Override
	public Object saveReplacement(Index context) { return new Ticket(name()); }
}
