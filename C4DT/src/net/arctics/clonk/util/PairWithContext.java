package net.arctics.clonk.util;

import net.arctics.clonk.ClonkCore;

public class PairWithContext<First, Second> extends Pair<First, Second> implements IHasContext {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	
	private Object context;
	
	public PairWithContext(First first, Second second, Object context) {
		super(first, second);
		this.context = context;
	}
	
	public PairWithContext(Pair<First, Second> pair, Object context) {
		this (pair.getFirst(), pair.getSecond(), context);
	}

	public Object context() {
		return context;
	}

}
