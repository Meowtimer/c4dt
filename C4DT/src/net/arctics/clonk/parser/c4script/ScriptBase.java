package net.arctics.clonk.parser.c4script;

import java.io.IOException;
import java.io.InputStream;
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

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.ProjectDefinition;
import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.ID;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.parser.c4script.Directive.C4DirectiveType;
import net.arctics.clonk.parser.c4script.Function.C4FunctionScope;
import net.arctics.clonk.parser.c4script.Variable.C4VariableScope;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
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
 * Subclasses include C4Object, C4StandaloneScript etc.
 */
public abstract class ScriptBase extends Structure implements ITreeNode, IHasConstraint {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	private static final Collection<ScriptBase> NO_INCLUDES = new ArrayList<ScriptBase>(0);

	protected List<Function> definedFunctions = new LinkedList<Function>();
	protected List<Variable> definedVariables = new LinkedList<Variable>();
	protected List<Directive> definedDirectives = new LinkedList<Directive>();
	
	// set of scripts this script is using functions and/or static variables from
	private Set<ScriptBase> usedProjectScripts;
	
	public String getScriptText() {
		return ""; //$NON-NLS-1$
	}
	
	public Function[] calculateLineToFunctionMap() {
		String scriptText = this.getScriptText();
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
				lineEnd = scanner.getPosition();
			}
			if (newLine) {
				region.setOffset(lineStart);
				region.setLength(lineEnd-lineStart);
				Function f = this.funcAt(region);
				if (f == null)
					f = this.funcAt(lineEnd);
				mappingAsList.add(f);
				lineStart = scanner.getPosition();
				lineEnd = lineStart;
			}
		}
		
		return mappingAsList.toArray(new Function[mappingAsList.size()]);
	}

	/**
	 * Returns the strict level of the script
	 * @return the #strict level set for this script or the default level supplied by the engine configuration
	 */
	public int getStrictLevel() {
		long level = getEngine() != null ? getEngine().getCurrentSettings().strictDefaultLevel : -1;
		for (Directive d : this.definedDirectives) {
			if (d.getType() == C4DirectiveType.STRICT) {
				try {
					level = Math.max(level, Integer.parseInt(d.getContent()));
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
	public Directive[] getIncludeDirectives() {
		List<Directive> result = new ArrayList<Directive>();
		for (Directive d : definedDirectives) {
			if (d.getType() == C4DirectiveType.INCLUDE || d.getType() == C4DirectiveType.APPENDTO) {
				result.add(d);
			}
		}
		return result.toArray(new Directive[result.size()]);
	}

	/**
	 * Tries to gather all of the script's includes (including appendtos in the case of object scripts)
	 * @param list The list to be filled with the includes
	 * @param index The project index to search for includes in (has greater priority than EXTERN_INDEX which is always searched)
	 */
	protected void gatherIncludes(List<ScriptBase> list, ClonkIndex index) {
		for (Directive d : definedDirectives) {
			if (d.getType() == C4DirectiveType.INCLUDE || d.getType() == C4DirectiveType.APPENDTO) {
				Definition obj = getNearestObjectWithId(d.contentAsID());
				if (obj != null)
					list.add(obj);
			}
		}
	}

	/**
	 * Does the same as gatherIncludes except that the user does not have to create their own list
	 * @param index The index to be passed to gatherIncludes
	 * @return The includes
	 */
	public Collection<ScriptBase> getIncludes(ClonkIndex index) {
		List<ScriptBase> result = new ArrayList<ScriptBase>();
		gatherIncludes(result, index);
		return result;
	}

	/**
	 * Does the same as gatherIncludes except that the user does not have to create their own list and does not even have to supply an index (defaulting to getIndex()) 
	 * @return The includes
	 */
	public final Collection<ScriptBase> getIncludes() {
		ClonkIndex index = getIndex();
		if (index == null)
			return NO_INCLUDES;
		return getIncludes(index);
	}

	/**
	 * Returns an include directive that include a specific object's script
	 * @param obj The object
	 * @return The directive 
	 */
	public Directive getIncludeDirectiveFor(Definition obj) {
		for (Directive d : getIncludeDirectives()) {
			if ((d.getType() == C4DirectiveType.INCLUDE || d.getType() == C4DirectiveType.APPENDTO) && getNearestObjectWithId(d.contentAsID()) == obj)
				return d;
		}
		return null;
	}

	/**
	 * Finds a declaration in this script or an included one
	 * @return The declaration or null if not found
	 */
	public Declaration findDeclaration(String name) {
		return findDeclaration(name, new FindDeclarationInfo(getIndex()));
	}

	@Override
	public Declaration findDeclaration(String declarationName, Class<? extends Declaration> declarationClass) {
		FindDeclarationInfo info = new FindDeclarationInfo(getIndex());
		info.setDeclarationClass(declarationClass);
		return findDeclaration(declarationName, info);
	}

	@Override
	public Declaration findLocalDeclaration(String declarationName, Class<? extends Declaration> declarationClass) {
		if (Variable.class.isAssignableFrom(declarationClass)) {
			for (Variable v : definedVariables) {
				if (v.getName().equals(declarationName))
					return v;
			}
		}
		if (Function.class.isAssignableFrom(declarationClass)) {
			for (Function f : definedFunctions) {
				if (f.getName().equals(declarationName))
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
	protected Declaration getThisDeclaration(String name, FindDeclarationInfo info) {
		return null;
	}

	/**
	 * Returns all declarations of this script (functions, variables and directives)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Iterable<Declaration> allSubDeclarations(int mask) {
		Iterable<?>[] its = new Iterable<?>[3];
		int fill = 0;
		if ((mask & FUNCTIONS) != 0)
			its[fill++] = definedFunctions;
		if ((mask & VARIABLES) != 0)
			its[fill++] = definedVariables;
		if ((mask & INCLUDES) != 0)
			its[fill++] = getIncludes();
		return new CompoundIterable<Declaration>(definedFunctions, definedVariables, definedDirectives);
	}

	/**
	 * Finds a declaration with the given name using information from the helper object
	 * @param name The name
	 * @param info Additional info
	 * @return the declaration or <tt>null</tt> if not found
	 */
	public Declaration findDeclaration(String name, FindDeclarationInfo info) {

		// prevent infinite recursion
		if (info.getAlreadySearched().contains(this))
			return null;
		info.getAlreadySearched().add(this);
		
		Class<? extends Declaration> decClass = info.getDeclarationClass();

		// local variable?
		if (info.recursion == 0) {
			if (info.getContextFunction() != null && (decClass == null || decClass == Variable.class)) {
				Declaration v = info.getContextFunction().findVariable(name);
				if (v != null)
					return v;
			}
		}

		// this object?
		Declaration thisDec = getThisDeclaration(name, info);
		if (thisDec != null) {
			return thisDec;
		}

		// a function defined in this object
		if (decClass == null || decClass == Function.class) {
			for (Function f : definedFunctions) {
				if (f.getName().equals(name))
					return f;
			}
		}
		// a variable
		if (decClass == null || decClass == Variable.class) {
			for (Variable v : definedVariables) {
				if (v.getName().equals(name))
					return v;
			}
		}

		// search in included definitions
		info.recursion++;
		for (ScriptBase o : getIncludes(info.index)) {
			Declaration result = o.findDeclaration(name, info);
			if (result != null)
				return result;
		}
		info.recursion--;

		// finally look if it's something global
		if (info.recursion == 0 && !(this instanceof Engine)) { // .-.
			Declaration f = null;
			// definition from extern index
			if (getEngine().acceptsId(name)) {
				f = info.index.getObjectNearestTo(getResource(), ID.getID(name));
				if (f != null && info.declarationClass == Variable.class && f instanceof ProjectDefinition) {
					f = ((ProjectDefinition)f).getStaticVariable();
				}
			}
			// global stuff defined in project
			if (f == null) {
				for (ClonkIndex index : info.getAllRelevantIndexes()) {
					f = index.findGlobalDeclaration(name, getResource());
					if (f != null)
						break;
				}
			}
			// engine function
			if (f == null)
				f = getIndex().getEngine().findDeclaration(name, info);

			if (f != null && (info.declarationClass == null || info.declarationClass.isAssignableFrom(f.getClass())))
				return f;
		}
		return null;
	}

	public void addDeclaration(Declaration field) {
		field.setScript(this);
		if (field instanceof Function) {
			definedFunctions.add((Function)field);
			//			for(IC4ObjectListener listener : changeListeners) {
			//				listener.fieldAdded(this, field);
			//			}
		}
		else if (field instanceof Variable) {
			definedVariables.add((Variable)field);
			//			for(IC4ObjectListener listener : changeListeners) {
			//				listener.fieldAdded(this, field);
			//			}
		}
		else if (field instanceof Directive) {
			definedDirectives.add((Directive)field);
		}
	}

	public void removeDeclaration(Declaration declaration) {
		if (declaration.getScript() != this) declaration.setScript(this);
		if (declaration instanceof Function) {
//			if (declaration.isGlobal())
//				getIndex().getGlobalFunctions().remove(declaration);
			definedFunctions.remove((Function)declaration);
		}
		else if (declaration instanceof Variable) {
//			if (declaration.isGlobal())
//				getIndex().getStaticVariables().remove(declaration);
			definedVariables.remove((Variable)declaration);
		}
	}

	public void clearDeclarations() {
		if (usedProjectScripts != null)
			usedProjectScripts = null;
		if (definedDirectives != null)
			definedDirectives.clear();
		if (definedFunctions != null)
			while (definedFunctions.size() > 0)
				removeDeclaration(definedFunctions.get(definedFunctions.size()-1));
		if (definedVariables != null)
			while (definedVariables.size() > 0)
				removeDeclaration(definedVariables.get(definedVariables.size()-1));
	}

	public void setName(String name) {
		this.name = name;
	}

	public abstract IStorage getScriptStorage();
	
	public final IFile getScriptFile() {
		IStorage storage = getScriptStorage();
		return storage instanceof IFile ? (IFile)storage : null;
	}

	@Override
	public ScriptBase getScript() {
		return this;
	}

	@Override
	public Structure getTopLevelStructure() {
		return this;
	}

	@Override
	public IResource getResource() {
		return null;
	}

	public Function findFunction(String functionName, FindDeclarationInfo info) {
		info.resetState();
		info.setDeclarationClass(Function.class);
		return (Function) findDeclaration(functionName, info);
	}

	public Function findFunction(String functionName) {
		FindDeclarationInfo info = new FindDeclarationInfo(getIndex());
		return findFunction(functionName, info);
	}

	public Variable findVariable(String varName) {
		FindDeclarationInfo info = new FindDeclarationInfo(getIndex());
		return findVariable(varName, info);
	}

	public Variable findVariable(String varName, FindDeclarationInfo info) {
		info.resetState();
		info.setDeclarationClass(Variable.class);
		return (Variable) findDeclaration(varName, info);
	}

	public Function funcAt(int offset) {
		return funcAt(new Region(offset, 1));
	}

	public Function funcAt(IRegion region) {
		for (Function f : definedFunctions) {
			int fStart = f.getBody().getOffset();
			int fEnd   = f.getBody().getOffset()+f.getBody().getLength();
			int rStart = region.getOffset();
			int rEnd   = region.getOffset()+region.getLength();
			if (rStart <= fStart && rEnd >= fEnd || rStart >= fStart && rStart <= fEnd || rEnd >= fEnd && rEnd <= fEnd)
				return f;
		}
		return null;
	}
	
	public Variable variableWithInitializationAt(IRegion region) {
		for (Variable v : definedVariables) {
			ExprElm initialization = v.getInitializationExpression();
			if (initialization != null && initialization.containsOffset(region.getOffset())) {
				return v;
			}
		}
		return null;
	}

	// OMG, IRegion <-> ITextSelection
	public Function funcAt(ITextSelection region) {
		for (Function f : definedFunctions) {
			if (f.getLocation().getOffset() <= region.getOffset() && region.getOffset()+region.getLength() <= f.getBody().getOffset()+f.getBody().getLength())
				return f;
		}
		return null;
	}

	public boolean includes(ScriptBase other) {
		return includes(other, new HashSet<ScriptBase>());
	}

	public boolean includes(ScriptBase other, Set<ScriptBase> dontRevisit) {
		if (other == this)
			return true;
		if (dontRevisit.contains(this))
			return false;
		dontRevisit.add(this);
		Iterable<ScriptBase> incs = this.getIncludes();
		for (ScriptBase o : incs) {
			if (o == other)
				return true;
			if (o.includes(other, dontRevisit))
				return true;
		}
		return false;
	}

	public abstract ClonkIndex getIndex();

	public Variable findLocalVariable(String name, boolean includeIncludes) {
		return findLocalVariable(name, includeIncludes, new HashSet<ScriptBase>());
	}

	public Function findLocalFunction(String name, boolean includeIncludes) {
		return findLocalFunction(name, includeIncludes, new HashSet<ScriptBase>());
	}

	public Function findLocalFunction(String name, boolean includeIncludes, HashSet<ScriptBase> alreadySearched) {
		if (alreadySearched.contains(this))
			return null;
		alreadySearched.add(this);
		for (Function func: definedFunctions) {
			if (func.getName().equals(name))
				return func;
		}
		if (includeIncludes) {
			for (ScriptBase script : getIncludes()) {
				Function func = script.findLocalFunction(name, includeIncludes, alreadySearched);
				if (func != null)
					return func;
			}
		}
		return null;
	}

	public Variable findLocalVariable(String name, boolean includeIncludes, HashSet<ScriptBase> alreadySearched) {
		if (alreadySearched.contains(this))
			return null;
		alreadySearched.add(this);
		for (Variable var : definedVariables) {
			if (var.getName().equals(name))
				return var;
		}
		if (includeIncludes) {
			for (ScriptBase script : getIncludes()) {
				Variable var = script.findLocalVariable(name, includeIncludes, alreadySearched);
				if (var != null)
					return var;
			}
		}
		return null;
	}

	public boolean removeDuplicateVariables() {
		Map<String, Variable> variableMap = new HashMap<String, Variable>();
		Collection<Variable> toBeRemoved = new LinkedList<Variable>();
		for (Variable v : definedVariables) {
			Variable inHash = variableMap.get(v.getName());
			if (inHash != null)
				toBeRemoved.add(v);
			else
				variableMap.put(v.getName(), v);
		}
		for (Variable v : toBeRemoved)
			definedVariables.remove(v);
		return toBeRemoved.size() > 0;
	}

	/**
	 * Returns an iterator to iterate over all functions defined in this script
	 */
	public Iterable<Function> functions() {
		return Collections.unmodifiableList(definedFunctions);
	}

	/**
	 * Returns an iterator to iterate over all variables defined in this script
	 */
	public Iterable<Variable> variables() {
		return Collections.unmodifiableList(definedVariables);
	}

	/**
	 * Returns an iterator to iterate over all directives defined in this script
	 */
	public Iterable<Directive> directives() {
		return Collections.unmodifiableList(definedDirectives);
	}

	public int numVariables() {
		return definedVariables.size();
	}

	public int numFunctions() {
		return definedFunctions.size();
	}

	public Definition getNearestObjectWithId(ID id) {
		ClonkIndex index = getIndex();
		if (index != null)
			return index.getObjectNearestTo(getResource(), id);
		return null;
	}

	/**
	 * Returns an iterator that can be used to iterate over all scripts that are included by this script plus the script itself.
	 * @return the Iterable
	 */
	public Iterable<ScriptBase> conglomerate() {
		return conglomerate(this.getIndex());
	}

	/**
	 * Returns an iterator that can be used to iterate over all scripts that are included by this script plus the script itself.
	 * @param index index used to look for includes
	 * @return the Iterable
	 */
	public Iterable<ScriptBase> conglomerate(final ClonkIndex index) {
		final ScriptBase thisScript = this;
		return new Iterable<ScriptBase>() {
			void gather(ScriptBase script, List<ScriptBase> list, Set<ScriptBase> duplicatesCatcher) {
				if (duplicatesCatcher.contains(script))
					return;
				duplicatesCatcher.add(script);
				list.add(script);
				for (ScriptBase s : script.getIncludes(index)) {
					gather(s, list, duplicatesCatcher);
				}
			}
			public Iterator<ScriptBase> iterator() {
				List<ScriptBase> list = new LinkedList<ScriptBase>();
				Set<ScriptBase> catcher = new HashSet<ScriptBase>();
				gather(thisScript, list, catcher);
				return list.iterator();
			}
		};
	}

	@Override
	public INode[] getSubDeclarationsForOutline() {
		List<Object> all = new LinkedList<Object>();
		all.addAll(definedFunctions);
		all.addAll(definedVariables);
		return all.toArray(new INode[all.size()]);
	}

	@Override
	public boolean hasSubDeclarationsInOutline() {
		return definedFunctions.size() > 0 || definedVariables.size() > 0;
	}

	private boolean dirty;

	@Override
	public void setDirty(boolean d) {
		dirty = d;
	}

	@Override
	public boolean dirty() {
		return dirty;
	}

	public void exportAsXML(Writer writer) throws IOException {
		writer.write("<script>\n"); //$NON-NLS-1$
		writer.write("\t<functions>\n"); //$NON-NLS-1$
		for (Function f : functions()) {
			writer.write(String.format("\t\t<function name=\"%s\" return=\"%s\">\n", f.getName(), f.getReturnType().typeName(true))); //$NON-NLS-1$
			writer.write("\t\t\t<parameters>\n"); //$NON-NLS-1$
			for (Variable p : f.getParameters()) {
				writer.write(String.format("\t\t\t\t<parameter name=\"%s\" type=\"%s\" />\n", p.getName(), p.getType().typeName(true))); //$NON-NLS-1$
			}
			writer.write("\t\t\t</parameters>\n"); //$NON-NLS-1$
			if (f.getUserDescription() != null) {
				writer.write("\t\t\t<description>"); //$NON-NLS-1$
				writer.write(f.getUserDescription());
				writer.write("</description>\n"); //$NON-NLS-1$
			}
			writer.write("\t\t</function>\n"); //$NON-NLS-1$
		}
		writer.write("\t</functions>\n"); //$NON-NLS-1$
		writer.write("\t<variables>\n"); //$NON-NLS-1$
		for (Variable v : variables()) {
			writer.write(String.format("\t\t<variable name=\"%s\" type=\"%s\" const=\"%s\">\n", v.getName(), v.getType().typeName(true), Boolean.valueOf(v.getScope() == C4VariableScope.CONST))); //$NON-NLS-1$
			if (v.getUserDescription() != null) {
				writer.write("\t\t\t<description>\n"); //$NON-NLS-1$
				writer.write("\t\t\t\t"+v.getUserDescription()+"\n"); //$NON-NLS-1$ //$NON-NLS-2$
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
			v.setScope(variable.getAttributes().getNamedItem("const").getNodeValue().equals(Boolean.TRUE.toString()) ? C4VariableScope.CONST : C4VariableScope.STATIC); //$NON-NLS-1$
			Node desc = (Node) xPath.evaluate("./description[1]", variable, XPathConstants.NODE); //$NON-NLS-1$
			if (desc != null)
				v.setUserDescription(desc.getTextContent());
			this.addDeclaration(v);
			monitor.worked(1);
		}
		monitor.done();
	}

	@Override
	public String getInfoText() {
		Object f = getScriptStorage();
		if (f instanceof IFile) {
			IResource infoFile = Utilities.findMemberCaseInsensitively(((IFile)f).getParent(), "Desc"+ClonkPreferences.getLanguagePref()+".txt"); //$NON-NLS-1$ //$NON-NLS-2$
			if (infoFile instanceof IFile) {
				try {
					return StreamUtil.stringFromFileDocument((IFile) infoFile);
				} catch (Exception e) {
					e.printStackTrace();
					return super.getInfoText();
				}
			}
		}
		return super.getInfoText();
	}

	public ITreeNode getParentNode() {
		return getParentDeclaration() instanceof ITreeNode ? (ITreeNode)getParentDeclaration() : null;
	}

	public boolean subNodeOf(ITreeNode node) {
		return ITreeNode.Default.subNodeOf(this, node);
	}

	public IPath getPath() {
		return ITreeNode.Default.getPath(this);
	}

	@SuppressWarnings("unchecked")
	public Collection<? extends INode> getChildCollection() {
		return Utilities.collectionFromArray(LinkedList.class, getSubDeclarationsForOutline());
	}

	public void addChild(ITreeNode node) {
		if (node instanceof Declaration)
			addDeclaration((Declaration)node);
	}

	public boolean containsGlobals() {
	    for (Function f : this.definedFunctions)
	    	if (f.getVisibility() == C4FunctionScope.GLOBAL)
	    		return true;
	    for (Variable v : this.definedVariables)
	    	if (v.getScope() == C4VariableScope.STATIC)
	    		return true;
	    return false;
    }
	
	public void addUsedProjectScript(ScriptBase script) {
		if (script.getIndex() == this.getIndex()) {
			((usedProjectScripts == null) ? (usedProjectScripts = new HashSet<ScriptBase>()) : usedProjectScripts).add(script);
		}
	}
	
	public boolean usedProjectScript(ScriptBase script) {
		return usedProjectScripts != null && usedProjectScripts.contains(script);
	}

	//	public boolean removeDWording() {
	//		boolean result = false;
	//		for (C4Function f : functions()) {
	//			if (f.getReturnType() == C4Type.DWORD) {
	//				f.setReturnType(C4Type.INT);
	//				result = true;
	//			}
	//			for (C4Variable parm : f.getParameters()) {
	//				if (parm.getType() == C4Type.DWORD) {
	//					parm.setType(C4Type.INT);
	//					result = true;
	//				}
	//			}
	//		}
	//		return result;
	//	}

	//	public boolean convertFuncsToConstsIfTheyLookLikeConsts() {
	//	boolean didSomething = false;
	//	List<C4Function> toBeRemoved = new LinkedList<C4Function>();
	//	for (C4Function f : definedFunctions) {
	//		if (f.getParameters().size() == 0 && looksLikeConstName(f.getName())) {
	//			toBeRemoved.add(f);
	//			definedVariables.add(new C4Variable(f.getName(), f.getReturnType(), f.getUserDescription(), C4VariableScope.VAR_CONST));
	//			didSomething = true;
	//		}
	//	}
	//	for (C4Variable v : definedVariables) {
	//		if (v.getScope() != C4VariableScope.VAR_CONST) {
	//			v.setScope(C4VariableScope.VAR_CONST);
	//			didSomething = true;
	//		}
	//		if (v.getScript() != this) {
	//			v.setScript(this);
	//			didSomething = true;
	//		}
	//	}
	//	for (C4Function f : toBeRemoved)
	//		definedFunctions.remove(f);
	//	C4Variable v = findLocalVariable("_inherited", false);
	//	if (v != null) {
	//		definedVariables.remove(v);
	//		definedFunctions.add(new C4Function("_inherited", this, C4FunctionScope.FUNC_PUBLIC));
	//		didSomething = true;
	//	}
	//	didSomething |= removeDuplicateVariables();
	//	return didSomething;
	//}

	//public void addFuncsFromList(String file) throws IOException {
	//Reader r = new FileReader(file);
	//LineNumberReader lReader = new LineNumberReader(r);
	//String funcName, type;
	//for (funcName = lReader.readLine(); funcName != null; funcName = type) {
	//	C4Function func = findLocalFunction(funcName, false);
	//	C4Type retType = null;
	//	List<C4Variable> parms = new LinkedList<C4Variable>();
	//	int numParms = 0;
	//	while ((type = lReader.readLine()) != null) {
	//		C4Type t = type.equals("any") ? C4Type.ANY : C4Type.makeType(type);
	//		if (t == C4Type.UNKNOWN)
	//			break;
	//		if (retType == null) {
	//			retType = t;
	//			continue;
	//		}
	//		parms.add(new C4Variable("par"+numParms++, t));
	//	}
	//	if (func == null && ClonkCore.getDefault().EXTERN_INDEX.findGlobalFunction(funcName) == null) {
	//		func = new C4Function(funcName, retType, parms.toArray(new C4Variable[numParms]));
	//		addField(func);
	//	}
	//}
	//}
	
	/**
	 * notification sent by the index when a script is removed
	 */
	public void scriptRemovedFromIndex(ScriptBase script) {
		if (usedProjectScripts != null)
			usedProjectScripts.remove(script);
	}
	
	@Override
	public Engine getEngine() {
		return getIndex().getEngine();
	}
	
	public static ScriptBase get(IResource resource, boolean onlyForScriptFile) {
		ScriptBase script;
		if (resource == null)
			return null;
		try {
			script = StandaloneProjectScript.pinnedScript(resource, false);
		} catch (CoreException e) {
			script = null;
		}
		if (script == null)
			script = ProjectDefinition.objectCorrespondingTo(resource.getParent());
		// there can only be one script oO (not ScriptDE or something)
		if (onlyForScriptFile && (script == null || script.getScriptStorage() == null || !script.getScriptStorage().equals(resource)))
			return null;
		return script;
	}
	
	@Override
	public ScriptBase constraintScript() {
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
	public static ScriptBase scriptFrom(IType type) {
		if (type instanceof IHasConstraint)
			return ((IHasConstraint)type).constraintScript();
		else
			return null;
	}

}