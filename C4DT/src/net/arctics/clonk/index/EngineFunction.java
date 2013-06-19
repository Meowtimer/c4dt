package net.arctics.clonk.index;

import static net.arctics.clonk.util.ArrayUtil.filter;
import static net.arctics.clonk.util.ArrayUtil.iterable;
import static net.arctics.clonk.util.ArrayUtil.map;

import java.io.Serializable;

import net.arctics.clonk.Core;
import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.typing.IType;
import net.arctics.clonk.util.IPredicate;

public class EngineFunction extends Function implements IReplacedWhenSaved {
	private static class Ticket implements Serializable, IDeserializationResolvable {
		private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
		private final String name;
		public Ticket(String name) {
			super();
			this.name = name;
		}
		@Override
		public Object resolve(Index index, IndexEntity deserializee) {
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
	@Override
	public Object[] occurenceScope(Iterable<Index> indexes) {
		return super.occurenceScope(iterable(filter(
			map(ClonkProjectNature.allInWorkspace(), Index.class, ClonkProjectNature.SELECT_INDEX),
			new IPredicate<Index>() {
				@Override
				public boolean test(Index item) { return item.engine() == EngineFunction.this.engine(); }
			})
		));
	}
}
