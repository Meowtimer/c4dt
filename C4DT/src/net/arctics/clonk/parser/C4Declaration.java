package net.arctics.clonk.parser;

import java.io.Serializable;


import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.C4Engine;
import net.arctics.clonk.index.C4ObjectIntern;
import net.arctics.clonk.index.C4Scenario;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.c4script.C4ScriptIntern;
import net.arctics.clonk.parser.c4script.IHasUserDescription;
import net.arctics.clonk.parser.stringtbl.StringTbl;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.util.IHasRelatedResource;
import net.arctics.clonk.util.INode;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IRegion;

/**
 * Base class for all declarations (object definitions, actmaps, functions, variables etc)
 * @author madeen
 *
 */
public abstract class C4Declaration implements Serializable, IHasRelatedResource, INode  {
	/**
	 * 
	 */
	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	
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
	protected transient C4Declaration parentDeclaration;
	
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
	 * Sets the name.
	 * @param name the new name
	 */
	public void setName(String name) {
		this.name = name;
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
	
	public IRegion getRegionToSelect() {
		return getLocation();
	}
	
	/**
	 * Returns an integer that is supposed to be different for different types of declarations (functions, variables)
	 * so that sorting of declarations by type is possible based on this value.
	 * @return the category value
	 */
	public int sortCategory() {
		return 0;
	}
	
	/**
	 * Set the script of this declaration.
	 * @param script the object to set
	 */
	public void setScript(C4ScriptBase script) {
		setParentDeclaration(script);
	}
	
	@SuppressWarnings("unchecked")
	protected final <T extends C4Declaration> T getTopLevelParentDeclarationOfType(Class<T> type) {
		T result = null;
		for (C4Declaration f = this; f != null; f = f.parentDeclaration)
			if (type.isAssignableFrom(f.getClass()))
				result = (T) f;
		return result;
	}
	
	/**
	 * Returns the toplevel C4Structure this declaration is declared in.
	 * @return the structure
	 */
	public C4Structure getTopLevelStructure() {
		return getTopLevelParentDeclarationOfType(C4Structure.class);
	}
	
	/**
	 * Returns the script this declaration is declared in.
	 * @return the script
	 */
	public C4ScriptBase getScript() {
		return getTopLevelParentDeclarationOfType(C4ScriptBase.class);
	}
	
	public C4Scenario getScenario() {
		Object file = getScript() != null ? getScript().getScriptFile() : null;
		if (file instanceof IResource) {
			for (IResource r = (IResource) file; r != null; r = r.getParent()) {
				if (r instanceof IContainer) {
					C4Scenario s = C4Scenario.get((IContainer) r);
					if (s != null)
						return s;
				}
			}
		}
		return null;
	}
	
	/**
	 * Sets the parent declaration of this declaration.
	 * @param field the new parent declaration
	 */
	public void setParentDeclaration(C4Declaration field) {
		this.parentDeclaration = field;
	}
	
	/**
	 * Returns a brief info string describing the declaration.
	 * @return The short info string.
	 */
	public String getInfoText() {
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
	public boolean hasSubDeclarationsInOutline() {
		return false;
	}
	
	/**
	 * Returns the latest version of this declaration, obtaining it by searching for a declaration with its name in its parent declaration
	 * @return The latest version of this declaration
	 */
	public C4Declaration latestVersion() {
		if (parentDeclaration != null)
			parentDeclaration = parentDeclaration.latestVersion();
		if (parentDeclaration instanceof C4Structure)
			return ((C4Structure)parentDeclaration).findLocalDeclaration(getName(), getClass());
		return this;
	}
	
	/**
	 * Returns the name of this declaration
	 */
	@Override
	public String toString() {
		return name != null ? name : getClass().getSimpleName();
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
		return getParentDeclaration() != null ? getParentDeclaration().getResource() : null;
	}
	
	/**
	 * Returns the parent declaration this one is contained in
	 * @return
	 */
	public C4Declaration getParentDeclaration() {
		return parentDeclaration;
	}
	
	@SuppressWarnings("unchecked")
	public final <T extends C4Declaration> T getParentDeclarationOfType(Class<T> cls) {
		for (C4Declaration d = getParentDeclaration(); d != null; d = d.getParentDeclaration()) {
			if (cls.isAssignableFrom(d.getClass())) {
				return (T) d;
			}
		}
		return null;
	}
	
	/**
	 * Returns an Iterable for iterating over all sub declaration of this declaration.
	 * Might return null if there are none.
	 * @return The Iterable for iterating over sub declarations or null.
	 */
	public Iterable<? extends C4Declaration> allSubDeclarations() {
		return null;
	}
	
	/**
	 * Called after deserialization to restore transient references
	 * @param parent the parent
	 */
	public void postSerialize(C4Declaration parent) {
		setParentDeclaration(parent);
		Iterable<? extends C4Declaration> subDecs = this.allSubDeclarations();
		if (subDecs != null)
			for (C4Declaration d : subDecs)
				d.postSerialize(this);
	}
	
	/**
	 * Called before serializing this object
	 * @param parent the parent
	 */
	public void preSerialize() {
		Iterable<? extends C4Declaration> subDecs = this.allSubDeclarations();
		if (subDecs != null)
			for (C4Declaration d : subDecs)
				d.preSerialize();
	}
	
	/**
	 * Returns whether this declaration is global (functions are global when declared as "global" while variables are global when declared as "static") 
	 * @return true if global, false if not
	 */
	public boolean isGlobal() {
		return false;
	}
	
	/**
	 * Used to filter declarations based on their name
	 * @param part The filter string
	 * @return whether this declaration should be filtered (false) or not (true)
	 */
	public boolean nameContains(String part) {
		return getName().toLowerCase().contains(part.toLowerCase());
	}
	
	public String getNodeName() {
		return getName();
	}
	
	/**
	 * Returns whether the supplied name looks like the name of a constant e.g begins with a prefix in caps followed by an underscore and a name
	 * @param name the string to check
	 * @return whether it does or not
	 */
	public static boolean looksLikeConstName(String name) {
		boolean underscore = false;
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if (i > 0 && c == '_') {
				if (!underscore)
					underscore = true;
				else
					return false;
			}
			if (!underscore) {
				if (Character.toUpperCase(c) != c) {
					return false;
				}
			}
		}
		return underscore || name.equals(name.toUpperCase());
	}

	public boolean isEngineDeclaration() {
		return getParentDeclaration() instanceof C4Engine;
	}
	
	public C4Engine getEngine() {
		return parentDeclaration != null ? parentDeclaration.getEngine() : null; 
	}

	/**
	 * Take internal state from other declaration and make it your own. This will mess up ownership relations so discard of the absorbed one
	 * @param declaration
	 */
	public void absorb(C4Declaration declaration) {
		if (this instanceof IHasUserDescription && declaration instanceof IHasUserDescription) {
			((IHasUserDescription)this).setUserDescription(((IHasUserDescription)declaration).getUserDescription());
		}
	}
	
	public void sourceCodeRepresentation(StringBuilder builder, Object cookie) {
		System.out.println("dunno");
	}
	
	public StringTbl getStringTblForLanguagePref() {
		try {
			IResource res = getResource();
			if (res == null)
				return null;
			IContainer container = res instanceof IContainer ? (IContainer) res : res.getParent();
			String pref = ClonkPreferences.getLanguagePref();
			IResource tblFile = Utilities.findMemberCaseInsensitively(container, "StringTbl"+pref+".txt"); //$NON-NLS-1$ //$NON-NLS-2$
			if (tblFile instanceof IFile)
				return (StringTbl) C4Structure.pinned((IFile) tblFile, true, false);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return null;
	}
	
}
