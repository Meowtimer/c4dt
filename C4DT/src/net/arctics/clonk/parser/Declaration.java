package net.arctics.clonk.parser;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.IHasSubDeclarations;
import net.arctics.clonk.index.IIndexEntity;
import net.arctics.clonk.index.IPostLoadable;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.IndexEntity;
import net.arctics.clonk.index.Scenario;
import net.arctics.clonk.parser.c4script.DeclarationObtainmentContext;
import net.arctics.clonk.parser.c4script.FindDeclarationInfo;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.stringtbl.StringTbl;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.IHasRelatedResource;
import net.arctics.clonk.util.IHasUserDescription;
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
public abstract class Declaration implements Serializable, IHasRelatedResource, INode, IPostLoadable<Declaration, Index>, IHasSubDeclarations, IIndexEntity {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	
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
	protected transient Declaration parentDeclaration;
	
	/**
	 * result to be returned of occurenceScope if there is no scope
	 */
	//private static final Object[] EMPTY_SCOPE = new IResource[0];

	/**
	 * @return the name
	 */
	@Override
	public String name() {
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
	 * @param Set the location of the declaration in its declaring file.
	 */
	public void setLocation(SourceLocation location) {
		this.location = location;
	}
	
	/**
	 * @return Return the location of the declaration in its declaring file.
	 */
	public SourceLocation location() {
		return location;
	}
	
	/**
	 * Return the region to be selected when using editor navigation commands such as jump to definition. By default, this method returns the same location as {@link #location()}
	 * @return The region to select when using editor navigation commands
	 */
	public IRegion regionToSelect() {
		return location();
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
	public void setScript(Script script) {
		setParentDeclaration(script);
	}
	
	/**
	 * Return the first declaration in the parent chain that is of the specified type.
	 * @param type The type the parent declaration needs to be of
	 * @return A matching parent declaration or null
	 */
	@SuppressWarnings("unchecked")
	public final <T> T firstParentDeclarationOfType(Class<T> type) {
		T result = null;
		for (Declaration f = this; f != null; f = f.parentDeclaration)
			if (type.isAssignableFrom(f.getClass()))
				return (T) f;
		return result;
	}
	
	/**
	 * Same as {@link #firstParentDeclarationOfType(Class)}, but will return the last parent declaration matching the type instead of the first one.
	 * @param type
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public final <T> T topLevelParentDeclarationOfType(Class<T> type) {
		T result = null;
		for (Declaration f = this; f != null; f = f.parentDeclaration)
			if (type.isAssignableFrom(f.getClass()))
				result = (T) f;
		return result;
	}
	
	/**
	 * Returns the top-level {@link Structure} this declaration is declared in.
	 * @return the {@link Structure}
	 */
	public Structure topLevelStructure() {
		return topLevelParentDeclarationOfType(Structure.class);
	}
	
	/**
	 * Returns the {@link Script} this declaration is declared in.
	 * @return the {@link Script}
	 */
	public Script script() {
		return topLevelParentDeclarationOfType(Script.class);
	}
	
	/**
	 * Return the {@link Scenario} this declaration is declared in.
	 * @return The {@link Scenario}
	 */
	public Scenario scenario() {
		Object file = script() != null ? script().scriptStorage() : null;
		if (file instanceof IResource)
			for (IResource r = (IResource) file; r != null; r = r.getParent())
				if (r instanceof IContainer) {
					Scenario s = Scenario.get((IContainer) r);
					if (s != null)
						return s;
				}
		return null;
	}
	
	/**
	 * Sets the parent declaration of this declaration.
	 * @param field the new parent declaration
	 */
	public void setParentDeclaration(Declaration field) {
		this.parentDeclaration = field;
	}
	
	/**
	 * Returns a brief info string describing the declaration. Meant for UI presentation.
	 * @return The short info string.
	 */
	@Override
	public String infoText() {
		return name();
	}
	
	public String displayString() {
		return infoText();
	}
	
	/**
	 * Returns an array of all sub declarations meant to be displayed in the outline.
	 * @return
	 */
	public Object[] subDeclarationsForOutline() {
		return null;
	}

	/**
	 * Returns the latest version of this declaration, obtaining it by searching for a declaration with its name in its parent declaration
	 * @return The latest version of this declaration
	 */
	public Declaration latestVersion() {
		Declaration parent = parentDeclaration;
		if (parent != null)
			parent = parent.latestVersion();
		if (parent instanceof ILatestDeclarationVersionProvider) {
			Declaration latest = ((ILatestDeclarationVersionProvider)parent).latestVersionOf(this);
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
	public String toString() {
		return name != null ? name : getClass().getSimpleName();
	}
	
	/**
	 * Returns the objects this declaration might be referenced in (includes {@link Function}s, {@link IResource}s and other {@link Script}s)
	 * @param project
	 * @return
	 */
	public Object[] occurenceScope(ClonkProjectNature project) {
		//final Script script = script();
		return new Object[] {project.getProject()};
		/*
		if (isGlobal())
			return (project != null) ? new Object[] {project.getProject()} : EMPTY_SCOPE;
		final Set<Object> scope = new HashSet<Object>();
		scope.add(script);
		ClonkProjectNature nat = ClonkProjectNature.get(script.resource());
		nat.index().forAllRelevantIndexes(new r() {
			@Override
			public void run(Index index) {
				for (Script s : index.allScripts())
					if (s.usedScripts() != null && s.usedScripts().contains(script))
						scope.add(s);
			}
		});
		return scope.toArray();
		//return (project != null) ? new Object[] {project.getProject()} : EMPTY_SCOPE;
		*/
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
		return parentDeclaration;
	}
	
	protected static final Iterable<Declaration> NO_SUB_DECLARATIONS = ArrayUtil.iterable();
	
	/**
	 * Returns an Iterable for iterating over all sub declaration of this declaration.
	 * Might return null if there are none.
	 * @return The Iterable for iterating over sub declarations or null.
	 */
	@Override
	public Iterable<? extends Declaration> subDeclarations(Index contextIndex, int mask) {
		return NO_SUB_DECLARATIONS;
	}
	
	/**
	 * Return {@link #subDeclarations(Index, int)} with the contextIndex parameter set to {@link #index()}
	 * @param mask The mask, passed on to {@link #subDeclarations(Index, int)}
	 * @return Result of {@link #subDeclarations(Index, int)}
	 */
	public final Iterable<? extends Declaration> accessibleDeclarations(int mask) {
		return subDeclarations(index(), mask);
	}
	
	@Override
	public Function findFunction(String functionName) {
		return null;
	}
	
	@Override
	public Declaration findDeclaration(String name, FindDeclarationInfo info) {
		return null;
	}
	
	/**
	 * Adds a sub-declaration
	 * @param declaration
	 */
	public void addSubDeclaration(Declaration declaration) {
		System.out.println(String.format("Attempt to add sub declaration %s to %s", declaration, this));
	}
	
	/**
	 * Called after deserialization to restore transient references
	 * @param parent the parent
	 */
	@Override
	public void postLoad(Declaration parent, Index root) {
		if (name != null)
			name = name.intern();
		setParentDeclaration(parent);
		Iterable<? extends Declaration> subDecs = this.accessibleDeclarations(ALL);
		if (subDecs != null)
			for (Declaration d : subDecs)
				d.postLoad(this, root);
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
	 * @param matcher The matcher, obtained from a {@link Pattern}, that will be {@link Matcher#reset(CharSequence)} with all the strings the user might want to filter for in order to refer to this declaration.
	 * @return whether this declaration should be filtered out (false) or not (true)
	 */
	@Override
	public boolean matchedBy(Matcher matcher) {
		if (name() != null && matcher.reset(name()).lookingAt())
			return true;
		if (topLevelStructure() != null && topLevelStructure() != this && topLevelStructure().matchedBy(matcher))
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
			char c = name.charAt(i);
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

	public boolean isEngineDeclaration() {
		return parentDeclaration() instanceof Engine;
	}
	
	public Engine engine() {
		return parentDeclaration != null ? parentDeclaration.engine() : null; 
	}

	@Override
	public Index index() {
		if (parentDeclaration != null)
			return parentDeclaration.index();
		else {
			IResource res = resource();
			if (res != null) {
				ClonkProjectNature nat = ClonkProjectNature.get(res);
				return nat != null ? nat.index() : null;
			}
			else
				return null;
		}
	}
	
	/**
	 * Take internal state from other declaration and make it your own. This will mess up ownership relations so discard of the absorbed one
	 * @param declaration
	 */
	public void absorb(Declaration declaration) {
		if (this instanceof IHasUserDescription && declaration instanceof IHasUserDescription)
			((IHasUserDescription)this).setUserDescription(((IHasUserDescription)declaration).obtainUserDescription());
	}
	
	public void sourceCodeRepresentation(StringBuilder builder, Object cookie) {
		System.out.println("dunno");
	}
	
	public StringTbl localStringTblMatchingLanguagePref() {
		try {
			IResource res = resource();
			if (res == null)
				return null;
			IContainer container = res instanceof IContainer ? (IContainer) res : res.getParent();
			String pref = ClonkPreferences.languagePref();
			IResource tblFile = Utilities.findMemberCaseInsensitively(container, "StringTbl"+pref+".txt"); //$NON-NLS-1$ //$NON-NLS-2$
			if (tblFile instanceof IFile)
				return (StringTbl) Structure.pinned(tblFile, true, false);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	protected int absoluteExpressionsOffset() {return 0;}
	
	/**
	 * Return a {@link DeclarationObtainmentContext} describing the surrounding environment of this {@link Declaration}. 
	 * @return The context
	 */
	public DeclarationObtainmentContext declarationObtainmentContext() {
		return new DeclarationObtainmentContext() {
			
			@Override
			public IType queryTypeOfExpression(ExprElm exprElm, IType defaultType) {
				return null;
			}
			
			@Override
			public void reportProblems(Function function) {
			}

			@Override
			public Function currentFunction() {
				return Declaration.this instanceof Function ? (Function)Declaration.this : null;
			}
			
			@Override
			public Definition containerAsDefinition() {
				return script() instanceof Definition ? (Definition)script() : null;
			}
			
			@Override
			public Script containingScript() {
				return script();
			}

			@Override
			public void storeType(ExprElm exprElm, IType type) {
				// yeah right
			}

			@Override
			public Declaration currentDeclaration() {
				return Declaration.this;
			}

			@Override
			public SourceLocation absoluteSourceLocationFromExpr(ExprElm expression) {
				int bodyOffset = absoluteExpressionsOffset();
				return new SourceLocation(expression.start()+bodyOffset, expression.end()+bodyOffset);
			}

			@Override
			public Object[] arguments() {
				return new Object[0];
			}

			@Override
			public Function function() {
				return Declaration.this instanceof Function ? (Function)Declaration.this : null;
			}

			@Override
			public Script script() {
				return Declaration.this.script();
			}

			@Override
			public int codeFragmentOffset() {
				return 0;
			}

			@Override
			public void reportOriginForExpression(ExprElm expression, IRegion location, IFile file) {}

			@Override
			public Object valueForVariable(String varName) {
				return null;
			}
		};
	}

	public DeclarationLocation[] declarationLocations() {
		return new DeclarationLocation[] {
			new DeclarationLocation(this, location(), resource())
		};
	}
	
	/**
	 * Return a name that uniquely identifies the declaration in its script
	 * @return The unique name
	 */
	public String makeNameUniqueToParent() {
		int othersWithSameName = 0;
		int ownIndex = -1;
		for (Declaration d : parentDeclaration().accessibleDeclarations(ALL|OTHER))
			if (d == this) {
				ownIndex = othersWithSameName++;
				continue;
			}
			else if (d.name().equals(this.name()))
				othersWithSameName++;
		if (othersWithSameName == 1)
			return name();
		else
			return name() + ownIndex;
	}
	
	/**
	 * Return the {@link Declaration}'s path, which is a concatenation of its parent declaration's and its own name, separated by '.'
	 * Concatenating the path will stop at the earliest {@link IndexEntity} in the declaration hierarchy.
	 * @return The path
	 */
	public String pathRelativeToIndexEntity() {
		StringBuilder builder = new StringBuilder();
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
	 * Return a string identifying the declaration and the {@link Script} it's declared in.
	 * @return
	 */
	public String qualifiedName() {
		if (parentDeclaration() != null)
			return String.format("%s::%s", parentDeclaration().qualifiedName(), this.name());
		else
			return name();
	}
	
	/**
	 * Whether this Declaration is contained in the given one.
	 * @param parent The declaration to check for parentedness
	 * @return true or false
	 */
	public boolean containedIn(Declaration parent) {
		for (Declaration d = this.parentDeclaration(); d != null; d = d.parentDeclaration())
			if (d == parent)
				return true;
		return false;
	}
	
}
