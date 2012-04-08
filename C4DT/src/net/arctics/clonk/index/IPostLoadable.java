package net.arctics.clonk.index;

public interface IPostLoadable<ParentType, RootType> {
	void postLoad(ParentType parent, RootType root);
}
