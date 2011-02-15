package net.arctics.clonk.parser.c4script;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jface.text.IRegion;

import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.c4script.Variable.C4VariableScope;
import net.arctics.clonk.parser.c4script.ast.Block;
import net.arctics.clonk.parser.c4script.ast.Conf;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.TypeExpectancyMode;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.CompoundIterable;
import net.arctics.clonk.util.Utilities;

/**
 * A function in a script.
 * @author ZokRadonh
 *
 */
public class Function extends Structure implements Serializable, ITypedDeclaration, IHasUserDescription, IRegion {
	
	private static final long serialVersionUID = 3848213897251037684L;
	private C4FunctionScope visibility; 
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
		visibility = C4FunctionScope.GLOBAL;
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
		visibility = C4FunctionScope.GLOBAL;
		name = ""; //$NON-NLS-1$
		parameter = new ArrayList<Variable>();
		localVars = new ArrayList<Variable>();
	}
	
	public Function(String name, ScriptBase parent, C4FunctionScope scope) {
		this.name = name;
		visibility = scope;
		parameter = new ArrayList<Variable>();
		localVars = new ArrayList<Variable>();
		setScript(parent);
	}
	
	public Function(String name, Definition parent, String scope) {
		this(name,parent,C4FunctionScope.makeScope(scope));
	}
	
	public Function(String name, C4FunctionScope scope) {
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
	public C4FunctionScope getVisibility() {
		return visibility;
	}
	
	/**
	 * @return the description
	 */
	public String getUserDescription() {
		return description == null && isEngineDeclaration() ? getEngine().descriptionFor(this) : description;
	}

	/**
	 * @param description the description to set
	 */
	public void setUserDescription(String description) {
		this.description = description;
	}

	/**
	 * @param visibility the visibility to set
	 */
	public void setVisibility(C4FunctionScope visibility) {
		this.visibility = visibility;
	}

	/**
	 * The scope of a function.
	 * @author ZokRadonh
	 *
	 */
	public enum C4FunctionScope {
		GLOBAL,
		PUBLIC,
		PROTECTED,
		PRIVATE;
		
		private String lowerCaseName;
		
		public static C4FunctionScope makeScope(String scopeString) {
			if (scopeString == null) return C4FunctionScope.PUBLIC;
			if (scopeString.equals(Keywords.Public)) return C4FunctionScope.PUBLIC;
			if (scopeString.equals(Keywords.Protected)) return C4FunctionScope.PROTECTED;
			if (scopeString.equals(Keywords.Private)) return C4FunctionScope.PRIVATE;
			if (scopeString.equals(Keywords.Global)) return C4FunctionScope.GLOBAL;
			//if (C4FunctionScope.valueOf(scopeString) != null) return C4FunctionScope.valueOf(scopeString);
			return C4FunctionScope.PUBLIC;
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
		return Variable.C4VariableScope.values().length + visibility.ordinal();
	}

	public static String getDocumentationURL(String functionName, Engine engine) {
		String docURLTemplate = engine.getCurrentSettings().docURLTemplate;
		return String.format(docURLTemplate, functionName, ClonkPreferences.getLanguagePrefForDocumentation());
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
		String description = getUserDescription();
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
		Collection<ScriptBase> includesCollection = getScript().getIncludes();
		ScriptBase[] includes = includesCollection.toArray(new ScriptBase[includesCollection.size()]);
		for (int i = includes.length-1; i >= 0; i--) {
			Function fun = includes[i].findFunction(getName());
			if (fun != null && fun != this)
				return fun;
		}
		
		// search in index
		List<Declaration> decsWithSameName = getScript().getIndex().getDeclarationMap().get(this.getName());
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
		Function f = getScript().getIndex().getEngine().findFunction(getName());
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
		for (Function f = this; f != null; f = f.getInherited())
			result = f;
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
			parameter.add(new Variable("par"+i, C4VariableScope.VAR)); //$NON-NLS-1$
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
			ITypedDeclaration.Default.expectedToBeOfType(this, t);
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
	 * @return true if related, false if not
	 */
	public boolean inheritsFrom(Function otherFunc) {
		for (Function f = this; f != null; f = f.getInherited())
			if (otherFunc == f)
				return true;
		return false;
	}
	
	/**
	 * Returns whether the function passed to this method is in the same override line as the calling function 
	 * @param otherFunc
	 * @return true if both functions are related, false if not
	 */
	public boolean isRelatedFunction(Function otherFunc) {
		if (this.inheritsFrom(otherFunc))
			return true; 
		for (Function f = this; f != null; f = f.getInherited())
			if (otherFunc.inheritsFrom(f))
				return true;
		return false;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Iterable<Declaration> allSubDeclarations(int mask) {
		if ((mask & VARIABLES) != 0)
			return new CompoundIterable<Declaration>(localVars, parameter, otherDeclarations);
		else
			return otherDeclarations != null ? otherDeclarations : NO_SUB_DECLARATIONS;
	}
	
	@Override
	public boolean isGlobal() {
		return getVisibility() == C4FunctionScope.GLOBAL;
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
			this.returnType = f.returnType;
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
	 * @return
	 */
	public Declaration addOtherDeclaration(Declaration d) {
		if (otherDeclarations == null) {
			otherDeclarations = new ArrayList<Declaration>(3);
		} else {
			for (Declaration existing : otherDeclarations) {
				if (existing.getLocation().equals(d.getLocation())) {
					return existing;
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
		if (blockSourceHash != -1 && blockSourceHash == source.hashCode()) {
			if (!codeBlockDefrosted) {
				codeBlockDefrosted = true;
				codeBlock.postSerialize(null, getDeclarationObtainmentContext());
			}
			return codeBlock;
		} else{
			return codeBlock = null;
		}
	}
	
	/**
	 * Return the cached block without performing checks.
	 * @return The cached code block
	 */
	public Block getCodeBlock() {
		return codeBlock;
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
	
	/**
	 * The hash code of a function is the hash code of its name.
	 */
	@Override
	public int hashCode() {
		return name != null ? name.hashCode() : super.hashCode();
	}
	
}
