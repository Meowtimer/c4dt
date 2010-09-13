package net.arctics.clonk.parser.c4script;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import net.arctics.clonk.index.C4Engine;
import net.arctics.clonk.index.C4Object;
import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.C4Structure;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.ExprElm;
import net.arctics.clonk.parser.c4script.C4Variable.C4VariableScope;
import net.arctics.clonk.parser.inireader.IniField;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.util.CompoundIterable;

public class C4Function extends C4Structure implements Serializable, ITypedDeclaration, IHasUserDescription {
	
	private static final long serialVersionUID = 3848213897251037684L;
	private C4FunctionScope visibility; 
	private List<C4Variable> localVars;
	private List<C4Variable> parameter;
	private IType returnType;
	private ObjectType returnObjectType;
	private String description;
	private boolean isCallback;
	private boolean isOldStyle;
	private SourceLocation body, header;
	
	@IniField
	public boolean isCriteriaSearch;

	public C4Function(String name, C4Type returnType, C4Variable... pars) {
		this.name = name;
		this.returnType = returnType;
		parameter = new ArrayList<C4Variable>(pars.length);
		for (C4Variable var : pars)
			parameter.add(var);
		visibility = C4FunctionScope.GLOBAL;
	}
	
	/**
	 * Do NOT use this constructor! Its for engine-functions only.
	 * @param name
	 * @param type
	 * @param desc
	 * @param pars
	 */
	public C4Function(String name, String type, String desc, C4Variable... pars) {
		this(name, C4Type.makeType(type), pars);
		description = desc;
		parentDeclaration = null; // since engine function only
		localVars = null;
	}
	
	public C4Function() {
		visibility = C4FunctionScope.GLOBAL;
		name = ""; //$NON-NLS-1$
		parameter = new ArrayList<C4Variable>();
		localVars = new ArrayList<C4Variable>();
	}
	
	public C4Function(String name, C4ScriptBase parent, C4FunctionScope scope) {
		this.name = name;
		visibility = scope;
		parameter = new ArrayList<C4Variable>();
		localVars = new ArrayList<C4Variable>();
		setScript(parent);
	}
	
	public C4Function(String name, C4Object parent, String scope) {
		this(name,parent,C4FunctionScope.makeScope(scope));
	}
	
	/**
	 * @return the localVars
	 */
	public List<C4Variable> getLocalVars() {
		return localVars;
	}

	/**
	 * @param localVars the localVars to set
	 */
	public void setLocalVars(List<C4Variable> localVars) {
		this.localVars = localVars;
	}

	/**
	 * @return the parameter
	 */
	public List<C4Variable> getParameters() {
		return parameter;
	}

	/**
	 * @param parameter the parameter to set
	 */
	public void setParameter(List<C4Variable> parameter) {
		this.parameter = parameter;
	}

	/**
	 * @return the returnType
	 */
	public IType getReturnType() {
		if (returnType == null)
			returnType = C4Type.UNKNOWN;
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
			for(C4Variable par : getParameters()) {
				IType staticType = engineCompatible ? par.getType().staticType() : par.getType();
				if (engineCompatible && !par.isActualParm())
					continue;
				if (staticType != C4Type.UNKNOWN && staticType != null) {
					if (!engineCompatible || (staticType instanceof C4Type && staticType != C4Type.ANY)) {
						output.append(par.getType().toString());
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
		return C4Variable.C4VariableScope.values().length + visibility.ordinal();
	}

	public static String getDocumentationURL(String functionName, C4Engine engine) {
		String docURLTemplate = engine.getCurrentSettings().docURLTemplate;
		return String.format(docURLTemplate, functionName, ClonkPreferences.getLanguagePref().toLowerCase());
	}
	
	// to be called on engine functions
	public String getDocumentationURL() {
		return getDocumentationURL(getName(), getScript().getEngine());
	}

	@Override
	public String getInfoText() {
		String description = getUserDescription();
		return String.format(Messages.C4Function_InfoTextTemplate, getReturnType() != null ? getReturnType().toString() : "", getLongParameterString(true, false), description != null && !description.equals("") ? description : Messages.DescriptionNotAvailable, getScript().toString()); //$NON-NLS-1$
	}

	@Override
	public C4Variable findDeclaration(String declarationName, Class<? extends C4Declaration> declarationClass) {
		return findLocalDeclaration(declarationName, declarationClass);
	}
	
	public C4Variable findLocalDeclaration(String declarationName, Class<? extends C4Declaration> declarationClass) {
		if (declarationClass.isAssignableFrom(C4Variable.class)) {
			if (declarationName.equals(C4Variable.THIS.getName()))
				return C4Variable.THIS;
			for (C4Variable v : localVars) {
				if (v.getName().equals(declarationName))
					return v;
			}
			for (C4Variable p : parameter) {
				if (p.getName().equals(declarationName))
					return p;
			}
		}
		return null;
	}
	
	public C4Variable findVariable(String variableName) {
		return findDeclaration(variableName, C4Variable.class);
	}

	public boolean isOldStyle() {
		return isOldStyle;
	}

	public void setOldStyle(boolean isOldStyle) {
		this.isOldStyle = isOldStyle;
	}
	
	public C4Function getInherited() {
		
		// search in #included scripts
		C4ScriptBase[] includes = getScript().getIncludes();
		for (int i = includes.length-1; i >= 0; i--) {
			C4Function fun = includes[i].findFunction(getName());
			if (fun != null && fun != this)
				return fun;
		}
		
		// search in index
		List<C4Declaration> decsWithSameName = getScript().getIndex().getDeclarationMap().get(this.getName());
		if (decsWithSameName != null) {
			C4Function f = null;
			int rating = -1;
			for (C4Declaration d : decsWithSameName) {
				// get latest version since getInherited() might also be called when finding links in a modified but not yet saved script
				// in which case the calling function (on-the-fly-parsed) differs from the function in the index 
				d = d.latestVersion();
				if (d == this || !(d instanceof C4Function))
					continue;
				int rating_ = 0;
				if (d.getParentDeclaration() == this.getParentDeclaration())
					rating_++;
				if (rating_ > rating) {
					f = (C4Function) d;
					rating = rating_;
				}
			}
			if (f != null)
				return f;
		}
		
		// search in engine
		C4Function f = getScript().getIndex().getEngine().findFunction(getName());
		if (f != null)
			return f;
		
		return null;
	}
	
	public C4Function baseFunction() {
		C4Function result = this;
		for (C4Function f = this; f != null; f = f.getInherited())
			result = f;
		return result;
	}

	@Override
	public boolean hasSubDeclarationsInOutline() {
		return getLocalVars() != null && getLocalVars().size() > 0;
	}

	@Override
	public Object[] getSubDeclarationsForOutline() {
		return getLocalVars().toArray();
	}

	public void createParameters(int num) {
		for (int i = parameter.size(); i < num; i++) {
			parameter.add(new C4Variable("par"+i, C4VariableScope.VAR)); //$NON-NLS-1$
		}
	}

	public SourceLocation getHeader() {
		return header;
	}

	public void setHeader(SourceLocation header) {
		this.header = header;
	}
	
	public void printHeader(StringBuilder output) {
		printHeader(output, isOldStyle());
	}
	
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
	
	public String getHeaderString(boolean oldStyle) {
		StringBuilder builder = new StringBuilder();
		printHeader(builder, oldStyle);
		return builder.toString();
	}
	
	public String getHeaderString() {
		return getHeaderString(isOldStyle());
	}

	@Override
	public void expectedToBeOfType(IType t) {
		ITypedDeclaration.Default.expectedToBeOfType(this, t);
	}

	public void inferTypeFromAssignment(ExprElm val, C4ScriptParser context) {
		ITypedDeclaration.Default.inferTypeFromAssignment(this, val, context);
	}

	@Override
	public IType getType() {
		return getReturnType();
	}

	@Override
	public void forceType(IType type) {
		setReturnType(type);
		setReturnObjectType(C4TypeSet.objectIngredient(type));
	}
	
	public void setReturnType(IType returnType) {
		this.returnType = returnType;
	}
	
	public void setReturnObjectType(C4Object objType) {
		if (objType != null) {
			if (returnObjectType == null)
				returnObjectType = new ObjectType();
			returnObjectType.setObject(objType);
		} else {
			returnObjectType = null;
		}
	}
	
	public C4Object getReturnObjectType() {
		return returnObjectType != null ? returnObjectType.getObject() : null;
	}

	public IType getCombinedType() {
		return C4TypeSet.create(getReturnObjectType(), getReturnType());
	}
	
	@Override
	public C4Object getObjectType() {
		return null;
	}
	
	public void setObjectType(C4Object object) {
		//expectedContent = object;
	}
	
	@Override
	public void postSerialize(C4Declaration parent) {
		super.postSerialize(parent);
		if (returnObjectType != null && parent instanceof C4ScriptBase) {
			returnObjectType.restoreType((C4ScriptBase) parent);
		}
	}
	
	/**
	 * Returns whether this function inherits from the calling function
	 * @param otherFunc
	 * @return true if related, false if not
	 */
	public boolean inheritsFrom(C4Function otherFunc) {
		for (C4Function f = this; f != null; f = f.getInherited())
			if (otherFunc == f)
				return true;
		return false;
	}
	
	/**
	 * Returns whether the function passed to this method is in the same override line as the calling function 
	 * @param otherFunc
	 * @return true if both functions are related, false if not
	 */
	public boolean isRelatedFunction(C4Function otherFunc) {
		if (this.inheritsFrom(otherFunc))
			return true; 
		for (C4Function f = this; f != null; f = f.getInherited())
			if (otherFunc.inheritsFrom(f))
				return true;
		return false;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Iterable<C4Declaration> allSubDeclarations() {
		return new CompoundIterable<C4Declaration>(localVars, parameter); 
	}
	
	@Override
	public boolean isGlobal() {
		return getVisibility() == C4FunctionScope.GLOBAL;
	}
	
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
	public void absorb(C4Declaration declaration) {
		if (declaration instanceof C4Function) {
			C4Function f = (C4Function) declaration;
			if (f.parameter.size() >= this.parameter.size())
				this.parameter = f.parameter;
			this.returnType = f.returnType;
			f.parameter = null;
		}
		super.absorb(declaration);
	}
	
}
