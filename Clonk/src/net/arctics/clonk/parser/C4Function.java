package net.arctics.clonk.parser;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.C4ScriptExprTree.ExprElm;
import net.arctics.clonk.parser.C4ScriptParser.Keywords;
import net.arctics.clonk.parser.C4Variable.C4VariableScope;

public class C4Function extends C4Structure implements Serializable, ITypedField {

	private static final long serialVersionUID = 3848213897251037684L;
	private C4FunctionScope visibility; 
	private List<C4Variable> localVars;
	private List<C4Variable> parameter;
	private C4Type returnType;
	private String description;
	private boolean isCallback;
	private boolean isOldStyle;
	private SourceLocation body, header;
	private C4Object expectedContent;

	public C4Function(String name, C4Type type, C4Variable... pars) {
		this.name = name;
		this.returnType = type;
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
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
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
			if (scopeString.equals("public")) return C4FunctionScope.FUNC_PUBLIC;
			if (scopeString.equals("protected")) return C4FunctionScope.FUNC_PROTECTED;
			if (scopeString.equals("private")) return C4FunctionScope.FUNC_PRIVATE;
			if (scopeString.equals("global")) return C4FunctionScope.FUNC_GLOBAL;
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
		printParameterString(string, false);
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
		return String.format("http://www.clonk.de/docs/de/sdk/script/fn/%s.html",getName());
	}

	@Override
	public String getShortInfo() {
		if (getScript() == ClonkCore.getDefault().ENGINE_OBJECT) {
			//return String.format("<b>%s</b><br>%s<br><i><a href='%s'>Online Documentation</a></i>", getName(), getDescription(), getDocumentationURL());
			// engine function
			return String.format("<b>%s</b><br>%s", getLongParameterString(true), getUserDescription());
		}
		return getLongParameterString(true);
	}

	public C4Variable findVariable(String fieldName) {
		if (fieldName.equals(C4Variable.THIS.getName()))
			return C4Variable.THIS;
		for (C4Variable v : localVars) {
			if (v.getName().equals(fieldName))
				return v;
		}
		for (C4Variable p : parameter) {
			if (p.getName().equals(fieldName))
				return p;
		}
		return null;
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
			for (C4Function funcWithSameName : getScript().getIndex().fieldsWithName(this.name, C4Function.class)) {
				if (funcWithSameName == this) {
					break;
				}
				f = funcWithSameName;
			}
			if (f == null)
				f = ClonkCore.getDefault().EXTERN_INDEX.findGlobalFunction(getName());
			if (f == null)
				f = ClonkCore.getDefault().ENGINE_OBJECT.findFunction(getName());
			return f;
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
	public boolean hasChildFields() {
		return getLocalVars() != null && getLocalVars().size() > 0;
	}

	@Override
	public Object[] getChildFieldsForOutline() {
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
		ITypedField.Default.expectedToBeOfType(this, t);
	}

	public void inferTypeFromAssignment(ExprElm val, C4ScriptParser context) {
		ITypedField.Default.inferTypeFromAssignment(this, val, context);
	}

	public C4Type getType() {
		return getReturnType();
	}

	public void setType(C4Type type) {
		setReturnType(type);
	}

	public C4Object getExpectedContent() {
		return expectedContent;
	}

	public void setExpectedContent(C4Object object) {
		expectedContent = object;
	}
	
	public boolean inheritsFrom(C4Function otherFunc) {
		for (C4Function f = this; f != null; f = f.getInherited())
			if (otherFunc == f)
				return true;
		return false;
	}
	
	public boolean relatedFunction(C4Function otherFunc) {
		if (this.inheritsFrom(otherFunc))
			return true; 
		for (C4Function f = this; f != null; f = f.getInherited())
			if (otherFunc.inheritsFrom(f))
				return true;
		return false;
	}
	
}
