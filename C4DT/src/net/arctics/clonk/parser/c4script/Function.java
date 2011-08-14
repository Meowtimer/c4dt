package net.arctics.clonk.parser.c4script;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IRegion;

import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.IHasIncludes;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.c4script.Variable.Scope;
import net.arctics.clonk.parser.c4script.ast.Block;
import net.arctics.clonk.parser.c4script.ast.Conf;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.TypeExpectancyMode;
import net.arctics.clonk.parser.c4script.ast.evaluate.IEvaluationContext;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.CompoundIterable;
import net.arctics.clonk.util.Utilities;

/**
 * A function in a script.
 * @author ZokRadonh
 *
 */
public class Function extends Structure implements Serializable, ITypeable, IHasUserDescription, IRegion, IEvaluationContext {
	
	private static final long serialVersionUID = 3848213897251037684L;
	private FunctionScope visibility; 
	private List<Variable> localVars;
	private List<Variable> parameter;
	/**
	 * Various other declarations (like proplists) that aren't variables/parameters
	 */
	private List<Declaration> otherDeclarations;
	private IType returnType;
	private String description;
	private boolean isCallback;
	private boolean isOldStyle;
	private SourceLocation body, header;
	
	/**
	 * Code block kept in memory for speed optimization
	 */
	private Block codeBlock;
	
	/**
	 * Hash code of the string the block was parsed from.
	 */
	private int blockSourceHash;
	
	/**
	 * false if postSerialize still needs to be called on codeBlock. postSerialize will be called when getCodeBlock is called.<br/>
	 * Added so loading an index won't lag too much.
	 */
	private transient boolean codeBlockDefrosted;

	/**
	 * Create a new function.
	 * @param name Name of the function
	 * @param returnType Return type of the function
	 * @param pars Parameter variables to add to the parameter list of the function
	 */
	public Function(String name, IType returnType, Variable... pars) {
		this.name = name;
		this.returnType = returnType;
		parameter = new ArrayList<Variable>(pars.length);
		for (Variable var : pars) {
			parameter.add(var);
			var.setParentDeclaration(this);
		}
		visibility = FunctionScope.GLOBAL;
	}
	
	/**
	 * Do NOT use this constructor! Its for engine-functions only.
	 * @param name
	 * @param type
	 * @param desc
	 * @param pars
	 */
	public Function(String name, String type, String desc, Variable... pars) {
		this(name, PrimitiveType.makeType(type), pars);
		description = desc;
		parentDeclaration = null; // since engine function only
		localVars = null;
	}
	
	public Function() {
		visibility = FunctionScope.GLOBAL;
		name = ""; //$NON-NLS-1$
		parameter = new ArrayList<Variable>();
		localVars = new ArrayList<Variable>();
	}
	
	public Function(String name, ScriptBase parent, FunctionScope scope) {
		this.name = name;
		visibility = scope;
		parameter = new ArrayList<Variable>();
		localVars = new ArrayList<Variable>();
		setScript(parent);
	}
	
	public Function(String name, Definition parent, String scope) {
		this(name,parent,FunctionScope.makeScope(scope));
	}
	
	public Function(String name, FunctionScope scope) {
		this(name, null, scope);
	}
	
	/**
	 * @return the localVars
	 */
	public List<Variable> getLocalVars() {
		return localVars;
	}

	/**
	 * @param localVars the localVars to set
	 */
	public void setLocalVars(List<Variable> localVars) {
		this.localVars = localVars;
	}

	/**
	 * @return the parameter
	 */
	public List<Variable> getParameters() {
		return parameter;
	}

	/**
	 * @param parameter the parameter to set
	 */
	public void setParameters(List<Variable> parameter) {
		this.parameter = parameter;
	}

	/**
	 * @return the returnType
	 */
	public IType getReturnType() {
		if (returnType == null)
			returnType = PrimitiveType.UNKNOWN;
		return returnType;
	}

	/**
	 * @return the visibility
	 */
	public FunctionScope getVisibility() {
		return visibility;
	}
	
	/**
	 * @return the description
	 */
	@Override
	public String obtainUserDescription() {
		if (isEngineDeclaration())
			return getEngine().getDescriptionPossiblyReadingItFromRepositoryDocs(this);
		else
			return description;
	}
	
	@Override
	public String getCurrentlySetUserDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	@Override
	public void setUserDescription(String description) {
		this.description = description;
	}

	/**
	 * @param visibility the visibility to set
	 */
	public void setVisibility(FunctionScope visibility) {
		this.visibility = visibility;
	}

	/**
	 * The scope of a function.
	 * @author ZokRadonh
	 *
	 */
	public enum FunctionScope {
		GLOBAL,
		PUBLIC,
		PROTECTED,
		PRIVATE;
		
		private static final Map<String, FunctionScope> scopeMap = new HashMap<String, FunctionScope>();
		static {
			for (FunctionScope s : values())
				scopeMap.put(s.name().toLowerCase(), s);
		}
		
		private String lowerCaseName;
		
		public static FunctionScope makeScope(String scopeString) {
			return scopeMap.get(scopeString);
		}
		
		@Override
		public String toString() {
			if (lowerCaseName == null)
				lowerCaseName = this.name().toLowerCase();
			return lowerCaseName;
		}

		public Object toKeyword() {
			return toString();
		}
	}
	
	/**
	 * Generates a function string in the form of
	 * function(type1 parName1, type2 parName2)
	 * if <code>withFuncName</code> is true, else
	 * type1 parName1, type1 parName2
	 * 
	 * @param withFuncName include function name
	 * @param engineCompatible print parameters in an engine-parseable manner
	 * @return the function string
	 */
	public String getLongParameterString(boolean withFuncName, boolean engineCompatible) {
		StringBuilder string = new StringBuilder();
		if (withFuncName) {
			string.append(getName());
			string.append("("); //$NON-NLS-1$
		}
		printParameterString(string, engineCompatible);
		if (withFuncName) string.append(")"); //$NON-NLS-1$
		return string.toString();
	}
	
	public String getLongParameterString(boolean withFuncName) {
		return getLongParameterString(withFuncName, true);	
	}

	private void printParameterString(StringBuilder output, boolean engineCompatible) {
		if (getParameters().size() > 0) {
			for(Variable par : getParameters()) {
				IType staticType = engineCompatible ? par.getType().staticType() : par.getType();
				if (engineCompatible && !par.isActualParm())
					continue;
				if (staticType != PrimitiveType.UNKNOWN && staticType != null) {
					if (!engineCompatible || (staticType instanceof PrimitiveType && staticType != PrimitiveType.ANY)) {
						output.append(staticType.typeName(false));
						output.append(' ');
					}
					output.append(par.getName());
					output.append(',');
					output.append(' ');
				}
				else {
					output.append(par.getName());
					output.append(',');
					output.append(' ');
				}
			}
			if (output.length() > 0) {
				if (output.charAt(output.length() - 1) == ' ') {
					output.delete(output.length() - 2,output.length());
				}
			}
		}
	}

	/**
	 * @param isCallback the isCallback to set
	 */
	public void setCallback(boolean isCallback) {
		this.isCallback = isCallback;
	}

	/**
	 * @return the isCallback
	 */
	public boolean isCallback() {
		return isCallback;
	}

	/**
	 * @param body the body to set
	 */
	public void setBody(SourceLocation body) {
		this.body = body;
	}

	/**
	 * @return the body
	 */
	public SourceLocation getBody() {
		return body;
	}
	
	public int sortCategory() {
		return Variable.Scope.values().length + visibility.ordinal();
	}

	public static String getDocumentationURL(String functionName, Engine engine) {
		return engine.getCurrentSettings().getDocumentationURLForFunction(functionName);
	}
	
	/**
	 * For engine functions: Return URL string for documentation
	 * @return The documentation URl
	 */
	public String getDocumentationURL() {
		return getDocumentationURL(getName(), getScript().getEngine());
	}

	@Override
	public String getInfoText() {
		String description = obtainUserDescription();
		return String.format(Messages.C4Function_InfoTextTemplate, getReturnType() != null ? Utilities.htmlerize(getReturnType().typeName(true)) : "", Utilities.htmlerize(getLongParameterString(true, false)), description != null && !description.equals("") ? description : Messages.DescriptionNotAvailable, getScript().toString()); //$NON-NLS-1$
	}

	@Override
	public Variable findDeclaration(String declarationName, Class<? extends Declaration> declarationClass) {
		return findLocalDeclaration(declarationName, declarationClass);
	}
	
	public Variable findLocalDeclaration(String declarationName, Class<? extends Declaration> declarationClass) {
		if (declarationClass.isAssignableFrom(Variable.class)) {
			if (declarationName.equals(Variable.THIS.getName()))
				return Variable.THIS;
			for (Variable v : localVars) {
				if (v.getName().equals(declarationName))
					return v;
			}
			for (Variable p : parameter) {
				if (p.getName().equals(declarationName))
					return p;
			}
		}
		return null;
	}
	
	public Variable findVariable(String variableName) {
		return findDeclaration(variableName, Variable.class);
	}

	public boolean isOldStyle() {
		return isOldStyle;
	}

	public void setOldStyle(boolean isOldStyle) {
		this.isOldStyle = isOldStyle;
	}
	
	/**
	 * Return the function this one inherits from
	 * @return The inherited function
	 */
	public Function getInherited() {
		
		// search in #included scripts
		Collection<? extends IHasIncludes> includesCollection = getScript().getIncludes(false);
		IHasIncludes[] includes = includesCollection.toArray(new IHasIncludes[includesCollection.size()]);
		for (int i = includes.length-1; i >= 0; i--) {
			Function fun = includes[i].findFunction(getName());
			if (fun != null && fun != this)
				return fun;
		}
		
		// search in index
		List<Declaration> decsWithSameName = getIndex().declarationMap().get(this.getName());
		if (decsWithSameName != null) {
			Function f = null;
			int rating = -1;
			for (Declaration d : decsWithSameName) {
				// get latest version since getInherited() might also be called when finding links in a modified but not yet saved script
				// in which case the calling function (on-the-fly-parsed) differs from the function in the index 
				d = d.latestVersion();
				if (d == this || !(d instanceof Function))
					continue;
				int rating_ = 0;
				if (d.getParentDeclaration() == this.getParentDeclaration())
					rating_++;
				if (rating_ > rating) {
					f = (Function) d;
					rating = rating_;
				}
			}
			if (f != null)
				return f;
		}
		
		// search in engine
		Function f = getIndex().getEngine().findFunction(getName());
		if (f != null)
			return f;
		
		return null;
	}
	
	/**
	 * Return the first function in the inherited chain.
	 * @return The first function in the inherited chain.
	 */
	public Function baseFunction() {
		Function result = this;
		Set<Function> alreadyVisited = new HashSet<Function>();
		for (Function f = this; f != null; f = f.getInherited()) {
			if (alreadyVisited.contains(f)) {
				System.out.println(String.format("%s causes inherited loop", f.getQualifiedName()));
				break;
			}
			result = f;
			alreadyVisited.add(f);
		}
		return result;
	}

	@Override
	public boolean hasSubDeclarationsInOutline() {
		return otherDeclarations != null || (getLocalVars() != null && getLocalVars().size() > 0);
	}

	@Override
	public Object[] getSubDeclarationsForOutline() {
		return ArrayUtil.concat(getLocalVars().toArray(), otherDeclarations != null ? otherDeclarations.toArray() : null);
	}

	/**
	 * Create num generically named parameters (par1, par2, ...)
	 * @param num Number of parameters to create
	 */
	public void createParameters(int num) {
		for (int i = parameter.size(); i < num; i++) {
			parameter.add(new Variable("par"+i, Scope.VAR)); //$NON-NLS-1$
		}
	}

	/**
	 * Return the location of the function header
	 * @return
	 */
	public SourceLocation getHeader() {
		return header;
	}

	/**
	 * Set the location of the function header.
	 * @param header
	 */
	public void setHeader(SourceLocation header) {
		this.header = header;
	}
	
	/**
	 * Print the function header into the passed string builder
	 * @param output The StringBuilder to add the header string to
	 */
	public void printHeader(StringBuilder output) {
		printHeader(output, isOldStyle());
	}
	
	/**
	 * Print the function header into the passed string builder
	 * @param output The StringBuilder to add the header string to
	 * @param oldStyle Whether to print in old 'label-style'
	 */
	public void printHeader(StringBuilder output, boolean oldStyle) {
		output.append(getVisibility().toString());
		if (!oldStyle) {
			output.append(" "); //$NON-NLS-1$
			output.append(Keywords.Func);
		}
		output.append(" "); //$NON-NLS-1$
		output.append(getName());
		if (!oldStyle) {
			output.append("("); //$NON-NLS-1$
			printParameterString(output, true);
			output.append(")"); //$NON-NLS-1$
		}
		else
			output.append(":"); //$NON-NLS-1$
	}
	
	/**
	 * Return the header string
	 * @param oldStyle Whether to return the header string in old 'label-style'
	 * @return The header string
	 */
	public String getHeaderString(boolean oldStyle) {
		StringBuilder builder = new StringBuilder();
		printHeader(builder, oldStyle);
		return builder.toString();
	}
	
	/*+
	 * Return the header string
	 */
	public String getHeaderString() {
		return getHeaderString(isOldStyle());
	}

	@Override
	public void expectedToBeOfType(IType t, TypeExpectancyMode mode) {
		if (mode == TypeExpectancyMode.Force) {
			ITypeable.Default.expectedToBeOfType(this, t);
		}
	}

	@Override
	public IType getType() {
		return getReturnType();
	}

	@Override
	public void forceType(IType type) {
		setReturnType(type);
	}
	
	public void setReturnType(IType returnType) {
		this.returnType = returnType;
	}
	
	public void setObjectType(Definition object) {
		//expectedContent = object;
	}
	
	/**
	 * Returns whether this function inherits from the calling function
	 * @param otherFunc
	 * @param recursionCatcher Recursion catcher!
	 * @return true if related, false if not
	 */
	private boolean inheritsFrom(Function otherFunc, Set<Function> recursionCatcher) {
		Function f = this;
		while (f != null && !recursionCatcher.contains(f)) {
			recursionCatcher.add(f);
			if (otherFunc == f)
				return true;
			f = f.getInherited();
		}
		return false;
	}
	
	/**
	 * Returns whether the function passed to this method is in the same override line as the calling function 
	 * @param otherFunc
	 * @return true if both functions are related, false if not
	 */
	public boolean isRelatedFunction(Function otherFunc) {
		Set<Function> recursionCatcher = new HashSet<Function>();
		if (this.inheritsFrom(otherFunc, recursionCatcher))
			return true;
		Function f = this;
		while (f != null && !recursionCatcher.contains(f)) {
			recursionCatcher.add(f);
			if (otherFunc.inheritsFrom(f, recursionCatcher))
				return true;
			f = f.getInherited();
		}
		return false;
	}
	
	@Override
	public Iterable<Declaration> allSubDeclarations(int mask) {
		List<Iterable<? extends Declaration>> l = new ArrayList<Iterable<? extends Declaration>>(3);
		if ((mask & VARIABLES) != 0) {
			l.add(localVars);
			l.add(parameter);
		}
		if ((mask & OTHER) != 0)
			l.add(otherDeclarations);
		return new CompoundIterable<Declaration>(l);
	}
	
	@Override
	public boolean isGlobal() {
		return getVisibility() == FunctionScope.GLOBAL;
	}
	
	/**
	 * Return whether num parameters are more than needed for this function
	 * @param num Number of parameters to test for
	 * @return See above
	 */
	public boolean tooManyParameters(int num) {
		return
			(getParameters().size() == 0 || getParameters().get(getParameters().size()-1).isActualParm()) &&
			num > getParameters().size();
	}
	
	/**
	 * Invoke this function. Left empty for 'regular' functions, only defined in special interpreter functions
	 * @param args the arguments to pass to the function
	 * @return the result
	 */
	public Object invoke(Object... args) {
		return null;
	}
	
	@Override
	public void absorb(Declaration declaration) {
		if (declaration instanceof Function) {
			Function f = (Function) declaration;
			if (f.parameter.size() >= this.parameter.size())
				this.parameter = f.parameter;
			this.setReturnType(f.returnType);
			f.parameter = null;
		}
		super.absorb(declaration);
	}
	
	@Override
	public void sourceCodeRepresentation(StringBuilder builder, Object cookie) {
		builder.append(getVisibility().toKeyword());
		builder.append(" ");
		builder.append(Keywords.Func);
		builder.append(" ");
		builder.append(getLongParameterString(true));
		switch (Conf.braceStyle) {
		case NewLine:
			builder.append("\n{\n");
			break;
		case SameLine:
			builder.append(" {\n");
			break;
		}
		if (cookie instanceof ExprElm) {
			((ExprElm)cookie).print(builder, 1);
		}
		builder.append("\n}");
	}

	/**
	 * Remove local variables.
	 */
	public void clearLocalVars() {
		getLocalVars().clear();
		if (otherDeclarations != null) {
			otherDeclarations.clear();
		}
	}
	
	/**
	 * Add declaration that is neither parameter nor variable. Most likely an implicit proplist.
	 * @param d The declaration to add
	 * @return Return d. Any proplist declarations already added to the other declarations list with the same location will be removed in favor of d.
	 */
	public Declaration addOtherDeclaration(Declaration d) {
		if (otherDeclarations == null)
			otherDeclarations = new ArrayList<Declaration>(3);
		else {
			for (Iterator<Declaration> it = otherDeclarations.iterator(); it.hasNext();) {
				Declaration existing = it.next();
				if (existing.getLocation().equals(d.getLocation())) {
					it.remove();
					break;
				}
			}
		}
		otherDeclarations.add(d);
		return d;
	}
	
	private static final List<Declaration> NO_OTHER_DECLARATIONS = new ArrayList<Declaration>();
	
	/**
	 * Return 'other' declarations (neither parameters nor variables)
	 * @return The list of other declarations. Will not be null, even if there are not other declarations.
	 */
	public List<Declaration> getOtherDeclarations() {
		if (otherDeclarations == null) {
			return NO_OTHER_DECLARATIONS;
		} else {
			return otherDeclarations;
		}
	}
	
	@Override
	public boolean typeIsInvariant() {
		return isEngineDeclaration();
	}

	public void resetLocalVarTypes() {
		for (Variable v : getLocalVars()) {
			v.forceType(PrimitiveType.UNKNOWN);
		}
	}
	
	public void storeBlock(Block block, String source) {
		codeBlock = block;
		blockSourceHash = source.hashCode();
		codeBlockDefrosted = true;
	}
	
	/**
	 * Return cached code block if it was created from the given source. This is tested by hash code of the source string.
	 * @param source The source to test against
	 * @return The code block or null if it was created from differing source.
	 */
	public Block getCodeBlock(String source) {
		if (source == null || (blockSourceHash != -1 && blockSourceHash == source.hashCode())) {
			if (!codeBlockDefrosted) {
				codeBlockDefrosted = true;
				if (codeBlock != null)
					codeBlock.postLoad(null, getDeclarationObtainmentContext());
			}
			return codeBlock;
		} else {
			return codeBlock = null;
		}
	}
	
	/**
	 * Return the cached block without performing checks.
	 * @return The cached code block
	 */
	public Block getCodeBlock() {
		return getCodeBlock(null);
	}

	@Override
	public int getLength() {
		return getBody().getLength();
	}

	@Override
	public int getOffset() {
		return getBody().getOffset();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends Declaration> T getLatestVersion(T from) {
		if (from instanceof Variable) {
			return super.getLatestVersion(from);
		} else {
			for (Declaration other : otherDeclarations) {
				if (other.getClass() == from.getClass() && other.getLocation() == from.getLocation())
					return (T) other;
			}
		}
		return null;
	};
	
	/**
	 * Assign parameter types to existing parameters. If more types than parameters are given, no new parameters will be created.
	 * @param types The types to assign to the parameters
	 */
	public void assignParameterTypes(IType... types) {
		for (int i = 0; i < types.length; i++) {
			if (i >= getParameters().size())
				break;
			getParameters().get(i).forceType(types[i], true);
		}
	}

	@Override
	public int absoluteExpressionsOffset() {
		return getBody().getOffset();
	}

	@Override
	public Object getValueForVariable(String varName) {
		return findVariable(varName); // return meta object instead of concrete value
	}

	@Override
	public Object[] getArguments() {
		return getParameters().toArray();
	}

	@Override
	public Function getFunction() {
		return this;
	}

	@Override
	public void reportOriginForExpression(ExprElm expression, IRegion location, IFile file) {
		// oh interesting
	}
	
	@Override
	public int getCodeFragmentOffset() {
		return body != null ? body.getOffset() : 0;
	}
	
}
