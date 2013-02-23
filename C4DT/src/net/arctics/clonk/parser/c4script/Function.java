package net.arctics.clonk.parser.c4script;

import static net.arctics.clonk.util.ArrayUtil.map;
import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.defaulting;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.IIndexEntity;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.ASTNodePrinter;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.IEvaluationContext;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.parser.c4script.Variable.Scope;
import net.arctics.clonk.parser.c4script.ast.AppendableBackedExprWriter;
import net.arctics.clonk.parser.c4script.ast.ControlFlowException;
import net.arctics.clonk.parser.c4script.ast.FunctionBody;
import net.arctics.clonk.parser.c4script.ast.ReturnException;
import net.arctics.clonk.parser.c4script.ast.TypeChoice;
import net.arctics.clonk.parser.c4script.ast.TypingJudgementMode;
import net.arctics.clonk.resource.ProjectSettings.Typing;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.IConverter;
import net.arctics.clonk.util.IHasUserDescription;
import net.arctics.clonk.util.IPredicate;
import net.arctics.clonk.util.StringUtil;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IRegion;

/**
 * A function in a script.
 * @author ZokRadonh
 *
 */
public class Function extends Structure implements Serializable, ITypeable, IHasUserDescription, IEvaluationContext, IHasCode {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
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
	private boolean staticallyTyped;
	private SourceLocation bodyLocation, header;
	private int nameStart;

	/**
	 * Code block kept in memory for speed optimization
	 */
	private FunctionBody body;

	/**
	 * Hash code of the string the block was parsed from.
	 */
	private int blockSourceHash;

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
			var.setParent(this);
		}
		visibility = FunctionScope.GLOBAL;
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
	public List<Variable> locals() {
		return localVars;
	}

	/**
	 * @return the parameter
	 */
	public List<Variable> parameters() {
		return parameters;
	}

	public Variable addParameter(Variable parameter) {
		synchronized (parameters) {
			parameters.add(parameter);
			return parameter;
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

	public IType returnType(Script script) {
		IType retType = null;
		if (script != null && script.functionReturnTypes() != null)
			retType = script.functionReturnTypes().get(this.name());
		if (retType == null)
			retType = returnType();
		return retType;
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

	public final class FunctionInvocation implements IEvaluationContext {
		private final Object[] args;
		private final IEvaluationContext up;
		private final Object context;
		public FunctionInvocation(Object[] args, IEvaluationContext up, Object context) {
			this.args = args;
			this.up = up;
			this.context = context;
		}
		@Override
		public Object valueForVariable(String varName) {
			int i = 0;
			for (Variable v : parameters) {
				if (v.name().equals(varName))
					return args[i];
				i++;
			}
			return up != null ? up.valueForVariable(varName) : null;
		}
		@Override
		public Script script() { return Function.this.script(); }
		@Override
		public void reportOriginForExpression(ASTNode expression, IRegion location, IFile file) {
			Function.this.reportOriginForExpression(expression, location, file);
		}
		@Override
		public Function function() { return Function.this; }
		@Override
		public int codeFragmentOffset() { return Function.this.codeFragmentOffset(); }
		@Override
		public Object[] arguments() { return args; }
		@Override
		public Object cookie() { return context; }
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

	public enum ParameterStringOption {
		FunctionName,
		EngineCompatible,
		ParameterComments
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
	public String longParameterString(EnumSet<ParameterStringOption> options) {
		StringBuilder string = new StringBuilder();
		if (options.contains(ParameterStringOption.FunctionName)) {
			string.append(name());
			string.append("("); //$NON-NLS-1$
		}
		printParameterString(new AppendableBackedExprWriter(string), options);
		if (options.contains(ParameterStringOption.FunctionName))
			string.append(")"); //$NON-NLS-1$
		return string.toString();
	}

	@Override
	public String displayString(IIndexEntity context) {
		return longParameterString(EnumSet.of(ParameterStringOption.FunctionName, ParameterStringOption.EngineCompatible));
	}

	private void printParameterString(ASTNodePrinter output, final EnumSet<ParameterStringOption> options) {
		if (numParameters() > 0)
			StringUtil.writeBlock(output, "", "", ", ", map(parameters(), new IConverter<Variable, String>() {
				final boolean engineCompatible = options.contains(ParameterStringOption.EngineCompatible);
				@Override
				public String convert(Variable par) {
					IType type = engineCompatible ? par.type().simpleType() : par.type();
					type = TypeChoice.remove(type, new IPredicate<IType>() {
						@Override
						public boolean test(IType item) {
							return item instanceof ParameterType;
						}
					});
					if (engineCompatible && !par.isActualParm())
						return null;
					String comment = par.userDescription() != null && options.contains(ParameterStringOption.ParameterComments) ? ("/* " + par.userDescription() + "*/ ") : "";
					if (type != PrimitiveType.UNKNOWN && type != null &&
						(!engineCompatible || (type instanceof PrimitiveType && type != PrimitiveType.ANY)))
						return comment + type.typeName(false) + " " + par.name();
					else
						return comment + par.name();
				}
			}));
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
	 * Set the body location.
	 * @param body the body location to set
	 */
	public void setBodyLocation(SourceLocation body) {
		this.bodyLocation = body;
	}

	/**
	 * @return the body the location.
	 */
	public SourceLocation bodyLocation() {
		return bodyLocation;
	}

	public SourceLocation wholeBody() {
		if (bodyLocation == null)
			return null;
		if (isOldStyle())
			return bodyLocation;
		else
			return new SourceLocation(bodyLocation.start()-1, bodyLocation.end()+1);
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
	public String infoText(IIndexEntity context) {
		String description = obtainUserDescription();
		StringBuilder builder = new StringBuilder();
		String scriptPath = script().resource() != null
			? script().resource().getProjectRelativePath().toOSString()
			: script().name();
		for (String line : new String[] {
			MessageFormat.format("<i>{0}</i><br/>", scriptPath), //$NON-NLS-1$ //$NON-NLS-2$
			MessageFormat.format("<b>{0}</b><br/>", longParameterString(EnumSet.of(ParameterStringOption.FunctionName, ParameterStringOption.EngineCompatible))), //$NON-NLS-1$ //$NON-NLS-2$
			"<br/>", //$NON-NLS-1$
			description != null && !description.equals("") ? description : Messages.DescriptionNotAvailable, //$NON-NLS-1$
			"<br/>", //$NON-NLS-1$
		})
			builder.append(line);
		if (numParameters() > 0) {
			builder.append(MessageFormat.format("<br/><b>{0}</b><br/>", Messages.Parameters)); //$NON-NLS-1$ //$NON-NLS-3$
			for (Variable p : parameters())
				builder.append(MessageFormat.format("<b>{0} {1}</b> {2}<br/>", StringUtil.htmlerize(p.type().typeName(true)), p.name(), p.userDescription())); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			builder.append("<br/>"); //$NON-NLS-1$
		}
		IType retType = returnType(as(context, Script.class));

		if (retType != PrimitiveType.UNKNOWN) {
			builder.append(MessageFormat.format("<br/><b>{0} </b>{1}<br/>", //$NON-NLS-1$
				Messages.Returns,
				StringUtil.htmlerize(retType.typeName(true))));
			if (returnDescription != null)
				builder.append(StringUtil.htmlerize(returnDescription)+"<br/>");
		}
		return builder.toString();
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
			for (Variable v : localVars)
				if (v.name().equals(declarationName))
					return v;
			for (Variable p : parameters)
				if (p.name().equals(declarationName))
					return p;
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

	private transient Function cachedInherited;

	private static final Function NO_FUNCTION = new Function();

	/**
	 * Return the function this one inherits from
	 * @return The inherited function
	 */
	public Function inheritedFunction() {
		return cachedInherited == NO_FUNCTION ? null : cachedInherited;
	}

	public final void findInherited() {
		cachedInherited = defaulting(internalInherited(), NO_FUNCTION);
	}

	private Function internalInherited() {
		// search in #included scripts
		Collection<Script> includesCollection = script().includes(0);
		Script[] includes = includesCollection.toArray(new Script[includesCollection.size()]);
		for (int i = includes.length-1; i >= 0; i--) {
			Function fun = includes[i].findFunction(name());
			if (fun != null && fun != this)
				return fun;
		}

		// search global
		Function global = index().findGlobal(Function.class, name());
		if (global != null && global != this)
			return global;

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
			if (!alreadyVisited.add(f))
				break;
			result = f;
		}
		return result;
	}

	@Override
	public Object[] subDeclarationsForOutline() {
		return ArrayUtil.concat(locals().toArray(), otherDeclarations != null ? otherDeclarations.toArray() : null);
	}

	/**
	 * Create num generically named parameters (par1, par2, ...)
	 * @param num Number of parameters to create
	 */
	public void createParameters(int num) {
		for (int i = parameters.size(); i < num; i++)
			parameters.add(new Variable("par"+i, Scope.VAR)); //$NON-NLS-1$
	}

	/**
	 * Return the location of the function header
	 * @return
	 */
	public SourceLocation header() { return header; }
	/**
	 * Set the location of the function header.
	 * @param header
	 */
	public void setHeader(SourceLocation header) { this.header = header; }

	public void setNameStart(int start) { this.nameStart = start; }
	@Override
	public int nameStart() { return nameStart; }

	/**
	 * Print the function header into the passed string builder
	 * @param output The StringBuilder to add the header string to
	 */
	public void printHeader(ASTNodePrinter output) {
		printHeader(output, isOldStyle());
	}

	/**
	 * Print the function header into the passed string builder
	 * @param output The StringBuilder to add the header string to
	 * @param oldStyle Whether to print in old 'label-style'
	 */
	public void printHeader(ASTNodePrinter output, boolean oldStyle) {
		output.append(visibility().toString());
		if (!oldStyle) {
			output.append(" "); //$NON-NLS-1$
			output.append(Keywords.Func);
			Typing typing = typing();
			switch (typing) {
			case Dynamic:
				break;
			case ParametersOptionallyTyped:
				if (returnType == PrimitiveType.REFERENCE)
					output.append(" &");
				break;
			case Static:
				output.append(" ");
				output.append(returnType.typeName(false));
				break;
			}
		}
		output.append(" "); //$NON-NLS-1$
		output.append(name());
		if (!oldStyle) {
			output.append("("); //$NON-NLS-1$
			printParameterString(output, EnumSet.of(ParameterStringOption.FunctionName));
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
		printHeader(new AppendableBackedExprWriter(builder), oldStyle);
		return builder.toString();
	}

	/*+
	 * Return the header string
	 */
	public String headerString() {
		return headerString(isOldStyle());
	}

	@Override
	public void expectedToBeOfType(IType t, TypingJudgementMode mode) {
		if (mode == TypingJudgementMode.Force)
			ITypeable.Default.expectedToBeOfType(this, t);
	}

	@Override
	public IType type() {
		return returnType();
	}

	@Override
	public void forceType(IType type) {
		this.returnType = type;
		this.staticallyTyped = true;
	}

	@Override
	public void assignType(IType returnType, boolean _static) {
		if (!staticallyTyped || _static) {
			this.returnType = returnType;
			this.staticallyTyped = _static;
		}
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
	 * @param context TODO
	 * @return the result
	 */
	public Object invoke(IEvaluationContext context) {
		try {
			return body != null ? body.evaluate(context) : null;
		} catch (ReturnException result) {
			return result.result();
		} catch (ControlFlowException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public void sourceCodeRepresentation(StringBuilder builder, Object cookie) {
		builder.append(visibility().toKeyword());
		builder.append(" "); //$NON-NLS-1$
		builder.append(Keywords.Func);
		builder.append(" "); //$NON-NLS-1$
		builder.append(longParameterString(EnumSet.of(ParameterStringOption.FunctionName)));
		switch (Conf.braceStyle) {
		case NewLine:
			builder.append("\n{\n"); //$NON-NLS-1$
			break;
		case SameLine:
			builder.append(" {\n"); //$NON-NLS-1$
			break;
		}
		if (cookie instanceof ASTNode)
			((ASTNode)cookie).print(builder, 1);
		builder.append("\n}"); //$NON-NLS-1$
	}

	/**
	 * Remove local variables.
	 */
	public void clearLocalVars() {
		localVars.clear();
		if (otherDeclarations != null)
			otherDeclarations.clear();
	}

	/**
	 * Add declaration that is neither parameter nor variable. Most likely an implicit proplist.
	 * @param d The declaration to add
	 * @return Return d. Any proplist declarations already added to the other declarations list with the same location will be removed in favor of d.
	 */
	public Declaration addOtherDeclaration(Declaration d) {
		if (otherDeclarations == null)
			otherDeclarations = new ArrayList<Declaration>(3);
		else
			for (Iterator<Declaration> it = otherDeclarations.iterator(); it.hasNext();) {
				Declaration existing = it.next();
				if (existing.sameLocation(d)) {
					it.remove();
					break;
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
		if (otherDeclarations == null)
			return NO_OTHER_DECLARATIONS;
		else
			return otherDeclarations;
	}

	@Override
	public boolean staticallyTyped() {
		return staticallyTyped;
	}

	public void resetLocalVarTypes() {
		for (Variable v : locals())
			v.forceType(PrimitiveType.UNKNOWN);
	}

	public void storeBody(ASTNode block, String source) {
		body = (FunctionBody)block;
		blockSourceHash = source.hashCode();
		if (bodyLocation != null)
			body.setLocation(0, bodyLocation.getLength());
		body.setParent(this);
	}

	/**
	 * Return cached code block if it was created from the given source. This is tested by hash code of the source string.
	 * @param source The source to test against
	 * @return The code block or null if it was created from differing source.
	 */
	public FunctionBody bodyMatchingSource(String source) {
		if (source == null || (blockSourceHash != -1 && blockSourceHash == source.hashCode())) {
			if (body != null)
				body.postLoad(this, TypeUtil.problemReportingContext(this));
			return body;
		} else
			return body = null;
	}

	/**
	 * Return the cached block without performing checks.
	 * @return The cached code block
	 */
	public FunctionBody body() {
		return bodyMatchingSource(null);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Declaration> T latestVersionOf(T from) {
		if (from instanceof Variable)
			return super.latestVersionOf(from);
		else {
			if (otherDeclarations == null)
				return null;
			for (Declaration other : otherDeclarations)
				if (other.getClass() == from.getClass() && other.sameLocation(from))
					return (T) other;
		}
		return null;
	}

	/**
	 * Assign parameter types to existing parameters. If more types than parameters are given, no new parameters will be created.
	 * @param types The types to assign to the parameters
	 */
	public void assignParameterTypes(IType... types) {
		if (types == null)
			return;
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
		return bodyLocation().getOffset();
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
	public Object cookie() { return null; }
	@Override
	public Function function() { return this; }
	@Override
	public void reportOriginForExpression(ASTNode expression, IRegion location, IFile file) { /* oh interesting */ }
	@Override
	public int codeFragmentOffset() { return bodyLocation != null ? bodyLocation.getOffset() : 0; }
	@Override
	public boolean isLocal() { return true; }
	@Override
	public ASTNode code() { return body(); }

	public static String scaffoldTextRepresentation(String functionName, FunctionScope scope, Variable... parameters) {
		StringBuilder builder = new StringBuilder();
		Function f = new Function(functionName, scope);
		for (Variable p : parameters)
			f.addParameter(p);
		ASTNodePrinter printer = new AppendableBackedExprWriter(builder);
		f.printHeader(printer);
		Conf.blockPrelude(printer, 0);
		builder.append("{\n"); //$NON-NLS-1$
		builder.append(Conf.indentString);
		builder.append("\n}"); //$NON-NLS-1$
		return builder.toString();
	}

	@Override
	public ASTNode[] subElements() { return new ASTNode[] { body() }; }
	@Override
	public void setSubElements(ASTNode[] elms) { storeBody(elms[0], ""); }

	@Override
	public void doPrint(ASTNodePrinter output, int depth) {
		printHeader(output);
		Conf.blockPrelude(output, depth);
		body.print(output, depth);
	}

}
