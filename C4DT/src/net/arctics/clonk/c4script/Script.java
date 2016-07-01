package net.arctics.clonk.c4script;

import static java.util.Arrays.stream;
import static net.arctics.clonk.util.ArrayUtil.addAllSynchronized;
import static net.arctics.clonk.util.ArrayUtil.copyListOrReturnDefaultList;
import static net.arctics.clonk.util.ArrayUtil.filteredIterable;
import static net.arctics.clonk.util.ArrayUtil.iterable;
import static net.arctics.clonk.util.ArrayUtil.purgeNullEntries;
import static net.arctics.clonk.util.StreamUtil.ofType;
import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.block;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

import net.arctics.clonk.Core;
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
import net.arctics.clonk.c4script.ast.AccessDeclaration;
import net.arctics.clonk.c4script.ast.AccessVar;
import net.arctics.clonk.c4script.ast.CallDeclaration;
import net.arctics.clonk.c4script.ast.Comment;
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
import net.arctics.clonk.util.ITreeNode;
import net.arctics.clonk.util.StreamUtil;
import net.arctics.clonk.util.Utilities;

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

	public static boolean looksLikeScriptFile(final String name) {
		return name.endsWith(".c");
	}

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
		private final Map<String, IType> variableTypes;

		/**
		 * Map mapping function name to return type.
		 */
		private final Map<String, Function.Typing> functionTypings;

		public Typings(
			final Map<String, IType> variableTypes,
			final Map<String, Function.Typing> functionTypings
		) {
			super();
			this.variableTypes = variableTypes == Collections.EMPTY_MAP ? new HashMap<>() : variableTypes;
			this.functionTypings = functionTypings == Collections.EMPTY_MAP ? new HashMap<>() : functionTypings;
		}

		public Function.Typing get(final Function function) {
			return functionTypings != null ? functionTypings.get(function.name()) : null;
		}

		public IType get(final Variable variable) {
			return variableTypes != null ? variableTypes.get(variable.name()) : null;
		}

		public IType get(final ASTNode node) {
			final Function f = node.parent(Function.class);
			if (f == null) {
				return null;
			}
			final Function.Typing typing = get(f);
			return typing != null ? typing.nodeTypes[node.localIdentifier()] : null;
		}

		public void update(final Map<String, IType> variableTypes, final Map<String, Function.Typing> functionTypings) {
			synchronized (this) {
				for (final Entry<String, IType> x : variableTypes.entrySet()) {
					this.variableTypes.put(x.getKey(), x.getValue());
				}
				for (final Entry<String, Function.Typing> x : functionTypings.entrySet()) {
					this.functionTypings.put(x.getKey(), x.getValue());
				}
			}
		}

		public IType getVariableType(String variableName) {
			return variableTypes != null ? variableTypes.get(variableName) : null;
		}

		public Function.Typing getFunctionTyping(String functionName) {
			return functionTypings != null ? functionTypings.get(functionName) : null;
		}

	}
	private transient Typings typings;

	// serialized directly
	/** set of scripts this script is using functions and/or static variables from */
	private Set<Script> usedScripts;
	protected List<Directive> directives;

	// cache all the things
	private transient Map<String, Function> cachedFunctionMap;
	private transient Map<String, Variable> cachedVariableMap;
	private transient Scenario scenario;
	private transient Map<String, CallDeclaration[]> callMap = new HashMap<>();
	private transient Map<String, List<AccessVar>> varReferencesMap = new HashMap<>();

	private Set<String> dictionary;
	private List<TypeAnnotation> typeAnnotations;

	private static final Typings NO_TYPINGS = new Typings(
		Collections.<String, IType>emptyMap(),
		Collections.<String, Function.Typing>emptyMap()
	);

	public Typings typings() { return defaulting(typings, NO_TYPINGS); }
	public void setTypings(final Typings typings) { this.typings = typings; }

	public List<TypeAnnotation> typeAnnotations() { return typeAnnotations; }
	public void setTypeAnnotations(final List<TypeAnnotation> typeAnnotations) {
		this.typeAnnotations = typeAnnotations;
		if (this.typeAnnotations != null) {
			for (final TypeAnnotation a : typeAnnotations) {
				if (a.parent() == null) {
					a.setParent(this);
				}
			}
		}
	}

	public Map<String, CallDeclaration[]> callMap() { return defaulting(callMap, Collections.<String, CallDeclaration[]>emptyMap()); }
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

	protected Script(final Index index) { super(index); }

	protected static class SaveState implements Serializable {
		private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
		public Map<String, Effect> effects;
		public List<Function> functions;
		public List<Variable> variables;
		public Set<Script> used;
		public Typings typings;
		public Map<String, ProplistDeclaration> proplistDeclarations;
		public void initialize(
			final Map<String, Effect> effects,
			final List<Function> functions,
			final List<Variable> variables,
			final Set<Script> used,
			final Typings typings,
			final Map<String, ProplistDeclaration> proplistDeclarations
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
	public void save(final ObjectOutputStream stream) throws IOException {
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
	public void load(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
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
			if (usedScripts != null) {
				for (final Iterator<Script> it = usedScripts.iterator(); it.hasNext();) {
					final Script s = it.next();
					if (s != null) {
						s.requireLoaded();
					} else {
						it.remove();
					}
				}
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void postLoad(final Declaration parent, final Index root) {
		loadIncludes();
		loadUsedScripts();
		super.postLoad(parent, root);
		deriveInformation();
		for (final Function f : functions()) {
			f.findInherited();
		}
	}

	private void loadIncludes() {
		if (directives != null && index != null) {
			for (final Directive d : directives) {
				switch (d.type()) {
				case APPENDTO: case INCLUDE:
					final ID id = d.contentAsID();
					if (id != null) {
						final List<? extends Definition> defs = index.definitionsWithID(id);
						if (defs != null) {
							for (final Definition def : defs) {
								def.requireLoaded();
							}
						}
					}
					break;
				default:
					break;
				}
			}
		}
	}

	private static int nextCamelBack(final String s, int offset) {
		for (++offset; offset < s.length() && !Character.isUpperCase(s.charAt(offset)); offset++) {
			;
		}
		return offset;
	}

	public void detectEffects() {
		final List<EffectFunction> allEffectFunctions = new ArrayList<EffectFunction>(10);
		for (final EffectFunction f : functions(EffectFunction.class)) {
			allEffectFunctions.add(f);
		}
		while (allEffectFunctions.size() > 0) {
			final String s = allEffectFunctions.get(0).name().substring(EffectFunction.FUNCTION_NAME_PREFIX.length());
			List<EffectFunction> effectCandidates = null;
			String effectName = null;
			for (int i = nextCamelBack(s, 0); i < s.length(); i = nextCamelBack(s, i)) {
				final String sub = s.substring(0, i);
				final List<EffectFunction> matching = allEffectFunctions.stream().filter(item -> {
					try {
						return item.name().substring(EffectFunction.FUNCTION_NAME_PREFIX.length(), EffectFunction.FUNCTION_NAME_PREFIX.length()+sub.length()).equals(sub);
					} catch (final StringIndexOutOfBoundsException e) {
						return false;
					}
				}).collect(Collectors.toList());
				if (matching.isEmpty()) {
					break;
				}
				effectName = sub;
				effectCandidates = matching;
			};
			if (effectName != null) {
				allEffectFunctions.removeAll(effectCandidates);
				if (effects == null) {
					effects = new HashMap<String, Effect>();
				}
				final Effect existing = effects.get(effectName);
				if (existing == null) {
					final Effect effect = new Effect(effectName, effectCandidates);
					effect.setParent(this);
					effects.put(effectName, effect);
				}
			}
		}
	}

	protected void populateDictionary(final List<Script> conglomerate) {
		dictionary = conglomerate.stream()
			.flatMap(s -> s.subDeclarations(index(), DeclMask.ALL).stream())
			.map(d -> d.name())
			.collect(Collectors.toSet());
	}

	protected final void populateDictionary() { populateDictionary(conglomerate()); }

	/**
	 * Returns the strict level of the script
	 * @return the #strict level set for this script or the default level supplied by the engine configuration
	 */
	public int strictLevel() {
		requireLoaded();
		return this.directives().stream()
			.filter(d -> d.type() == DirectiveType.STRICT)
			.mapToInt(d -> {
				try {
					return Integer.parseInt(d.contents());
				}
				catch (final NumberFormatException e) {
					return 1;
				}
			})
			.reduce((int)(engine() != null ? engine().settings().strictDefaultLevel : -1), Math::max);
	}

	/**
	 * Returns \#include and \#appendto directives
	 * @return The directives
	 */
	public Directive[] includeDirectives() {
		requireLoaded();
		final List<Directive> result = directives().stream()
			.filter(d -> d.type() == DirectiveType.INCLUDE || d.type() == DirectiveType.APPENDTO)
			.collect(Collectors.toList());
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
	public boolean gatherIncludes(final Index contextIndex, final Script origin, final Collection<Script> set, int options) {
		if (!set.add(this)) {
			return false;
		}
		if (directives != null) {
			List<Directive> directivesCopy;
			synchronized(directives) {
				directivesCopy = new ArrayList<Directive>(directives);
			}
			for (final Directive d : directivesCopy) {
				if (d == null) {
					continue;
				}
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
							: pickNearest(defs.stream(), origin.resource(), null);
						if (pick != null) {
							if ((options & GatherIncludesOptions.Recursive) == 0) {
								set.add(pick);
							} else {
								if (d.type() == DirectiveType.INCLUDE) {
									options &= ~GatherIncludesOptions.NoAppendages;
								}
								pick.gatherIncludes(contextIndex, origin, set, options);
							}
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
	public Collection<Script> includes(final int options) {
		final Index ndx = index();
		return ndx == null ? Collections.emptySet() : includes(ndx, this, options);
	}

	@Override
	public Collection<Script> includes(final Index index, final Script origin, final int options) {
		return index != null
			? index.includes(new IncludesParameters(this, origin, options))
			: block(() -> {
				final HashSet<Script> result = new HashSet<Script>();
				gatherIncludes(index, origin, result, options);
				result.remove(this);
				return result;
			});
	}

	public boolean directlyIncludes(final Definition other) {
		return directives().stream().anyMatch(d -> d.refersTo(other));
	}

	/**
	 * Returns an include directive that includes a specific {@link Definition}'s script
	 * @param definition The {@link Definition} to return a corresponding {@link Directive} for
	 * @return The {@link Directive} or null if no matching directive exists in this script.
	 */
	public Directive directiveIncludingDefinition(final Definition definition) {
		requireLoaded();
		return stream(includeDirectives()).filter(
			d ->
				(d.type() == DirectiveType.INCLUDE || d.type() == DirectiveType.APPENDTO) &&
				nearestDefinitionWithId(d.contentAsID()) == definition
		).findFirst().orElse(null);
	}

	/**
	 * Finds a declaration in this script or an included one
	 * @param name Name of declaration to find.
	 * @return The declaration or null if not found
	 */
	@Override
	public Declaration findDeclaration(final String name) {
		return findDeclaration(new FindDeclarationInfo(name, index()));
	}

	@Override
	public Declaration findDeclaration(final String declarationName, final Class<? extends Declaration> declarationClass) {
		final FindDeclarationInfo info = new FindDeclarationInfo(declarationName, index());
		info.declarationClass = declarationClass;
		return findDeclaration(info);
	}

	@Override
	public Declaration findLocalDeclaration(final String declarationName, final Class<? extends Declaration> declarationClass) {
		requireLoaded();
		if (declarationClass.isAssignableFrom(Effect.class)) {
			final Effect e = effects().get(declarationName);
			if (e != null) {
				return e;
			}
		}
		if (declarationClass.isAssignableFrom(ProplistDeclaration.class)) {
			final ProplistDeclaration dec = proplistDeclarations().get(declarationName);
			if (dec != null) {
				return dec;
			}
		}
		if (declarationClass.isAssignableFrom(Variable.class)) {
			for (final Variable v : variables()) {
				if (v.name().equals(declarationName)) {
					return v;
				}
			}
		}
		if (declarationClass.isAssignableFrom(Function.class)) {
			for (final Function f : functions()) {
				if (f.name().equals(declarationName)) {
					return f;
				}
			}
		}
		return null;
	}

	/**
	 * Returns a declaration representing this script if the name matches the name of the script
	 * @param name The name
	 * @param info Additional info
	 * @return the declaration or null if there is no match
	 */
	protected Declaration representingDeclaration(final String name, final FindDeclarationInfo info) {
		return null;
	}

	@Override
	public List<Declaration> subDeclarations(final Index contextIndex, final int mask) {
		requireLoaded();
		final ArrayList<Declaration> decs = new ArrayList<Declaration>();
		List<Variable> vars = null;
		if ((mask & DeclMask.VARIABLES|DeclMask.STATIC_VARIABLES) != 0) {
			if (variables != null) {
				synchronized (variables) {
					vars = new ArrayList<>(variables);
				}
			}
		}
		if ((mask & DeclMask.DIRECTIVES) != 0) {
			addAllSynchronized(directives, decs, null);
		}
		if (vars != null) {
			if ((mask & DeclMask.VARIABLES) != 0) {
				for (final Variable v : vars) {
					if (!v.isGlobal()) {
						decs.add(v);
					}
				}
			}
			if ((mask & DeclMask.STATIC_VARIABLES) != 0) {
				for (final Variable v : vars) {
					if (v.isGlobal()) {
						decs.add(v);
					}
				}
			}
		}
		if ((mask & DeclMask.FUNCTIONS) != 0) {
			addAllSynchronized(functions, decs, null);
		}
		if ((mask & DeclMask.EFFECTS) != 0 && effects != null) {
			addAllSynchronized(effects.values(), decs, effects);
		}
		if ((mask & DeclMask.PROPLISTS) != 0 && proplistDeclarations != null) {
			addAllSynchronized(proplistDeclarations.values(), decs, proplistDeclarations);
		}
		return decs;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Declaration> T latestVersionOf(final T from) {
		final int mask =
			from instanceof Variable ? DeclMask.VARIABLES :
			from instanceof Function ? DeclMask.FUNCTIONS :
			from instanceof Directive ? DeclMask.DIRECTIVES : 0;
		T candidate = null;
		boolean locationMatch = false;
		for (final Declaration d : subDeclarations(this.index(), mask)) {
			if (d == from) {
				return (T)d;
			}
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

	private Declaration findUsingCache(final FindDeclarationInfo info) {
		final Class<? extends Declaration> decClass = info.declarationClass;
		// prefer using the cache
		requireLoaded();
		if (cachedVariableMap != null && (decClass == null || decClass == Variable.class)) {
			final Declaration d = cachedVariableMap.get(info.name);
			if (d != null) {
				return d;
			}
		}
		if (cachedFunctionMap != null && (decClass == null || decClass == Function.class)) {
			final Declaration d = cachedFunctionMap.get(info.name);
			if (d != null) {
				return d;
			}
		}
		return null;
	}

	/**
	 * Finds a declaration with the given name using information from the helper object
	 * @param info Information container describing the search
	 * @return the declaration or <tt>null</tt> if not found
	 */
	@Override
	public Declaration findDeclaration(final FindDeclarationInfo info) {

		// prevent infinite recursion
		if (!info.startSearchingIn(this)) {
			return null;
		}

		final Class<? extends Declaration> decClass = info.declarationClass;
		final String name = info.name;

		if (info.recursion == 0 && info.contextFunction != null && info.findGlobalVariables &&
			(decClass == null || decClass.isAssignableFrom(Variable.class))) {
			final Declaration v = info.contextFunction.findVariable(name);
			if (v != null) {
				return v;
			}
		}

		final boolean knows = dictionary() == null || dictionary().contains(name);
		if (knows) {
			// prefer using the cache
			requireLoaded();
			final Declaration cacheFind = findUsingCache(info);
			if (cacheFind != null && !info.reject(cacheFind)) {
				return cacheFind;
			}
		}

		// this object?
		final Declaration thisDec = representingDeclaration(name, info);
		if (thisDec != null) {
			return thisDec;
		}

		info.recursion++;
		{
			for (final Script o : includes(info.index, info.searchOrigin(), 0)) {
				final Declaration result = o.findDeclaration(info);
				if (result != null && !info.reject(result)) {
					return result;
				}
			}
		}
		info.recursion--;

		// finally look if it's something global
		if (info.recursion == 0 && info.index != null) {
			info.recursion++;
			final Declaration f = findGlobalDeclaration(info);
			info.recursion--;
			if (f != null && (info.declarationClass == null || info.declarationClass.isAssignableFrom(f.getClass()))) {
				return f;
			}
		}
		return null;
	}

	private Declaration findGlobalDeclaration(final FindDeclarationInfo info) {

		// prefer static variables associated with scenario context
		if (info.findGlobalVariables && info.scenario() != null) {
			final Variable scenarioStaticVariable = info.scenario().getStaticVariable(info.name);
			if (scenarioStaticVariable != null) {
				return scenarioStaticVariable;
			}
		}

		// prefer declarations from scripts that were previously determined to be the providers of global declarations
		// this will also probably and rightly lead to those scripts being fully loaded from their index file.
		if (usedScripts != null) {
			for (final Script s : usedScripts()) {
				final Declaration f = s.findDeclaration(info);
				if (f != null && f.isGlobal()) {
					return f;
				}
			}
		}

		if (info.findGlobalVariables && engine().acceptsID(info.name)) {
			final Declaration definition = findDefinition(info);
			if (definition != null) {
				return definition;
			}
		}

		// global stuff defined in relevant projects
		for (final Index index : info.index.relevantIndexes()) {
			final Declaration f = index.findGlobalDeclaration(info.name, resource());
			if (f != null && (info.findGlobalVariables || !(f instanceof Variable))) {
				return f;
			}
		}

		// engine function
		return index().engine().findDeclaration(info);
	}

	private Declaration findDefinition(final FindDeclarationInfo info) {
		final Definition d = info.index.definitionNearestTo(resource(), ID.get(info.name));
		if (d != null) {
			if (info.declarationClass == Variable.class) {
				return d.proxyVar();
			} else {
				return d;
			}
		}
		if (info.name.equals(Scenario.PROPLIST_NAME)) {
			Scenario scenario = Scenario.nearestScenario(this.resource());
			if (scenario == null) {
				scenario = engine().templateScenario();
			}
			if (scenario.propList() != null) {
				return scenario.propList();
			}
		}
		return null;
	}

	/**
	 * Add declaration to this script. Proplists will be named automatically when added without prior name.
	 * @param declaration Declaration to add. Can be a {@link Variable}, a {@link Function}, a {@link Directive} or a {@link ProplistDeclaration}
	 */
	@Override
	public <T extends Declaration> T addDeclaration(final T declaration) {
		requireLoaded();
		declaration.setParent(this);
		if (declaration instanceof Function) {
			synchronized (this) {
				if (functions == null) {
					functions = new ArrayList<Function>(5);
				}
				functions.add((Function)declaration);
				// function added after generating cache? put it
				if (cachedFunctionMap != null) {
					cachedFunctionMap.put(declaration.name(), (Function) declaration);
				}
			}
		} else if (declaration instanceof Variable) {
			synchronized (this) {
				if (variables == null) {
					variables = new ArrayList<Variable>(5);
				}
				variables.add((Variable)declaration);
				// variable added after generating cache? put it
				if (cachedVariableMap != null) {
					cachedVariableMap.put(declaration.name(), (Variable) declaration);
				}
			}
		} else if (declaration instanceof Directive) {
			synchronized (this) {
				if (directives == null) {
					directives = new ArrayList<Directive>(5);
				}
				directives.add((Directive)declaration);
			}
		} else if (declaration instanceof ProplistDeclaration) {
			synchronized (this) {
				if (proplistDeclarations == null) {
					proplistDeclarations = new HashMap<>();
				}
				if (declaration.name() == null) {
					declaration.setName("proplist"+proplistDeclarations.size());
				}
				proplistDeclarations.put(declaration.name(), (ProplistDeclaration) declaration);
			}
		} else {
			throw new IllegalArgumentException("declaration");
		}
		if (dictionary != null) {
			synchronized (dictionary) { dictionary.add(declaration.name()); }
		}
		return declaration;
	}

	public void removeDeclaration(final Declaration declaration) {
		requireLoaded();
		if (declaration.script() != this) {
			declaration.setParent(this);
		}
		if (declaration instanceof Function) {
			if (functions != null) {
				synchronized (functions) {
					functions.remove(declaration);
				}
			}
		}
		else if (declaration instanceof Variable) {
			if (variables != null) {
				synchronized (functions) {
					variables.remove(declaration);
				}
			}
		}
		else if (declaration instanceof Directive) {
			if (directives != null) {
				synchronized (directives) {
					directives.remove(declaration);
				}
			}
		}
	}

	public synchronized void clearDeclarations() {
		loaded = Loaded.Yes;
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

	public Function findFunction(final FindDeclarationInfo info) {
		info.resetState();
		info.declarationClass = Function.class;
		return (Function) findDeclaration(info);
	}

	@Override
	public Function findFunction(final String functionName) {
		final FindDeclarationInfo info = new FindDeclarationInfo(functionName, index());
		return findFunction(info);
	}

	public Variable findVariable(final String varName) {
		final FindDeclarationInfo info = new FindDeclarationInfo(varName, index());
		return findVariable(info);
	}

	public Variable findVariable(final FindDeclarationInfo info) {
		info.resetState();
		info.declarationClass = Variable.class;
		return (Variable) findDeclaration(info);
	}

	public Function funcAt(final int offset) {
		return funcAt(new Region(offset, 1));
	}

	public Function funcAt(final IRegion region) {
		requireLoaded();
		for (final Function f : functions()) {
			final int fStart = f.bodyLocation().getOffset();
			final int fEnd   = f.bodyLocation().getOffset()+f.bodyLocation().getLength();
			final int rStart = region.getOffset();
			final int rEnd   = region.getOffset()+region.getLength();
			if (rStart <= fStart && rEnd >= fEnd || rStart >= fStart && rStart <= fEnd || rEnd >= fEnd && rEnd <= fEnd) {
				return f;
			}
		}
		return null;
	}

	public Variable variableInitializedAt(final IRegion region) {
		requireLoaded();
		for (final Variable v : variables()) {
			final ASTNode initialization = v.initializationExpression();
			if (initialization != null) {
				final Function owningFunc = as(initialization.owner(), Function.class);
				final SourceLocation loc = owningFunc != null ? owningFunc.bodyLocation().add(initialization) : initialization;
				if (loc.containsOffset(region.getOffset())) {
					return v;
				}
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
	public boolean doesInclude(final Index contextIndex, final Script other) {
		requireLoaded();
		if (other == this) {
			return true;
		}
		final Iterable<Script> incs = this.includes(0);
		for (final Script o : incs) {
			if (o == other) {
				return true;
			}
		}
		return false;
	}

	public Variable findLocalVariable(final String name, final boolean searchIncludes) {
		return findLocalVariable(name, searchIncludes ? new HashSet<Script>() : null);
	}

	public Function findLocalFunction(final String name, final boolean includeIncludes) {
		return findLocalFunction(name, includeIncludes ? new HashSet<Script>() : null);
	}

	private Function findLocalFunction(final String name, final HashSet<Script> catcher) {
		requireLoaded();
		if (catcher != null && !catcher.add(this)) {
			return null;
		}
		for (final Function func: functions()) {
			if (func.name().equals(name)) {
				return func;
			}
		}
		if (catcher != null) {
			for (final Script script : filteredIterable(includes(0), Script.class)) {
				final Function func = script.findLocalFunction(name, catcher);
				if (func != null) {
					return func;
				}
			}
		}
		return null;
	}

	private Variable findLocalVariable(final String name, final HashSet<Script> catcher) {
		requireLoaded();
		if (catcher != null && !catcher.add(this)) {
			return null;
		}
		for (final Variable var : variables()) {
			if (var.name().equals(name)) {
				return var;
			}
		}
		if (catcher != null) {
			for (final Script script : filteredIterable(includes(GatherIncludesOptions.NoAppendages), Script.class)) {
				final Variable var = script.findLocalVariable(name, catcher);
				if (var != null) {
					return var;
				}
			}
		}
		return null;
	}

	public boolean removeDuplicateVariables() {
		requireLoaded();
		final Map<String, Variable> variableMap = new HashMap<String, Variable>();
		final Collection<Variable> toBeRemoved = new LinkedList<Variable>();
		for (final Variable v : variables()) {
			final Variable inHash = variableMap.get(v.name());
			if (inHash != null) {
				toBeRemoved.add(v);
			} else {
				variableMap.put(v.name(), v);
			}
		}
		for (final Variable v : toBeRemoved) {
			variables.remove(v);
		}
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
	public <T extends Function> Iterable<T> functions(final Class<T> cls) {
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

	public Definition nearestDefinitionWithId(final ID id) {
		final Index index = index();
		if (index != null) {
			return index.definitionNearestTo(resource(), id);
		}
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
			public boolean add(final Script e) {
				if (contains(e)) {
					return false;
				} else {
					return super.add(e);
				}
			}
		};
		gatherIncludes(index(), this, s, GatherIncludesOptions.Recursive);
		return s;
	}

	@Override
	public INode[] subDeclarationsForOutline() {
		requireLoaded();
		final List<Object> all = new LinkedList<Object>();
		for (final Script c : conglomerate()) {
			for (final Declaration sd : c.subDeclarations(index(), DeclMask.FUNCTIONS|DeclMask.VARIABLES|(c==this?DeclMask.DIRECTIVES:0))) {
				if (sd instanceof SynthesizedFunction) {
					continue;
				}
				if (sd instanceof Function && !seesFunction(((Function)sd))) {
					continue;
				}
				all.add(sd);
			}
		}
		return all.toArray(new INode[all.size()]);
	}

	private String sourceComment;
	public String sourceComment() { return sourceComment; }
	public void setSourceComment(final String s) { sourceComment = s; }

	@Override
	public String infoText(final IIndexEntity context) {
		if (sourceComment != null) {
			return sourceComment;
		}
		final Object f = source();
		if (f instanceof IFile) {
			final IResource infoFile = Utilities.findMemberCaseInsensitively(((IFile)f).getParent(), "Desc"+ClonkPreferences.languagePref()+".txt"); //$NON-NLS-1$ //$NON-NLS-2$
			if (infoFile instanceof IFile) {
				try {
					return StreamUtil.stringFromFileDocument((IFile) infoFile);
				} catch (final Exception e) {
					e.printStackTrace();
					return super.infoText(context);
				}
			}
		}
		return "";
	}

	@Override
	public ITreeNode parentNode() {
		return parentDeclaration() instanceof ITreeNode ? (ITreeNode)parentDeclaration() : null;
	}

	@Override
	public Collection<? extends INode> childCollection() {
		return stream(subDeclarationsForOutline()).collect(Collectors.toList());
	}

	@Override
	public void addChild(final ITreeNode node) {
		requireLoaded();
		if (node instanceof Declaration) {
			addDeclaration((Declaration)node);
		}
	}

	public void addUsedScript(final Script script) {
		if (script == null) {
			// this does happen, for example when adding the script of some variable read from PlayerControls.txt
			// which is null
			return;
		}
		if (script == this || script instanceof Engine) {
			return;
		}
		requireLoaded();
		if (usedScripts == null) {
			usedScripts = new HashSet<Script>();
		}
		synchronized (usedScripts) {
			usedScripts.add(script);
		}
	}

	/**
	 * notification sent by the index when a script is removed
	 */
	public void scriptRemovedFromIndex(final Script script) {
		if (usedScripts != null) {
			synchronized (usedScripts) {
				usedScripts.remove(script);
			}
		}
	}

	@Override
	public Engine engine() {
		final Index index = index();
		return index != null ? index.engine() : null;
	}

	public static Script get(final IResource resource, final boolean onlyForScriptFile) {
		final ClonkProjectNature nat = ClonkProjectNature.get(resource);
		if (nat != null) {
			nat.index();
		}
		Script script;
		if (resource == null) {
			return null;
		}
		script = SystemScript.pinned(resource, false);
		if (script == null) {
			script = Definition.at(resource.getParent());
		}
		// there can only be one script oO (not ScriptDE or something)
		if (onlyForScriptFile && (script == null || script.source() == null || !script.source().equals(resource))) {
			return null;
		}
		return script;
	}

	@Override
	public String typeName(final boolean special) {
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
	public void reportOriginForExpression(final ASTNode expression, final IRegion location, final IFile file) { /* cool */ }
	@Override
	public Object[] arguments() { return new Object[0]; }
	@Override
	public IVariable variable(final AccessDeclaration access, final Object obj) {
		if (access.predecessor() == null) {
			final Variable v = findLocalVariable(access.name(), true);
			if (v != null) {
				return new Constant(v);
			}
			for (final Script s : includes(0)) {
				final IVariable vv = s.variable(access, obj);
				if (vv != null) {
					return vv;
				}
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
	public final boolean seesFunction(final Function function) {
		if (cachedFunctionMap != null) {
			final Function mine = cachedFunctionMap.get(function.name());
			return mine == null || mine.latestVersion() == function;
		} else {
			return true;
		}
	}

	public Function override(final Function function) {
		if (cachedFunctionMap != null) {
			final Function ovr = cachedFunctionMap.get(function);
			if (ovr != null) {
				return ovr;
			}
		}
		return function;
	}

	@Override
	public boolean seesSubDeclaration(final Declaration subDeclaration) {
		if (subDeclaration instanceof Function) {
			return seesFunction((Function)subDeclaration);
		} else {
			return true;
		}
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
		if (file() != null) {
			pinTo(file());
		}
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
		public TraversalContinuation visitNode(final ASTNode node, final Script script) {
			if (node instanceof CallDeclaration) {
				final CallDeclaration call = (CallDeclaration)node;
				List<CallDeclaration> list = callMap.get(call.name());
				if (list == null) {
					callMap.put(call.name(), list = new ArrayList<>(3));
				}
				list.add(call);
			}
			else if (node instanceof AccessVar) {
				final AccessVar var = (AccessVar)node;
				List<AccessVar> list = varReferencesMap.get(var.name());
				if (list == null) {
					varReferencesMap.put(var.name(), list = new ArrayList<>(3));
				}
				list.add(var);
			}
			return TraversalContinuation.Continue;
		}
	};

	private void generateNodeMaps() {
		final NodeMapsPopulator populator = new NodeMapsPopulator();
		if (functions != null && index() != null) {
			for (final Function f : functions()) {
				f.traverse(populator, this);
			}
		}
		this.callMap = Collections.unmodifiableMap(
			populator.callMap.entrySet().stream().collect(Collectors.toMap(
				kv -> kv.getKey(),
				kv -> kv.getValue().toArray(new CallDeclaration[kv.getValue().size()])
			))
		);	
		this.varReferencesMap = Collections.unmodifiableMap(populator.varReferencesMap);
	}

	private void generateFindDeclarationCache(final List<Script> conglo) {
		final Map<String, Function> cachedFunctionMap = new HashMap<>();
		final Map<String, Variable> cachedVariableMap = new HashMap<>();
		for (final Script i : conglo) {
			if (i.functions != null) {
				for (final Function f1 : i.functions) {
					// prefer putting non-global functions into the map so when in doubt the object function is picked
					// for cases where one script defines two functions with same name that differ in their globality (Power.ocd)
					final Function existing = cachedFunctionMap.get(f1.name());
					if (existing != null && existing.script() == i && f1.isGlobal() && !existing.isGlobal()) {
						continue;
					}
					cachedFunctionMap.put(f1.name(), f1);
				}
			}
			if (i.variables != null) {
				for (final Variable v : i.variables) {
					cachedVariableMap.put(v.name(), v);
				}
			}
		}
		this.cachedFunctionMap = cachedFunctionMap;
		this.cachedVariableMap = cachedVariableMap;
	}

	public Function function(final String name) { return cachedFunctionMap != null ? cachedFunctionMap.get(name) : null; }
	public Variable variable(final String name) { return cachedVariableMap != null ? cachedVariableMap.get(name) : null; }
	public Map<String, Function> functionMap() { return defaulting(cachedFunctionMap, Collections.<String, Function>emptyMap()); }

	@Override
	public String qualifiedName() {
		if (resource() == null) {
			return this.toString();
		} else {
			return resource().getProjectRelativePath().toOSString();
		}
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

	public Variable createVarInScope(
		final IVariableFactory factory,
		final Function function, final String varName, final Scope scope,
		final int start, final int end,
		final Comment description
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
		if (result != null) {
			return result;
		}

		result = factory.newVariable(scope, varName);
		switch (scope) {
		case PARAMETER:
			result.setParent(function);
			function.addParameter(result);
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
	public void setScriptFile(final IFile f) {}
	@Override
	public boolean isGlobal() { return true; }
	public boolean hasAppendTo() {
		for (final Directive d : directives()) {
			if (d.type() == DirectiveType.APPENDTO) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void setSubElements(ASTNode[] elms) {
		this.clearDeclarations();
		// FIXME: dropping non-declarations here
		ofType(stream(elms), Declaration.class).forEach(this::addDeclaration);
	}

	@Override
	public ASTNode[] subElements() {
		final List<ASTNode> decs = new ArrayList<ASTNode>(subDeclarations(index(), DeclMask.ALL));
		if (typeAnnotations != null) {
			decs.addAll(typeAnnotations);
		}
		return decs.toArray(new ASTNode[decs.size()]);
	}

	private static Class<? extends ASTNode> categoryClass(ASTNode node) {
		if (node instanceof Function) {
			return Function.class;
		} else if (node instanceof Variable) {
			return Variable.class;
		} else if (node instanceof Directive) {
			return Directive.class;
		} else {
			return node.getClass();
		}
	}

	private static boolean inRegularFunction(ASTNode node) {
		final Function f = node.parent(Function.class);
		return f != null && !(f instanceof SynthesizedFunction);
	}

	@Override
	public void doPrint(final ASTNodePrinter output, final int depth) {

		if (sourceComment != null) {
			new Comment(sourceComment, true, false).print(output, depth);
			output.lb();
			output.lb();
		}

		ASTNode prev = null;
		for (final ASTNode se : subElements()) {
			final boolean skip =
				se == null || se instanceof ProplistDeclaration ||
				se instanceof SynthesizedFunction ||
				(se instanceof Variable && ((Variable)se).initializationExpression() != null && inRegularFunction(((Variable)se).initializationExpression()));
			if (!skip) {
				if (prev != null) {
					output.lb();
					if (prev instanceof Function || categoryClass(prev) != categoryClass(se)) {
						output.lb();
					}
				}
				se.print(output, depth);
				prev = se;
			}
		}
	}

	public static IFile findScriptFile(final IContainer container) {
		final String[] names = new String[] { "Script.c", "C4Script.c" };
		for (final String name : names) {
			final IResource f = findMemberCaseInsensitively(container, name);
			if (f instanceof IFile) {
				return (IFile) f;
			}
		}
		return null;
	}
}