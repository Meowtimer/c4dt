package net.arctics.clonk.parser.c4script;

import static net.arctics.clonk.util.ArrayUtil.addAllSynchronized;
import static net.arctics.clonk.util.ArrayUtil.copyListOrReturnDefaultList;
import static net.arctics.clonk.util.ArrayUtil.filteredIterable;
import static net.arctics.clonk.util.ArrayUtil.iterable;
import static net.arctics.clonk.util.ArrayUtil.purgeNullEntries;
import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.defaulting;
import static net.arctics.clonk.util.Utilities.filter;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.Writer;
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
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import net.arctics.clonk.Core;
import net.arctics.clonk.Core.IDocumentAction;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.IIndexEntity;
import net.arctics.clonk.index.IVariableFactory;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.IndexEntity;
import net.arctics.clonk.index.Scenario;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.DeclMask;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.IASTVisitor;
import net.arctics.clonk.parser.ID;
import net.arctics.clonk.parser.IEvaluationContext;
import net.arctics.clonk.parser.IHasIncludes;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.parser.TraversalContinuation;
import net.arctics.clonk.parser.c4script.Directive.DirectiveType;
import net.arctics.clonk.parser.c4script.Variable.Scope;
import net.arctics.clonk.parser.c4script.ast.AccessVar;
import net.arctics.clonk.parser.c4script.ast.CallDeclaration;
import net.arctics.clonk.parser.c4script.ast.Comment;
import net.arctics.clonk.parser.c4script.ast.FunctionBody;
import net.arctics.clonk.parser.c4script.effect.Effect;
import net.arctics.clonk.parser.c4script.effect.EffectFunction;
import net.arctics.clonk.parser.c4script.typing.TypeAnnotation;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.util.INode;
import net.arctics.clonk.util.IPredicate;
import net.arctics.clonk.util.ITreeNode;
import net.arctics.clonk.util.StreamUtil;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Region;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Base class for various objects that act as containers of stuff declared in scripts/ini files.
 * Subclasses include {@link Definition}, {@link SystemScript} etc.
 */
public abstract class Script extends IndexEntity implements ITreeNode, IRefinedPrimitiveType, IEvaluationContext, IHasIncludes<Script> {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	// will be written by #save
	protected transient List<Function> definedFunctions;
	protected transient List<Variable> definedVariables;
	protected transient Map<String, Effect> definedEffects;

	// set of scripts this script is using functions and/or static variables from
	private Set<Script> usedScripts;
	protected List<Directive> definedDirectives;

	// cache all the things
	private transient Map<String, Function> cachedFunctionMap;
	private transient Map<String, Variable> cachedVariableMap;
	private transient Collection<Script> includes;
	private transient Scenario scenario;
	private transient Map<String, List<CallDeclaration>> callMap = new HashMap<>();
	private transient Map<String, List<AccessVar>> varReferencesMap = new HashMap<>();

	private Set<String> dictionary;
	private List<TypeAnnotation> typeAnnotations;
	private transient Map<Variable, IType> variableTypes;
	private transient Map<String, IType> functionReturnTypes;

	public Map<Variable, IType> variableTypes() {
		requireLoaded();
		return defaulting(variableTypes, Collections.<Variable, IType>emptyMap());
	}
	public Map<String, IType> functionReturnTypes() {
		requireLoaded();
		return defaulting(functionReturnTypes, Collections.<String, IType>emptyMap());
	}

	public void setTypings(Map<Variable, IType> variableTypes, Map<String, IType> functionReturnTypes) {
		this.variableTypes = variableTypes;
		this.functionReturnTypes = functionReturnTypes;
	}

	public List<TypeAnnotation> typeAnnotations() { return typeAnnotations; }
	public void setTypeAnnotations(List<TypeAnnotation> typeAnnotations) { this.typeAnnotations = typeAnnotations; }

	public Map<String, List<CallDeclaration>> callMap() { return callMap; }
	public Map<String, List<AccessVar>> varReferences() { return varReferencesMap; }

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
		return copyListOrReturnDefaultList(usedScripts, NO_SCRIPTS);
	}

	protected Script(Index index) { super(index); }

	public static class Declarations implements Serializable {
		private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
		public Map<String, Effect> effects;
		public List<Function> functions;
		public List<Variable> variables;
		public Set<Script> used;
		public Map<Variable, IType> variableTypes;
		public Map<String, IType> functionReturnTypes;
		public Declarations(Map<String, Effect> effects, List<Function> functions, List<Variable> variables, Set<Script> used, Map<Variable, IType> variableTypes, Map<String, IType> functionTypes) {
			super();
			this.effects = effects;
			this.functions = functions;
			this.variables = variables;
			this.used = used;
			this.variableTypes = variableTypes;
			this.functionReturnTypes = functionTypes;
		}
	}

	@Override
	public void save(ObjectOutputStream stream) throws IOException {
		super.save(stream);
		stream.writeObject(new Declarations(
			definedEffects,
			definedFunctions,
			definedVariables,
			usedScripts,
			variableTypes,
			functionReturnTypes
		));
		populateDictionary();
	}

	@Override
	public void load(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		super.load(stream);
		loadIncludes();
		final Declarations declarations = (Declarations)stream.readObject();
		definedEffects = declarations.effects;
		definedFunctions = declarations.functions;
		definedVariables = declarations.variables;
		usedScripts = declarations.used;
		variableTypes = declarations.variableTypes;
		functionReturnTypes = declarations.functionReturnTypes;
		purgeNullEntries(definedFunctions, definedVariables, usedScripts);
		// also load scripts this script uses global declarations from so they will be present when the script gets parsed
		try {
			if (usedScripts != null)
				for (final Script s : usedScripts)
					if (s != null)
						s.requireLoaded();
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	private void loadIncludes() {
		if (definedDirectives != null && index != null)
			for (final Directive d : definedDirectives)
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
				if (definedEffects == null)
					definedEffects = new HashMap<String, Effect>();
				final Effect effect = new Effect(effectName, effectCandidates);
				effect.setParent(this);
				definedEffects.put(effectName, effect);
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
	 * Return an array that acts as a map line number -> function at that line. Used for fast function lookups when only the line number is known.
	 * @return The pseudo-map for getting the function at some line.
	 */
	public Function[] calculateLineToFunctionMap() {
		requireLoaded();
		String scriptText;
		try {
			scriptText = StreamUtil.stringFromInputStream(this.source().getContents());
		} catch (final CoreException e) {
			e.printStackTrace();
			return null;
		}
		int lineStart = 0;
		int lineEnd = 0;
		final List<Function> mappingAsList = new LinkedList<Function>();
		final MutableRegion region = new MutableRegion(0, 0);
		for (final BufferedScanner scanner = new BufferedScanner(scriptText); !scanner.reachedEOF();) {
			final int read = scanner.read();
			boolean newLine = false;
			switch (read) {
			case '\r':
				newLine = true;
				if (scanner.read() != '\n')
					scanner.unread();
				break;
			case '\n':
				newLine = true;
				break;
			default:
				lineEnd = scanner.tell();
			}
			if (newLine) {
				region.setOffset(lineStart);
				region.setLength(lineEnd-lineStart);
				Function f = this.funcAt(region);
				if (f == null)
					f = this.funcAt(lineEnd);
				mappingAsList.add(f);
				lineStart = scanner.tell();
				lineEnd = lineStart;
			}
		}
		return mappingAsList.toArray(new Function[mappingAsList.size()]);
	}

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
	public boolean gatherIncludes(Index contextIndex, Object origin, Collection<Script> set, int options) {
		if (!set.add(this))
			return false;
		if (definedDirectives != null) {
			List<Directive> directivesCopy;
			synchronized(definedDirectives) {
				directivesCopy = new ArrayList<Directive>(definedDirectives);
			}
			for (final Directive d : directivesCopy) {
				if (d == null)
					continue;
				if (d.type() == DirectiveType.INCLUDE || (d.type() == DirectiveType.APPENDTO && (options & GatherIncludesOptions.NoAppendages) == 0)) {
					final ID id = d.contentAsID();
					for (final Index in : contextIndex.relevantIndexes()) {
						final Iterable<? extends Definition> defs = in.definitionsWithID(id);
						if (defs != null)
							for (final Definition def : defs)
								if ((options & GatherIncludesOptions.Recursive) == 0)
									set.add(def);
								else {
									if (d.type() == DirectiveType.INCLUDE)
										options &= ~GatherIncludesOptions.NoAppendages;
									def.gatherIncludes(contextIndex, origin, set, options);
								}
					}
				}
			}
		}
		return true;
	}

	/**
	 * Return {@link #includes(Index, boolean)}({@link #index()}, {@code recursive});
	 * @param recursive Whether the returned collection also contains includes of the includes.
	 * @return The includes
	 */
	public Collection<Script> includes(int options) {
		if (index() == null)
			return Collections.emptySet();
		else
			return includes(index(), this, options);
	}

	private transient int _lastIncludesIndex;
	private transient int _lastIncludesOrigin;
	private transient int _lastIncludesOptions;

	/**
	 * Does the same as gatherIncludes except that the user does not have to create their own list
	 * @param index The index to be passed to gatherIncludes
	 * @param recursive Whether the returned collection also contains includes of the includes.
	 * @return The includes
	 */
	@Override
	public Collection<Script> includes(Index index, Object origin, int options) {
		synchronized (this) {
			final int indexHash = index != null ? index.hashCode() : 0;
			final int originHash = origin != null ? origin.hashCode() : 0;
			if (includes != null && indexHash == _lastIncludesIndex && originHash == _lastIncludesOrigin && options == _lastIncludesOptions)
				return includes;
			//System.out.println(this.name() + ": reget");
			_lastIncludesIndex = indexHash;
			_lastIncludesOrigin = originHash;
			_lastIncludesOptions = options;
			return includes = IHasIncludes.Default.includes(index, this, origin, options);
		}
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
		return findDeclaration(name, new FindDeclarationInfo(index()));
	}

	@Override
	public Declaration findDeclaration(String declarationName, Class<? extends Declaration> declarationClass) {
		final FindDeclarationInfo info = new FindDeclarationInfo(index());
		info.declarationClass = declarationClass;
		return findDeclaration(declarationName, info);
	}

	@Override
	public Declaration findLocalDeclaration(String declarationName, Class<? extends Declaration> declarationClass) {
		requireLoaded();
		if (declarationClass.isAssignableFrom(Variable.class))
			for (final Variable v : variables())
				if (v.name().equals(declarationName))
					return v;
		if (declarationClass.isAssignableFrom(Function.class))
			for (final Function f : functions())
				if (f.name().equals(declarationName))
					return f;
		if (declarationClass.isAssignableFrom(Effect.class)) {
			final Effect e = effects().get(declarationName);
			if (e != null)
				return e;
		}
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
		if ((mask & DeclMask.FUNCTIONS) != 0)
			addAllSynchronized(definedFunctions, decs);
		if ((mask & DeclMask.VARIABLES) != 0)
			addAllSynchronized(definedVariables, decs);
		if ((mask & DeclMask.DIRECTIVES) != 0)
			addAllSynchronized(definedDirectives, decs);
		if ((mask & DeclMask.IMPLICIT) != 0 && definedEffects != null)
			addAllSynchronized(definedEffects.values(), decs);
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

	/**
	 * Finds a declaration with the given name using information from the helper object
	 * @param name The name
	 * @param info Additional info
	 * @return the declaration or <tt>null</tt> if not found
	 */
	@Override
	public Declaration findDeclaration(String name, FindDeclarationInfo info) {

		// prevent infinite recursion
		if (!info.startSearchingIn(this))
			return null;

		final Class<? extends Declaration> decClass = info.declarationClass;

		// local variable?
		if (info.recursion == 0)
			if (info.contextFunction != null && (decClass == Declaration.class || decClass == null || decClass == Variable.class)) {
				final Declaration v = info.contextFunction.findVariable(name);
				if (v != null)
					return v;
			}


		final boolean knows = dictionary() == null || dictionary().contains(name);
		boolean didUseCacheForLocalDeclarations = false;
		if (knows) {
			// prefer using the cache
			requireLoaded();
			if ((cachedVariableMap != null || cachedFunctionMap != null) && info.index == this.index() && (info.searchOrigin == null || scenario() == info.searchOrigin.scenario())) {
				if (cachedVariableMap != null && (decClass == null || decClass == Variable.class)) {
					final Declaration d = cachedVariableMap.get(name);
					if (d != null)
						return d;
					didUseCacheForLocalDeclarations = true;
				}
				if (cachedFunctionMap != null && (decClass == null || decClass == Function.class)) {
					final Declaration d = cachedFunctionMap.get(name);
					if (d != null)
						return d;
					didUseCacheForLocalDeclarations = true;
				}
			}
		}

		// this object?
		final Declaration thisDec = representingDeclaration(name, info);
		if (thisDec != null)
			return thisDec;

		if (!didUseCacheForLocalDeclarations) {
			if (knows) {
				requireLoaded();
				// a function defined in this object
				if (decClass == null || decClass == Function.class) {
					final Function f = definedDeclarationNamed(name, Function.class);
					if (f != null)
						return f;
				}
				// a variable
				if (decClass == null || decClass == Variable.class) {
					final Variable v = definedDeclarationNamed(name, Variable.class);
					if (v != null)
						return v;
				}
			}

			info.recursion++;
			{
				for (final Script o : includes(info.index, info.searchOrigin, 0)) {
					final Declaration result = o.findDeclaration(name, info);
					if (result != null)
						return result;
				}
			}
			info.recursion--;
		}

		// finally look if it's something global
		if (info.recursion == 0 && info.index != null) {
			info.recursion++;
			final Declaration f = findGlobalDeclaration(name, info);
			info.recursion--;
			if (f != null && (info.declarationClass == null || info.declarationClass.isAssignableFrom(f.getClass())))
				return f;
		}
		return null;
	}

	private Declaration findGlobalDeclaration(String name, FindDeclarationInfo info) {

		// prefer declarations from scripts that were previously determined to be the providers of global declarations
		// this will also probably and rightly lead to those scripts being fully loaded from their index file.
		if (usedScripts != null)
			for (final Script s : usedScripts()) {
				final Declaration f = s.findDeclaration(name, info);
				if (f != null && f.isGlobal())
					return f;
			}

		if (info.findGlobalVariables && engine().acceptsId(name)) {
			final Definition d = info.index.definitionNearestTo(resource(), ID.get(name));
			if (d != null)
				if (info.declarationClass == Variable.class)
					return d.proxyVar();
				else
					return d;
			if (name.equals(Scenario.PROPLIST_NAME)) {
				Scenario scenario = Scenario.nearestScenario(this.resource());
				if (scenario == null)
					scenario = engine().templateScenario();
				if (scenario.propList() != null)
					return scenario.propList();
			}
			else if (name.equals(Index.GLOBAL_PROPLIST_NAME) && index().global() != null)
				return index().global();
		}

		// global stuff defined in relevant projects
		for (final Index index : info.index.relevantIndexes()) {
			final Declaration f = index.findGlobalDeclaration(name, resource());
			if (f != null && (info.findGlobalVariables || !(f instanceof Variable)))
				return f;
		}

		// engine function
		return index().engine().findDeclaration(name, info);
	}

	public void addDeclaration(Declaration declaration) {
		requireLoaded();
		declaration.setParent(this);
		if (declaration instanceof Function)
			synchronized (this) {
				if (definedFunctions == null)
					definedFunctions = new ArrayList<Function>(5);
				definedFunctions.add((Function)declaration);
				// function added after generating cache? put it
				if (cachedFunctionMap != null)
					cachedFunctionMap.put(declaration.name(), (Function) declaration);
			}
		else if (declaration instanceof Variable)
			synchronized (this) {
				if (definedVariables == null)
					definedVariables = new ArrayList<Variable>(5);
				definedVariables.add((Variable)declaration);
				// variable added after generating cache? put it
				if (cachedVariableMap != null)
					cachedVariableMap.put(declaration.name(), (Variable) declaration);
			}
		else if (declaration instanceof Directive)
			synchronized (this) {
				if (definedDirectives == null)
					definedDirectives = new ArrayList<Directive>(5);
				definedDirectives.add((Directive)declaration);
			}
		if (dictionary != null)
			synchronized (dictionary) { dictionary.add(declaration.name()); }
	}

	public void removeDeclaration(Declaration declaration) {
		requireLoaded();
		if (declaration.script() != this)
			declaration.setParent(this);
		if (declaration instanceof Function) {
			if (definedFunctions != null) synchronized (definedFunctions) {
				definedFunctions.remove(declaration);
			}
		}
		else if (declaration instanceof Variable) {
			if (definedVariables != null) synchronized (definedFunctions) {
				definedVariables.remove(declaration);
			}
		}
		else if (declaration instanceof Directive)
			if (definedDirectives != null) synchronized (definedDirectives) {
				definedDirectives.remove(declaration);
			}
	}

	public synchronized void clearDeclarations() {
		loaded = true;
		usedScripts = null;
		definedDirectives = null;
		definedFunctions = null;
		definedVariables = null;
		cachedFunctionMap = null;
		cachedVariableMap = null;
	}

	public abstract IStorage source();

	public final IFile scriptFile() {
		final IStorage storage = source();
		return storage instanceof IFile ? (IFile)storage : null;
	}

	@Override
	public Script script() { return this; }
	@Override
	public Structure topLevelStructure() { return this; }
	@Override
	public IResource resource() { return null; }

	public Function findFunction(String functionName, FindDeclarationInfo info) {
		info.resetState();
		info.declarationClass = Function.class;
		return (Function) findDeclaration(functionName, info);
	}

	@Override
	public Function findFunction(String functionName) {
		final FindDeclarationInfo info = new FindDeclarationInfo(index());
		return findFunction(functionName, info);
	}

	public Variable findVariable(String varName) {
		final FindDeclarationInfo info = new FindDeclarationInfo(index());
		return findVariable(varName, info);
	}

	public Variable findVariable(String varName, FindDeclarationInfo info) {
		info.resetState();
		info.declarationClass = Variable.class;
		return (Variable) findDeclaration(varName, info);
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

	public Variable variableWithInitializationAt(IRegion region) {
		requireLoaded();
		for (final Variable v : variables()) {
			final ASTNode initialization = v.initializationExpression();
			if (initialization != null) {
				final Function owningFunc = as(initialization.owningDeclaration(), Function.class);
				final SourceLocation loc = owningFunc != null ? owningFunc.bodyLocation().add(initialization) : initialization;
				if (loc.containsOffset(region.getOffset()))
					return v;
			}
		}
		return null;
	}

	// OMG, IRegion <-> ITextSelection
	public Function funcAt(ITextSelection region) {
		requireLoaded();
		for (final Function f : functions())
			if (f.start() <= region.getOffset() && region.getOffset()+region.getLength() <= f.bodyLocation().end())
				return f;
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

	public Variable findLocalVariable(String name, boolean includeIncludes) {
		return findLocalVariable(name, includeIncludes ? new HashSet<Script>() : null);
	}

	public Function findLocalFunction(String name, boolean includeIncludes) {
		return findLocalFunction(name, includeIncludes ? new HashSet<Script>() : null);
	}

	public Function findLocalFunction(String name, HashSet<Script> alreadySearched) {
		requireLoaded();
		if (alreadySearched != null && !alreadySearched.add(this))
			return null;
		for (final Function func: functions())
			if (func.name().equals(name))
				return func;
		if (alreadySearched != null)
			for (final Script script : filteredIterable(includes(0), Script.class)) {
				final Function func = script.findLocalFunction(name, alreadySearched);
				if (func != null)
					return func;
			}
		return null;
	}

	public Variable findLocalVariable(String name, HashSet<Script> alreadySearched) {
		requireLoaded();
		if (alreadySearched != null) {
			if (alreadySearched.contains(this))
				return null;
			alreadySearched.add(this);
		}
		for (final Variable var : variables())
			if (var.name().equals(name))
				return var;
		if (alreadySearched != null)
			for (final Script script : filteredIterable(includes(0), Script.class)) {
				final Variable var = script.findLocalVariable(name, alreadySearched);
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
			definedVariables.remove(v);
		return toBeRemoved.size() > 0;
	}

	private static final List<Directive> NO_DIRECTIVES = Collections.unmodifiableList(new ArrayList<Directive>());
	private static final List<Function> NO_FUNCTIONS = Collections.unmodifiableList(new ArrayList<Function>());
	private static final List<Variable> NO_VARIABLES = Collections.unmodifiableList(new ArrayList<Variable>());
	private static final List<Script> NO_SCRIPTS = Collections.unmodifiableList(new ArrayList<Script>());
	private static final Map<String, Effect> NO_EFFECTS = Collections.unmodifiableMap(new HashMap<String, Effect>());

	/**
	 * Return the list of functions defined in this script.
	 * @return The functions list
	 */
	public List<? extends Function> functions() {
		requireLoaded();
		return definedFunctions != null ? definedFunctions : NO_FUNCTIONS;
	}

	@SuppressWarnings("unchecked")
	public <T extends Declaration> T definedDeclarationNamed(String name, Class<T> cls) {
		List<T> list;
		if (cls == Variable.class)
			list = (List<T>)definedVariables;
		else if (cls == Function.class)
			list = (List<T>)definedFunctions;
		else if (cls == Directive.class)
			list = (List<T>)definedDirectives;
		else
			return null;
		synchronized (this) {
			if (list != null)
				for (final T f : list)
					if (f.name().equals(name))
						return f;
			return null;
		}
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
		return copyListOrReturnDefaultList(definedVariables, NO_VARIABLES);
	}

	/**
	 * Return the list of directives defined in this script.
	 * @return The directives list
	 */
	public List<? extends Directive> directives() {
		requireLoaded();
		return definedDirectives != null ? definedDirectives : NO_DIRECTIVES;
	}

	/**
	 * Return a map mapping effect name to {@link Effect} object
	 * @return The map
	 */
	public Map<String, Effect> effects() {
		requireLoaded();
		return definedEffects != null ? definedEffects : NO_EFFECTS;
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
				if (sd instanceof InitializationFunction)
					continue;
				if (sd instanceof Function && !seesFunction(((Function)sd)))
					continue;
					all.add(sd);
			}
		return all.toArray(new INode[all.size()]);
	}

	public void exportAsXML(Writer writer) throws IOException {
		requireLoaded();
		writer.write("<script>\n"); //$NON-NLS-1$
		writer.write("\t<functions>\n"); //$NON-NLS-1$
		for (final Function f : functions()) {
			writer.write(String.format("\t\t<function name=\"%s\" return=\"%s\">\n", f.name(), f.returnType().typeName(true))); //$NON-NLS-1$
			writer.write("\t\t\t<parameters>\n"); //$NON-NLS-1$
			for (final Variable p : f.parameters())
				writer.write(String.format("\t\t\t\t<parameter name=\"%s\" type=\"%s\" />\n", p.name(), p.type().typeName(true))); //$NON-NLS-1$
			writer.write("\t\t\t</parameters>\n"); //$NON-NLS-1$
			if (f.obtainUserDescription() != null) {
				writer.write("\t\t\t<description>"); //$NON-NLS-1$
				writer.write(f.obtainUserDescription());
				writer.write("</description>\n"); //$NON-NLS-1$
			}
			writer.write("\t\t</function>\n"); //$NON-NLS-1$
		}
		writer.write("\t</functions>\n"); //$NON-NLS-1$
		writer.write("\t<variables>\n"); //$NON-NLS-1$
		for (final Variable v : variables()) {
			writer.write(String.format("\t\t<variable name=\"%s\" type=\"%s\" const=\"%s\">\n", v.name(), v.type().typeName(true), Boolean.valueOf(v.scope() == Scope.CONST))); //$NON-NLS-1$
			if (v.obtainUserDescription() != null) {
				writer.write("\t\t\t<description>\n"); //$NON-NLS-1$
				writer.write("\t\t\t\t"+v.obtainUserDescription()+"\n"); //$NON-NLS-1$ //$NON-NLS-2$
				writer.write("\t\t\t</description>\n"); //$NON-NLS-1$
			}
			writer.write("\t\t</variable>\n"); //$NON-NLS-1$
		}
		writer.write("\t</variables>\n"); //$NON-NLS-1$
		writer.write("</script>\n"); //$NON-NLS-1$
	}

	public void importFromXML(InputStream stream, IProgressMonitor monitor) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {

		final XPathFactory xpathF = XPathFactory.newInstance();
		final XPath xPath = xpathF.newXPath();

		final DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		final Document doc = builder.parse(stream);

		final NodeList functions = (NodeList) xPath.evaluate("./functions/function", doc.getFirstChild(), XPathConstants.NODESET); //$NON-NLS-1$
		final NodeList variables = (NodeList) xPath.evaluate("./variables/variable", doc.getFirstChild(), XPathConstants.NODESET); //$NON-NLS-1$
		monitor.beginTask(Messages.ImportingEngineFromXML, functions.getLength()+variables.getLength());
		for (int i = 0; i < functions.getLength(); i++) {
			final Node function = functions.item(i);
			final NodeList parms = (NodeList) xPath.evaluate("./parameters/parameter", function, XPathConstants.NODESET); //$NON-NLS-1$
			final Variable[] p = new Variable[parms.getLength()];
			for (int j = 0; j < p.length; j++)
				p[j] = new Variable(parms.item(j).getAttributes().getNamedItem("name").getNodeValue(), PrimitiveType.fromString(parms.item(j).getAttributes().getNamedItem("type").getNodeValue(), true)); //$NON-NLS-1$ //$NON-NLS-2$
			final Function f = new Function(function.getAttributes().getNamedItem("name").getNodeValue(), PrimitiveType.fromString(function.getAttributes().getNamedItem("return").getNodeValue(), true), p); //$NON-NLS-1$ //$NON-NLS-2$
			final Node desc = (Node) xPath.evaluate("./description[1]", function, XPathConstants.NODE); //$NON-NLS-1$
			if (desc != null)
				f.setUserDescription(desc.getTextContent());
			this.addDeclaration(f);
			monitor.worked(1);
		}
		for (int i = 0; i < variables.getLength(); i++) {
			final Node variable = variables.item(i);
			final Variable v = new Variable(variable.getAttributes().getNamedItem("name").getNodeValue(), PrimitiveType.fromString(variable.getAttributes().getNamedItem("type").getNodeValue(), true)); //$NON-NLS-1$ //$NON-NLS-2$
			v.setScope(variable.getAttributes().getNamedItem("const").getNodeValue().equals(Boolean.TRUE.toString()) ? Scope.CONST : Scope.STATIC); //$NON-NLS-1$
			final Node desc = (Node) xPath.evaluate("./description[1]", variable, XPathConstants.NODE); //$NON-NLS-1$
			if (desc != null)
				v.setUserDescription(desc.getTextContent());
			this.addDeclaration(v);
			monitor.worked(1);
		}
		monitor.done();
	}

	private String sourceComment;
	public String sourceComment() { return sourceComment; }
	public void setSourceComment(String s) { sourceComment = s; }

	@Override
	public String infoText(IIndexEntity context) {
		//requireLoaded();
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

	/**
	 * Return script the passed type is associated with (or is literally)
	 * @param type Type to return a script from
	 * @return Associated script or null, if type is some primitive type or what have you
	 */
	public static Script scriptFrom(IType type) { return as(type, Script.class); }

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
	public Object valueForVariable(AccessVar access) {
		if (access.predecessorInSequence() == null)
			return findLocalVariable(access.name(), true);
		else
			return null;
	}
	@Override
	public int codeFragmentOffset() { return 0; }
	@Override
	public Object cookie() { return null; }

	/**
	 * Return whether this function is accessible from this script - that is, it belongs to this script or one included by it and is
	 * not overridden.
	 * @param function
	 * @return
	 */
	public final boolean seesFunction(Function function) {
		if (cachedFunctionMap != null) {
			final Function mine = cachedFunctionMap.get(function.name());
			return mine == null || mine == function;
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

	private static final IASTVisitor<Script> NODEMAPS_POPULATOR = new IASTVisitor<Script>() {
		@Override
		public TraversalContinuation visitNode(ASTNode node, Script script) {
			if (node instanceof CallDeclaration) {
				final CallDeclaration call = (CallDeclaration)node;
				List<CallDeclaration> list = script.callMap.get(call.name());
				if (list == null)
					script.callMap.put(call.name(), list = new ArrayList<>(3));
				list.add(call);
			}
			else if (node instanceof AccessVar) {
				final AccessVar var = (AccessVar)node;
				List<AccessVar> list = script.varReferencesMap.get(var.name());
				if (list == null)
					script.varReferencesMap.put(var.name(), list = new ArrayList<>(3));
				list.add(var);
			}
			return TraversalContinuation.Continue;
		}
	};

	public void generateCaches() {
		final List<Script> conglo = this.conglomerate();
		Collections.reverse(conglo);
		populateDictionary(conglo);
		generateFindDeclarationCache(conglo);
		callMap = new HashMap<>();
		varReferencesMap = new HashMap<>();
		if (definedFunctions != null && index() != null)
			for (final Function f : definedFunctions) {
				f.findInherited();
				detectMapNodesInFunction(f, false);
			}
	}
	
	private void generateFindDeclarationCache(final List<Script> conglo) {
		cachedFunctionMap = new HashMap<>();
		cachedVariableMap = new HashMap<>();
		for (final Script i : conglo)
			if (i instanceof Script) {
				final Script s = i;
				if (s.definedFunctions != null)
					for (final Function f1 : s.definedFunctions) {
						// prefer putting non-global functions into the map so when in doubt the object function is picked
						// for cases where one script defines two functions with same name that differ in their globality (Power.ocd)
						final Function existing = cachedFunctionMap.get(f1.name());
						if (existing != null && existing.script() == i && f1.isGlobal() && !existing.isGlobal())
							continue;
						cachedFunctionMap.put(f1.name(), f1);
					}
				if (s.definedVariables != null)
					for (final Variable v : s.definedVariables)
						cachedVariableMap.put(v.name(), v);
			}
	}

	private static <T extends ASTNode> void clearNodes(ASTNode container, Map<String, List<T>> map) {
		for (final Iterator<List<T>> it = map.values().iterator(); it.hasNext();) {
			final List<T> list = it.next();
			for (final Iterator<T> it2 = list.iterator(); it2.hasNext();)
				if (it2.next().containedIn(container))
					it2.remove();
			if (list.size() == 0)
				it.remove();
		}
	}

	public void detectMapNodesInFunction(Function function, boolean clearOld) {
		synchronized (callMap) {
			if (clearOld) {
				clearNodes(function, callMap);
				clearNodes(function, varReferencesMap);
			}
			function.traverse(NODEMAPS_POPULATOR, this);
		}
	}

	@Override
	public void postLoad(Declaration parent, Index root) {
		super.postLoad(parent, root);
		generateCaches();
		indexRefresh();
	}

	@Override
	public String qualifiedName() {
		if (resource() == null)
			return this.toString();
		else
			return resource().getProjectRelativePath().toOSString();
	}

	public void indexRefresh() {
		if (loaded) {
			final IResource res = resource();
			scenario = res != null ? Scenario.containingScenario(res) : null;
			detectEffects();
		}
	}

	/**
	 * Return the {@link Scenario} the {@link Script} is contained in.
	 */
	@Override
	public Scenario scenario() {
		return scenario;
	}

	public void saveNodes(final Collection<? extends ASTNode> expressions, final boolean absoluteLocations) {
		Core.instance().performActionsOnFileDocument(scriptFile(), new IDocumentAction<Boolean>() {
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
			result = script().findLocalVariable(varName, true);
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
			function.locals().add(result);
			break;
		case CONST: case STATIC: case LOCAL:
			result.setParent(script());
			script().addDeclaration(result);
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

	@Override
	public ASTNode[] subElements() {
		final List<Declaration> decs = subDeclarations(index(), DeclMask.FUNCTIONS|DeclMask.VARIABLES|DeclMask.DIRECTIVES);
		return decs.toArray(new ASTNode[decs.size()]);
	}
}