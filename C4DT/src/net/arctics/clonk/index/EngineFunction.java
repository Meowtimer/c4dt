package net.arctics.clonk.index;

import static net.arctics.clonk.util.ArrayUtil.filter;
import static net.arctics.clonk.util.ArrayUtil.iterable;
import static net.arctics.clonk.util.ArrayUtil.map;

import java.io.Serializable;

import net.arctics.clonk.Core;
import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.typing.IType;

public class EngineFunction extends Function implements IReplacedWhenSaved {
	private static class Ticket implements Serializable, IDeserializationResolvable {
		private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
		private final String name;
		public Ticket(final String name) {
			super();
			this.name = name;
		}
		@Override
		public Object resolve(final Index index, final IndexEntity deserializee) {
			return index.engine().findFunction(name);
		}
	}
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	public EngineFunction(final String name, final IType returnType) { super(name, returnType); }
	public EngineFunction(final String name, final FunctionScope scope) { super(name, scope); }
	public EngineFunction() { super(); }
	@Override
	public Function inheritedFunction() { return null; }
	@Override
	public String obtainUserDescription() { return engine().obtainDescription(this); }
	@Override
	public boolean staticallyTyped() { return true; }
	@Override
	public Object saveReplacement(final Index context) { return new Ticket(name()); }
	@Override
	public Object[] occurenceScope(final Iterable<Index> indexes) {
		return super.occurenceScope(iterable(filter(
			map(ClonkProjectNature.allInWorkspace(), Index.class, ClonkProjectNature.SELECT_INDEX),
			item -> item.engine() == EngineFunction.this.engine())
		));
	}
}
