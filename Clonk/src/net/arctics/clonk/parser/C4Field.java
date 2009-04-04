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
	protected String name;
	protected SourceLocation location;
	protected transient C4Field parentDeclaration;
	
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
	public int sortCategory() {
		// TODO Auto-generated method stub
		return 0;
	}
	/**
	 * @param script the object to set
	 */
	public void setScript(C4ScriptBase script) {
		setParentDeclaration(script);
	}
	/**
	 * @return the object
	 */
	public C4ScriptBase getScript() {
		for (C4Field f = this; f != null; f = f.parentDeclaration)
			if (f instanceof C4ScriptBase)
				return (C4ScriptBase)f;
		return null;
	}
	
	public void setParentDeclaration(C4Field field) {
		this.parentDeclaration = field;
	}
	
	public String getShortInfo() {
		return getName();
	}
	
	public Object[] getChildFieldsForOutline() {
		return null;
	}
	
	public boolean hasChildFields() {
		return false;
	}
	
	public C4Field latestVersion() {
		if (parentDeclaration != null)
			parentDeclaration = parentDeclaration.latestVersion();
		if (parentDeclaration instanceof C4Structure)
			return ((C4Structure)parentDeclaration).findDeclaration(getName());
		return this;
	}
	
	@Override
	public String toString() {
		return getName();
	}
	
	public Object[] occurenceScope(ClonkProjectNature project) {
		C4ScriptBase script = getScript();
		if (script instanceof C4ObjectIntern || script instanceof C4ScriptIntern) {
			return new Object[] {((IResource) script.getScriptFile()).getProject()};
		}
		return (project != null) ? new Object[] {project.getProject()} : EMPTY_SCOPE;
	}
	
	public IResource getResource() {
		return getScript().getResource();
	}
	
	public Iterable<C4Field> allSubDeclarations() {
		return null;
	}
	
	public void fixReferencesAfterSerialization(C4Field parent) {
		setParentDeclaration(parent);
		Iterable<C4Field> subDecs = this.allSubDeclarations();
		if (subDecs != null)
			for (C4Field d : subDecs)
				d.fixReferencesAfterSerialization(this);
	}
	
}
