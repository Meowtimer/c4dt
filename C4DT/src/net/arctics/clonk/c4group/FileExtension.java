package net.arctics.clonk.c4group;

/**
 * Well-known file extension. Actual extension value dependent on engine.
 * @author madeen
 *
 */
public enum FileExtension {
	Other(false),
	DefinitionGroup,
	ResourceGroup,
	ScenarioGroup,
	FolderGroup,
	Material(false);
	public final boolean group;
	FileExtension() { this(true); }
	FileExtension(boolean group) { this.group = group; }
}