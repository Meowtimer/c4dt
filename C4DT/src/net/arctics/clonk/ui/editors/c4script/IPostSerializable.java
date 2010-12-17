package net.arctics.clonk.ui.editors.c4script;

public interface IPostSerializable<T extends IPostSerializable<?>> {
	void postSerialize(T parent);
}
