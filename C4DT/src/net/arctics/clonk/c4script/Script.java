package net.arctics.clonk.c4script;

import static net.arctics.clonk.util.ArrayUtil.addAllSynchronized;
import static net.arctics.clonk.util.ArrayUtil.copyListOrReturnDefaultList;
import static net.arctics.clonk.util.ArrayUtil.filter;
import static net.arctics.clonk.util.ArrayUtil.filteredIterable;
import static net.arctics.clonk.util.ArrayUtil.iterable;
import static net.arctics.clonk.util.ArrayUtil.purgeNullEntries;
import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.defaulting;
import static net.arctics.clonk.util.Utilities.findMemberCaseInsensitively;
import static net.arctics.clonk.util.Utilities.pickNearest;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.arctics.clonk.Core;
import net.arctics.clonk.Core.IDocumentAction;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodePrinter;
import net.arctics.clonk.ast.DeclMask;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ast.IASTVisitor;
import net.arctics.clonk.ast.IEvaluationContext;
import net.arctics.clonk.ast.SourceLocation;
import net.arctics.clonk.ast.Structure;
import net.arctics.clonk.ast.TraversalContinuation;
import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.c4script.Directive.DirectiveType;
import net.arctics.clonk.c4script.Variable.Scope;
import net.arctics.clonk.c4script.ast.AccessVar;
import net.arctics.clonk.c4script.ast.CallDeclaration;
import net.arctics.clonk.c4script.ast.Comment;
import net.arctics.clonk.c4script.ast.FunctionBody;
import net.arctics.clonk.c4script.ast.evaluate.Constant;
import net.arctics.clonk.c4script.ast.evaluate.IVariable;
import net.arctics.clonk.c4script.effect.Effect;
import net.arctics.clonk.c4script.effect.EffectFunction;
import net.arctics.clonk.c4script.typing.IRefinedPrimitiveType;
import net.arctics.clonk.c4script.typing.IType;
import net.arctics.clonk.c4script.typing.PrimitiveType;
import net.arctics.clonk.c4script.typing.TypeAnnotation;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.ID;
import net.arctics.clonk.index.IIndexEntity;
import net.arctics.clonk.index.IVariableFactory;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.IndexEntity;
import net.arctics.clonk.index.Scenario;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.util.INode;
import net.arctics.clonk.util.IPredicate;
import net.arctics.clonk.util.ITreeNode;
import net.arctics.clonk.util.StreamUtil;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

/**
 * Base class for various objects that act as containers of stuff declared in scripts/ini files.
 * Subclasses include {@link Definition}, {@link SystemScript} etc.
 */
public abstract class Script extends IndexEntity implements ITreeNode, IRefinedPrimitiveType, IEvaluationContext, IHasIncludes<Script> {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	// will be written by #save
	protected transient List<Function> functions;
	protected transient List<Variable> variables;
	protected transient Map<String, Effect> effects;
	protected transient Map<String, ProplistDeclaration> proplistDeclarations;

	/**
	 * Typing judgments on variables and function return types.
	 * @author madeen
	 *
	 */
	public static final class Typings implements Serializable {
		private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
		/**
		 * Map mapping field variable name to type.
		 */
		public final Map<String, IType> variableTypes;
		/**
		 * Map mapping function name to return type.
		 */
		public final Map<String, Function.Typing> functionTypings;
		public Typings(
			Map<String, IType> variableTypes,
			Map<String, Function.Typing> functionTypings
		) {
			super();
			this.variableTypes = variableTypes;
			this.functionTypings = functionTypings;
		}
		public Function.Typing get(Function function) {
			return functionTypings != null ? functionTypings.get(function.name()) : null;
		}
		public IType get(Variable variable) {
			return variableTypes != null ? variableTypes.get(variable.name()) : null;
		}
		public IType get(ASTNode node) {
			final Function f = node.parent(Function.class);
			if (f == null)
				return null;
			final Function.Typing typing = get(f);
			return typing != null ? typing.nodeTypes[node.localIdentifier()] : null;
		}
		public void update(Map<String, IType> variableTypes, Map<String, Function.Typing> functionTypings) {
			synchronized (this) {
				for (final Entry<String, IType> x : variableTypes.entrySet())
					this.variableTypes.put(x.getKey(), x.getValue());
				for (final Entry<String, Function.Typing> x : functionTypings.entrySet())
					this.functionTypings.put(x.getKey(), x.getValue());
			}
		}
	}
	private transient Typings typings;

	// serialized directly
	/** set of scripts this script is using functions and/or static variables from */
	private Set<Script> usedScripts;
	protected List<Directive> directives;

	@SuppressWarnings("serial")
	private static final class CachedIncludes extends HashSet<Script> {
		private final int indexHash;
		private final int originHash;
		private final int options;
		boolean matches(Index index, Script origin, int options) {
			final int indexHash = index != null ? index.hashCode() : 0;
			final int originHash = origin != null ? origin.hashCode() : 0;
			return this.indexHash == indexHash && this.originHash == originHash && this.options == options;
		}
		public CachedIncludes(Script script, Index index, Script origin, int options) {
			this.indexHash = index != null ? index.hashCode() : 0;
			this.originHash = origin != null ? origin.hashCode() : 0;
			this.options = options;
			script.gatherIncludes(index, origin, this, options);
			remove(script);
		}
	}

	// cache all the things
	private transient Map<String, Function> cachedFunctionMap;
	private transient Map<String, Variable> cachedVariableMap;
	private transient CachedIncludes includes;
	private transient Scenario scenario;
	private transient Map<String, List<CallDeclaration>> callMap = new HashMap<>();
	private transient Map<String, List<AccessVar>> varReferencesMap = new HashMap<>();

	private Set<String> dictionary;
	private List<TypeAnnotation> typeAnnotations;

	private static final Typings NO_TYPINGS = new Typings(
		Collections.<String, IType>emptyMap(),
		Collections.<String, Function.Typing>emptyMap()
	);

	public Typings typings() { return defaulting(typings, NO_TYPINGS); }
	public void setTypings(Typings typings) { this.typings = typings; }

	public List<TypeAnnotation> typeAnnotations() { return typeAnnotations; }
	public void setTypeAnnotations(List<TypeAnnotation> typeAnnotations) {
		this.typeAnnotations = typeAnnotations;
		if (this.typeAnnotations != null)
			for (final TypeAnnotation a : typeAnnotations)
				if (a.parent() == null)
					a.setParent(this);
	}

	public Map<String, List<CallDeclaration>> callMap() { return defaulting(callMap, Collections.<String, List<CallDeclaration>>emptyMap()); }
	public Map<String, List<AccessVar>> varReferences() { return defaulting(varReferencesMap, Collections.<String, List<AccessVar>>emptyMap()); }

	/**
	 * The script's dictionary contains names of variables and functions defined in it.
	 * It can be queried before {@link #requireLoaded()} was called, enabling one to look before-hand whether the script contains
	 * a declaration with some name.
	 * @return The dictionary
	 */
	public Set<String> dictionary() { return dictionary; }

	/**
	 * Return list of scripts used by this one. A script is considered to be using another one if it calls a global function or accesses a static variable defined in the other script.
	 * Kept and managed to make reparsing a script using global declarations from some other script work without requiring a reload of all scripts in the index.
	 * @return The list of used scripts
	 */
	public Collection<? extends Script> usedScripts() {
		return copyListOrReturnDefaultList(usedScripts, Collections.<Script>emptyList());
	}

	protected Script(Index index) { super(index); }

	protected static class SaveState implements Serializable {
		private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
		public Map<String, Effect> effects;
		public List<Function> functions;
		public List<Variable> variables;
		public Set<Script> used;
		public Typings typings;
		public Map<String, ProplistDeclaration> proplistDeclarations;
		public void initialize(
			Map<String, Effect> effects,
			List<Function> functions,
			List<Variable> variables,
			Set<Script> used,
			Typings typings,
			Map<String, ProplistDeclaration> proplistDeclarations
		) {
			this.effects = effects;
			this.functions = functions;
			this.variables = variables;
			this.used = used;
			this.typings = typings;
			this.proplistDeclarations = proplistDeclarations;
		}
	}

	@Override
	public void save(ObjectOutputStream stream) throws IOException {
		super.save(stream);
		final SaveState state = makeSaveState();
		state.initialize(
			effects,
			functions,
			variables,
			usedScripts,
			typings,
			proplistDeclarations
		);
		try {
			stream.writeObject(state);
		} catch (final IllegalStateException e) {
			System.out.println(String.format("Problems saving %s", name()));
			e.printStackTrace();
		}
		populateDictionary();
	}

	public SaveState makeSaveState() { return new SaveState(); }

	@Override
	public void load(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		super.load(stream);
		loadIncludes();
		extractSaveState((SaveState)stream.readObject());
		loadUsedScripts();
	}

	protected void extractSaveState(final SaveState state) {
		effects     = state.effects;
		functions   = state.functions;
		variables   = state.variables;
		usedScripts = state.used;
		typings     = state.typings;
		proplistDeclarations = state.proplistDeclarations;

		purgeNullEntries(functions, variables, usedScripts);
	}

	private void loadUsedScripts() {
		// also load scripts this script uses global declarations from
		// so they will be present when the script gets parsed
		try {
			if (usedScripts != null)
				for (final Iterator<Script> it = usedScripts.iterator(); it.hasNext();) {
					final Script s = it.next();
					if (s != null)
						s.requireLoaded();
					else
						it.remove();
				}
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void postLoad(Declaration parent, Index root) {
		loadIncludes();
		loadUsedScripts();
		super.postLoad(parent, root);
		deriveInformation();
		findScenario();
		for (final Function f : functions())
			f.findInherited();
	}

	private void loadIncludes() {
		if (directives != null && index != null)
			for (final Directive d : directives)
				switch (d.type()) {
				case APPENDTO: case INCLUDE:
					final ID id = d.contentAsID();
					if (id != null) {
						final List<? extends Definition> defs = index.definitionsWithID(id);
						if (defs != null)
							for (final Definition def : defs)
								def.requireLoaded();
					}
					break;
				default:
					break;
				}
	}

	private static int nextCamelBack(String s, int offset) {
		for (++offset; offset < s.length() && !Character.isUpperCase(s.charAt(offset)); offset++);
		return offset;
	}

	public void detectEffects() {
		final List<EffectFunction> allEffectFunctions = new ArrayList<EffectFunction>(10);
		for (final EffectFunction f : functions(EffectFunction.class))
			allEffectFunctions.add(f);
		while (allEffectFunctions.size() > 0) {
			final String s = allEffectFunctions.get(0).name().substring(EffectFunction.FUNCTION_NAME_PREFIX.length());
			List<EffectFunction> effectCandidates = null;
			String effectName = null;
			for (int i = nextCamelBack(s, 0); i < s.length(); i = nextCamelBack(s, i)) {
				final String sub = s.substring(0, i);
				final List<EffectFunction> matching = filter(allEffectFunctions, new IPredicate<EffectFunction>() {
					@Override
					public boolean test(EffectFunction item) {
						try {
							return item.name().substring(EffectFunction.FUNCTION_NAME_PREFIX.length(), EffectFunction.FUNCTION_NAME_PREFIX.length()+sub.length()).equals(sub);
						} catch (final StringIndexOutOfBoundsException e) {
							return false;
						}
					}
				});
				if (matching.size() == 0)
					break;
				effectName = sub;
				effectCandidates = matching;
			};
			if (effectName != null) {
				allEffectFunctions.removeAll(effectCandidates);
				if (effects == null)
					effects = new HashMap<String, Effect>();
				final Effect existing = effects.get(effectName);
				if (existing == null) {
					final Effect effect = new Effect(effectName, effectCandidates);
					effect.setParent(this);
					effects.put(effectName, effect);
				}
			}
		}
	}

	protected void populateDictionary(List<Script> conglomerate) {
		if (dictionary != null)
			dictionary.clear();
		else
			dictionary = new HashSet<String>();
		for (final Script s : conglomerate)
			for (final Declaration d : s.subDeclarations(index(), DeclMask.ALL))
				dictionary.add(d.name());
	}

	protected final void populateDictionary() { populateDictionary(conglomerate()); }

	/**
	 * Returns the strict level of the script
	 * @return the #strict level set for this script or the default level supplied by the engine configuration
	 */
	public int strictLevel() {
		requireLoaded();
		long level = engine() != null ? engine().settings().strictDefaultLevel : -1;
		for (final Directive d : this.directives())
			if (d.type() == DirectiveType.STRICT)
				try {
					level = Math.max(level, Integer.parseInt(d.contents()));
				}
				catch (final NumberFormatException e) {
					if (level < 1)
						level = 1;
				}
		return (int)level;
	}

	/**
	 * Returns \#include and \#appendto directives
	 * @return The directives
	 */
	public Directive[] includeDirectives() {
		requireLoaded();
		final List<Directive> result = new ArrayList<Directive>();
		for (final Directive d : directives())
			if (d.type() == DirectiveType.INCLUDE || d.type() == DirectiveType.APPENDTO)
				result.add(d);
		return result.toArray(new Directive[result.size()]);
	}

	/**
	 * Tries to gather all of the script's includes (including appendtos in the case of object scripts).
	 * Attention: The passed collection needs to reject duplicates, meaning its {@link Collection#add(Object)} method needs to return false when an attempt is being
	 * made to add an already-existing item. {@link Set} classes are good candidates.
	 * @param set A collection to be filled with the includes gathered.
	 * @param contextIndex The project index to search for includes in.
	 */
	@Override
	public boolean gatherIncludes(Index contextIndex, final Script origin, Collection<Script> set, int options) {
		if (!set.add(this))
			return false;
		if (directives != null) {
			List<Directive> directivesCopy;
			synchronized(directives) {
				directivesCopy = new ArrayList<Directive>(directives);
			}
			for (final Directive d : directivesCopy) {
				if (d == null)
					continue;
				switch (d.type()) {
				case INCLUDE:
				case APPENDTO:
					final ID id = d.contentAsID();
					for (final Index in : contextIndex.relevantIndexes()) {
						final List<? extends Definition> defs = in.definitionsWithID(id);
						final Definition pick = defs == null
							? null
							: (origin == null || origin.resource() == null)
							? defs.get(0)
							: pickNearest(defs, origin.resource(), null);
						if (pick != null)
							if ((options & GatherIncludesOptions.Recursive) == 0)
								set.add(pick);
							else {
								if (d.type() == DirectiveType.INCLUDE)
									options &= ~GatherIncludesOptions.NoAppendages;
								pick.gatherIncludes(contextIndex, origin, set, options);
							}
					}
					break;
				default:
					break;
				}
			}
		}
		return true;
	}

	/**
	 * Return {@link #includes(Index, boolean)}({@link #index()}, {@code recursive});
	 * @param options Whether the returned collection also contains includes of the includes.
	 * @return The includes
	 */
	public Collection<Script> includes(int options) {
		if (index() == null)
			return Collections.emptySet();
		else
			return includes(index(), this, options);
	}

	@Override
	public Collection<Script> includes(Index index, Script origin, int options) {
		return includes != null && index == index() && origin == this && options == GatherIncludesOptions.Recursive
			? includes : new CachedIncludes(this, index, origin, options);
	}

	public boolean directlyIncludes(Definition other) {
		for (final Directive d : directives())
			if (d.refersTo(other))
				return true;
		return false;
	}

	/**
	 * Returns an include directive that includes a specific {@link Definition}'s script
	 * @param definition The {@link Definition} to return a corresponding {@link Directive} for
	 * @return The {@link Directive} or null if no matching directive exists in this script.
	 */
	public Directive directiveIncludingDefinition(Definition definition) {
		requireLoaded();
		for (final Directive d : includeDirectives())
			if ((d.type() == DirectiveType.INCLUDE || d.type() == DirectiveType.APPENDTO) && nearestDefinitionWithId(d.contentAsID()) == definition)
				return d;
		return null;
	}

	/**
	 * Finds a declaration in this script or an included one
	 * @param name Name of declaration to find.
	 * @return The declaration or null if not found
	 */
	@Override
	public Declaration findDeclaration(String name) {
		return findDeclaration(new FindDeclarationInfo(name, index()));
	}

	@Override
	public Declaration findDeclaration(String declarationName, Class<? extends Declaration> declarationClass) {
		final FindDeclarationInfo info = new FindDeclarationInfo(declarationName, index());
		info.declarationClass = declarationClass;
		return findDeclaration(info);
	}

	@Override
	public Declaration findLocalDeclaration(String declarationName, Class<? extends Declaration> declarationClass) {
		requireLoaded();
		if (declarationClass.isAssignableFrom(Effect.class)) {
			final Effect e = effects().get(declarationName);
			if (e != null)
				return e;
		}
		if (declarationClass.isAssignableFrom(ProplistDeclaration.class)) {
			final ProplistDeclaration dec = proplistDeclarations().get(declarationName);
			if (dec != null)
				return dec;
		}
		if (declarationClass.isAssignableFrom(Variable.class))
			for (final Variable v : variables())
				if (v.name().equals(declarationName))
					return v;
		if (declarationClass.isAssignableFrom(Function.class))
			for (final Function f : functions())
				if (f.name().equals(declarationName))
					return f;
		return null;
	}

	/**
	 * Returns a declaration representing this script if the name matches the name of the script
	 * @param name The name
	 * @param info Additional info
	 * @return the declaration or null if there is no match
	 */
	protected Declaration representingDeclaration(String name, FindDeclarationInfo info) {
		return null;
	}

	@Override
	public List<Declaration> subDeclarations(Index contextIndex, int mask) {
		requireLoaded();
		final ArrayList<Declaration> decs = new ArrayList<Declaration>();
		List<Variable> vars = null;
		if ((mask & DeclMask.VARIABLES|DeclMask.STATIC_VARIABLES) != 0)
			if (variables != null)
				synchronized (variables) {
					vars = new ArrayList<>(variables);
				}
		if ((mask & DeclMask.DIRECTIVES) != 0)
			addAllSynchronized(directives, decs, null);
		if (vars != null) {
			if ((mask & DeclMask.VARIABLES) != 0)
				for (final Variable v : vars)
					if (!v.isGlobal())
						decs.add(v);
			if ((mask & DeclMask.STATIC_VARIABLES) != 0)
				for (final Variable v : vars)
					if (v.isGlobal())
						decs.add(v);
		}
		if ((mask & DeclMask.FUNCTIONS) != 0)
			addAllSynchronized(functions, decs, null);
		if ((mask & DeclMask.EFFECTS) != 0 && effects != null)
			addAllSynchronized(effects.values(), decs, effects);
		if ((mask & DeclMask.PROPLISTS) != 0 && proplistDeclarations != null)
			addAllSynchronized(proplistDeclarations.values(), decs, proplistDeclarations);
		return decs;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Declaration> T latestVersionOf(T from) {
		final int mask =
			from instanceof Variable ? DeclMask.VARIABLES :
			from instanceof Function ? DeclMask.FUNCTIONS :
			from instanceof Directive ? DeclMask.DIRECTIVES : 0;
		T candidate = null;
		boolean locationMatch = false;
		for (final Declaration d : subDeclarations(this.index(), mask)) {
			if (d == from)
				return (T)d;
			if (d.name().equals(from.name())) {
				final boolean newLocationMatch = d.sameLocation(from);
				if (candidate == null || (newLocationMatch && !locationMatch)) {
					candidate = (T)d;
					locationMatch = newLocationMatch;
				}
			}
		}
		return candidate;
	};

	private Declaration findUsingCache(FindDeclarationInfo info) {
		final Class<? extends Declaration> decClass = info.declarationClass;
		// prefer using the cache
		requireLoaded();
		if (cachedVariableMap != null && (decClass == null || decClass == Variable.class)) {
			final Declaration d = cachedVariableMap.get(info.name);
			if (d != null)
				return d;
		}
		if (cachedFunctionMap != null && (decClass == null || decClass == Function.class)) {
			final Declaration d = cachedFunctionMap.get(info.name);
			if (d != null)
				return d;
		}
		return null;
	}

	/**
	 * Finds a declaration with the given name using information from the helper object
	 * @param info Information container describing the search
	 * @return the declaration or <tt>null</tt> if not found
	 */
	@Override
	public Declaration findDeclaration(FindDeclarationInfo info) {

		// prevent infinite recursion
		if (!info.startSearchingIn(this))
			return null;

		final Class<? extends Declaration> decClass = info.declarationClass;
		final String name = info.name;

		if (info.recursion == 0 && info.contextFunction != null &&
			(decClass == Declaration.class || decClass == null || decClass == Variable.class)) {
			final Declaration v = info.contextFunction.findVariable(name);
			if (v != null)
				return v;
		}

		final boolean knows = dictionary() == null || dictionary().contains(name);
		if (knows) {
			// prefer using the cache
			requireLoaded();
			final Declaration cacheFind = findUsingCache(info);
			if (cacheFind != null)
				return cacheFind;
		}

		// this object?
		final Declaration thisDec = representingDeclaration(name, info);
		if (thisDec != null)
			return thisDec;

		info.recursion++;
		{
			for (final Script o : includes(info.index, info.searchOrigin(), 0)) {
				final Declaration result = o.findDeclaration(info);
				if (result != null)
					return result;
			}
		}
		info.recursion--;

		// finally look if it's something global
		if (info.recursion == 0 && info.index != null) {
			info.recursion++;
			final Declaration f = findGlobalDeclaration(info);
			info.recursion--;
			if (f != null && (info.declarationClass == null || info.declarationClass.isAssignableFrom(f.getClass())))
				return f;
		}
		return null;
	}

	private Declaration findGlobalDeclaration(FindDeclarationInfo info) {
		// prefer declarations from scripts that were previously determined to be the providers of global declarations
		// this will also probably and rightly lead to those scripts being fully loaded from their index file.
		if (usedScripts != null)
			for (final Script s : usedScripts()) {
				final Declaration f = s.findDeclaration(info);
				if (f != null && f.isGlobal())
					return f;
			}

		if (info.findGlobalVariables && engine().acceptsId(info.name)) {
			final Declaration d  = findDefinition(info);
			if (d != null)
				return d;
		}

		// global stuff defined in relevant projects
		for (final Index index : info.index.relevantIndexes()) {
			final Declaration f = index.findGlobalDeclaration(info.name, resource());
			if (f != null && (info.findGlobalVariables || !(f instanceof Variable)))
				return f;
		}

		// engine function
		return index().engine().findDeclaration(info);
	}

	private Declaration findDefinition(FindDeclarationInfo info) {
		final Definition d = info.index.definitionNearestTo(resource(), ID.get(info.name));
		if (d != null)
			if (info.declarationClass == Variable.class)
				return d.proxyVar();
			else
				return d;
		if (info.name.equals(Scenario.PROPLIST_NAME)) {
			Scenario scenario = Scenario.nearestScenario(this.resource());
			if (scenario == null)
				scenario = engine().templateScenario();
			if (scenario.propList() != null)
				return scenario.propList();
		}
		return null;
	}

	/**
	 * Add declaration to this script. Proplists will be named automatically when added without prior name.
	 * @param declaration Declaration to add. Can be a {@link Variable}, a {@link Function}, a {@link Directive} or a {@link ProplistDeclaration}
	 */
	@Override
	public <T extends Declaration> T addDeclaration(T declaration) {
		requireLoaded();
		declaration.setParent(this);
		if (declaration instanceof Function)
			synchronized (this) {
				if (functions == null)
					functions = new ArrayList<Function>(5);
				functions.add((Function)declaration);
				// function added after generating cache? put it
				if (cachedFunctionMap != null)
					cachedFunctionMap.put(declaration.name(), (Function) declaration);
			}
		else if (declaration instanceof Variable)
			synchronized (this) {
				if (variables == null)
					variables = new ArrayList<Variable>(5);
				variables.add((Variable)declaration);
				// variable added after generating cache? put it
				if (cachedVariableMap != null)
					cachedVariableMap.put(declaration.name(), (Variable) declaration);
			}
		else if (declaration instanceof Directive)
			synchronized (this) {
				if (directives == null)
					directives = new ArrayList<Directive>(5);
				directives.add((Directive)declaration);
			}
		else if (declaration instanceof ProplistDeclaration)
			synchronized (this) {
				if (proplistDeclarations == null)
					proplistDeclarations = new HashMap<>();
				if (declaration.name() == null)
					declaration.setName("proplist"+proplistDeclarations.size());
				proplistDeclarations.put(declaration.name(), (ProplistDeclaration) declaration);
			}
		else
			throw new IllegalArgumentException("declaration");
		if (dictionary != null)
			synchronized (dictionary) { dictionary.add(declaration.name()); }
		return declaration;
	}

	public void removeDeclaration(Declaration declaration) {
		requireLoaded();
		if (declaration.script() != this)
			declaration.setParent(this);
		if (declaration instanceof Function) {
			if (functions != null) synchronized (functions) {
				functions.remove(declaration);
			}
		}
		else if (declaration instanceof Variable) {
			if (variables != null) synchronized (functions) {
				variables.remove(declaration);
			}
		}
		else if (declaration instanceof Directive)
			if (directives != null) synchronized (directives) {
				directives.remove(declaration);
			}
	}

	public synchronized void clearDeclarations() {
		loaded = true;
		usedScripts = null;
		directives = null;
		functions = null;
		variables = null;
		cachedFunctionMap = null;
		cachedVariableMap = null;
		effects = null;
		typeAnnotations = null;
	}

	public abstract IStorage source();

	@Override
	public IFile file() {
		final IStorage storage = source();
		return storage instanceof IFile ? (IFile)storage : null;
	}

	@Override
	public Script script() { return this; }
	@Override
	public Structure topLevelStructure() { return this; }
	@Override
	public IResource resource() { return null; }

	public Function findFunction(FindDeclarationInfo info) {
		info.resetState();
		info.declarationClass = Function.class;
		return (Function) findDeclaration(info);
	}

	@Override
	public Function findFunction(String functionName) {
		final FindDeclarationInfo info = new FindDeclarationInfo(functionName, index());
		return findFunction(info);
	}

	public Variable findVariable(String varName) {
		final FindDeclarationInfo info = new FindDeclarationInfo(varName, index());
		return findVariable(info);
	}

	public Variable findVariable(FindDeclarationInfo info) {
		info.resetState();
		info.declarationClass = Variable.class;
		return (Variable) findDeclaration(info);
	}

	public Function funcAt(int offset) {
		return funcAt(new Region(offset, 1));
	}

	public Function funcAt(IRegion region) {
		requireLoaded();
		for (final Function f : functions()) {
			final int fStart = f.bodyLocation().getOffset();
			final int fEnd   = f.bodyLocation().getOffset()+f.bodyLocation().getLength();
			final int rStart = region.getOffset();
			final int rEnd   = region.getOffset()+region.getLength();
			if (rStart <= fStart && rEnd >= fEnd || rStart >= fStart && rStart <= fEnd || rEnd >= fEnd && rEnd <= fEnd)
				return f;
		}
		return null;
	}

	public Variable variableInitializedAt(IRegion region) {
		requireLoaded();
		for (final Variable v : variables()) {
			final ASTNode initialization = v.initializationExpression();
			if (initialization != null) {
				final Function owningFunc = as(initialization.owner(), Function.class);
				final SourceLocation loc = owningFunc != null ? owningFunc.bodyLocation().add(initialization) : initialization;
				if (loc.containsOffset(region.getOffset()))
					return v;
			}
		}
		return null;
	}

	/**
	 * Return whether this script includes another one.
	 * @param other The other script
	 * @return True if this script includes the other one, false if not.
	 */
	@Override
	public boolean doesInclude(Index contextIndex, Script other) {
		requireLoaded();
		if (other == this)
			return true;
		final Iterable<Script> incs = this.includes(0);
		for (final Script o : incs)
			if (o == other)
				return true;
		return false;
	}

	public Variable findLocalVariable(String name, boolean searchIncludes) {
		return findLocalVariable(name, searchIncludes ? new HashSet<Script>() : null);
	}

	public Function findLocalFunction(String name, boolean includeIncludes) {
		return findLocalFunction(name, includeIncludes ? new HashSet<Script>() : null);
	}

	private Function findLocalFunction(String name, HashSet<Script> catcher) {
		requireLoaded();
		if (catcher != null && !catcher.add(this))
			return null;
		for (final Function func: functions())
			if (func.name().equals(name))
				return func;
		if (catcher != null)
			for (final Script script : filteredIterable(includes(0), Script.class)) {
				final Function func = script.findLocalFunction(name, catcher);
				if (func != null)
					return func;
			}
		return null;
	}

	private Variable findLocalVariable(String name, HashSet<Script> catcher) {
		requireLoaded();
		if (catcher != null && !catcher.add(this))
			return null;
		for (final Variable var : variables())
			if (var.name().equals(name))
				return var;
		if (catcher != null)
			for (final Script script : filteredIterable(includes(GatherIncludesOptions.NoAppendages), Script.class)) {
				final Variable var = script.findLocalVariable(name, catcher);
				if (var != null)
					return var;
			}
		return null;
	}

	public boolean removeDuplicateVariables() {
		requireLoaded();
		final Map<String, Variable> variableMap = new HashMap<String, Variable>();
		final Collection<Variable> toBeRemoved = new LinkedList<Variable>();
		for (final Variable v : variables()) {
			final Variable inHash = variableMap.get(v.name());
			if (inHash != null)
				toBeRemoved.add(v);
			else
				variableMap.put(v.name(), v);
		}
		for (final Variable v : toBeRemoved)
			variables.remove(v);
		return toBeRemoved.size() > 0;
	}

	/**
	 * Return the list of functions defined in this script.
	 * @return The functions list
	 */
	public List<? extends Function> functions() {
		requireLoaded();
		return copyListOrReturnDefaultList(functions, Collections.<Function>emptyList());
	}

	/**
	 * Iterate over all functions of a certain function class.
	 * @param cls The {@link Function} class
	 * @return The {@link Iterable}
	 */
	public <T extends Function> Iterable<T> functions(Class<T> cls) {
		requireLoaded();
		return filteredIterable(functions(), cls);
	}

	/**
	 * Return the list of variables defined in this script.
	 * @return The variables list
	 */
	public List<? extends Variable> variables() {
		requireLoaded();
		return copyListOrReturnDefaultList(variables, Collections.<Variable>emptyList());
	}

	/**
	 * Return the list of directives defined in this script.
	 * @return The directives list
	 */
	public List<? extends Directive> directives() {
		requireLoaded();
		return directives != null ? directives : Collections.<Directive>emptyList();
	}

	/**
	 * Return a map mapping effect name to {@link Effect} object
	 * @return The map
	 */
	public Map<String, Effect> effects() {
		requireLoaded();
		return effects != null ? effects : Collections.<String, Effect>emptyMap();
	}

	public Map<String, ProplistDeclaration> proplistDeclarations() {
		requireLoaded();
		return proplistDeclarations != null ? proplistDeclarations : Collections.<String, ProplistDeclaration>emptyMap();
	}

	public Definition nearestDefinitionWithId(ID id) {
		final Index index = index();
		if (index != null)
			return index.definitionNearestTo(resource(), id);
		return null;
	}

	/**
	 * Returns a list containing all scripts that are included by this script plus the script itself.
	 * @return The list
	 */
	public List<Script> conglomerate() {
		requireLoaded();
		@SuppressWarnings("serial")
		final List<Script> s = new ArrayList<Script>(10) {
			@Override
			public boolean add(Script e) {
				if (contains(e))
					return false;
				else
					return super.add(e);
			}
		};
		gatherIncludes(index(), this, s, GatherIncludesOptions.Recursive);
		return s;
	}

	@Override
	public INode[] subDeclarationsForOutline() {
		requireLoaded();
		final List<Object> all = new LinkedList<Object>();
		for (final Script c : conglomerate())
			for (final Declaration sd : c.subDeclarations(index(), DeclMask.FUNCTIONS|DeclMask.VARIABLES|(c==this?DeclMask.DIRECTIVES:0))) {
				if (sd instanceof SynthesizedFunction)
					continue;
				if (sd instanceof Function && !seesFunction(((Function)sd)))
					continue;
				all.add(sd);
			}
		return all.toArray(new INode[all.size()]);
	}

	private String sourceComment;
	public String sourceComment() { return sourceComment; }
	public void setSourceComment(String s) { sourceComment = s; }

	@Override
	public String infoText(IIndexEntity context) {
		if (sourceComment != null)
			return sourceComment;
		final Object f = source();
		if (f instanceof IFile) {
			final IResource infoFile = Utilities.findMemberCaseInsensitively(((IFile)f).getParent(), "Desc"+ClonkPreferences.languagePref()+".txt"); //$NON-NLS-1$ //$NON-NLS-2$
			if (infoFile instanceof IFile)
				try {
					return StreamUtil.stringFromFileDocument((IFile) infoFile);
				} catch (final Exception e) {
					e.printStackTrace();
					return super.infoText(context);
				}
		}
		return "";
	}

	@Override
	public ITreeNode parentNode() {
		return parentDeclaration() instanceof ITreeNode ? (ITreeNode)parentDeclaration() : null;
	}

	@Override
	public boolean subNodeOf(ITreeNode node) {
		return ITreeNode.Default.subNodeOf(this, node);
	}

	@Override
	public IPath path() {
		return ITreeNode.Default.path(this);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Collection<? extends INode> childCollection() {
		return Utilities.collectionFromArray(LinkedList.class, subDeclarationsForOutline());
	}

	@Override
	public void addChild(ITreeNode node) {
		requireLoaded();
		if (node instanceof Declaration)
			addDeclaration((Declaration)node);
	}

	public void addUsedScript(Script script) {
		if (script == null)
			// this does happen, for example when adding the script of some variable read from PlayerControls.txt
			// which is null
			return;
		if (script == this || script instanceof Engine)
			return;
		requireLoaded();
		if (usedScripts == null)
			usedScripts = new HashSet<Script>();
		synchronized (usedScripts) {
			usedScripts.add(script);
		}
	}

	/**
	 * notification sent by the index when a script is removed
	 */
	public void scriptRemovedFromIndex(Script script) {
		if (usedScripts != null)
			synchronized (usedScripts) {
				usedScripts.remove(script);
			}
	}

	@Override
	public Engine engine() {
		final Index index = index();
		return index != null ? index.engine() : null;
	}

	public static Script get(IResource resource, boolean onlyForScriptFile) {
		final ClonkProjectNature nat = ClonkProjectNature.get(resource);
		if (nat != null)
			nat.index();
		Script script;
		if (resource == null)
			return null;
		script = SystemScript.pinned(resource, false);
		if (script == null)
			script = Definition.definitionCorrespondingToFolder(resource.getParent());
		// there can only be one script oO (not ScriptDE or something)
		if (onlyForScriptFile && (script == null || script.source() == null || !script.source().equals(resource)))
			return null;
		return script;
	}

	@Override
	public String typeName(boolean special) {
		return special ? name() : PrimitiveType.OBJECT.typeName(false);
	}

	@Override
	public Iterator<IType> iterator() {
		return iterable(new IType[] {PrimitiveType.OBJECT, this}).iterator();
	}

	@Override
	public IType simpleType() { return PrimitiveType.OBJECT; }
	@Override
	public Function function() { return null; }
	@Override
	public void reportOriginForExpression(ASTNode expression, IRegion location, IFile file) { /* cool */ }
	@Override
	public Object[] arguments() { return new Object[0]; }
	@Override
	public IVariable variable(AccessVar access, Object obj) {
		if (access.predecessor() == null) {
			final Variable v = findLocalVariable(access.name(), true);
			if (v != null)
				return new Constant(v);
			for (final Script s : includes(0)) {
				final IVariable vv = s.variable(access, obj);
				if (vv != null)
					return vv;
			}
		}
		return null;
	}
	@Override
	public int codeFragmentOffset() { return 0; }
	@Override
	public Object self() { return null; }

	/**
	 * Return whether this function is accessible from this script - that is, it belongs to this script or one included by it and is
	 * not overridden.
	 * @param function
	 * @return
	 */
	public final boolean seesFunction(Function function) {
		if (cachedFunctionMap != null) {
			final Function mine = cachedFunctionMap.get(function.name());
			return mine == null || mine.latestVersion() == function;
		} else
			return true;
	}

	public Function override(Function function) {
		if (cachedFunctionMap != null) {
			final Function ovr = cachedFunctionMap.get(function);
			if (ovr != null)
				return ovr;
		}
		return function;
	}

	@Override
	public boolean seesSubDeclaration(Declaration subDeclaration) {
		if (subDeclaration instanceof Function)
			return seesFunction((Function)subDeclaration);
		else
			return true;
	}

	/**
	 * Populate various helper structures and caches with information derived from the truth
	 * read from the source. These things include:
	 * <ol>
	 * 	<li>{@link #dictionary()} is populated with names of declarations contained in this script.</li>
	 *  <li>An internal cache for finding declarations is generated so that {@link #findDeclaration(String, FindDeclarationInfo)} does faster lookup.</li>
	 *  <li>{@link #varReferences()} and {@link #callMap()} are populated with references to respective AST nodes ({@link AccessVar} and {@link CallDeclaration}).</li>
	 *  <li>{@link Effect} objects are created by applying some camel-case finding strategy on functions named Fx.* ({@link #effects()})
	 * </ol>
	 */
	public synchronized void deriveInformation() {
		includes = new CachedIncludes(this, index(), this, GatherIncludesOptions.Recursive);
		if (file() != null)
			pinTo(file());
		findScenario();
		detectEffects();
		final List<Script> conglo = this.conglomerate();
		Collections.reverse(conglo);
		populateDictionary(conglo);
		generateFindDeclarationCache(conglo);
		generateNodeMaps();
	}

	static class NodeMapsPopulator implements IASTVisitor<Script> {
		final Map<String, List<CallDeclaration>> callMap = new HashMap<>();
		final Map<String, List<AccessVar>> varReferencesMap = new HashMap<>();
		@Override
		public TraversalContinuation visitNode(ASTNode node, Script script) {
			if (node instanceof CallDeclaration) {
				final CallDeclaration call = (CallDeclaration)node;
				List<CallDeclaration> list = callMap.get(call.name());
				if (list == null)
					callMap.put(call.name(), list = new ArrayList<>(3));
				list.add(call);
			}
			else if (node instanceof AccessVar) {
				final AccessVar var = (AccessVar)node;
				List<AccessVar> list = varReferencesMap.get(var.name());
				if (list == null)
					varReferencesMap.put(var.name(), list = new ArrayList<>(3));
				list.add(var);
			}
			return TraversalContinuation.Continue;
		}
	};

	private void generateNodeMaps() {
		final NodeMapsPopulator populator = new NodeMapsPopulator();
		if (functions != null && index() != null)
			for (final Function f : functions())
				f.traverse(populator, this);
		this.callMap = Collections.unmodifiableMap(populator.callMap);
		this.varReferencesMap = Collections.unmodifiableMap(populator.varReferencesMap);
	}

	private void generateFindDeclarationCache(final List<Script> conglo) {
		final Map<String, Function> cachedFunctionMap = new HashMap<>();
		final Map<String, Variable> cachedVariableMap = new HashMap<>();
		for (final Script i : conglo) {
			if (i.functions != null)
				for (final Function f1 : i.functions) {
					// prefer putting non-global functions into the map so when in doubt the object function is picked
					// for cases where one script defines two functions with same name that differ in their globality (Power.ocd)
					final Function existing = cachedFunctionMap.get(f1.name());
					if (existing != null && existing.script() == i && f1.isGlobal() && !existing.isGlobal())
						continue;
					cachedFunctionMap.put(f1.name(), f1);
				}
			if (i.variables != null)
				for (final Variable v : i.variables)
					cachedVariableMap.put(v.name(), v);
		}
		this.cachedFunctionMap = cachedFunctionMap;
		this.cachedVariableMap = cachedVariableMap;
	}

	public Function function(String name) { return cachedFunctionMap != null ? cachedFunctionMap.get(name) : null; }
	public Variable variable(String name) { return cachedVariableMap != null ? cachedVariableMap.get(name) : null; }
	public Map<String, Function> functionMap() { return cachedFunctionMap; }

	@Override
	public String qualifiedName() {
		if (resource() == null)
			return this.toString();
		else
			return resource().getProjectRelativePath().toOSString();
	}

	private void findScenario() {
		final IResource res = resource();
		scenario = res != null ? Scenario.containingScenario(res) : null;
	}

	/**
	 * Return the {@link Scenario} the {@link Script} is contained in.
	 */
	@Override
	public Scenario scenario() {
		return scenario;
	}

	public void saveNodes(final Collection<? extends ASTNode> expressions, final boolean absoluteLocations) {
		Core.instance().performActionsOnFileDocument(file(), new IDocumentAction<Boolean>() {
			@Override
			public Boolean run(IDocument document) {
				try {
					final List<ASTNode> l = new ArrayList<ASTNode>(expressions);
					Collections.sort(l, new Comparator<ASTNode>() {
						@Override
						public int compare(ASTNode o1, ASTNode o2) {
							final IRegion r1 = absoluteLocations ? o1.absolute() : o1;
							final IRegion r2 = absoluteLocations ? o2.absolute() : o2;
							return r2.getOffset() - r1.getOffset();
						}
					});
					for (final ASTNode e : l) {
						final IRegion region = absoluteLocations ? e.absolute() : e;
						int depth;
						ASTNode n;
						for (depth = 0, n = e; n != null && !(n instanceof Declaration || n instanceof FunctionBody); depth++, n = n.parent());
						document.replace(region.getOffset(), region.getLength(), e.printed(depth));
					}
					return true;
				} catch (final BadLocationException e) {
					e.printStackTrace();
					return false;
				}
			}
		}, true);
	}

	public Variable createVarInScope(
		IVariableFactory factory,
		Function function, String varName, Scope scope,
		int start, int end,
		Comment description
	) {
		Variable result;
		switch (scope) {
		case VAR:
			result = function != null ? function.findVariable(varName) : null;
			break;
		case CONST: case STATIC: case LOCAL:
			result = findLocalVariable(varName, true);
			break;
		default:
			result = null;
			break;
		}
		if (result != null)
			return result;

		result = factory.newVariable(varName, scope);
		switch (scope) {
		case PARAMETER:
			result.setParent(function);
			function.parameters().add(result);
			break;
		case VAR:
			result.setParent(function);
			function.addLocal(result);
			break;
		case CONST: case STATIC: case LOCAL:
			result.setParent(this);
			addDeclaration(result);
		}
		result.setLocation(start, end);
		result.setUserDescription(description != null ? description.text().trim() : null);
		return result;
	}

	@Override
	public PrimitiveType primitiveType() { return PrimitiveType.OBJECT; }
	public void setScriptFile(IFile f) {}
	@Override
	public boolean isGlobal() { return true; }
	public boolean hasAppendTo() {
		for (final Directive d : directives())
			if (d.type() == DirectiveType.APPENDTO)
				return true;
		return false;
	}

	@Override
	public ASTNode[] subElements() {
		final List<ASTNode> decs = new ArrayList<ASTNode>(subDeclarations(index(), DeclMask.ALL));
		if (typeAnnotations != null)
			decs.addAll(typeAnnotations);
		return decs.toArray(new ASTNode[decs.size()]);
	}

	@Override
	public void doPrint(ASTNodePrinter output, int depth) {
		for (final ASTNode se : subElements())
			if (se != null) {
				if (se instanceof Function)
					output.append("\n\n");
				se.print(output, depth);
			}
	}

	public static IFile findScriptFile(IContainer container) {
		final String[] names = new String[] { "Script.c", "C4Script.c" };
		for (final String name : names) {
			final IResource f = findMemberCaseInsensitively(container, name);
			if (f instanceof IFile)
				return (IFile) f;
		}
		return null;
	}
}