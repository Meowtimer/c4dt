package net.arctics.clonk.parser.c4script;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.C4Object;
import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.C4Structure;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.ExprElm;
import net.arctics.clonk.parser.c4script.C4Variable.C4VariableScope;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.util.CompoundIterable;

public class C4Function extends C4Structure implements Serializable, ITypedDeclaration, IHasUserDescription {

	private static final long serialVersionUID = 3848213897251037684L;
	private C4FunctionScope visibility; 
	private List<C4Variable> localVars;
	private List<C4Variable> parameter;
	private C4Type returnType;
	private String description;
	private boolean isCallback;
	private boolean isOldStyle;
	private SourceLocation body, header;
	//private transient C4Object expectedContent;

	public C4Function(String name, C4Type returnType, C4Variable... pars) {
		this.name = name;
		this.returnType = returnType;
		parameter = new ArrayList<C4Variable>(pars.length);
		for (C4Variable var : pars)
			parameter.add(var);
		visibility = C4FunctionScope.FUNC_GLOBAL;
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
		visibility = C4FunctionScope.FUNC_GLOBAL;
		name = "";
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
	public C4Type getReturnType() {
		if (returnType == null)
			returnType = C4Type.UNKNOWN;
		return returnType;
	}

	/**
	 * @param returnType the returnType to set
	 */
	public void setReturnType(C4Type returnType) {
		this.returnType = returnType;
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
		return description;
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
		FUNC_GLOBAL,
		FUNC_PUBLIC,
		FUNC_PROTECTED,
		FUNC_PRIVATE;
		
		private String lowerCaseName;
		
		public static C4FunctionScope makeScope(String scopeString) {
			if (scopeString == null) return C4FunctionScope.FUNC_PUBLIC;
			if (scopeString.equals(Keywords.Public)) return C4FunctionScope.FUNC_PUBLIC;
			if (scopeString.equals(Keywords.Protected)) return C4FunctionScope.FUNC_PROTECTED;
			if (scopeString.equals(Keywords.Private)) return C4FunctionScope.FUNC_PRIVATE;
			if (scopeString.equals(Keywords.Global)) return C4FunctionScope.FUNC_GLOBAL;
			//if (C4FunctionScope.valueOf(scopeString) != null) return C4FunctionScope.valueOf(scopeString);
			return C4FunctionScope.FUNC_PUBLIC;
		}
		
		@Override
		public String toString() {
			if (lowerCaseName == null)
				lowerCaseName = this.name().substring(5).toLowerCase();
			return lowerCaseName;
		}
	}
	
	/**
	 * Generates a function string in the form of
	 * function(int parName1, int parName2)
	 * if <code>withFuncName</code> is true, else
	 * int parName1, int parName2
	 * 
	 * @param withFuncName
	 * @return
	 */
	public String getLongParameterString(boolean withFuncName) {
		StringBuilder string = new StringBuilder();
		if (withFuncName) {
			string.append(getName());
			string.append("(");
		}
		printParameterString(string, true);
		if (withFuncName) string.append(")");
		return string.toString();
	}

	private void printParameterString(StringBuilder output, boolean engineCompatible) {
		if (getParameters().size() > 0) {
			for(C4Variable par : getParameters()) {
				if (par.getType() != C4Type.UNKNOWN && par.getType() != null) {
					if (!engineCompatible || (par.getType() != C4Type.ANY && par.getType() != C4Type.UNKNOWN)) {
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

	// to be called on engine functions
	public String getDocumentationURL() {
		String docURLTemplate = ClonkPreferences.getPreferenceOrDefault(ClonkPreferences.DOC_URL_TEMPLATE);
		return String.format(docURLTemplate, getName(), ClonkCore.getDefault().getLanguagePref().toLowerCase());
	}

	@Override
	public String getInfoText() {
		return String.format(Messages.C4Function_InfoTextTemplate, getLongParameterString(true), getUserDescription() != null && !getUserDescription().equals("") ? getUserDescription() : Messages.DescriptionNotAvailable, getScript().toString());
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
		if (getVisibility() == C4FunctionScope.FUNC_GLOBAL) {
			C4Function f = null;
			for (C4Function funcWithSameName : getScript().getIndex().declarationsWithName(this.name, C4Function.class)) {
				if (funcWithSameName == this) {
					break;
				}
				f = funcWithSameName;
			}
			if (f == null)
				f = ClonkCore.getDefault().getExternIndex().findGlobalFunction(getName());
			if (f == null)
				f = getScript().getIndex().getEngine().findFunction(getName());
			return f;
		}
		List<C4Declaration> decsWithSameName = getScript().getIndex().getDeclarationMap().get(this.getName());
		if (decsWithSameName != null) {
			C4Function inheritedInSameScript = null;
			for (C4Declaration d : decsWithSameName) {
				if (d == this)
					break;
				if (d instanceof C4Function && d.getParentDeclaration() == this.getParentDeclaration())
					inheritedInSameScript = (C4Function) d;
			}
			if (inheritedInSameScript != null)
				return inheritedInSameScript;
		}
		C4ScriptBase[] includes = getScript().getIncludes();
		for (int i = includes.length-1; i >= 0; i--) {
			C4Function field = includes[i].findFunction(getName());
			if (field != null && field != this)
				return field;
		}
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
		if (parameter.size() == 0)
			for (int i = 0; i < num; i++) {
				parameter.add(new C4Variable("par"+i, C4VariableScope.VAR_VAR));
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
			output.append(" ");
			output.append(Keywords.Func);
		}
		output.append(" ");
		output.append(getName());
		if (!oldStyle) {
			output.append("(");
			printParameterString(output, true);
			output.append(")");
		}
		else
			output.append(":");
	}
	
	public String getHeaderString(boolean oldStyle) {
		StringBuilder builder = new StringBuilder();
		printHeader(builder, oldStyle);
		return builder.toString();
	}
	
	public String getHeaderString() {
		return getHeaderString(isOldStyle());
	}

	public void expectedToBeOfType(C4Type t) {
		ITypedDeclaration.Default.expectedToBeOfType(this, t);
	}

	public void inferTypeFromAssignment(ExprElm val, C4ScriptParser context) {
		ITypedDeclaration.Default.inferTypeFromAssignment(this, val, context);
	}

	public C4Type getType() {
		return getReturnType();
	}

	public void forceType(C4Type type) {
		setReturnType(type);
	}

	public C4Object getObjectType() {
		return null;
	}
	
	public void setObjectType(C4Object object) {
		//expectedContent = object;
	}
	
	/**
	 * Returns whether this functino inherites from the calling function
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
		return getVisibility() == C4FunctionScope.FUNC_GLOBAL;
	}
	
	/**
	 * Invoke this function. Left empty for 'regular' functions, only defined in special interpreter functions
	 * @param args the arguments to pass to the function
	 * @return the result
	 */
	public Object invoke(Object... args) {
		return null;
	}
	
}
