package net.arctics.clonk.parser;

public abstract class C4Structure extends C4Field {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public abstract C4Variable findVariable(String variableName);
	public C4Field findDeclaration(String fieldName) {
		return findVariable(fieldName);
	}
}
