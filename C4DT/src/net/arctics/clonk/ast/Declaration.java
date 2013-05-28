package net.arctics.clonk.ast;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.arctics.clonk.Core;
import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.builder.ProjectSettings.Typing;
import net.arctics.clonk.c4script.FindDeclarationInfo;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.IHasName;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.IIndexEntity;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.IndexEntity;
import net.arctics.clonk.index.Scenario;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.stringtbl.StringTbl;
import net.arctics.clonk.util.IHasRelatedResource;
import net.arctics.clonk.util.INode;
import net.arctics.clonk.util.Sink;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.IRegion;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Base class for all declarations (object definitions, actmaps, functions, variables etc)
 * @author madeen
 *
 */
public abstract class Declaration extends ASTNode implements Serializable, IHasRelatedResource, INode, IIndexEntity, IAdaptable, IPlaceholderPatternMatchTarget, IHasName {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	protected Declaration() {}
	protected Declaration(int start, int end) { super(start, end); }

	/**
	 * The name of this declaration
	 */
	protected String name;

	/**
	 * @return the name
	 */
	@Override
	public String name() { return name; }
	/**
	 * Sets the name.
	 * @param name the new name
	 */
	public void setName(String name) { this.name = name; }

	/**
	 * @param Set the location of the declaration in its declaring file.
	 */
	public void setLocation(SourceLocation location) {
		setLocation(location != null ? location.start() : 0, location != null ? location.end() : 0);
	}

	/**
	 * Return the region to be selected when using editor navigation commands such as jump to definition. By default, this method returns this object since it already is a location.
	 * @return The region to select when using editor navigation commands
	 */
	public IRegion regionToSelect() { return this; }

	/**
	 * Returns an integer that is supposed to be different for different types of declarations (functions, variables)
	 * so that sorting of declarations by type is possible based on this value.
	 * @return the category value
	 */
	public int sortCategory() { return 0; }

	/**
	 * Returns the top-level {@link Structure} this declaration is declared in.
	 * @return the {@link Structure}
	 */
	public Structure topLevelStructure() { return topLevelParentDeclarationOfType(Structure.class); }

	/**
	 * Returns the {@link Script} this declaration is declared in.
	 * @return the {@link Script}
	 */
	public Script script() { return topLevelParentDeclarationOfType(Script.class); }

	/**
	 * Return the {@link Scenario} this declaration is declared in.
	 * @return The {@link Scenario}
	 */
	public Scenario scenario() { return parent != null ? ((Declaration)parent).scenario() : null; }

	/**
	 * Returns a brief info string describing the declaration. Meant for UI presentation.
	 * @return The short info string.
	 */
	@Override
	public String infoText(IIndexEntity context) { return name(); }
	public String displayString(IIndexEntity context) { return infoText(this); }

	/**
	 * Returns an array of all sub declarations meant to be displayed in the outline.
	 * @return
	 */
	public Object[] subDeclarationsForOutline() { return null; }

	/**
	 * Returns the latest version of this declaration, obtaining it by searching for a declaration with its name in its parent declaration
	 * @return The latest version of this declaration
	 */
	public Declaration latestVersion() {
		Declaration parent = parentDeclaration();
		if (parent != null)
			parent = parent.latestVersion();
		if (parent instanceof ILatestDeclarationVersionProvider) {
			final Declaration latest = ((ILatestDeclarationVersionProvider)parent).latestVersionOf(this);
			if (latest != null)
				return latest;
			else
				// fallback on returning this declaration if latest version providing not properly implemented
				return this;
		}
		else
			return this;
	}

	/**
	 * Returns the name of this declaration
	 */
	@Override
	public String toString() { return name != null ? name : getClass().getSimpleName(); }

	/**
	 * Returns the objects this declaration might be referenced in (includes {@link Function}s, {@link IResource}s and other {@link Script}s)
	 * @param project
	 * @return
	 */
	public Object[] occurenceScope(ClonkProjectNature project) {
		final Set<Object> result = new LinkedHashSet<Object>();
		// first, add the script this declaration is declared in. Matches will most likely be found in there
		// so it helps to make it the first item to be searched
		if (parent instanceof Script)
			result.add(parent);
		// next, in case of a definition, add items including the definition, so matches in derived definitions
		// will be found next
		if (parent instanceof Definition) {
			// first, add the definition this
			final Definition def = (Definition)parent;
			final Index projectIndex = project.index();
			result.add(def);
			def.index().allDefinitions(new Sink<Definition>() {
				@Override
				public void receivedObject(Definition item) {
					result.add(item);
				}
				@Override
				public boolean filter(Definition item) {
					return item.doesInclude(projectIndex, def);
				}
			});
		}
		// then add all the scripts, because everything might potentially be accessed from everything
		for (final Index index : project.index().relevantIndexes())
			index.allScripts(new Sink<Script>() {
				@Override
				public void receivedObject(Script item) {
					result.add(item);
				}
			});
		return result.toArray();
	}

	/**
	 * Returns the resource this declaration is declared in
	 */
	@Override
	public IResource resource() {
		return parentDeclaration() != null ? parentDeclaration().resource() : null;
	}

	/**
	 * Returns the parent declaration this one is contained in
	 * @return
	 */
	public Declaration parentDeclaration() {
		return (Declaration)parent;
	}

	/**
	 * Return an {@link Iterable} to iterate over declarations accessible from this object that match the supplied bit mask
	 * @param contextIndex {@link Index} Context index. Required for correctly returning appended scripts if a project a completion proposal is invoked in contains scripts appending themselves to scripts from another project.
	 * @param mask a bit mask specifying what to include in the returned {@link Iterable}, formed by the static variables in this interface
	 * @return An iterable to iterate over sub declarations satifying the passed mask
	 */
	public List<? extends Declaration> subDeclarations(Index contextIndex, int mask) { return Collections.emptyList(); }
	public boolean seesSubDeclaration(Declaration subDeclaration) { return true; }

	public Function findFunction(String functionName) { return null; }
	public Declaration findDeclaration(FindDeclarationInfo info) { return null; }

	/**
	 * Adds a sub-declaration
	 * @param declaration
	 */
	public <T extends Declaration> T addDeclaration(T declaration) {
		throw new NotImplementedException();
	}

	/**
	 * Called after deserialization to restore transient references
	 * @param parent the parent
	 */
	public void postLoad(Declaration parent, Index index) {
		if (name != null)
			name = name.intern();
		postLoad(parent);
	}

	@Override
	public ASTNode[] subElements() {
		final List<? extends Declaration> sd = this.subDeclarations(this.index(), DeclMask.ALL);
		return sd.toArray(new ASTNode[sd.size()]);
	}

	/**
	 * Returns whether this declaration is global (functions are global when declared as "global" while variables are global when declared as "static")
	 * @return true if global, false if not
	 */
	public boolean isGlobal() { return false; }

	/**
	 * Used to filter declarations based on their name
	 * @param matcher The matcher, obtained from a {@link Pattern}, that will be {@link Matcher#reset(CharSequence)} with all the strings the user might want to filter for in order to refer to this declaration.
	 * @return whether this declaration should be filtered out (false) or not (true)
	 */
	@Override
	public boolean matchedBy(Matcher matcher) {
		if (name() != null && matcher.reset(name()).lookingAt())
			return true;
		final Structure tls = topLevelStructure();
		if (tls != null && tls != this && tls.matchedBy(matcher))
			return true;
		return false;
	}

	@Override
	public String nodeName() {
		return name();
	}

	/**
	 * Returns whether the supplied name looks like the name of a constant e.g begins with a prefix in caps followed by an underscore and a name
	 * @param name the string to check
	 * @return whether it does or not
	 */
	public static boolean looksLikeConstName(String name) {
		boolean underscore = false;
		for (int i = 0; i < name.length(); i++) {
			final char c = name.charAt(i);
			if (i > 0 && c == '_')
				if (!underscore)
					underscore = true;
				else
					return false;
			if (!underscore)
				if (Character.toUpperCase(c) != c)
					return false;
		}
		return underscore || name.equals(name.toUpperCase());
	}

	public boolean isEngineDeclaration() { return parentDeclaration() instanceof Engine; }
	public Engine engine() {
		final Declaration parent = parentDeclaration();
		return parent != null ? parent.engine() : null;
	}

	@Override
	public Index index() {
		if (parentDeclaration() != null)
			return parentDeclaration().index();
		else {
			final IResource res = resource();
			if (res != null) {
				final ClonkProjectNature nat = ClonkProjectNature.get(res);
				return nat != null ? nat.index() : null;
			}
			else
				return null;
		}
	}

	public StringTbl localStringTblMatchingLanguagePref() {
		final IResource res = resource();
		if (res == null)
			return null;
		final IContainer container = res instanceof IContainer ? (IContainer) res : res.getParent();
		final String pref = ClonkPreferences.languagePref();
		final IResource tblFile = Utilities.findMemberCaseInsensitively(container, "StringTbl"+pref+".txt"); //$NON-NLS-1$ //$NON-NLS-2$
		if (tblFile instanceof IFile)
			return (StringTbl) Structure.pinned(tblFile, true, false);
		return null;
	}

	public int absoluteExpressionsOffset() {return 0;}

	public DeclarationLocation[] declarationLocations() {
		return new DeclarationLocation[] {
			new DeclarationLocation(this, this, resource())
		};
	}

	/**
	 * Return a name that uniquely identifies the declaration in its script
	 * @return The unique name
	 */
	public String makeNameUniqueToParent() { return this.pathRelativeToIndexEntity(); }

	/**
	 * Return the {@link Declaration}'s path, which is a concatenation of its parent declaration's and its own name, separated by '.'
	 * Concatenating the path will stop at the earliest {@link IndexEntity} in the declaration hierarchy.
	 * @return The path
	 */
	public String pathRelativeToIndexEntity() {
		final StringBuilder builder = new StringBuilder();
		for (Declaration d = this; d != null; d = d.parentDeclaration())
			if (d instanceof IndexEntity)
				break;
			else {
				if (builder.length() > 0)
					builder.insert(0, '.');
				builder.insert(0, d.name());
			}
		return builder.toString();
	}

	/**
	 * Return a string representing this declaration sufficiently as to be recognizable by the user.
	 * @return Qualified name.
	 */
	public String qualifiedName() { return qualifiedName(parentDeclaration()); }

	/**
	 * Return a string identifying the declaration and the parent declaration.
	 * @param parent The parent declaration to use
	 * @return A string specifying both the parent and this declaration
	 */
	public String qualifiedName(Declaration parent) {
		if (parent != null)
			return String.format("%s::%s", parent.qualifiedName(), this.name());
		else
			return name();
	}

	@Override
	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
		return adapter.isInstance(this) ? this : null;
	}

	@Override
	public boolean equals(Object other) { return this == other; /* identity */ }

	public Typing typing() { return index() != null ? index().typing() : Typing.PARAMETERS_OPTIONALLY_TYPED; }

	@Override
	public String patternMatchingText() { return name(); }

	@Override
	public boolean equalAttributes(ASTNode other) {
		if (!super.equalAttributes(other))
			return false;
		final Declaration d = (Declaration)other;
		if (!d.name().equals(name()))
			return false;
		return true;
	}

	public int nameStart() { return start(); }

}
