package net.arctics.clonk.parser.c4script;

import static net.arctics.clonk.util.ArrayUtil.addAllSynchronized;
import static net.arctics.clonk.util.ArrayUtil.copyListOrReturnDefaultList;
import static net.arctics.clonk.util.ArrayUtil.filteredIterable;
import static net.arctics.clonk.util.ArrayUtil.iterable;
import static net.arctics.clonk.util.ArrayUtil.purgeNullEntries;
import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.filter;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.IndexEntity;
import net.arctics.clonk.index.Scenario;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.ID;
import net.arctics.clonk.parser.IHasIncludes;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.parser.c4script.Directive.DirectiveType;
import net.arctics.clonk.parser.c4script.Variable.Scope;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.evaluate.IEvaluationContext;
import net.arctics.clonk.parser.c4script.effect.Effect;
import net.arctics.clonk.parser.c4script.effect.EffectFunction;
import net.arctics.clonk.parser.c4script.statictyping.TypeAnnotation;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.util.INode;
import net.arctics.clonk.util.IPredicate;
import net.arctics.clonk.util.ITreeNode;
import net.arctics.clonk.util.StreamUtil;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
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
public abstract class Script extends IndexEntity implements ITreeNode, IHasConstraint, IRefinedPrimitiveType, IEvaluationContext, IHasIncludes {

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
	private transient Collection<? extends IHasIncludes> includes;
	private transient Scenario scenario;
	
	private Set<String> dictionary;
	
	private List<TypeAnnotation> typeAnnotations;
	
	public List<TypeAnnotation> typeAnnotations() {
		return typeAnnotations;
	}

	public void setTypeAnnotations(List<TypeAnnotation> typeAnnotations) {
		this.typeAnnotations = typeAnnotations;
	}

	/**
	 * The script's dictionary contains names of variables and functions defined in it.
	 * It can be queried before {@link #requireLoaded()} was called, enabling one to look before-hand whether the script contains
	 * a declaration with some name.
	 * @return The dictionary
	 */
	public Set<String> dictionary() {
		return dictionary;
	}
	
	/**
	 * Return list of scripts used by this one. A script is considered to be using another one if it calls a global function or accesses a static variable defined in the other script.
	 * Kept and managed to make reparsing a script using global declarations from some other script work without requiring a reload of all scripts in the index. 
	 * @return The list of used scripts
	 */
	public Collection<? extends Script> usedScripts() {
		return copyListOrReturnDefaultList(usedScripts, NO_SCRIPTS);
	}
	
	/**
	 * Flag hinting that this script contains global functions/static variables. This flag will be consulted to decide whether to fully load the script when looking for global declarations.
	 */
	public boolean containsGlobals;
	
	protected Script(Index index) {
		super(index);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void load(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		super.load(stream);
		definedFunctions = (List<Function>) stream.readObject();
		definedVariables = (List<Variable>) stream.readObject();
		usedScripts = (Set<Script>) stream.readObject();
		try {
			definedEffects = (Map<String, Effect>)stream.readObject();
		} catch (Exception e) {
			// that's ok
		}
		purgeNullEntries(definedFunctions, definedVariables, usedScripts);
		// also load scripts this script uses global declarations from so they will be present when the script gets parsed
		try {
			if (usedScripts != null)
				for (Script s : usedScripts)
					if (s != null)
						s.requireLoaded();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void save(ObjectOutputStream stream) throws IOException {
		super.save(stream);
		stream.writeObject(definedFunctions);
		stream.writeObject(definedVariables);
		stream.writeObject(usedScripts);
		stream.writeObject(definedEffects);
		populateDictionary();
	}
	
	private static int nextCamelBack(String s, int offset) {
		for (++offset; offset < s.length() && !Character.isUpperCase(s.charAt(offset)); offset++);
		return offset;
	}
	
	public void detectEffects() {
		List<EffectFunction> allEffectFunctions = new ArrayList<EffectFunction>(10);
		for (EffectFunction f : functions(EffectFunction.class))
			allEffectFunctions.add(f);
		while (allEffectFunctions.size() > 0) {
			String s = allEffectFunctions.get(0).name().substring(EffectFunction.FUNCTION_NAME_PREFIX.length());
			List<EffectFunction> effectCandidates = null;
			String effectName = null;
			for (int i = nextCamelBack(s, 0); i < s.length(); i = nextCamelBack(s, i)) {
				final String sub = s.substring(0, i);
				List<EffectFunction> matching = filter(allEffectFunctions, new IPredicate<EffectFunction>() {
					@Override
					public boolean test(EffectFunction item) {
						try {
							return item.name().substring(EffectFunction.FUNCTION_NAME_PREFIX.length(), EffectFunction.FUNCTION_NAME_PREFIX.length()+sub.length()).equals(sub);
						} catch (StringIndexOutOfBoundsException e) {
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
				Effect effect = new Effect(effectName, effectCandidates);
				effect.setParentDeclaration(this);
				definedEffects.put(effectName, effect);
			}
		}
	}

	protected void populateDictionary() {
		if (dictionary != null)
			dictionary.clear();
		else
			dictionary = new HashSet<String>();
		for (Declaration d : accessibleDeclarations(ALL))
			dictionary.add(d.name());
	}
	
	public String scriptText() {
		return ""; //$NON-NLS-1$
	}
	
	/**
	 * Return an array that acts as a map line number -> function at that line. Used for fast function lookups when only the line number is known.
	 * @return The pseudo-map for getting the function at some line.
	 */
	public Function[] calculateLineToFunctionMap() {
		requireLoaded();
		String scriptText = this.scriptText();
		int lineStart = 0;
		int lineEnd = 0;
		List<Function> mappingAsList = new LinkedList<Function>();
		MutableRegion region = new MutableRegion(0, 0);
		for (BufferedScanner scanner = new BufferedScanner(scriptText); !scanner.reachedEOF();) {
			int read = scanner.read();
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
		for (Directive d : this.directives())
			if (d.type() == DirectiveType.STRICT)
				try {
					level = Math.max(level, Integer.parseInt(d.contents()));
				}
				catch (NumberFormatException e) {
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
		List<Directive> result = new ArrayList<Directive>();
		for (Directive d : directives())
			if (d.type() == DirectiveType.INCLUDE || d.type() == DirectiveType.APPENDTO)
				result.add(d);
		return result.toArray(new Directive[result.size()]);
	}

	/**
	 * Tries to gather all of the script's includes (including appendtos in the case of object scripts)
	 * @param set The list to be filled with the includes
	 * @param index The project index to search for includes in (has greater priority than EXTERN_INDEX which is always searched)
	 */
	@Override
	public boolean gatherIncludes(Index contextIndex, IHasIncludes origin, List<IHasIncludes> set, int options) {
		if (set.contains(this))
			return false;
		else
			set.add(this);
		if (definedDirectives != null) {
			List<Directive> directivesCopy;
			synchronized(definedDirectives) {
				directivesCopy = new ArrayList<Directive>(definedDirectives);
			}
			for (Directive d : directivesCopy) {
				if (d == null)
					continue;
				if (d.type() == DirectiveType.INCLUDE || (d.type() == DirectiveType.APPENDTO && (options & GatherIncludesOptions.NoAppendages) == 0)) {
					ID id = d.contentAsID();
					for (Index in : contextIndex.relevantIndexes()) {
						Iterable<? extends Definition> defs = in.definitionsWithID(id);
						if (defs != null)
							for (Definition def : defs)
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
	public Collection<? extends IHasIncludes> includes(int options) {
		if (index() == null)
			return NO_INCLUDES;
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
	public Collection<? extends IHasIncludes> includes(Index index, IHasIncludes origin, int options) {
		synchronized (this) {
			int indexHash = index != null ? index.hashCode() : 0;
			int originHash = origin != null ? origin.hashCode() : 0;
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
		for (Directive d : directives())
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
		for (Directive d : includeDirectives())
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
		FindDeclarationInfo info = new FindDeclarationInfo(index());
		info.declarationClass = declarationClass;
		return findDeclaration(declarationName, info);
	}

	@Override
	public Declaration findLocalDeclaration(String declarationName, Class<? extends Declaration> declarationClass) {
		requireLoaded();
		if (declarationClass.isAssignableFrom(Variable.class))
			for (Variable v : variables())
				if (v.name().equals(declarationName))
					return v;
		if (declarationClass.isAssignableFrom(Function.class))
			for (Function f : functions())
				if (f.name().equals(declarationName))
					return f;
		if (declarationClass.isAssignableFrom(Effect.class)) {
			Effect e = effects().get(declarationName);
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
	public Iterable<Declaration> subDeclarations(Index contextIndex, int mask) {
		requireLoaded();
		ArrayList<Declaration> decs = new ArrayList<Declaration>();
		if ((mask & FUNCTIONS) != 0)
			addAllSynchronized(definedFunctions, decs);
		if ((mask & VARIABLES) != 0)
			addAllSynchronized(definedVariables, decs);
		if ((mask & DIRECTIVES) != 0)
			addAllSynchronized(definedDirectives, decs);
		return decs;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends Declaration> T latestVersionOf(T from) {
		int mask =
			from instanceof Variable ? VARIABLES :
			from instanceof Function ? FUNCTIONS :
			from instanceof Directive ? DIRECTIVES : 0;
		T candidate = null;
		boolean locationMatch = false;
		for (Declaration d : subDeclarations(this.index(), mask)) {
			if (d == from)
				return (T)d;
			if (d.name().equals(from.name())) {
				boolean newLocationMatch = Utilities.objectsEqual(d.location(), from.location());
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
		
		Class<? extends Declaration> decClass = info.declarationClass;

		// local variable?
		if (info.recursion == 0)
			if (info.contextFunction != null && (decClass == Declaration.class || decClass == null || decClass == Variable.class)) {
				Declaration v = info.contextFunction.findVariable(name);
				if (v != null)
					return v;
			}

		
		boolean knows = dictionary() == null || dictionary().contains(name);
		boolean didUseCacheForLocalDeclarations = false;
		if (knows) {
			// prefer using the cache
			requireLoaded();
			if ((cachedVariableMap != null || cachedFunctionMap != null) && info.index == this.index() && (info.searchOrigin == null || scenario() == info.searchOrigin.scenario())) {
				if (cachedVariableMap != null && (decClass == null || decClass == Variable.class)) {
					Declaration d = cachedVariableMap.get(name);
					if (d != null)
						return d;
					didUseCacheForLocalDeclarations = true;
				}
				if (cachedFunctionMap != null && (decClass == null || decClass == Function.class)) {
					Declaration d = cachedFunctionMap.get(name);
					if (d != null)
						return d;
					didUseCacheForLocalDeclarations = true;
				}
			}
		}

		// this object?
		Declaration thisDec = representingDeclaration(name, info);
		if (thisDec != null)
			return thisDec;

		if (!didUseCacheForLocalDeclarations) {
			if (knows) {
				requireLoaded();
				// a function defined in this object
				if (decClass == null || decClass == Function.class) {
					Function f = definedDeclarationNamed(name, Function.class);
					if (f != null)
						return f;
				}
				// a variable
				if (decClass == null || decClass == Variable.class) {
					Variable v = definedDeclarationNamed(name, Variable.class);
					if (v != null)
						return v;
				}
			}
			
			info.recursion++;
			{
				for (IHasIncludes o : includes(info.index, info.searchOrigin, 0)) {
					Declaration result = o.findDeclaration(name, info);
					if (result != null)
						return result;
				}
			}
			info.recursion--;
		}
		
		// finally look if it's something global
		if (info.recursion == 0 && info.index != null) {
			info.recursion++;
			Declaration f = findGlobalDeclaration(name, info);
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
			for (Script s : usedScripts()) {
				Declaration f = s.findDeclaration(name, info);
				if (f != null && f.isGlobal())
					return f;
			}
		
		if (info.findGlobalVariables && engine().acceptsId(name)) {
			Definition d = info.index.definitionNearestTo(resource(), ID.get(name));
			if (d != null)
				if (info.declarationClass == Variable.class)
					return d.proxyVar();
				else
					return d;
			if (name.equals(Scenario.PROPLIST_NAME)) {
				Scenario scenario = Scenario.nearestScenario(this.resource());
				if (scenario != null && scenario.propList() != null)
					return scenario.propList();
			}
			else if (name.equals(Index.GLOBAL_PROPLIST_NAME) && index().global() != null)
				return index().global();
		}

		// global stuff defined in relevant projects
		for (Index index : info.index.relevantIndexes()) {
			Declaration f = index.findGlobalDeclaration(name, resource());
			if (f != null && (info.findGlobalVariables || !(f instanceof Variable)))
				return f;
		}
		
		// engine function
		return index().engine().findDeclaration(name, info);
	}
	
	public void addDeclaration(Declaration declaration) {
		requireLoaded();
		declaration.setScript(this);
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
	}

	public void removeDeclaration(Declaration declaration) {
		requireLoaded();
		if (declaration.script() != this)
			declaration.setScript(this);
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

	@Override
	public void setName(String name) {
		this.name = name;
	}

	public abstract IStorage scriptStorage();
	
	public final IFile scriptFile() {
		IStorage storage = scriptStorage();
		return storage instanceof IFile ? (IFile)storage : null;
	}

	@Override
	public Script script() {
		return this;
	}

	@Override
	public Structure topLevelStructure() {
		return this;
	}

	@Override
	public IResource resource() {
		return null;
	}

	public Function findFunction(String functionName, FindDeclarationInfo info) {
		info.resetState();
		info.declarationClass = Function.class;
		return (Function) findDeclaration(functionName, info);
	}

	@Override
	public Function findFunction(String functionName) {
		FindDeclarationInfo info = new FindDeclarationInfo(index());
		return findFunction(functionName, info);
	}

	public Variable findVariable(String varName) {
		FindDeclarationInfo info = new FindDeclarationInfo(index());
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
		//System.out.println("");
		requireLoaded();
		for (Function f : functions()) {
			int fStart = f.bodyLocation().getOffset();
			int fEnd   = f.bodyLocation().getOffset()+f.bodyLocation().getLength();
			int rStart = region.getOffset();
			int rEnd   = region.getOffset()+region.getLength();
			//System.out.println(String.format("Shit: %d %d", fStart, fEnd));
			if (rStart <= fStart && rEnd >= fEnd || rStart >= fStart && rStart <= fEnd || rEnd >= fEnd && rEnd <= fEnd)
				return f;
		}
		return null;
	}
	
	public Variable variableWithInitializationAt(IRegion region) {
		requireLoaded();
		for (Variable v : variables()) {
			ExprElm initialization = v.initializationExpression();
			if (initialization != null) {
				Function owningFunc = as(initialization.owningDeclaration(), Function.class);
				SourceLocation loc = owningFunc != null ? owningFunc.bodyLocation().add(initialization) : initialization;
				if (loc.containsOffset(region.getOffset()))
					return v;
			}
		}
		return null;
	}

	// OMG, IRegion <-> ITextSelection
	public Function funcAt(ITextSelection region) {
		requireLoaded();
		for (Function f : functions())
			if (f.location().getOffset() <= region.getOffset() && region.getOffset()+region.getLength() <= f.bodyLocation().getOffset()+f.bodyLocation().getLength())
				return f;
		return null;
	}

	/**
	 * Return whether this script includes another one.
	 * @param other The other script
	 * @return True if this script includes the other one, false if not.
	 */
	@Override
	public boolean doesInclude(Index contextIndex, IHasIncludes other) {
		requireLoaded();
		if (other == this)
			return true;
		Iterable<? extends IHasIncludes> incs = this.includes(0);
		for (IHasIncludes o : incs)
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
		if (alreadySearched != null) {
			if (alreadySearched.contains(this))
				return null;
			alreadySearched.add(this);
		}
		for (Function func: functions())
			if (func.name().equals(name))
				return func;
		if (alreadySearched != null)
			for (Script script : filteredIterable(includes(0), Script.class)) {
				Function func = script.findLocalFunction(name, alreadySearched);
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
		for (Variable var : variables())
			if (var.name().equals(name))
				return var;
		if (alreadySearched != null)
			for (Script script : filteredIterable(includes(0), Script.class)) {
				Variable var = script.findLocalVariable(name, alreadySearched);
				if (var != null)
					return var;
			}
		return null;
	}

	public boolean removeDuplicateVariables() {
		requireLoaded();
		Map<String, Variable> variableMap = new HashMap<String, Variable>();
		Collection<Variable> toBeRemoved = new LinkedList<Variable>();
		for (Variable v : variables()) {
			Variable inHash = variableMap.get(v.name());
			if (inHash != null)
				toBeRemoved.add(v);
			else
				variableMap.put(v.name(), v);
		}
		for (Variable v : toBeRemoved)
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
				for (T f : list)
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
		return definedDirectives != null ? definedDirectives : NO_DIRECTIVES;
	}
	
	/**
	 * Return a map mapping effect name to {@link Effect} object
	 * @return The map
	 */
	public Map<String, Effect> effects() {
		return definedEffects != null ? definedEffects : NO_EFFECTS;
	}

	public Definition nearestDefinitionWithId(ID id) {
		Index index = index();
		if (index != null)
			return index.definitionNearestTo(resource(), id);
		return null;
	}

	/**
	 * Returns a list containing all scripts that are included by this script plus the script itself.
	 * @return The list
	 */
	public List<IHasIncludes> conglomerate() {
		requireLoaded();
		List<IHasIncludes> s = new ArrayList<IHasIncludes>(10);
		gatherIncludes(index(), this, s, GatherIncludesOptions.Recursive);
		return s;
	}

	@Override
	public INode[] subDeclarationsForOutline() {
		requireLoaded();
		List<Object> all = new LinkedList<Object>();
		for (IHasIncludes c : conglomerate())
			for (Declaration sd : c.subDeclarations(index(), FUNCTIONS|VARIABLES|(c==this?DIRECTIVES:0)))
				all.add(sd);
		return all.toArray(new INode[all.size()]);
	}

	public void exportAsXML(Writer writer) throws IOException {
		requireLoaded();
		writer.write("<script>\n"); //$NON-NLS-1$
		writer.write("\t<functions>\n"); //$NON-NLS-1$
		for (Function f : functions()) {
			writer.write(String.format("\t\t<function name=\"%s\" return=\"%s\">\n", f.name(), f.returnType().typeName(true))); //$NON-NLS-1$
			writer.write("\t\t\t<parameters>\n"); //$NON-NLS-1$
			for (Variable p : f.parameters())
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
		for (Variable v : variables()) {
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

		XPathFactory xpathF = XPathFactory.newInstance();
		XPath xPath = xpathF.newXPath();

		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc = builder.parse(stream);

		NodeList functions = (NodeList) xPath.evaluate("./functions/function", doc.getFirstChild(), XPathConstants.NODESET); //$NON-NLS-1$
		NodeList variables = (NodeList) xPath.evaluate("./variables/variable", doc.getFirstChild(), XPathConstants.NODESET); //$NON-NLS-1$
		monitor.beginTask(Messages.ImportingEngineFromXML, functions.getLength()+variables.getLength());
		for (int i = 0; i < functions.getLength(); i++) {
			Node function = functions.item(i);
			NodeList parms = (NodeList) xPath.evaluate("./parameters/parameter", function, XPathConstants.NODESET); //$NON-NLS-1$
			Variable[] p = new Variable[parms.getLength()];
			for (int j = 0; j < p.length; j++)
				p[j] = new Variable(parms.item(j).getAttributes().getNamedItem("name").getNodeValue(), PrimitiveType.fromString(parms.item(j).getAttributes().getNamedItem("type").getNodeValue(), true)); //$NON-NLS-1$ //$NON-NLS-2$
			Function f = new Function(function.getAttributes().getNamedItem("name").getNodeValue(), PrimitiveType.fromString(function.getAttributes().getNamedItem("return").getNodeValue(), true), p); //$NON-NLS-1$ //$NON-NLS-2$
			Node desc = (Node) xPath.evaluate("./description[1]", function, XPathConstants.NODE); //$NON-NLS-1$
			if (desc != null)
				f.setUserDescription(desc.getTextContent());
			this.addDeclaration(f);
			monitor.worked(1);
		}
		for (int i = 0; i < variables.getLength(); i++) {
			Node variable = variables.item(i);
			Variable v = new Variable(variable.getAttributes().getNamedItem("name").getNodeValue(), PrimitiveType.fromString(variable.getAttributes().getNamedItem("type").getNodeValue(), true)); //$NON-NLS-1$ //$NON-NLS-2$
			v.setScope(variable.getAttributes().getNamedItem("const").getNodeValue().equals(Boolean.TRUE.toString()) ? Scope.CONST : Scope.STATIC); //$NON-NLS-1$
			Node desc = (Node) xPath.evaluate("./description[1]", variable, XPathConstants.NODE); //$NON-NLS-1$
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
		Object f = scriptStorage();
		if (f instanceof IFile) {
			IResource infoFile = Utilities.findMemberCaseInsensitively(((IFile)f).getParent(), "Desc"+ClonkPreferences.languagePref()+".txt"); //$NON-NLS-1$ //$NON-NLS-2$
			if (infoFile instanceof IFile)
				try {
					return StreamUtil.stringFromFileDocument((IFile) infoFile);
				} catch (Exception e) {
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
		Index index = index();
		return index != null ? index.engine() : null;
	}
	
	public static Script get(IResource resource, boolean onlyForScriptFile) {
		Script script;
		if (resource == null)
			return null;
		script = SystemScript.pinned(resource, false);
		if (script == null)
			script = Definition.definitionCorrespondingToFolder(resource.getParent());
		// there can only be one script oO (not ScriptDE or something)
		if (onlyForScriptFile && (script == null || script.scriptStorage() == null || !script.scriptStorage().equals(resource)))
			return null;
		return script;
	}
	
	@Override
	public Script constraint() {
		return this;
	}
	
	@Override
	public ConstraintKind constraintKind() {
		return ConstraintKind.Exact;
	}
	
	/**
	 * Return script the passed type is associated with (or is literally)
	 * @param type Type to return a script from
	 * @return Associated script or null, if type is some primitive type or what have you
	 */
	public static Script scriptFrom(IType type) {
		if (type instanceof IHasConstraint)
			return Utilities.as(((IHasConstraint)type).constraint(), Script.class);
		else
			return null;
	}
	
	@Override
	public boolean canBeAssignedFrom(IType other) {
		return PrimitiveType.OBJECT.canBeAssignedFrom(other) || PrimitiveType.PROPLIST.canBeAssignedFrom(other);
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
	public IType simpleType() {
		return PrimitiveType.OBJECT;
	}
	
	@Override
	public Function function() {
		return null;
	}
	
	@Override
	public void reportOriginForExpression(ExprElm expression, IRegion location, IFile file) {
		// cool.
	}
	
	@Override
	public Object[] arguments() {
		return new Object[0];
	}
	
	@Override
	public Object valueForVariable(String varName) {
		return findLocalVariable(varName, true); // whatever
	}
	
	@Override
	public int codeFragmentOffset() {
		return 0;
	}
	
	@Override
	public void setTypeDescription(String description) {}
	
	@Override
	public IType resolve(DeclarationObtainmentContext context, IType callerType) {
		return this;
	}

	private void _generateFindDeclarationCache() {
		List<IHasIncludes> conglo = this.conglomerate();
		Collections.reverse(conglo);
		for (IHasIncludes i : conglo)
			if (i instanceof Script) {
				Script s = (Script)i;
				if (s.definedFunctions != null)
					for (Function f : s.definedFunctions) {
						// prefer putting non-global functions into the map so when in doubt the object function is picked
						// for cases where one script defines two functions with same name that differ in their globality (Power.ocd)
						Function existing = cachedFunctionMap.get(f.name());
						if (existing != null && existing.script() == i && f.isGlobal() && !existing.isGlobal())
							continue;
						cachedFunctionMap.put(f.name(), f);
					}
				if (s.definedVariables != null)
					for (Variable v : s.definedVariables)
						cachedVariableMap.put(v.name(), v);
			}
	}
	
	public void generateFindDeclarationCache() {
		//populateDictionary();
		cachedFunctionMap = new HashMap<String, Function>();
		cachedVariableMap = new HashMap<String, Variable>();
		_generateFindDeclarationCache();
		populateDictionary();
	}
	
	@Override
	public void postLoad(Declaration parent, Index root) {
		super.postLoad(parent, root);
		generateFindDeclarationCache();
		indexRefresh();
	}
	
	@Override
	public String qualifiedName() {
		if (resource() == null) {
			System.out.println("No qualified name: " + this.toString());
			return this.toString();
		}
		else
			return resource().getProjectRelativePath().toOSString();
	}
	
	public void indexRefresh() {
		if (loaded) {
			IResource res = resource();
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
	
	public void saveExpressions(final Collection<? extends ExprElm> expressions, final boolean absoluteLocations) {
		Core.instance().performActionsOnFileDocument(scriptFile(), new IDocumentAction<Boolean>() {
			@Override
			public Boolean run(IDocument document) {
				try {
					List<ExprElm> l = new ArrayList<ExprElm>(expressions);
					Collections.sort(l, new Comparator<ExprElm>() {
						@Override
						public int compare(ExprElm o1, ExprElm o2) {
							IRegion r1 = absoluteLocations ? o1.absolute() : o1;
							IRegion r2 = absoluteLocations ? o2.absolute() : o2;
							return r2.getOffset() - r1.getOffset();
						}
					});
					for (ExprElm e : l) {
						IRegion region = absoluteLocations ? e.absolute() : e;
						document.replace(region.getOffset(), region.getLength(), e.toString());
					}
					return true;
				} catch (BadLocationException e) {
					e.printStackTrace();
					return false;
				}
			}
		});
	}
	
	@Override
	public PrimitiveType primitiveType() {
		return PrimitiveType.OBJECT;
	}

	public void setScriptFile(IFile f) {}
	
	@Override
	public boolean isGlobal() { return true; }
}