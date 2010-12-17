package net.arctics.clonk.parser;


public interface ILatestDeclarationVersionProvider {
	<T extends C4Declaration> T getLatestVersion(T from);
}
