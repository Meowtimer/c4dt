package net.arctics.clonk.util;

public class Tagged<C, T> {
	public final C item;
	public final T tag;
	public Tagged(C item, T tag) {
		super();
		this.item = item;
		this.tag = tag;
	}
}
