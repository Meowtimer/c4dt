package net.arctics.clonk.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.Utilities;
import net.arctics.clonk.parser.C4Directive.C4DirectiveType;
import net.arctics.clonk.parser.C4Function.C4FunctionScope;
import net.arctics.clonk.parser.C4Variable.C4VariableScope;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Region;

public abstract class C4ScriptBase extends C4Structure {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	static public class FindFieldInfo {
		private ClonkIndex index;
		private int recursion;
		private Class<? extends C4Field> fieldClass;
		private C4Function contextFunction;
		private Set<C4ScriptBase> alreadySearched;
		private C4ScriptBase searchOrigin;

		public FindFieldInfo(ClonkIndex clonkIndex) {
			super();
			index = clonkIndex;
			alreadySearched = new HashSet<C4ScriptBase>();
		}
		public FindFieldInfo(ClonkIndex clonkIndex, C4Function ctx) {
			this(clonkIndex);
			setContextFunction(ctx);
		}
		public Class<? extends C4Field> getFieldClass() {
			return fieldClass;
		}
		public void setFieldClass(Class<?extends C4Field> fieldClass) {
			this.fieldClass = fieldClass;
		}
		public void setContextFunction(C4Function ctx) {
			contextFunction = ctx;
		}
		public C4Function getContextFunction() {
			return contextFunction;
		}
		public Set<C4ScriptBase> getAlreadySearched() {
			return alreadySearched;
		}
		public C4ScriptBase getSearchOrigin() {
			return searchOrigin;
		}
		public void setSearchOrigin(C4ScriptBase searchOrigin) {
			this.searchOrigin = searchOrigin;
		}
		public void resetState() {
			alreadySearched.clear();
			recursion = 0;
		}
		
	}
	
	protected List<C4Function> definedFunctions = new LinkedList<C4Function>();
	protected List<C4Variable> definedVariables = new ArrayList<C4Variable>(); // default capacity of 10 is ok
	protected List<C4Directive> definedDirectives = new ArrayList<C4Directive>(4); // mostly 4 are enough
	
//	private List<IC4ObjectListener> changeListeners = new LinkedList<IC4ObjectListener>();
	
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
	
	public C4Object[] getIncludes(ClonkIndex index) {
		List<C4Object> result = new ArrayList<C4Object>();
		for (C4Directive d : definedDirectives) {
			if (d.getType() == C4DirectiveType.INCLUDE || d.getType() == C4DirectiveType.APPENDTO) {
				C4Object obj = d.getIncludedObject(index);
				if (obj != null)
					result.add(obj);
			}
		}
		return result.toArray(new C4Object[result.size()]);
	}
	
	public C4Directive getIncludeDirectiveFor(C4Object obj) {
		for (C4Directive d : getIncludeDirectives()) {
			if (d.getIncludedObject(getIndex()) == obj)
				return d;
		}
		return null;
	}
	
	public C4Object[] getIncludes() {
		return getIncludes(getIndex());
	}
	
	public C4Field findField(String name) {
		return findField(name, new FindFieldInfo(getIndex()));
	}
	
	protected boolean refersToThis(String name, FindFieldInfo info) {
		return false;
	}
	
	private static boolean resourceInsideContainer(IResource resource, IContainer container) {
		for (IContainer c = resource.getParent(); c != null; c = c.getParent())
			if (c.equals(container))
				return true;
		return false;
	}
	
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
		for (C4Object o : getIncludes(info.index)) {
			C4Field result = o.findField(name, info);
			if (result != null)
				return result;
		}
		info.recursion--;
		
		// finally look if it's something global
		if (info.recursion == 0 && this != ClonkCore.ENGINE_OBJECT) { // .-.
			C4Field f;
			// global stuff defined in project
			f = info.index.findGlobalField(name);
			// engine function
			if (f == null)
				f = ClonkCore.ENGINE_OBJECT.findField(name, info);
			// function in extern lib
			if (f == null && info.index != ClonkCore.EXTERN_INDEX) {
				f = ClonkCore.EXTERN_INDEX.findGlobalField(name);
			}
			// definition
			if (f == null && Utilities.looksLikeID(name)) {
				List<C4Object> objects = info.index.getObjects(C4ID.getID(name));
				f = info.index.getLastObjectWithId(C4ID.getID(name));
				if (f == null)
					f = ClonkCore.EXTERN_INDEX.getLastObjectWithId(C4ID.getID(name));
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

//	/**
//	 * @param objectFolder the objectFolder to set
//	 */
//	public void setObjectFolder(IContainer objectFolder) {
//		this.objectFolder = objectFolder;
//	}
	
	/**
	 * @deprecated if you use this function, you are not allowed to add or remove items
	 * @return the definedFunctions
	 */
	public List<C4Function> getDefinedFunctions() {
		return definedFunctions;
	}
	/**
	 * @deprecated never use this function
	 * @param definedFunctions the definedFunctions to set
	 */
	public void setDefinedFunctions(List<C4Function> definedFunctions) {
		this.definedFunctions = definedFunctions;
	}
	/**
	 * @deprecated if you use this function, you are not allowed to add or remove items
	 * @return the definedVariables
	 */
	public List<C4Variable> getDefinedVariables() {
		return definedVariables;
	}
	/**
	 * @deprecated never use this function
	 * @param definedVariables the definedVariables to set
	 */
	public void setDefinedVariables(List<C4Variable> definedVariables) {
		this.definedVariables = definedVariables;
	}
	/**
	 * @deprecated if you use this function, you are not allowed to add or remove items
	 * @return the definedDirectives
	 */
	public List<C4Directive> getDefinedDirectives() {
		return definedDirectives;
	}
	/**
	 * @deprecated never use this function
	 * @param definedDirectives the definedDirectives to set
	 */
	public void setDefinedDirectives(List<C4Directive> definedDirectives) {
		this.definedDirectives = definedDirectives;
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
		C4Object[] incs = this.getIncludes();
		for (C4Object o : incs) {
			if (o == other)
				return true;
			if (o.includes(other, dontRevisit))
				return true;
		}
		return false;
	}
	
	public abstract ClonkIndex getIndex();

	public C4Variable findLocalVariable(String name) {
		for (C4Variable var : definedVariables) {
			if (var.name.equals(name))
				return var;
		}
		return null;
	}
	
	private static boolean looksLikeConstName(String name) {
		boolean underscore = false;
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if (i > 0 && c == '_') {
				if (!underscore)
					underscore = true;
				else
					return false;
			}
			if (!underscore) {
				if (Character.toUpperCase(c) != c) {
					return false;
				}
			}
		}
		return underscore || name.equals(name.toUpperCase());
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

	public boolean convertFuncsToConstsIfTheyLookLikeConsts() {
		boolean didSomething = false;
		List<C4Function> toBeRemoved = new LinkedList<C4Function>();
		for (C4Function f : definedFunctions) {
			if (f.getParameters().size() == 0 && looksLikeConstName(f.getName())) {
				toBeRemoved.add(f);
				definedVariables.add(new C4Variable(f.getName(), f.getReturnType(), f.getUserDescription(), C4VariableScope.VAR_CONST));
				didSomething = true;
			}
		}
		for (C4Variable v : definedVariables) {
			if (v.getScope() != C4VariableScope.VAR_CONST) {
				v.setScope(C4VariableScope.VAR_CONST);
				didSomething = true;
			}
			if (v.getScript() != this) {
				v.setScript(this);
				didSomething = true;
			}
		}
		for (C4Function f : toBeRemoved)
			definedFunctions.remove(f);
		C4Variable v = findLocalVariable("_inherited");
		if (v != null) {
			definedVariables.remove(v);
			definedFunctions.add(new C4Function("_inherited", this, C4FunctionScope.FUNC_PUBLIC));
			didSomething = true;
		}
		didSomething |= removeDuplicateVariables();
		return didSomething;
	}

}
