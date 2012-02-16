package net.arctics.clonk.util;

import net.arctics.clonk.Core;

public class PairWithContext<First, Second> extends Pair<First, Second> implements IHasContext {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	
	private Object context;
	
	public PairWithContext(First first, Second second, Object context) {
		super(first, second);
		this.context = context;
	}
	
	public PairWithContext(Pair<First, Second> pair, Object context) {
		this (pair.first(), pair.second(), context);
	}

	@Override
	public Object context() {
		return context;
	}

}
