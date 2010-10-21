package net.arctics.clonk.parser.c4script;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
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
import net.arctics.clonk.index.C4Engine;
import net.arctics.clonk.index.C4Object;
import net.arctics.clonk.index.C4ObjectIntern;
import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.C4ID;
import net.arctics.clonk.parser.C4Structure;
import net.arctics.clonk.parser.c4script.C4Directive.C4DirectiveType;
import net.arctics.clonk.parser.c4script.C4Function.C4FunctionScope;
import net.arctics.clonk.parser.c4script.C4Variable.C4VariableScope;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.stringtbl.StringTbl;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.util.CompoundIterable;
import net.arctics.clonk.util.INode;
import net.arctics.clonk.util.ITreeNode;
import net.arctics.clonk.util.ReadOnlyIterator;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
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
public abstract class C4ScriptBase extends C4Structure implements ITreeNode {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	private static final C4Object[] NO_INCLUDES = new C4Object[] {};

	protected List<C4Function> definedFunctions = new LinkedList<C4Function>();
	protected List<C4Variable> definedVariables = new LinkedList<C4Variable>();
	protected List<C4Directive> definedDirectives = new LinkedList<C4Directive>();
	
	// set of scripts this script is using functions and/or static variables from
	private Set<C4ScriptBase> usedProjectScripts;
	
	public String getScriptText() {
		return ""; //$NON-NLS-1$
	}
	
	public C4Function[] calculateLineToFunctionMap() {
		String scriptText = this.getScriptText();
		int line = 0;
		int lineStart = 0;
		int lineEnd = 0;
		List<C4Function> mappingAsList = new LinkedList<C4Function>();
		MutableRegion region = new MutableRegion(0, 0);
		for (BufferedScanner scanner = new BufferedScanner(scriptText); !scanner.reachedEOF();) {
			int read = scanner.read();
			boolean newLine = false;
			switch (read) {
			case '\r':
				line++;
				newLine = true;
				if (scanner.read() != '\n')
					scanner.unread();
				break;
			case '\n':
				line++;
				newLine = true;
				break;
			default:
				lineEnd = scanner.getPosition();
			}
			if (newLine) {
				region.setOffset(lineStart);
				region.setLength(lineEnd-lineStart);
				C4Function f = this.funcAt(region);
				if (f == null)
					f = this.funcAt(lineEnd);
				mappingAsList.add(f);
				lineStart = scanner.getPosition();
				lineEnd = lineStart;
			}
		}
		
		return mappingAsList.toArray(new C4Function[mappingAsList.size()]);
	}

	/**
	 * Returns the strict level of the script
	 * @return the #strict level set for this script or the default level supplied by the engine configuration
	 */
	public int getStrictLevel() {
		int level = getEngine() != null ? getEngine().getCurrentSettings().strictDefaultLevel : -1;
		for (C4Directive d : this.definedDirectives) {
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
		return level;
	}

	/**
	 * Returns \#include and \#appendto directives 
	 * @return The directives
	 */
	public C4Directive[] getIncludeDirectives() {
		List<C4Directive> result = new ArrayList<C4Directive>();
		for (C4Directive d : definedDirectives) {
			if (d.getType() == C4DirectiveType.INCLUDE || d.getType() == C4DirectiveType.APPENDTO) {
				result.add(d);
			}
		}
		return result.toArray(new C4Directive[result.size()]);
	}

	/**
	 * Tries to gather all of the script's includes (including appendtos in the case of object scripts)
	 * @param list The list to be filled with the includes
	 * @param index The project index to search for includes in (has greater priority than EXTERN_INDEX which is always searched)
	 */
	protected void gatherIncludes(List<C4ScriptBase> list, ClonkIndex index) {
		for (C4Directive d : definedDirectives) {
			if (d.getType() == C4DirectiveType.INCLUDE || d.getType() == C4DirectiveType.APPENDTO) {
				C4Object obj = getNearestObjectWithId(d.contentAsID());
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
	public C4ScriptBase[] getIncludes(ClonkIndex index) {
		List<C4ScriptBase> result = new ArrayList<C4ScriptBase>();
		gatherIncludes(result, index);
		return result.toArray(new C4ScriptBase[result.size()]);
	}

	/**
	 * Does the same as gatherIncludes except that the user does not have to create their own list and does not even have to supply an index (defaulting to getIndex()) 
	 * @return The includes
	 */
	public final C4ScriptBase[] getIncludes() {
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
	public C4Directive getIncludeDirectiveFor(C4Object obj) {
		for (C4Directive d : getIncludeDirectives()) {
			if ((d.getType() == C4DirectiveType.INCLUDE || d.getType() == C4DirectiveType.APPENDTO) && getNearestObjectWithId(d.contentAsID()) == obj)
				return d;
		}
		return null;
	}

	/**
	 * Finds a declaration in this script or an included one
	 * @return The declaration or null if not found
	 */
	public C4Declaration findDeclaration(String name) {
		return findDeclaration(name, new FindDeclarationInfo(getIndex()));
	}

	@Override
	public C4Declaration findDeclaration(String declarationName, Class<? extends C4Declaration> declarationClass) {
		FindDeclarationInfo info = new FindDeclarationInfo(getIndex());
		info.setDeclarationClass(declarationClass);
		return findDeclaration(declarationName, info);
	}

	@Override
	public C4Declaration findLocalDeclaration(String declarationName, Class<? extends C4Declaration> declarationClass) {
		if (declarationClass.isAssignableFrom(C4Variable.class)) {
			for (C4Variable v : definedVariables) {
				if (v.getName().equals(declarationName))
					return v;
			}
		}
		if (declarationClass.isAssignableFrom(C4Function.class)) {
			for (C4Function f : definedFunctions) {
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
	protected C4Declaration getThisDeclaration(String name, FindDeclarationInfo info) {
		return null;
	}

	/**
	 * Returns all declarations of this script (functions, variables and directives)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Iterable<C4Declaration> allSubDeclarations() {
		return new CompoundIterable<C4Declaration>(definedFunctions, definedVariables, definedDirectives);
	}

	/**
	 * Finds a declaration with the given name using information from the helper object
	 * @param name The name
	 * @param info Additional info
	 * @return the declaration or <tt>null</tt> if not found
	 */
	public C4Declaration findDeclaration(String name, FindDeclarationInfo info) {

		// prevent infinite recursion
		if (info.getAlreadySearched().contains(this))
			return null;
		info.getAlreadySearched().add(this);
		
		Class<? extends C4Declaration> decClass = info.getDeclarationClass();

		// local variable?
		if (info.recursion == 0) {
			if (info.getContextFunction() != null && (decClass == null || decClass == C4Variable.class)) {
				C4Declaration v = info.getContextFunction().findVariable(name);
				if (v != null)
					return v;
			}
		}

		// this object?
		C4Declaration thisDec = getThisDeclaration(name, info);
		if (thisDec != null) {
			return thisDec;
		}

		// a function defined in this object
		if (decClass == null || decClass == C4Function.class) {
			for (C4Function f : definedFunctions) {
				if (f.getName().equals(name))
					return f;
			}
		}
		// a variable
		if (decClass == null || decClass == C4Variable.class) {
			for (C4Variable v : definedVariables) {
				if (v.getName().equals(name))
					return v;
			}
		}

		// search in included definitions
		info.recursion++;
		for (C4ScriptBase o : getIncludes(info.index)) {
			C4Declaration result = o.findDeclaration(name, info);
			if (result != null)
				return result;
		}
		info.recursion--;

		// finally look if it's something global
		if (info.recursion == 0 && !(this instanceof C4Engine)) { // .-.
			C4Declaration f = null;
			// definition from extern index
			if (getEngine().acceptsId(name)) {
				f = info.index.getObjectNearestTo(getResource(), C4ID.getID(name));
				if (f != null && info.declarationClass == C4Variable.class && f instanceof C4ObjectIntern) {
					f = ((C4ObjectIntern)f).getStaticVariable();
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

	public void addDeclaration(C4Declaration field) {
		field.setScript(this);
		if (field instanceof C4Function) {
			definedFunctions.add((C4Function)field);
			//			for(IC4ObjectListener listener : changeListeners) {
			//				listener.fieldAdded(this, field);
			//			}
		}
		else if (field instanceof C4Variable) {
			definedVariables.add((C4Variable)field);
			//			for(IC4ObjectListener listener : changeListeners) {
			//				listener.fieldAdded(this, field);
			//			}
		}
		else if (field instanceof C4Directive) {
			definedDirectives.add((C4Directive)field);
		}
	}

	public void removeDeclaration(C4Declaration declaration) {
		if (declaration.getScript() != this) declaration.setScript(this);
		if (declaration instanceof C4Function) {
			definedFunctions.remove((C4Function)declaration);
		}
		else if (declaration instanceof C4Variable) {
			definedVariables.remove((C4Variable)declaration);
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

	public abstract Object getScriptFile();

	@Override
	public C4ScriptBase getScript() {
		return this;
	}

	@Override
	public C4Structure getTopLevelStructure() {
		return this;
	}

	@Override
	public IResource getResource() {
		return null;
	}

	public C4Function findFunction(String functionName, FindDeclarationInfo info) {
		info.resetState();
		info.setDeclarationClass(C4Function.class);
		return (C4Function) findDeclaration(functionName, info);
	}

	public C4Function findFunction(String functionName) {
		FindDeclarationInfo info = new FindDeclarationInfo(getIndex());
		return findFunction(functionName, info);
	}

	public C4Variable findVariable(String varName) {
		FindDeclarationInfo info = new FindDeclarationInfo(getIndex());
		return findVariable(varName, info);
	}

	public C4Variable findVariable(String varName, FindDeclarationInfo info) {
		info.resetState();
		info.setDeclarationClass(C4Variable.class);
		return (C4Variable) findDeclaration(varName, info);
	}

	public C4Function funcAt(int offset) {
		return funcAt(new Region(offset, 1));
	}

	public C4Function funcAt(IRegion region) {
		for (C4Function f : definedFunctions) {
			int fStart = f.getBody().getOffset();
			int fEnd   = f.getBody().getOffset()+f.getBody().getLength();
			int rStart = region.getOffset();
			int rEnd   = region.getOffset()+region.getLength();
			if (rStart <= fStart && rEnd >= fEnd || rStart >= fStart && rStart <= fEnd || rEnd >= fEnd && rEnd <= fEnd)
				return f;
		}
		return null;
	}
	
	public C4Variable variableWithInitializationAt(IRegion region) {
		for (C4Variable v : definedVariables) {
			ExprElm initialization = v.getScriptScopeInitializationExpression();
			if (initialization != null && initialization.containsOffset(region.getOffset())) {
				return v;
			}
		}
		return null;
	}

	// OMG, IRegion <-> ITextSelection
	public C4Function funcAt(ITextSelection region) {
		for (C4Function f : definedFunctions) {
			if (f.getLocation().getOffset() <= region.getOffset() && region.getOffset()+region.getLength() <= f.getBody().getOffset()+f.getBody().getLength())
				return f;
		}
		return null;
	}

	public boolean includes(C4Object other) {
		return includes(other, new HashSet<C4ScriptBase>());
	}

	public boolean includes(C4Object other, Set<C4ScriptBase> dontRevisit) {
		if (dontRevisit.contains(this))
			return false;
		dontRevisit.add(this);
		C4ScriptBase[] incs = this.getIncludes();
		for (C4ScriptBase o : incs) {
			if (o == other)
				return true;
			if (o.includes(other, dontRevisit))
				return true;
		}
		return false;
	}

	public abstract ClonkIndex getIndex();

	public C4Variable findLocalVariable(String name, boolean includeIncludes) {
		return findLocalVariable(name, includeIncludes, new HashSet<C4ScriptBase>());
	}

	public C4Function findLocalFunction(String name, boolean includeIncludes) {
		return findLocalFunction(name, includeIncludes, new HashSet<C4ScriptBase>());
	}

	public C4Function findLocalFunction(String name, boolean includeIncludes, HashSet<C4ScriptBase> alreadySearched) {
		if (alreadySearched.contains(this))
			return null;
		alreadySearched.add(this);
		for (C4Function func: definedFunctions) {
			if (func.getName().equals(name))
				return func;
		}
		if (includeIncludes) {
			for (C4ScriptBase script : getIncludes()) {
				C4Function func = script.findLocalFunction(name, includeIncludes, alreadySearched);
				if (func != null)
					return func;
			}
		}
		return null;
	}

	public C4Variable findLocalVariable(String name, boolean includeIncludes, HashSet<C4ScriptBase> alreadySearched) {
		if (alreadySearched.contains(this))
			return null;
		alreadySearched.add(this);
		for (C4Variable var : definedVariables) {
			if (var.getName().equals(name))
				return var;
		}
		if (includeIncludes) {
			for (C4ScriptBase script : getIncludes()) {
				C4Variable var = script.findLocalVariable(name, includeIncludes, alreadySearched);
				if (var != null)
					return var;
			}
		}
		return null;
	}

	public boolean removeDuplicateVariables() {
		Map<String, C4Variable> variableMap = new HashMap<String, C4Variable>();
		Collection<C4Variable> toBeRemoved = new LinkedList<C4Variable>();
		for (C4Variable v : definedVariables) {
			C4Variable inHash = variableMap.get(v.getName());
			if (inHash != null)
				toBeRemoved.add(v);
			else
				variableMap.put(v.getName(), v);
		}
		for (C4Variable v : toBeRemoved)
			definedVariables.remove(v);
		return toBeRemoved.size() > 0;
	}

	/**
	 * Returns an iterator to iterate over all functions defined in this script
	 */
	public Iterable<C4Function> functions() {
		return new Iterable<C4Function>() {
			public Iterator<C4Function> iterator() {
				return new ReadOnlyIterator<C4Function>(definedFunctions.iterator());
			}
		};
	}

	/**
	 * Returns an iterator to iterate over all variables defined in this script
	 */
	public Iterable<C4Variable> variables() {
		return new Iterable<C4Variable>() {
			public Iterator<C4Variable> iterator() {
				return new ReadOnlyIterator<C4Variable>(definedVariables.iterator());
			}	
		};
	}

	/**
	 * Returns an iterator to iterate over all directives defined in this script
	 */
	public Iterable<C4Directive> directives() {
		return new Iterable<C4Directive>() {
			public Iterator<C4Directive> iterator() {
				return new ReadOnlyIterator<C4Directive>(definedDirectives.iterator());
			}	
		};
	}

	public int numVariables() {
		return definedVariables.size();
	}

	public int numFunctions() {
		return definedFunctions.size();
	}

	public C4Object getNearestObjectWithId(C4ID id) {
		ClonkIndex index = getIndex();
		if (index != null)
			return index.getObjectNearestTo(getResource(), id);
		return null;
	}

	/**
	 * Returns an iterator that can be used to iterate over all scripts that are included by this script plus the script itself.
	 * @return the Iterable
	 */
	public Iterable<C4ScriptBase> conglomerate() {
		return conglomerate(this.getIndex());
	}

	/**
	 * Returns an iterator that can be used to iterate over all scripts that are included by this script plus the script itself.
	 * @param index index used to look for includes
	 * @return the Iterable
	 */
	public Iterable<C4ScriptBase> conglomerate(final ClonkIndex index) {
		final C4ScriptBase thisScript = this;
		return new Iterable<C4ScriptBase>() {
			void gather(C4ScriptBase script, List<C4ScriptBase> list, Set<C4ScriptBase> duplicatesCatcher) {
				if (duplicatesCatcher.contains(script))
					return;
				duplicatesCatcher.add(script);
				list.add(script);
				for (C4ScriptBase s : script.getIncludes(index)) {
					gather(s, list, duplicatesCatcher);
				}
			}
			public Iterator<C4ScriptBase> iterator() {
				List<C4ScriptBase> list = new LinkedList<C4ScriptBase>();
				Set<C4ScriptBase> catcher = new HashSet<C4ScriptBase>();
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

	public void setDirty(boolean d) {
		dirty = d;
	}

	@Override
	public boolean dirty() {
		return dirty;
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

	public void exportAsXML(Writer writer) throws IOException {
		writer.write("<script>\n"); //$NON-NLS-1$
		writer.write("\t<functions>\n"); //$NON-NLS-1$
		for (C4Function f : functions()) {
			writer.write(String.format("\t\t<function name=\"%s\" return=\"%s\">\n", f.getName(), f.getReturnType().typeName(true))); //$NON-NLS-1$
			writer.write("\t\t\t<parameters>\n"); //$NON-NLS-1$
			for (C4Variable p : f.getParameters()) {
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
		for (C4Variable v : variables()) {
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
			C4Variable[] p = new C4Variable[parms.getLength()];
			for (int j = 0; j < p.length; j++) {
				p[j] = new C4Variable(parms.item(j).getAttributes().getNamedItem("name").getNodeValue(), C4Type.makeType(parms.item(j).getAttributes().getNamedItem("type").getNodeValue(), true)); //$NON-NLS-1$ //$NON-NLS-2$
			}
			C4Function f = new C4Function(function.getAttributes().getNamedItem("name").getNodeValue(), C4Type.makeType(function.getAttributes().getNamedItem("return").getNodeValue(), true), p); //$NON-NLS-1$ //$NON-NLS-2$
			Node desc = (Node) xPath.evaluate("./description[1]", function, XPathConstants.NODE); //$NON-NLS-1$
			if (desc != null)
				f.setUserDescription(desc.getTextContent());
			this.addDeclaration(f);
			monitor.worked(1);
		}
		for (int i = 0; i < variables.getLength(); i++) {
			Node variable = variables.item(i);
			C4Variable v = new C4Variable(variable.getAttributes().getNamedItem("name").getNodeValue(), C4Type.makeType(variable.getAttributes().getNamedItem("type").getNodeValue(), true)); //$NON-NLS-1$ //$NON-NLS-2$
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
		Object f = getScriptFile();
		if (f instanceof IFile) {
			IResource infoFile = Utilities.findMemberCaseInsensitively(((IFile)f).getParent(), "Desc"+ClonkPreferences.getLanguagePref()+".txt"); //$NON-NLS-1$ //$NON-NLS-2$
			if (infoFile instanceof IFile) {
				try {
					return Utilities.stringFromFileDocument((IFile) infoFile);
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
		if (node instanceof C4Declaration)
			addDeclaration((C4Declaration)node);
	}

	public boolean containsGlobals() {
	    for (C4Function f : this.definedFunctions)
	    	if (f.getVisibility() == C4FunctionScope.GLOBAL)
	    		return true;
	    for (C4Variable v : this.definedVariables)
	    	if (v.getScope() == C4VariableScope.STATIC)
	    		return true;
	    return false;
    }
	
	public void addUsedProjectScript(C4ScriptBase script) {
		if (script.getIndex() == this.getIndex()) {
			((usedProjectScripts == null) ? (usedProjectScripts = new HashSet<C4ScriptBase>()) : usedProjectScripts).add(script);
		}
	}
	
	public boolean usedProjectScript(C4ScriptBase script) {
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
	public void scriptRemovedFromIndex(C4ScriptBase script) {
		if (usedProjectScripts != null)
			usedProjectScripts.remove(script);
	}
	
	@Override
	public C4Engine getEngine() {
		return getIndex().getEngine();
	}
	
	public static C4ScriptBase get(IResource resource, boolean onlyForScriptFile) {
		C4ScriptBase script;
		if (resource == null)
			return null;
		try {
			script = C4ScriptIntern.pinnedScript(resource, false);
		} catch (CoreException e) {
			script = null;
		}
		if (script == null)
			script = C4ObjectIntern.objectCorrespondingTo(resource.getParent());
		// there can only be one script oO (not ScriptDE or something)
		if (onlyForScriptFile && (script == null || script.getScriptFile() == null || !script.getScriptFile().equals(resource)))
			return null;
		return script;
	}

}