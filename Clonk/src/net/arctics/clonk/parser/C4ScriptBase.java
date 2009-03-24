package net.arctics.clonk.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.C4Directive.C4DirectiveType;
import net.arctics.clonk.util.ReadOnlyIterator;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Region;

public abstract class C4ScriptBase extends C4Structure implements IRelatedResource {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final C4Object[] NO_INCLUDES = new C4Object[] {};

	protected List<C4Function> definedFunctions = new LinkedList<C4Function>();
	protected List<C4Variable> definedVariables = new ArrayList<C4Variable>(); // default capacity of 10 is ok
	protected List<C4Directive> definedDirectives = new ArrayList<C4Directive>(4); // mostly 4 are enough
	
	public int strictLevel() {
		int level = 0;
		for (C4Directive d : this.definedDirectives) {
			if (d.getType() == C4DirectiveType.STRICT) {
				try {
					level = Math.max(level, Integer.parseInt(d.getContent()));
				}
				catch (NumberFormatException e) {
					level = 1;
				}
			}
		}
		return level;
	}
	
	public C4Directive[] getIncludeDirectives() {
		List<C4Directive> result = new ArrayList<C4Directive>();
		for (C4Directive d : definedDirectives) {
			if (d.getType() == C4DirectiveType.INCLUDE || d.getType() == C4DirectiveType.APPENDTO) {
				result.add(d);
			}
		}
		return result.toArray(new C4Directive[result.size()]);
	}
	
	protected void gatherIncludes(List<C4ScriptBase> list, ClonkIndex index) {
		for (C4Directive d : definedDirectives) {
			if (d.getType() == C4DirectiveType.INCLUDE || d.getType() == C4DirectiveType.APPENDTO) {
				C4Object obj = getNearestObjectWithId(d.contentAsID());
				if (obj != null)
					list.add(obj);
			}
		}
	}
	
	public C4ScriptBase[] getIncludes(ClonkIndex index) {
		List<C4ScriptBase> result = new ArrayList<C4ScriptBase>();
		gatherIncludes(result, index);
		return result.toArray(new C4ScriptBase[result.size()]);
	}
	
	public C4Directive getIncludeDirectiveFor(C4Object obj) {
		for (C4Directive d : getIncludeDirectives()) {
			if ((d.getType() == C4DirectiveType.INCLUDE || d.getType() == C4DirectiveType.APPENDTO) && getNearestObjectWithId(d.contentAsID()) == obj)
				return d;
		}
		return null;
	}
	
	public C4ScriptBase[] getIncludes() {
		ClonkIndex index = getIndex();
		if (index == null)
			return NO_INCLUDES;
		return getIncludes(index);
	}
	
	public C4Field findField(String name) {
		return findField(name, new FindFieldInfo(getIndex()));
	}
	
	protected boolean refersToThis(String name, FindFieldInfo info) {
		return false;
	}
	
//	private static boolean resourceInsideContainer(IResource resource, IContainer container) {
//		for (IContainer c = resource.getParent(); c != null; c = c.getParent())
//			if (c.equals(container))
//				return true;
//		return false;
//	}
	
//	private static C4Object pickNearestObject(List<C4Object> objects, C4ScriptBase origin) {
//		if (!(origin.getScriptFile() instanceof IFile))
//			return objects.size() > 0 ? objects.get(0) : null;
//		IFile originFile = (IFile) origin.getScriptFile();
//		IContainer originContainer = originFile.getParent();
//		C4Object result = null;
//		int foldersDistance = 0;
//		for (C4Object o : objects) {
//			if (!(o.getScriptFile() instanceof IFile))
//				continue;
//			IContainer objContainer = ((IFile)o.getScriptFile()).getParent();
//			if (resou
//			int i;
//			for (i = )
//		}
//		return result;
//	}
	
	/**
	 * Finds field with specified name and infos
	 * @param name
	 * @param info
	 * @return the field or <tt>null</tt> if not found
	 */
	public C4Field findField(String name, FindFieldInfo info) {
		
		// prevent infinite recursion
		if (info.getAlreadySearched().contains(this))
			return null;
		info.getAlreadySearched().add(this);
		
		// local variable?
		if (info.recursion == 0) {
			if (info.getContextFunction() != null) {
				C4Field v = info.getContextFunction().findVariable(name);
				if (v != null)
					return v;
			}
		}
		
		// this object?
		if (refersToThis(name, info)) {
			return this;
		}
		
		// a function defined in this object
		if (info.getFieldClass() == null || info.getFieldClass() == C4Function.class) {
			for (C4Function f : definedFunctions) {
				if (f.getName().equals(name))
					return f;
			}
		}
		// a variable
		if (info.getFieldClass() == null || info.getFieldClass() == C4Variable.class) {
			for (C4Variable v : definedVariables) {
				if (v.getName().equals(name))
					return v;
			}
		}
		
		// search in included definitions
		info.recursion++;
		for (C4ScriptBase o : getIncludes(info.index)) {
			C4Field result = o.findField(name, info);
			if (result != null)
				return result;
		}
		info.recursion--;
		
		// finally look if it's something global
		if (info.recursion == 0 && this != ClonkCore.getDefault().ENGINE_OBJECT) { // .-.
			C4Field f = null;
			// definition from extern index
			if (Utilities.looksLikeID(name)) {
				f = info.index.getObjectNearestTo(getResource(), C4ID.getID(name));
			}
			// global stuff defined in project
			if (f == null)
				f = info.index.findGlobalField(name);
			// engine function
			if (f == null)
				f = ClonkCore.getDefault().ENGINE_OBJECT.findField(name, info);
			// function in extern lib
			if (f == null && info.index != ClonkCore.getDefault().EXTERN_INDEX) {
				f = ClonkCore.getDefault().EXTERN_INDEX.findGlobalField(name, getResource());
			}
			
			if (f != null && (info.fieldClass == null || info.fieldClass.isAssignableFrom(f.getClass())))
				return f;
		}
		return null;
	}
	
	public void addField(C4Field field) {
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
	
	public void removeField(C4Field field) {
		if (field.getScript() != this) field.setScript(this);
		if (field instanceof C4Function) {
			definedFunctions.remove((C4Function)field);
//			for(IC4ObjectListener listener : changeListeners) {
//				listener.fieldRemoved(this, field);
//			}
		}
		else if (field instanceof C4Variable) {
			definedVariables.remove((C4Variable)field);
//			for(IC4ObjectListener listener : changeListeners) {
//				listener.fieldRemoved(this, field);
//			}
		}
	}
	
	public void clearFields() {
		if (definedDirectives != null)
			definedDirectives.clear();
		if (definedFunctions != null)
		while (definedFunctions.size() > 0)
			removeField(definedFunctions.get(definedFunctions.size()-1));
		if (definedVariables != null)
		while (definedVariables.size() > 0)
			removeField(definedVariables.get(definedVariables.size()-1));
	}
	
	public void setName(String name) {
		this.name = name;
	}

//	public void addListener(IC4ObjectListener listener) {
//		changeListeners.add(listener);
//	}
//	
//	public void removeListener(IC4ObjectListener listener) {
//		changeListeners.remove(listener);
//	}
	
	public abstract Object getScriptFile();
	
	public IResource getResource() {
		return null;
	}

	public C4Function findFunction(String functionName, FindFieldInfo info) {
		info.resetState();
		info.setFieldClass(C4Function.class);
		return (C4Function) findField(functionName, info);
	}
	
	public C4Function findFunction(String functionName) {
		FindFieldInfo info = new FindFieldInfo(getIndex());
		return findFunction(functionName, info);
	}
	
	public C4Variable findVariable(String varName) {
		FindFieldInfo info = new FindFieldInfo(getIndex());
		return findVariable(varName, info);
	}
	
	public C4Variable findVariable(String varName, FindFieldInfo info) {
		info.resetState();
		info.setFieldClass(C4Variable.class);
		return (C4Variable) findField(varName, info);
	}

	public C4Function funcAt(int offset) {
		return funcAt(new Region(offset, 1));
	}
	
	public C4Function funcAt(IRegion region) {
		// from name to end of body should be enough... ?
		for (C4Function f : definedFunctions) {
			if (f.getLocation().getOffset() <= region.getOffset() && region.getOffset()+region.getLength() <= f.getBody().getOffset()+f.getBody().getLength())
				return f;
		}
		return null;
	}
	
	// OMG, IRegion <-> ITextSelection
	public C4Function funcAt(ITextSelection region) {
		// from name to end of body should be enough... ?
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
		return getIndex().getObjectNearestTo(getResource(), id);
	}
	
	public Iterable<C4ScriptBase> scriptsInBranch(final ClonkIndex index) {
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

//private static boolean looksLikeConstName(String name) {
//boolean underscore = false;
//for (int i = 0; i < name.length(); i++) {
//	char c = name.charAt(i);
//	if (i > 0 && c == '_') {
//		if (!underscore)
//			underscore = true;
//		else
//			return false;
//	}
//	if (!underscore) {
//		if (Character.toUpperCase(c) != c) {
//			return false;
//		}
//	}
//}
//return underscore || name.equals(name.toUpperCase());
//}

}
