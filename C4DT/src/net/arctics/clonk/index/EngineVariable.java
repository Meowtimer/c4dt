package net.arctics.clonk.index;

import static net.arctics.clonk.util.ArrayUtil.filter;
import static net.arctics.clonk.util.ArrayUtil.iterable;
import static net.arctics.clonk.util.ArrayUtil.map;

import java.io.Serializable;

import net.arctics.clonk.Core;
import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.c4script.typing.IType;

public class EngineVariable extends Variable implements IReplacedWhenSaved {
	protected static class Ticket implements Serializable, IDeserializationResolvable {
		private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
		private final String name;
		public Ticket(final String name) {
			super();
			this.name = name;
		}
		@Override
		public Object resolve(final Index index, final IndexEntity deserializee) {
			return index.engine().findVariable(name);
		}
	}
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	public EngineVariable(final String name, final IType type) { super(name, type); }
	public EngineVariable(final Scope scope, final String name) { super(scope, name); }
	public EngineVariable() { super(); }
	@Override
	public Object saveReplacement(final Index context) { return new Ticket(name()); }
	@Override
	public Object[] occurenceScope(final Iterable<Index> indexes) {
		return super.occurenceScope(iterable(filter(
			map(ClonkProjectNature.allInWorkspace(), Index.class, ClonkProjectNature.SELECT_INDEX),
			item -> item.engine() == EngineVariable.this.engine())
		));
	}
}