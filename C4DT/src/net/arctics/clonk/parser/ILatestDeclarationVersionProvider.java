package net.arctics.clonk.parser;


public interface ILatestDeclarationVersionProvider {
	<T extends Declaration> T getLatestVersion(T from);
}
