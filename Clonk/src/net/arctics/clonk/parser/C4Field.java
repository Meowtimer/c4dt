package net.arctics.clonk.parser;

import java.io.Serializable;

import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.util.IHasRelatedResource;

import org.eclipse.core.resources.IResource;

public abstract class C4Field implements Serializable, IHasRelatedResource  {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * The name of this declaration
	 */
	protected String name;
	
	/**
	 * The location this declaration is declared at
	 */
	protected SourceLocation location;
	
	/**
	 * The parent declaration
	 */
	protected transient C4Field parentDeclaration;
	
	/**
	 * result to be returned of occurenceScope if there is no scope
	 */
	private static final Object[] EMPTY_SCOPE = new IResource[0];

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param location the location to set
	 */
	public void setLocation(SourceLocation location) {
		this.location = location;
	}
	/**
	 * @return the location
	 */
	public SourceLocation getLocation() {
		return location;
	}
	/**
	 * Returns an integer that is supposed to be different for different types of declarations (functions, variables)
	 * so that sorting of declarations by type is possible based on this value.
	 * @return the category value
	 */
	public int sortCategory() {
		// TODO Auto-generated method stub
		return 0;
	}
	/**
	 * Set the script of this declaration.
	 * @param script the object to set
	 */
	public void setScript(C4ScriptBase script) {
		setParentDeclaration(script);
	}
	/**
	 * Returns the script this declaration is declared in.
	 * @return the script
	 */
	public C4ScriptBase getScript() {
		for (C4Field f = this; f != null; f = f.parentDeclaration)
			if (f instanceof C4ScriptBase)
				return (C4ScriptBase)f;
		return null;
	}
	
	/**
	 * Sets the parent declaration of this declaration.
	 * @param field the new parent declaration
	 */
	public void setParentDeclaration(C4Field field) {
		this.parentDeclaration = field;
	}
	
	/**
	 * Returns a brief info string describing the declaration.
	 * @return The short info string.
	 */
	public String getShortInfo() {
		return getName();
	}
	
	/**
	 * Returns an array of all sub declarations meant to be displayed in the outline.
	 * @return
	 */
	public Object[] getSubDeclarationsForOutline() {
		return null;
	}
	
	/**
	 * Returns whether this declaration has sub declarations
	 * @return
	 */
	public boolean hasSubDeclarations() {
		return false;
	}
	
	/**
	 * Returns the latest version of this declaration, obtaining it by searching for a declaration with its name in its parent declaration
	 * @return The latest version of this declaration
	 */
	public C4Field latestVersion() {
		if (parentDeclaration != null)
			parentDeclaration = parentDeclaration.latestVersion();
		if (parentDeclaration instanceof C4Structure)
			return ((C4Structure)parentDeclaration).findDeclaration(getName());
		return this;
	}
	
	/**
	 * Returns the name of this declaration
	 */
	@Override
	public String toString() {
		return getName();
	}
	
	/**
	 * Returns the objects this declaration might be referenced in (includes C4Functions, IResources and other types)
	 * @param project
	 * @return
	 */
	public Object[] occurenceScope(ClonkProjectNature project) {
		C4ScriptBase script = getScript();
		if (script instanceof C4ObjectIntern || script instanceof C4ScriptIntern) {
			return new Object[] {((IResource) script.getScriptFile()).getProject()};
		}
		return (project != null) ? new Object[] {project.getProject()} : EMPTY_SCOPE;
	}
	
	/**
	 * Returns the resource this declaration is declared in
	 */
	public IResource getResource() {
		return getScript().getResource();
	}
	
	/**
	 * Returns an Iterable for iterating over all sub declaration of this declaration.
	 * Might return null if there are none.
	 * @return The Iterable for iterating over sub declarations or null.
	 */
	public Iterable<C4Field> allSubDeclarations() {
		return null;
	}
	
	/**
	 * Called after deserialization to restore transient references
	 * @param parent
	 */
	public void fixReferencesAfterSerialization(C4Field parent) {
		setParentDeclaration(parent);
		Iterable<C4Field> subDecs = this.allSubDeclarations();
		if (subDecs != null)
			for (C4Field d : subDecs)
				d.fixReferencesAfterSerialization(this);
	}
	
	/**
	 * Returns whether this declaration is global (functions are global when declared as "global" while variables are global when declared as "static") 
	 * @return true if global, false if not
	 */
	public boolean isGlobal() {
		return false;
	}
	
}
