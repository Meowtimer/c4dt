package net.arctics.clonk.parser.c4script;

import static net.arctics.clonk.util.ArrayUtil.arrayIterable;
import static net.arctics.clonk.util.ArrayUtil.copyListOrReturnDefaultList;
import static net.arctics.clonk.util.ArrayUtil.filteredIterable;
import static net.arctics.clonk.util.ArrayUtil.purgeNullEntries;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.IndexEntity;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.ID;
import net.arctics.clonk.parser.IHasIncludes;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.parser.c4script.Directive.DirectiveType;
import net.arctics.clonk.parser.c4script.Variable.Scope;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.evaluate.IEvaluationContext;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.util.CompoundIterable;
import net.arctics.clonk.util.INode;
import net.arctics.clonk.util.ITreeNode;
import net.arctics.clonk.util.StreamUtil;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
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
public abstract class Script extends IndexEntity implements ITreeNode, IHasConstraint, IType, IEvaluationContext, IHasIncludes {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	protected transient List<Function> definedFunctions;
	protected transient List<Variable> definedVariables;
	protected transient List<Directive> definedDirectives;
	
	// set of scripts this script is using functions and/or static variables from
	private Set<Script> usedScripts;
	// scripts dependent on this one inside the same index
	private transient Set<Script> dependentScripts;
	
	private transient Map<String, Function> cachedFunctionMap;
	private transient Map<String, Variable> cachedVariableMap;
	private transient Collection<? extends IHasIncludes> includes;
	private Set<String> dictionary;
	
	public Set<String> dictionary() {
		return dictionary;
	}
	
	public Collection<? extends Script> usedScripts() {
		return copyListOrReturnDefaultList(usedScripts, NO_SCRIPTS);
	}
	
	/**
	 * Flag hinting that this script contains global functions/static variables. This will flag will be consulted to decide whether to fully load the script when looking for global declarations.
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
		definedDirectives = (List<Directive>) stream.readObject();
		usedScripts = (Set<Script>) stream.readObject();
		purgeNullEntries(definedFunctions, definedVariables, definedDirectives, usedScripts);
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
		stream.writeObject(definedDirectives);
		stream.writeObject(usedScripts);
		populateDictionary();
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
		for (Directive d : this.directives()) {
			if (d.type() == DirectiveType.STRICT) {
				try {
					level = Math.max(level, Integer.parseInt(d.contents()));
				}
				catch (NumberFormatException e) {
					if (level < 1)
						level = 1;
				}
			}
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
		for (Directive d : directives()) {
			if (d.type() == DirectiveType.INCLUDE || d.type() == DirectiveType.APPENDTO) {
				result.add(d);
			}
		}
		return result.toArray(new Directive[result.size()]);
	}

	/**
	 * Tries to gather all of the script's includes (including appendtos in the case of object scripts)
	 * @param set The list to be filled with the includes
	 * @param index The project index to search for includes in (has greater priority than EXTERN_INDEX which is always searched)
	 */
	@Override
	public boolean gatherIncludes(Index contextIndex, List<IHasIncludes> set, int options) {
		requireLoaded();
		if (set.contains(this))
			return false;
		else
			set.add(this);
		if (definedDirectives != null) synchronized(definedDirectives) {
			for (Directive d : definedDirectives) {
				if (d.type() == DirectiveType.INCLUDE || (d.type() == DirectiveType.APPENDTO && (options & GatherIncludesOptions.NoAppendages) == 0)) {
					ID id = d.contentAsID();
					for (Index in : contextIndex.relevantIndexes()) {
						Iterable<? extends Definition> defs = in.definitionsWithID(id);
						if (defs != null) {
							for (Definition def : defs) {
								if ((options & GatherIncludesOptions.Recursive) == 0)
									set.add(def);
								else {
									if (d.type() == DirectiveType.INCLUDE)
										options &= ~GatherIncludesOptions.NoAppendages;
									def.gatherIncludes(contextIndex, set, options);
								}
							}
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
			return includes(index(), options);
	}
	
	/**
	 * Does the same as gatherIncludes except that the user does not have to create their own list
	 * @param index The index to be passed to gatherIncludes
	 * @param recursive Whether the returned collection also contains includes of the includes.
	 * @return The includes
	 */
	@Override
	public Collection<? extends IHasIncludes> includes(Index index, int options) {
		requireLoaded();
		if (includes != null && (options & GatherIncludesOptions.Recursive) == 0 && index == this.index())
			return includes;
		else
			return IHasIncludes.Default.includes(index, this, options);
	}

	public Iterable<Script> dependentScripts() {
		requireLoaded();
		if (dependentScripts == null)
			return arrayIterable();
		else
			return dependentScripts;
	}
	
	public void clearDependentScripts() {
		dependentScripts = null;
	}
	
	public void addDependentScript(Script s) {
		requireLoaded();
		if (dependentScripts == null)
			dependentScripts = new HashSet<Script>();
		dependentScripts.add(s);
	}

	/**
	 * Returns an include directive that includes a specific {@link Definition}'s script
	 * @param obj The {@link Definition} to return a corresponding {@link Directive} for
	 * @return The {@link Directive} or null if no matching directive exists in this script.
	 */
	public Directive directiveIncludingDefinition(Definition obj) {
		requireLoaded();
		for (Directive d : includeDirectives()) {
			if ((d.type() == DirectiveType.INCLUDE || d.type() == DirectiveType.APPENDTO) && nearestDefinitionWithId(d.contentAsID()) == obj)
				return d;
		}
		return null;
	}

	/**
	 * Finds a declaration in this script or an included one
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
		if (Variable.class.isAssignableFrom(declarationClass)) {
			for (Variable v : variables()) {
				if (v.name().equals(declarationName))
					return v;
			}
		}
		if (Function.class.isAssignableFrom(declarationClass)) {
			for (Function f : functions()) {
				if (f.name().equals(declarationName))
					return f;
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
	protected Declaration representingDeclaration(String name, FindDeclarationInfo info) {
		return null;
	}

	@Override
	public Iterable<Declaration> subDeclarations(Index contextIndex, int mask) {
		requireLoaded();
		List<Iterable<? extends Declaration>> its = new ArrayList<Iterable<? extends Declaration>>(4);
		if ((mask & FUNCTIONS) != 0)
			its.add(functions());
		if ((mask & VARIABLES) != 0)
			its.add(variables());
		if ((mask & DIRECTIVES) != 0)
			its.add(directives());
		return new CompoundIterable<Declaration>(its);
	}

	/**
	 * Finds a declaration with the given name using information from the helper object
	 * @param name The name
	 * @param info Additional info
	 * @return the declaration or <tt>null</tt> if not found
	 */
	@Override
	public Declaration findDeclaration(String name, FindDeclarationInfo info) {
		requireLoaded();

		// prevent infinite recursion
		if (info.alreadySearched.contains(this))
			return null;
		info.alreadySearched.add(this);
		
		Class<? extends Declaration> decClass = info.declarationClass;

		// local variable?
		if (info.recursion == 0)
			if (info.contextFunction != null && (decClass == null || decClass == Variable.class)) {
				Declaration v = info.contextFunction.findVariable(name);
				if (v != null)
					return v;
			}
		
		// prefer using the cache
		boolean didUseCacheForLocalDeclarations = false;
		if ((cachedVariableMap != null || cachedFunctionMap != null) && info.index == this.index()) {
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

		// this object?
		Declaration thisDec = representingDeclaration(name, info);
		if (thisDec != null)
			return thisDec;

		if (!didUseCacheForLocalDeclarations) {

			// a function defined in this object
			if (decClass == null || decClass == Function.class)
				for (Function f : functions())
					if (f.name().equals(name))
						return f;
			// a variable
			if (decClass == null || decClass == Variable.class)
				for (Variable v : variables())
					if (v.name().equals(name))
						return v;

			info.recursion++;
			for (IHasIncludes o : includes(info.index, 0)) {
				Declaration result = o.findDeclaration(name, info);
				if (result != null)
					return result;
			}
			info.recursion--;
		}

		// finally look if it's something global
		if (info.recursion == 0 && !(this instanceof Engine)) { // .-.
			Declaration f = null;
			// prefer declarations from scripts that were previously determined to be the providers of global declarations
			// this will also probably and rightly lead to those scripts being fully loaded from their index file.
			if (usedScripts != null) {
				for (Script s : usedScripts()) {
					f = s.findDeclaration(name, info);
					if (f != null && f.isGlobal())
						return f;
				}
				f = null;
			}
			// definition from extern index
			if (info.findDefinitions && engine().acceptsId(name)) {
				f = info.index.definitionNearestTo(resource(), ID.get(name));
				if (f != null && info.declarationClass == Variable.class && f instanceof Definition) {
					f = ((Definition)f).proxyVar();
				}
			}
			// global stuff defined in project
			if (f == null) {
				for (Index index : info.index.relevantIndexes()) {
					f = index.findGlobalDeclaration(name, resource());
					if (f != null)
						break;
				}
			}
			// engine function
			if (f == null)
				f = index().engine().findDeclaration(name, info);

			if (f != null && (info.declarationClass == null || info.declarationClass.isAssignableFrom(f.getClass())))
				return f;
		}
		return null;
	}
	
	public void addDeclaration(Declaration declaration) {
		requireLoaded();
		declaration.setScript(this);
		if (declaration instanceof Function) {
			if (definedFunctions == null)
				definedFunctions = new ArrayList<Function>(5);
			synchronized (definedFunctions) {
				definedFunctions.add((Function)declaration);
			}
		}
		else if (declaration instanceof Variable) {
			if (definedVariables == null)
				definedVariables = new ArrayList<Variable>(5);
			synchronized (definedVariables) {
				definedVariables.add((Variable)declaration);
			}
		}
		else if (declaration instanceof Directive) {
			if (definedDirectives == null)
				definedDirectives = new ArrayList<Directive>(5);
			synchronized (definedDirectives) {
				definedDirectives.add((Directive)declaration);
			}
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
		else if (declaration instanceof Directive) {
			if (definedDirectives != null) synchronized (definedDirectives) {
				definedDirectives.remove(declaration);
			}
		}
	}

	public void clearDeclarations() {
		notFullyLoaded = false;
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
			int fStart = f.body().getOffset();
			int fEnd   = f.body().getOffset()+f.body().getLength();
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
			if (initialization != null && initialization.containsOffset(region.getOffset()))
				return v;
		}
		return null;
	}

	// OMG, IRegion <-> ITextSelection
	public Function funcAt(ITextSelection region) {
		requireLoaded();
		for (Function f : functions()) {
			if (f.location().getOffset() <= region.getOffset() && region.getOffset()+region.getLength() <= f.body().getOffset()+f.body().getLength())
				return f;
		}
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
		return findLocalVariable(name, includeIncludes, new HashSet<Script>());
	}

	public Function findLocalFunction(String name, boolean includeIncludes) {
		return findLocalFunction(name, includeIncludes, new HashSet<Script>());
	}

	public Function findLocalFunction(String name, boolean includeIncludes, HashSet<Script> alreadySearched) {
		requireLoaded();
		if (alreadySearched.contains(this))
			return null;
		alreadySearched.add(this);
		for (Function func: functions()) {
			if (func.name().equals(name))
				return func;
		}
		if (includeIncludes) {
			for (Script script : filteredIterable(includes(0), Script.class)) {
				Function func = script.findLocalFunction(name, includeIncludes, alreadySearched);
				if (func != null)
					return func;
			}
		}
		return null;
	}

	public Variable findLocalVariable(String name, boolean includeIncludes, HashSet<Script> alreadySearched) {
		requireLoaded();
		if (alreadySearched.contains(this))
			return null;
		alreadySearched.add(this);
		for (Variable var : variables()) {
			if (var.name().equals(name))
				return var;
		}
		if (includeIncludes) {
			for (Script script : filteredIterable(includes(0), Script.class)) {
				Variable var = script.findLocalVariable(name, includeIncludes, alreadySearched);
				if (var != null)
					return var;
			}
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

	/**
	 * Returns an iterator to iterate over all functions defined in this script
	 */
	public List<? extends Function> functions() {
		requireLoaded();
		return copyListOrReturnDefaultList(definedFunctions, NO_FUNCTIONS);
	}
	
	public <T extends Function> Iterable<T> functions(Class<T> cls) {
		requireLoaded();
		return filteredIterable(functions(), cls);
	}

	/**
	 * Returns an iterator to iterate over all variables defined in this script
	 */
	public List<? extends Variable> variables() {
		requireLoaded();
		return copyListOrReturnDefaultList(definedVariables, NO_VARIABLES);
	}

	/**
	 * Returns an iterator to iterate over all directives defined in this script
	 */
	public List<? extends Directive> directives() {
		requireLoaded();
		return copyListOrReturnDefaultList(definedDirectives, NO_DIRECTIVES);
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
		gatherIncludes(index(), s, GatherIncludesOptions.Recursive);
		return s;
	}

	@Override
	public INode[] subDeclarationsForOutline() {
		requireLoaded();
		List<Object> all = new LinkedList<Object>();
		for (IHasIncludes c : conglomerate()) {
			for (Declaration sd : c.subDeclarations(index(), FUNCTIONS|VARIABLES|(c==this?DIRECTIVES:0)))
				all.add(sd);
		}
		return all.toArray(new INode[all.size()]);
	}

	public void exportAsXML(Writer writer) throws IOException {
		requireLoaded();
		writer.write("<script>\n"); //$NON-NLS-1$
		writer.write("\t<functions>\n"); //$NON-NLS-1$
		for (Function f : functions()) {
			writer.write(String.format("\t\t<function name=\"%s\" return=\"%s\">\n", f.name(), f.returnType().typeName(true))); //$NON-NLS-1$
			writer.write("\t\t\t<parameters>\n"); //$NON-NLS-1$
			for (Variable p : f.parameters()) {
				writer.write(String.format("\t\t\t\t<parameter name=\"%s\" type=\"%s\" />\n", p.name(), p.type().typeName(true))); //$NON-NLS-1$
			}
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
			for (int j = 0; j < p.length; j++) {
				p[j] = new Variable(parms.item(j).getAttributes().getNamedItem("name").getNodeValue(), PrimitiveType.makeType(parms.item(j).getAttributes().getNamedItem("type").getNodeValue(), true)); //$NON-NLS-1$ //$NON-NLS-2$
			}
			Function f = new Function(function.getAttributes().getNamedItem("name").getNodeValue(), PrimitiveType.makeType(function.getAttributes().getNamedItem("return").getNodeValue(), true), p); //$NON-NLS-1$ //$NON-NLS-2$
			Node desc = (Node) xPath.evaluate("./description[1]", function, XPathConstants.NODE); //$NON-NLS-1$
			if (desc != null)
				f.setUserDescription(desc.getTextContent());
			this.addDeclaration(f);
			monitor.worked(1);
		}
		for (int i = 0; i < variables.getLength(); i++) {
			Node variable = variables.item(i);
			Variable v = new Variable(variable.getAttributes().getNamedItem("name").getNodeValue(), PrimitiveType.makeType(variable.getAttributes().getNamedItem("type").getNodeValue(), true)); //$NON-NLS-1$ //$NON-NLS-2$
			v.setScope(variable.getAttributes().getNamedItem("const").getNodeValue().equals(Boolean.TRUE.toString()) ? Scope.CONST : Scope.STATIC); //$NON-NLS-1$
			Node desc = (Node) xPath.evaluate("./description[1]", variable, XPathConstants.NODE); //$NON-NLS-1$
			if (desc != null)
				v.setUserDescription(desc.getTextContent());
			this.addDeclaration(v);
			monitor.worked(1);
		}
		monitor.done();
	}

	@Override
	public String infoText() {
		//requireLoaded();
		Object f = scriptStorage();
		if (f instanceof IFile) {
			IResource infoFile = Utilities.findMemberCaseInsensitively(((IFile)f).getParent(), "Desc"+ClonkPreferences.languagePref()+".txt"); //$NON-NLS-1$ //$NON-NLS-2$
			if (infoFile instanceof IFile) {
				try {
					return StreamUtil.stringFromFileDocument((IFile) infoFile);
				} catch (Exception e) {
					e.printStackTrace();
					return super.infoText();
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
	public boolean subNodeOf(ITreeNode node) {
		return ITreeNode.Default.subNodeOf(this, node);
	}

	@Override
	public IPath path() {
		return ITreeNode.Default.getPath(this);
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
		if (script == null) {
			// this does happen, for example when adding the script of some variable read from PlayerControls.txt
			// which is null
			return;
		}
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
		if (dependentScripts != null)
			dependentScripts.remove(script);
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
		try {
			script = SystemScript.pinnedScript(resource, false);
		} catch (CoreException e) {
			script = null;
		}
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
	public boolean containsType(IType type) {
		return
			type == PrimitiveType.OBJECT ||
			type == PrimitiveType.PROPLIST ||
			type == this ||
			(type instanceof ConstrainedProplist && this.doesInclude(this.index(), ((ConstrainedProplist)type).constraint())) ||
			type == PrimitiveType.ID; // gets rid of type sets <id or Clonk>
	}
	
	@Override
	public boolean containsAnyTypeOf(IType... types) {
		return IType.Default.containsAnyTypeOf(this, types);
	}

	@Override
	public int specificness() {
		return PrimitiveType.OBJECT.specificness()+3;
	}

	@Override
	public String typeName(boolean special) {
		return name();
	}

	@Override
	public Iterator<IType> iterator() {
		return arrayIterable(new IType[] {PrimitiveType.OBJECT, this}).iterator();
	}

	@Override
	public boolean intersects(IType typeSet) {
		for (IType t : typeSet) {
			if (t.canBeAssignedFrom(PrimitiveType.OBJECT))
				return true;
			if (t instanceof Definition) {
				Definition obj = (Definition) t;
				if (this.doesInclude(obj.index(), obj))
					return true;
			}
		}
		return false;
	}

	@Override
	public IType staticType() {
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
		for (IHasIncludes i : conglo) {
			if (i instanceof Script) {
				for (Function f : ((Script)i).functions())
					cachedFunctionMap.put(f.name(), f);
				for (Variable v : ((Script)i).variables())
					cachedVariableMap.put(v.name(), v);
			}
		}
	}
	
	public void generateFindDeclarationCache() {
		cachedFunctionMap = new HashMap<String, Function>();
		cachedVariableMap = new HashMap<String, Variable>();
		includes = null;
		includes = includes(0);
		_generateFindDeclarationCache();
	}
	
	@Override
	public void postLoad(Declaration parent, Index root) {
		super.postLoad(parent, root);
		generateFindDeclarationCache();
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

}