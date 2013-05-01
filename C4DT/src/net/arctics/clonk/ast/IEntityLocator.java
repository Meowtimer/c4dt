package net.arctics.clonk.ast;


public interface IEntityLocator {
	<X> X context(Class<X> cls);
}
