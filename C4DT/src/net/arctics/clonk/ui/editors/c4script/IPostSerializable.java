package net.arctics.clonk.ui.editors.c4script;

public interface IPostSerializable<ParentType, RootType> {
	void postSerialize(ParentType parent, RootType root);
}
