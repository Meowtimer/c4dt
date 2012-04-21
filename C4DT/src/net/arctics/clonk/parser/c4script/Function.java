package net.arctics.clonk.parser.c4script;

import static net.arctics.clonk.util.ArrayUtil.copyListOrReturnDefaultList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.IHasIncludes;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.parser.c4script.Variable.Scope;
import net.arctics.clonk.parser.c4script.ast.Block;
import net.arctics.clonk.parser.c4script.ast.Conf;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.TypeExpectancyMode;
import net.arctics.clonk.parser.c4script.ast.evaluate.IEvaluationContext;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.StringUtil;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IRegion;

/**
 * A function in a script.
 * @author ZokRadonh
 *
 */
public class Function extends Structure implements Serializable, ITypeable, IHasUserDescription, IRegion, IEvaluationContext {
	
	private static final long serialVersionUID = 3848213897251037684L;
	private FunctionScope visibility; 
	private List<Variable> localVars;
	protected List<Variable> parameters;
	/**
	 * Various other declarations (like proplists) that aren't variables/parameters
	 */
	private List<Declaration> otherDeclarations;
	private IType returnType;
	private String description;
	private String returnDescription;
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
		parameters = new ArrayList<Variable>(pars.length);
		for (Variable var : pars) {
			parameters.add(var);
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
		clearParameters();
		localVars = new ArrayList<Variable>();
	}
	
	public Function(String name, Script parent, FunctionScope scope) {
		this.name = name;
		visibility = scope;
		clearParameters();
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
	public List<Variable> localVars() {
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
	public Iterable<? extends Variable> parameters() {
		return copyListOrReturnDefaultList(parameters, null);
	}
	
	public void addParameter(Variable parameter) {
		synchronized (parameters) {
			parameters.add(parameter);
		}
	}
	
	public void clearParameters() {
		parameters = new ArrayList<Variable>();
	}
	
	public Variable parameter(int index) {
		synchronized (parameters) {
			return index >= 0 && index < parameters.size() ? parameters.get(index) : null;
		}
	}
	
	public int numParameters() {
		synchronized (parameters) {
			return parameters.size();
		}
	}

	/**
	 * @param parameters the parameter to set
	 */
	public void setParameters(List<Variable> parameters) {
		this.parameters = parameters;
	}

	/**
	 * @return the returnType
	 */
	public IType returnType() {
		if (returnType == null)
			returnType = PrimitiveType.UNKNOWN;
		return returnType;
	}

	/**
	 * @return the visibility
	 */
	public FunctionScope visibility() {
		return visibility;
	}
	
	/**
	 * @return the description
	 */
	@Override
	public String obtainUserDescription() {
		if (isEngineDeclaration())
			return engine().obtainDescription(this);
		else
			return description;
	}
	
	@Override
	public String userDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	@Override
	public void setUserDescription(String description) {
		this.description = description;
	}
	
	public void setReturnDescription(String returnDescription) {
		this.returnDescription = returnDescription;
	}
	
	public String returnDescription() {
		return returnDescription;
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
	public String longParameterString(boolean withFuncName, boolean engineCompatible) {
		StringBuilder string = new StringBuilder();
		if (withFuncName) {
			string.append(name());
			string.append("("); //$NON-NLS-1$
		}
		printParameterString(string, engineCompatible);
		if (withFuncName) string.append(")"); //$NON-NLS-1$
		return string.toString();
	}
	
	public String longParameterString(boolean withFuncName) {
		return longParameterString(withFuncName, true);	
	}
	
	@Override
	public String displayString() {
		return longParameterString(true);
	}

	private void printParameterString(StringBuilder output, boolean engineCompatible) {
		if (numParameters() > 0) {
			for(Variable par : parameters()) {
				IType staticType = engineCompatible ? par.type().staticType() : par.type();
				if (engineCompatible && !par.isActualParm())
					continue;
				if (staticType != PrimitiveType.UNKNOWN && staticType != null) {
					if (!engineCompatible || (staticType instanceof PrimitiveType && staticType != PrimitiveType.ANY)) {
						output.append(staticType.typeName(false));
						output.append(' ');
					}
					output.append(par.name());
					output.append(',');
					output.append(' ');
				}
				else {
					output.append(par.name());
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
	public SourceLocation body() {
		return body;
	}
	
	public SourceLocation wholeBody() {
		if (body == null)
			return null;
		if (isOldStyle())
			return body;
		else
			return new SourceLocation(body.start()-1, body.end()+1);
	}
	
	@Override
	public int sortCategory() {
		return Variable.Scope.values().length + visibility.ordinal();
	}

	public static String documentationURLForFunction(String functionName, Engine engine) {
		return engine.settings().documentationURLForFunction(functionName);
	}
	
	/**
	 * For engine functions: Return URL string for documentation
	 * @return The documentation URl
	 */
	public String documentationURL() {
		return documentationURLForFunction(name(), script().engine());
	}

	@Override
	public String infoText() {
		String description = obtainUserDescription();
		StringBuilder builder = new StringBuilder();
		String scriptPath = script().resource() != null
			? script().resource().getProjectRelativePath().toOSString()
			: script().name();
		for (String line : new String[] {
			"<i>"+scriptPath+"</i><br/>", //$NON-NLS-1$ //$NON-NLS-2$
			"<b>"+longParameterString(true, false)+"</b><br/>", //$NON-NLS-1$ //$NON-NLS-2$
			"<br/>", //$NON-NLS-1$
			description != null && !description.equals("") ? description : Messages.DescriptionNotAvailable, //$NON-NLS-1$
			"<br/>", //$NON-NLS-1$
		})
			builder.append(line);
		if (numParameters() > 0) {
			builder.append("<br/><b>"+Messages.Parameters+"</b><br/>"); //$NON-NLS-1$ //$NON-NLS-3$
			for (Variable p : parameters()) {
				builder.append("<b>"+p.name()+"</b> "+p.userDescription()+"<br/>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			builder.append("<br/>"); //$NON-NLS-1$
		}
		if (returnType() != PrimitiveType.UNKNOWN) {
			builder.append("<br/><b>"+Messages.Returns+ "</b> "+StringUtil.htmlerize(returnType().typeName(true))+"<br/>"); //$NON-NLS-1$ //$NON-NLS-3$
			if (returnDescription != null)
				builder.append(StringUtil.htmlerize(returnDescription)+"<br/>");
		}
		return builder.toString();
		/*return String.format(
			Messages.FunctionInfoTextTemplate,
			returnType() != null ? StringUtil.htmlerize(returnType().typeName(true)) : "",
			StringUtil.htmlerize(longParameterString(true, false)),
			description != null && !description.equals("") ? description : Messages.DescriptionNotAvailable,
			script().name()
		); //$NON-NLS-1$
		*/
	}

	@Override
	public Variable findDeclaration(String declarationName, Class<? extends Declaration> declarationClass) {
		return findLocalDeclaration(declarationName, declarationClass);
	}
	
	@Override
	public Variable findLocalDeclaration(String declarationName, Class<? extends Declaration> declarationClass) {
		if (declarationClass.isAssignableFrom(Variable.class)) {
			if (declarationName.equals(Variable.THIS.name()))
				return Variable.THIS;
			for (Variable v : localVars) {
				if (v.name().equals(declarationName))
					return v;
			}
			for (Variable p : parameters) {
				if (p.name().equals(declarationName))
					return p;
			}
		}
		return null;
	}
	
	public Variable findParameter(String parameterName) {
		for (Variable p : parameters())
			if (p.name().equals(parameterName))
				return p;
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
	public Function inheritedFunction() {
		
		// search in #included scripts
		Collection<? extends IHasIncludes> includesCollection = script().includes(0);
		IHasIncludes[] includes = includesCollection.toArray(new IHasIncludes[includesCollection.size()]);
		for (int i = includes.length-1; i >= 0; i--) {
			Function fun = includes[i].findFunction(name());
			if (fun != null && fun != this)
				return fun;
		}
		
		// search in index
		List<Declaration> decsWithSameName = index().declarationMap().get(this.name());
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
				if (d.parentDeclaration() == this.parentDeclaration())
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
		Function f = index().engine().findFunction(name());
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
		for (Function f = this; f != null; f = f.inheritedFunction()) {
			if (alreadyVisited.contains(f)) {
				System.out.println(String.format("%s causes inherited loop", f.qualifiedName())); //$NON-NLS-1$
				break;
			}
			result = f;
			alreadyVisited.add(f);
		}
		return result;
	}

	@Override
	public Object[] subDeclarationsForOutline() {
		return ArrayUtil.concat(localVars().toArray(), otherDeclarations != null ? otherDeclarations.toArray() : null);
	}

	/**
	 * Create num generically named parameters (par1, par2, ...)
	 * @param num Number of parameters to create
	 */
	public void createParameters(int num) {
		for (int i = parameters.size(); i < num; i++) {
			parameters.add(new Variable("par"+i, Scope.VAR)); //$NON-NLS-1$
		}
	}

	/**
	 * Return the location of the function header
	 * @return
	 */
	public SourceLocation header() {
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
		output.append(visibility().toString());
		if (!oldStyle) {
			output.append(" "); //$NON-NLS-1$
			output.append(Keywords.Func);
		}
		output.append(" "); //$NON-NLS-1$
		output.append(name());
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
	public String headerString(boolean oldStyle) {
		StringBuilder builder = new StringBuilder();
		printHeader(builder, oldStyle);
		return builder.toString();
	}
	
	/*+
	 * Return the header string
	 */
	public String headerString() {
		return headerString(isOldStyle());
	}

	@Override
	public void expectedToBeOfType(IType t, TypeExpectancyMode mode) {
		if (mode == TypeExpectancyMode.Force) {
			ITypeable.Default.expectedToBeOfType(this, t);
		}
	}

	@Override
	public IType type() {
		return returnType();
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
			f = f.inheritedFunction();
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
			f = f.inheritedFunction();
		}
		return false;
	}
	
	@Override
	public Iterable<Declaration> subDeclarations(Index contextIndex, int mask) {
		ArrayList<Declaration> decs = new ArrayList<Declaration>();
		if ((mask & VARIABLES) != 0) {
			decs.addAll(localVars);
			decs.addAll(parameters);
		}
		if ((mask & OTHER) != 0 && otherDeclarations != null)
			decs.addAll(otherDeclarations);
		return decs;
	}
	
	@Override
	public boolean isGlobal() {
		return visibility() == FunctionScope.GLOBAL;
	}
	
	/**
	 * Return whether num parameters are more than needed for this function
	 * @param num Number of parameters to test for
	 * @return See above
	 */
	public boolean tooManyParameters(int num) {
		synchronized (parameters) {
			return
				(parameters.size() == 0 || parameters.get(parameters.size()-1).isActualParm()) &&
				num > parameters.size();
		}
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
			if (f.parameters.size() >= this.parameters.size())
				this.parameters = f.parameters;
			this.setReturnType(f.returnType);
			f.parameters = null;
		}
		super.absorb(declaration);
	}
	
	@Override
	public void sourceCodeRepresentation(StringBuilder builder, Object cookie) {
		builder.append(visibility().toKeyword());
		builder.append(" "); //$NON-NLS-1$
		builder.append(Keywords.Func);
		builder.append(" "); //$NON-NLS-1$
		builder.append(longParameterString(true));
		switch (Conf.braceStyle) {
		case NewLine:
			builder.append("\n{\n"); //$NON-NLS-1$
			break;
		case SameLine:
			builder.append(" {\n"); //$NON-NLS-1$
			break;
		}
		if (cookie instanceof ExprElm) {
			((ExprElm)cookie).print(builder, 1);
		}
		builder.append("\n}"); //$NON-NLS-1$
	}

	/**
	 * Remove local variables.
	 */
	public void clearLocalVars() {
		localVars().clear();
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
				if (existing.location().equals(d.location())) {
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
	public List<Declaration> otherDeclarations() {
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
		for (Variable v : localVars()) {
			v.forceType(PrimitiveType.UNKNOWN);
		}
	}
	
	public void storeBlock(Block block, String source) {
		codeBlock = block;
		blockSourceHash = source.hashCode();
		codeBlockDefrosted = true;
		if (body != null)
			codeBlock.setExprRegion(0, body.getLength());
	}
	
	/**
	 * Return cached code block if it was created from the given source. This is tested by hash code of the source string.
	 * @param source The source to test against
	 * @return The code block or null if it was created from differing source.
	 */
	public Block codeBlockMatchingSource(String source) {
		if (source == null || (blockSourceHash != -1 && blockSourceHash == source.hashCode())) {
			if (!codeBlockDefrosted) {
				codeBlockDefrosted = true;
				if (codeBlock != null)
					codeBlock.postLoad(null, declarationObtainmentContext());
			}
			return codeBlock;
		} else
			return codeBlock = null;
	}
	
	/**
	 * Return the cached block without performing checks.
	 * @return The cached code block
	 */
	public Block codeBlock() {
		return codeBlockMatchingSource(null);
	}

	@Override
	public int getLength() {
		return body().getLength();
	}

	@Override
	public int getOffset() {
		return body().getOffset();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends Declaration> T latestVersionOf(T from) {
		if (from instanceof Variable) {
			return super.latestVersionOf(from);
		} else {
			if (otherDeclarations == null)
				return null;
			for (Declaration other : otherDeclarations) {
				if (other.getClass() == from.getClass() && other.location() == from.location())
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
		synchronized (parameters) {
			for (int i = 0; i < types.length; i++) {
				if (i >= parameters.size())
					break;
				parameters.get(i).forceType(types[i], true);
			}
		}
	}

	@Override
	public int absoluteExpressionsOffset() {
		return body().getOffset();
	}

	@Override
	public Object valueForVariable(String varName) {
		return findVariable(varName); // return meta object instead of concrete value
	}

	@Override
	public Object[] arguments() {
		synchronized (parameters) {
			return parameters.toArray();
		}
	}

	@Override
	public Function function() {
		return this;
	}

	@Override
	public void reportOriginForExpression(ExprElm expression, IRegion location, IFile file) {
		// oh interesting
	}
	
	@Override
	public int codeFragmentOffset() {
		return body != null ? body.getOffset() : 0;
	}
	
}
