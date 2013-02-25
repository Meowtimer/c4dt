package net.arctics.clonk.parser.c4script.inference.dabble;

import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.isAnyOf;
import static net.arctics.clonk.util.Utilities.threadPool;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.CachedEngineDeclarations;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.EngineSettings;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.MetaDefinition;
import net.arctics.clonk.index.Scenario;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.EntityRegion;
import net.arctics.clonk.parser.IASTPositionProvider;
import net.arctics.clonk.parser.IASTVisitor;
import net.arctics.clonk.parser.IEvaluationContext;
import net.arctics.clonk.parser.IHasIncludes.GatherIncludesOptions;
import net.arctics.clonk.parser.Markers;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.Problem;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.TraversalContinuation;
import net.arctics.clonk.parser.c4script.ArrayType;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.Directive;
import net.arctics.clonk.parser.c4script.Directive.DirectiveType;
import net.arctics.clonk.parser.c4script.FindDeclarationInfo;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.Function.FunctionScope;
import net.arctics.clonk.parser.c4script.FunctionType;
import net.arctics.clonk.parser.c4script.IProplistDeclaration;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.ITypeable;
import net.arctics.clonk.parser.c4script.InitializationFunction;
import net.arctics.clonk.parser.c4script.InitializationFunction.VarInitializationAccess;
import net.arctics.clonk.parser.c4script.Keywords;
import net.arctics.clonk.parser.c4script.Operator;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.ProblemReportingContext;
import net.arctics.clonk.parser.c4script.ProblemReportingStrategy;
import net.arctics.clonk.parser.c4script.ProblemReportingStrategy.Capabilities;
import net.arctics.clonk.parser.c4script.ProplistDeclaration;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.parser.c4script.SpecialEngineRules;
import net.arctics.clonk.parser.c4script.SpecialEngineRules.SpecialFuncRule;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.Variable.Scope;
import net.arctics.clonk.parser.c4script.ast.AccessDeclaration;
import net.arctics.clonk.parser.c4script.ast.AccessVar;
import net.arctics.clonk.parser.c4script.ast.ArrayElementExpression;
import net.arctics.clonk.parser.c4script.ast.ArrayExpression;
import net.arctics.clonk.parser.c4script.ast.ArraySliceExpression;
import net.arctics.clonk.parser.c4script.ast.BinaryOp;
import net.arctics.clonk.parser.c4script.ast.BoolLiteral;
import net.arctics.clonk.parser.c4script.ast.BreakStatement;
import net.arctics.clonk.parser.c4script.ast.CallDeclaration;
import net.arctics.clonk.parser.c4script.ast.CallExpr;
import net.arctics.clonk.parser.c4script.ast.CallInherited;
import net.arctics.clonk.parser.c4script.ast.Comment;
import net.arctics.clonk.parser.c4script.ast.ConditionalStatement;
import net.arctics.clonk.parser.c4script.ast.ContinueStatement;
import net.arctics.clonk.parser.c4script.ast.ControlFlow;
import net.arctics.clonk.parser.c4script.ast.FloatLiteral;
import net.arctics.clonk.parser.c4script.ast.ForStatement;
import net.arctics.clonk.parser.c4script.ast.FunctionDescription;
import net.arctics.clonk.parser.c4script.ast.GarbageStatement;
import net.arctics.clonk.parser.c4script.ast.IDLiteral;
import net.arctics.clonk.parser.c4script.ast.ILoop;
import net.arctics.clonk.parser.c4script.ast.IfStatement;
import net.arctics.clonk.parser.c4script.ast.IntegerLiteral;
import net.arctics.clonk.parser.c4script.ast.IterateArrayStatement;
import net.arctics.clonk.parser.c4script.ast.Literal;
import net.arctics.clonk.parser.c4script.ast.MemberOperator;
import net.arctics.clonk.parser.c4script.ast.MissingStatement;
import net.arctics.clonk.parser.c4script.ast.NewProplist;
import net.arctics.clonk.parser.c4script.ast.Nil;
import net.arctics.clonk.parser.c4script.ast.OperatorExpression;
import net.arctics.clonk.parser.c4script.ast.Parenthesized;
import net.arctics.clonk.parser.c4script.ast.Placeholder;
import net.arctics.clonk.parser.c4script.ast.PropListExpression;
import net.arctics.clonk.parser.c4script.ast.ReturnStatement;
import net.arctics.clonk.parser.c4script.ast.Sequence;
import net.arctics.clonk.parser.c4script.ast.SimpleStatement;
import net.arctics.clonk.parser.c4script.ast.Statement;
import net.arctics.clonk.parser.c4script.ast.StringLiteral;
import net.arctics.clonk.parser.c4script.ast.Tuple;
import net.arctics.clonk.parser.c4script.ast.TypeChoice;
import net.arctics.clonk.parser.c4script.ast.TypeUnification;
import net.arctics.clonk.parser.c4script.ast.TypingJudgementMode;
import net.arctics.clonk.parser.c4script.ast.UnaryOp;
import net.arctics.clonk.parser.c4script.ast.UnaryOp.Placement;
import net.arctics.clonk.parser.c4script.ast.Unfinished;
import net.arctics.clonk.parser.c4script.ast.VarDeclarationStatement;
import net.arctics.clonk.parser.c4script.ast.VarInitialization;
import net.arctics.clonk.parser.c4script.ast.WhileStatement;
import net.arctics.clonk.parser.stringtbl.StringTbl;
import net.arctics.clonk.resource.ClonkBuilder;
import net.arctics.clonk.resource.ProjectSettings.Typing;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.IConverter;
import net.arctics.clonk.util.PerClass;
import net.arctics.clonk.util.Profiled;
import net.arctics.clonk.util.Sink;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

@Capabilities(capabilities=Capabilities.ISSUES|Capabilities.TYPING)
public class DabbleInference extends ProblemReportingStrategy {

	private static class Shared {
		C4ScriptParser[] parsers;
		IProgressMonitor monitor;
		final Map<Script, ScriptProcessor> processors = new HashMap<>();
	}
	private Shared shared;

	@Override
	public void initialize(Markers markers, ClonkBuilder builder) {
		super.initialize(markers, builder);
		shared = new Shared();
		shared.parsers = builder.parsers().toArray(new C4ScriptParser[builder.parsers().size()]);
		shared.monitor = builder.monitor();
	}

	@Override
	public void run() {
		threadPool(new Sink<ExecutorService>() {
			@Override
			public void receivedObject(ExecutorService pool) {
				for (C4ScriptParser p : shared.parsers)
					if (p != null)
						shared.processors.put(p.script(), new ScriptProcessor(p, shared));
				for (final ScriptProcessor processor : shared.processors.values())
					pool.execute(new Visitation(processor));
			}
		}, 20);
		for (ScriptProcessor processor : shared.processors.values())
			dismissExperts(processor.script());
	}

	@Override
	public ProblemReportingContext localTypingContext(Script script) {
		return localTypingContext(new C4ScriptParser(script));
	}

	@Override
	public ProblemReportingContext localTypingContext(C4ScriptParser parser) {
		markers = parser.markers();
		Shared shared = new Shared();
		ScriptProcessor processor = new ScriptProcessor(parser, shared);
		shared.processors.put(parser.script(), processor);
		return new Visitation(processor);
	}

	private static final class RoamingMarkers extends Markers {
		private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
		public final Markers oldMarkers;
		public final Script origin;
		public int depth;
		private RoamingMarkers(Markers oldMarkers, Script origin) {
			this.oldMarkers = oldMarkers;
			this.origin = origin;
			this.depth = 1;
		}
		@Override
		public void marker(IASTPositionProvider positionProvider, Problem code, ASTNode node, int markerStart, int markerEnd, int flags, int severity, Object... args) throws ParsingException {
			if (node == null || node.parentOfType(Script.class) != origin)
				return;
			else
				oldMarkers.marker(positionProvider, code, node, markerStart, markerEnd, flags, severity, args);
		}
	}

	public static class CurrentFunctionReturnTypeVariable extends FunctionReturnTypeVariable {
		public boolean pending = true;
		public CurrentFunctionReturnTypeVariable(Function function) { super(function); }
		@Override
		public boolean binds(ASTNode expr, Visitation visitation) {
			return expr instanceof ReturnStatement && expr.parentOfType(Function.class) == function;
		}
		@Override
		public boolean same(ITypeVariable other) {
			return other instanceof CurrentFunctionReturnTypeVariable && ((CurrentFunctionReturnTypeVariable)other).function == function;
		}
		@Override
		public void apply(boolean soft, Visitation visitation) { /* done by Dabble */ }
	}

	public static class InheritedFunctionReturnTypeVariable extends FunctionReturnTypeVariable {
		public InheritedFunctionReturnTypeVariable(Function function) { super(function); }
		@Override
		public boolean binds(ASTNode expr, Visitation visitation) {
			return expr instanceof CallInherited && expr.parentOfType(Function.class) == function;
		}
		@Override
		public boolean same(ITypeVariable other) {
			return other instanceof InheritedFunctionReturnTypeVariable && ((InheritedFunctionReturnTypeVariable)other).function == function;
		}
	}

	protected final class ScriptProcessor {
		final C4ScriptParser parser;
		final Script script;
		final Index index;
		final Typing typing;
		final CachedEngineDeclarations cachedEngineDeclarations;
		final Shared shared;
		final int strictLevel;
		final boolean hasAppendTo;
		final Map<Function, CurrentFunctionReturnTypeVariable> finishedFunctions = new HashMap<>();
		final Map<String, IType> functionReturnTypes = new HashMap<>();
		final Map<Variable, IType> variableTypes = new HashMap<>();
		final IType thisType;
		final Map<String, Declaration> variableMap = new HashMap<>();
		final Map<String, Declaration> functionMap = new HashMap<>();
		boolean finished = false;
		public Script script() { return script; }
		public ScriptProcessor(C4ScriptParser parser, Shared shared) {
			this.shared = shared;
			this.parser = parser;
			this.script = parser.script();
			this.index = parser.script().index();
			this.typing = parser.typing();
			this.cachedEngineDeclarations = this.script.engine().cachedDeclarations();
			this.strictLevel = script.strictLevel();
			this.thisType = TypeChoice.make(
				script,
				script instanceof Definition ? ((Definition)script).metaDefinition() : PrimitiveType.ID
			);
			boolean hasAppendTo = false;
			for (Directive d : script.directives())
				if (d.type() == DirectiveType.APPENDTO) {
					hasAppendTo = true;
					break;
				}
			this.hasAppendTo = hasAppendTo;
		}
	}
	
	public class Visitation implements Runnable, ProblemReportingContext, IEvaluationContext {
		
		static final int
			MAX_PAR = 10,
			MAX_NUMVAR = 20,
			UNKNOWN_PARAMETERNUM = MAX_PAR+1;
		static final boolean
			UNUSEDPARMWARNING = false;

		private final ScriptProcessor base;
		
		private ControlFlow controlFlow;
		private Markers markers;
		private List<Script> visitees;
		private TypeEnvironment typeEnvironment;

		public Visitation(ScriptProcessor data) {
			this.markers = DabbleInference.this.markers();
			this.base = data;
		}

		public final SpecialFuncRule specialRuleFor(CallDeclaration node, int role) {
			Engine engine = script().engine();
			if (engine != null && engine.specialRules() != null)
				return engine.specialRules().funcRuleFor(node.name(), role);
			else
				return null;
		}

		private boolean assignDefaultParmTypesToFunction(Function function) {
			if (base.parser.specialEngineRules() != null)
				for (SpecialFuncRule funcRule : base.parser.specialEngineRules().defaultParmTypeAssignerRules())
					if (funcRule.assignDefaultParmTypes(this, function))
						return true;
			return false;
		}

		private void initialParameterTypesFromCalls(Function function, Function baseFunction, IType[] callTypes) {
			List<CallDeclaration> calls = base.index.callsTo(function.name());
			if (calls != null)
				for (CallDeclaration call : calls) {
					Function f = call.parentOfType(Function.class);
					Script other = f.parentOfType(Script.class);
					ScriptProcessor processor = base.shared.processors.get(other);
					Visitation visitation;
					if (processor != null) {
						visitation = new Visitation(processor);
						visitation.visitFunction(f);
					} else
						visitation = null;
					Function ref = as(visitation != null ? visitation.obtainDeclaration(call) : call.declaration(), Function.class);
					if (ref != null) {
						ref = ref.baseFunction();
						if (ref != null)
							ref = (Function)ref.latestVersion();
						if (ref == baseFunction) {
							int parNum = Math.min(callTypes.length, call.params().length);
							for (int pa = 0; pa < parNum; pa++)
								if (function.parameter(pa).type() == PrimitiveType.UNKNOWN) {
									ASTNode concretePar = call.params()[pa];
									if (concretePar != null)
										callTypes[pa] = TypeUnification.unify(callTypes[pa],
											visitation != null ? visitation.typeOf(concretePar) : concretePar.inferredType());
								}
						}
					}
				}
		}

		@Override
		public ITypeVariable visitFunction(Function function) {
			if (function == null || function.body() == null)
				return null;
			Script funScript = function.script();
			if (roaming || (visitees != null && visitees.contains(funScript)) || script() == funScript) {
				CurrentFunctionReturnTypeVariable returnType;
				synchronized (base.finishedFunctions) {
					returnType = base.finishedFunctions.get(function);
					if (returnType != null)
						return returnType;
					else
						base.finishedFunctions.put(function, returnType = new CurrentFunctionReturnTypeVariable(function));
				}
				synchronized (returnType) {
					try {
						ASTNode[] statements = function.body().statements();
						TypeEnvironment env = newTypeEnvironment();
						{
							env.add(returnType);
							boolean ownedFunction = !roaming || funScript == script();
							if (!ownedFunction)
								for (Variable l : function.locals()) {
									ITypeVariable ti = requestTypeInfo(new AccessVar(l));
									if (ti != null)
										ti.set(PrimitiveType.UNKNOWN);
								}
							List<Variable> parameters = function.parameters();
							Function baseFunction = function.baseFunction();
							boolean typeFromCalls =
								ownedFunction && !assignDefaultParmTypesToFunction(function) &&
								base.typing == Typing.ParametersOptionallyTyped &&
								baseFunction.visibility() != FunctionScope.GLOBAL &&
								base.script instanceof Definition &&
								!(base.script instanceof Scenario);
							IType[] callTypes = new IType[parameters.size()];
							if (typeFromCalls && parameters.size() > 0)
								initialParameterTypesFromCalls(function, baseFunction, callTypes);
							for (int i = 0; i < callTypes.length; i++)
								if (callTypes[i] == null)
									callTypes[i] = function.parameter(i).parameterType();
							for (int i = 0; i < callTypes.length; i++) {
								Variable p = function.parameter(i);
								if (ownedFunction)
									p.assignType(callTypes[i], false);
								ITypeVariable varTypeInfo = requestTypeInfo(new AccessVar(p));
								if (varTypeInfo != null)
									varTypeInfo.set(callTypes[i]);
							}
							ControlFlow old = controlFlow;
							controlFlow = ControlFlow.Continue;
							for (ASTNode s : statements)
								visitNode(s, true);
							controlFlow = old;
						}
						if (!roaming)
							env.apply(this, false);
						endTypeEnvironment(env, true, true);
						warnAboutPossibleProblemsWithFunctionLocalVariables(function, statements);
						dismissExperts(function);
					}
					catch (ParsingException e) { return null; }
					finally {
						function.assignType(returnType.get(), false);
						returnType.pending = false;
						returnType.notifyAll();
					}
				}
				return returnType;
			}
			return null;
		}

		@Override
		public void incompatibleTypes(ASTNode node, IRegion region, IType left, IType right) {
			try {
				if (left == null)
					left = PrimitiveType.ANY;
				if (right == null)
					right = PrimitiveType.ANY;
				this.markers().marker(this, Problem.IncompatibleTypes, node, region.getOffset(), region.getOffset()+region.getLength(), Markers.NO_THROW,
					base.typing == Typing.Static ? IMarker.SEVERITY_ERROR : IMarker.SEVERITY_WARNING,
					left.typeName(true), right.typeName(true)
				);
			} catch (ParsingException e) {}
		}

		/**
		 * Let an expression report errors. Calling {@link ASTNode#reportProblems(C4ScriptParser)} indirectly like that ensures
		 * that error markers created will be decorated with information about the expression reporting the error.
		 * @param expression The expression to report errors.
		 * @throws ParsingException
		 * @return The expression parameter is returned to allow for expression chaining.
		 */
		public final <T extends ASTNode> T visitNode(T expression, boolean recursive) throws ParsingException {
			if (expression == null)
				return null;
			Expert<? super T> expert = expert(expression);
			ControlFlow old = controlFlow;
			if (recursive && !expert.skipReportingProblemsForSubElements())
				for (ASTNode e : expression.subElements())
					if (e != null)
						visitNode(e, true);
			controlFlow = old;
			expert.visit(expression, this);
			if (controlFlow == ControlFlow.Continue)
				controlFlow = expression.controlFlow();
			return expression;
		}

		public TypeEnvironment newTypeEnvironment() {
			TypeEnvironment l = new TypeEnvironment();
			l.up = typeEnvironment;
			return typeEnvironment = l;
		}

		public void endTypeEnvironment(TypeEnvironment env, boolean inject, boolean ignoreLocals) {
			if (inject && env.up != null)
				env.up.inject(env, ignoreLocals);
			typeEnvironment = env.up;
		}

		/**
		 * Requests type information for an expression
		 * @param expression the expression
		 * @return the type information or null if none has been stored
		 */
		public ITypeVariable requestTypeInfo(ASTNode expression) {
			TypeEnvironment env = typeEnvironment;
			if (env == null || base.typing == Typing.Static || base.typing == Typing.Dynamic)
				return null;
			boolean topMostLayer = true;
			ITypeVariable base = null;
			for (TypeEnvironment list = env; list != null; list = list.up) {
				for (ITypeVariable info : list)
					if (info.binds(expression, this))
						if (!topMostLayer) {
							base = info;
							break;
						}
						else
							return info;
				topMostLayer = false;
			}
			ITypeVariable newlyCreated = expert(expression).createTypeVariable(expression, this);
			if (newlyCreated != null) {
				if (base != null)
					newlyCreated.merge(base);
				env.add(newlyCreated);
			}
			return newlyCreated;
		}

		/**
		 * Query the type of an arbitrary expression. With some luck the parser will be able to give an answer.
		 * @param expression the expression to query the type of
		 * @return The typeinfo or null if nothing was found
		 */
		public ITypeVariable queryTypeInfo(ASTNode expression) {
			TypeEnvironment env = typeEnvironment;
			if (env == null)
				return null;
			for (TypeEnvironment list = env; list != null; list = list.up)
				for (ITypeVariable info : list)
					if (info.binds(expression, this))
						return info;
			return null;
		}

		/**
		 * Look up stored type information for the passed expression, defaulting to the specified type if no
		 * information could be found.
		 * @param expression The expression to query the type for
		 * @param defaultType Default type to return if no type was found.
		 * @return Expression type as deduced by usage of the expression or the default type.
		 */
		@Override
		public IType queryTypeOfExpression(ASTNode expression, IType defaultType) {
			ITypeVariable info = queryTypeInfo(expression);
			return info != null ? info.get() : defaultType;
		}

		private boolean createWarningAtDeclarationOfVariable(
			ASTNode[] statements,
			Variable variable,
			Problem code,
			Object... format
		) {
			for (ASTNode s : statements)
				for (VarDeclarationStatement decl : s.collectionExpressionsOfType(VarDeclarationStatement.class))
					for (VarInitialization initialization : decl.variableInitializations())
						if (initialization.variable == variable) {
							this.markers().warning(this, code, initialization, initialization, 0, format);
							return true;
						}
			return false;
		}

		/**
		 * Warn about variables declared inside the given block that have not been referenced elsewhere ({@link Variable#isUsed() == false})
		 * @param func The function the block belongs to.
		 * @param block The {@link Block}
		 */
		public void warnAboutPossibleProblemsWithFunctionLocalVariables(Function func, ASTNode[] statements) {
			if (func == null)
				return;
			if (UNUSEDPARMWARNING)
				for (Variable p : func.parameters())
					if (!p.isUsed())
						this.markers().warning(this, Problem.UnusedParameter, null, p, Markers.ABSOLUTE_MARKER_LOCATION, p.name());
			if (func.locals() != null)
				for (Variable v : func.locals()) {
					if (!v.isUsed())
						createWarningAtDeclarationOfVariable(statements, v, Problem.Unused, v.name());
					Variable shadowed = script().findVariable(v.name());
					// ignore those pesky static variables from scenario scripts
					if (shadowed != null && !(shadowed.parentDeclaration() instanceof Scenario))
						createWarningAtDeclarationOfVariable(statements, v, Problem.IdentShadowed, v.qualifiedName(), shadowed.qualifiedName());
				}
		}

		private final void startRoaming() {
			if (markers instanceof RoamingMarkers)
				((RoamingMarkers)markers).depth++;
			else {
				markers = new RoamingMarkers(markers, script());
				roaming = true;
			}
		}
		private final void endRoaming() {
			if (--((RoamingMarkers)markers).depth == 0) {
				markers = ((RoamingMarkers)markers).oldMarkers;
				roaming = false;
			}
		}
		private boolean roaming = false;

		private void visit(Script script, boolean roaming) {
			script.requireLoaded();
			assignExperts(script);
			if (roaming)
				startRoaming();
			for (Function f : script.functions()) {
				// skip function that have been overridden
				if (roaming && !script().seesFunction(f))
					continue;
				visitFunction(f);
			}
			if (roaming)
				endRoaming();
		}
		
		@Override
		@Profiled
		public void reportProblems() {
			synchronized (base) {
				if (!base.finished) {
					base.finished = true;
					internalWork();
				}
			}
		}

		private void internalWork() {
			// revisit all inherited scripts since that is the only way to
			// accurately type inherited functions with respect to added things from this script
			TypeEnvironment env1 = newTypeEnvironment();
			{
				visitees = script().conglomerate();
				for (Script include : script().includes(GatherIncludesOptions.Recursive))
					visit(include, true);
				visitees = Arrays.asList(script());
				storeTypings(env1);
				for (ITypeVariable ti : env1) {
					Declaration d = ti.declaration(this);
					if (d != null && d.containedIn(script()))
						ti.apply(false, this);
				}
			}
			endTypeEnvironment(env1, false, false);

			TypeEnvironment env2 = newTypeEnvironment();
			{
				visit(script(), false);
				storeTypings(env2);
				script().setTypings(base.variableTypes, base.functionReturnTypes);
				env2.apply(this, false);
			}
			endTypeEnvironment(env2, false, false);
		}

		private void storeTypings(TypeEnvironment typeEnvironment) {
			for (ITypeVariable info : typeEnvironment) {
				VariableTypeVariable vti = as(info, VariableTypeVariable.class);
				if (vti != null && vti.variable().scope() == Scope.LOCAL)
					base.variableTypes.put(vti.variable(), vti.get());
				FunctionReturnTypeVariable ftri = as(info, FunctionReturnTypeVariable.class);
				if (ftri != null && ftri.function().visibility() != FunctionScope.GLOBAL && base.script.seesFunction(ftri.function()))
					base.functionReturnTypes.put(ftri.function().name(), ftri.get());
			}
		}

		@Override
		public void run() {
			if (base.finished)
				return;
			if (base.shared.monitor.isCanceled())
				return;
			base.shared.monitor.subTask(String.format("Reporting problems for '%s'", script().name()));
			reportProblems();
			base.shared.monitor.worked(1);
		}

		@Override
		public void storeType(ASTNode node, IType type) {
			ITypeVariable requested = requestTypeInfo(node);
			if (requested != null)
				requested.set(type);
		}

		@Override
		public Definition definition() { return base.parser.definition(); }
		@Override
		public SourceLocation absoluteSourceLocationFromExpr(ASTNode expression) {
			Function f = expression.parentOfType(Function.class);
			int bodyOffset = f != null ? f.bodyLocation().start() : 0;
			return base.parser.absoluteSourceLocation(expression.start()+bodyOffset, expression.end()+bodyOffset);
		}
		@Override
		public CachedEngineDeclarations cachedEngineDeclarations() { return base.cachedEngineDeclarations; }
		@Override
		public Script script() { return base.script; }
		@Override
		public IFile file() { return script().scriptFile(); }
		@Override
		public Declaration container() { return script(); }
		@Override
		public int fragmentOffset() { return base.parser.fragmentOffset(); }
		@Override
		public IType typeOf(ASTNode node) { return ty(node, this); }
		@Override
		public boolean isModifiable(ASTNode node) { return expert(node).isModifiable(node, this); }
		@Override
		public BufferedScanner scanner() { return base.parser; }

		@Override
		public <T extends AccessDeclaration> Declaration obtainDeclaration(T access) {
			@SuppressWarnings("unchecked")
			AccessDeclarationExpert<T> expert = (AccessDeclarationExpert<T>)expert(access);
			return expert.obtainDeclaration(access, this);
		}

		@Override
		public boolean validForType(ASTNode node, IType type) {
			return expert(node).unifyDeclaredAndGiven(node, type, this) != null;
		}

		@Override
		public void assignment(ASTNode leftSide, ASTNode rightSide) {
			expert(leftSide).assignment(leftSide, rightSide, this);
		}

		@Override
		public void typingJudgement(ASTNode node, IType type, TypingJudgementMode mode) {
			expert(node).typingJudgement(node, type, this, mode);
		}

		@Override
		@SuppressWarnings("unchecked")
		public final <T extends IType> T typeOf(ASTNode node, Class<T> cls) {
			for (IType t : typeOf(node))
				if (cls.isInstance(t))
					return (T)t;
			return null;
		}

		public final <T extends ASTNode> Expert<? super T> expert(T node) {
			return DabbleInference.this.expert(node);
		}

		@Override
		public Markers markers() { return markers; }
		@Override
		public Object valueForVariable(String varName) { return null; }
		@Override
		public Object[] arguments() { return null; }
		@Override
		public Function function() { return null; }
		@Override
		public int codeFragmentOffset() { return 0; }
		@Override
		public void reportOriginForExpression(ASTNode expression, IRegion location, IFile file) {}
		@Override
		public Object cookie() { return null; }
	}

	/**
	 * An object that knows how to answer various questions about nodes of type T.
	 * These questions span
	 * <ul>
	 * 	<li>typing ({@link Expert#type(ASTNode, Visitation)})</li>
	 *  <li></li>
	 * </ul>
	 * @author madeen
	 *
	 * @param <T>
	 */
	class Expert<T extends ASTNode> extends PerClass<ASTNode, T, Expert<? super T>> {
		public Expert(Class<T> cls) { super(cls); }
		public void findSuper() {
			for (Class<? super T> s = cls.getSuperclass(); s != null; s = s.getSuperclass()) {
				@SuppressWarnings("unchecked")
				Expert<? super T> sup = (Expert<? super T>)committee.get(s);
				if (sup != null) {
					supr = sup;
					return;
				}
			}
			supr = MASTER_OF_NONE;
		}

		/**
		 * Returning true tells the {@link Visitation} to not recursively call {@link #visit(ASTNode, Visitation)} on {@link ASTNode#subElements()}
		 * @return Do you just show up, play the music,
		 */
		public boolean skipReportingProblemsForSubElements() {return false;}
		public void visit(T node, Visitation visitation) throws ParsingException {}

		public IType type(T node, Visitation visitation) { return visitation.queryTypeOfExpression(node, PrimitiveType.UNKNOWN); }

		public IType callerType(T node, Visitation visitation) {
			ASTNode pred = node.predecessorInSequence();
			if (pred != null)
				return ty(pred, visitation);
			else
				return visitation.script();
		}

		public final IType predecessorType(ASTNode node, Visitation visitation) {
			ASTNode p = node.predecessorInSequence();
			return p != null ? ty(p, visitation) : null;
		}

		public final <X extends IType> X predecessorTypeAs(ASTNode node, Class<X> cls, Visitation visitation) {
			return as(predecessorType(node, visitation), cls);
		}

		/**
		 * Return whether this expression is valid as a value of the specified type.
		 * @param type The type to test against
		 * @param context Script parser context
		 * @return True if valid, false if not.
		 */
		public final IType unifyDeclaredAndGiven(ASTNode node, IType type, Visitation visitation) {
			IType myType = ty(node, visitation);
			if (type == null)
				return myType;
			return TypeUnification.unifyNoChoice(type, myType);
		}

		public boolean typingJudgement(T node, IType type, Visitation visitation, TypingJudgementMode mode) {
			ITypeVariable info;
			switch (mode) {
			case Expect:
				info = visitation.requestTypeInfo(node);
				if (info != null)
					if (info.get() == PrimitiveType.UNKNOWN || info.get() == PrimitiveType.ANY) {
						info.set(type);
						return true;
					} else
						return false;
				return true;
			case Force:
				info = visitation.requestTypeInfo(node);
				if (info != null) {
					info.set(type);
					return true;
				} else
					return false;
			case Hint:
				info = visitation.queryTypeInfo(node);
				return info == null || info.hint(type);
			case Unify:
				info = visitation.requestTypeInfo(node);
				if (info != null) {
					info.set(TypeUnification.unify(info.get(), type));
					return true;
				} else
					return false;
			default:
				return false;
			}
		}

		public void assignment(T leftSide, ASTNode rightSide, Visitation visitation) {
			if (visitation.base.typing == Typing.Static) {
				IType leftTy = ty(leftSide, this, visitation);
				IType rightTy = ty(rightSide, visitation);
				if (!leftTy.canBeAssignedFrom(rightTy))
					visitation.incompatibleTypes(rightSide, rightSide, leftTy, rightTy);
			} else
				judgement(leftSide, ty(rightSide, visitation), TypingJudgementMode.Force, visitation);
		}

		public ITypeVariable createTypeVariable(T node, Visitation visitation) {
			ITypeable d = ExpressionTypeVariable.typeableFromExpression(node, visitation);
			if (d != null && !d.staticallyTyped())
				return new ExpressionTypeVariable(node, visitation);
			return null;
		}

		@Override
		public String toString() { return String.format("Expert<%s>", cls.getSimpleName()); }

		public boolean isModifiable(T node, Visitation visitation) { return true; }
	}

	class AccessDeclarationExpert<T extends AccessDeclaration> extends Expert<T> {
		public AccessDeclarationExpert(Class<T> cls) { super(cls); }
		protected Declaration obtainDeclaration(T node, Visitation visitation) { return null; }
		@Override
		public void visit(T node, Visitation visitation) throws ParsingException {
			super.visit(node, visitation);
			internalObtainDeclaration(node, visitation);
		}
		protected final Declaration internalObtainDeclaration(T node, Visitation visitation) {
			if (!visitation.roaming || visitation.script() == node.parentOfType(Script.class)) {
				if (node.declaration() == null)
					node.setDeclaration(obtainDeclaration(node, visitation));
				if (node.declaration() == null) {
					visitation.script().index().loadScriptsContainingDeclarationsNamed(node.name());
					node.setDeclaration(obtainDeclaration(node, visitation));
				}
				return node.declaration();
			} else
				return obtainDeclaration(node, visitation);
		}
		@Override
		public ITypeVariable createTypeVariable(T node, Visitation visitation) {
			if (node.declaration() instanceof ITypeable && ((ITypeable)node.declaration()).staticallyTyped())
				return null;
			else
				return super.createTypeVariable(node, visitation);
		}
	}

	class ConditionalStatementExpert<T extends ConditionalStatement> extends Expert<T> {
		public ConditionalStatementExpert(Class<T> cls) { super(cls); }
		@Override
		public boolean skipReportingProblemsForSubElements() {return true;}
		@Override
		public void visit(ConditionalStatement node, Visitation visitation) throws ParsingException {
			ControlFlow t = visitation.controlFlow;
			visitation.controlFlow = ControlFlow.Continue;
			TypeEnvironment env = visitation.newTypeEnvironment();
			visitation.visitNode(node.condition(), true);
			visitation.endTypeEnvironment(env, true, false);
			env = visitation.newTypeEnvironment();
			visitation.visitNode(node.body(), true);
			visitation.endTypeEnvironment(env, true, false);
			loopConditionWarnings(node, visitation);
			visitation.controlFlow = t;
		}
		/**
		 * Emit warnings about loop conditions that could result in loops never executing or never ending.
		 * @param body The loop body. If the condition looks like it will always be true, checks are performed whether the body contains loop control flow statements.
		 * @param condition The loop condition to check
		 */
		protected void loopConditionWarnings(ConditionalStatement node, Visitation visitation) {
			ASTNode condition = node.condition();
			if (node.body() == null || condition == null || !(node instanceof ILoop))
				return;
			Object condEv = PrimitiveType.BOOL.convert(condition == null ? true : condition.evaluateStatic(node.parentOfType(Function.class)));
			if (Boolean.FALSE.equals(condEv))
				visitation.markers().warning(visitation, Problem.ConditionAlwaysFalse, condition, condition, Markers.NO_THROW, condition);
			else if (Boolean.TRUE.equals(condEv)) {
				EnumSet<ControlFlow> flows = node.body().possibleControlFlows();
				if (!(flows.contains(ControlFlow.BreakLoop) || flows.contains(ControlFlow.Return)))
					visitation.markers().warning(visitation, Problem.InfiniteLoop, node, node, Markers.NO_THROW);
			}
		}
	}

	private final Expert<ASTNode> MASTER_OF_NONE = new Expert<ASTNode>(ASTNode.class) {
		@Override
		public IType type(ASTNode node, Visitation visitation) {
			return PrimitiveType.UNKNOWN;
		}
		@Override
		public IType callerType(ASTNode node, Visitation visitation) {
			return PrimitiveType.ANY;
		}
	};

	private final <T extends ASTNode> Expert<? super T> findExpert(T node) {
		for (Class<?> cls = node.getClass(); cls != null; cls = cls.getSuperclass()) {
			@SuppressWarnings("unchecked")
			Expert<? super T> expert = (Expert<? super T>)committee.get(cls);
			if (expert != null)
				return expert;
		}
		return MASTER_OF_NONE;
	}
	
	private void dismissExperts(ASTNode node) {
		node.traverse(new IASTVisitor<Void>() {
			@Override
			public TraversalContinuation visitNode(ASTNode node, Void parser) {
				node.temporaryProblemReportingObject = null;
				return TraversalContinuation.Continue;
			}
		}, null);
	}

	private void assignExperts(ASTNode node) {
		node.traverse(new IASTVisitor<Void>() {
			@Override
			public TraversalContinuation visitNode(ASTNode node, Void parser) {
				node.temporaryProblemReportingObject = findExpert(node);
				return TraversalContinuation.Continue;
			}
		}, null);
	}

	@SuppressWarnings("unchecked")
	private final <T extends ASTNode> Expert<? super T> expert(T node) {
		if (node.temporaryProblemReportingObject != null)
			return (Expert<? super T>)node.temporaryProblemReportingObject;
		else
			return findExpert(node);
	}

	public final IType ty(ASTNode node, Visitation visitation) {
		return node != null ? ty(node, expert(node), visitation) : null;
	}

	public final <T extends ASTNode> IType ty(T node, Expert<T> expert, Visitation visitation) {
		IType type = expert.type(node, visitation);
		node.inferredType(type);
		return type;
	}

	public final void judgement(ASTNode node, IType type, TypingJudgementMode mode, Visitation visitation) {
		expert(node).typingJudgement(node, type, visitation, mode);
	}

	private final Map<Class<? extends ASTNode>, Expert<? extends ASTNode>> committee = new HashMap<Class<? extends ASTNode>, Expert<?>>();
	{
		@SuppressWarnings("rawtypes")
		Expert<?>[] classes = new Expert[] {

			new AccessDeclarationExpert<AccessDeclaration>(AccessDeclaration.class),

			new AccessDeclarationExpert<AccessVar>(AccessVar.class) {
				private Declaration findUsingType(Visitation visitation, AccessVar node, ASTNode predecessor, IType type) {
					for (IType t : type) {
						Script scriptToLookIn;
						if ((scriptToLookIn = Definition.scriptFrom(t)) == null) {
							// find pseudo-variable from proplist expression
							if (t instanceof IProplistDeclaration) {
								Variable proplistComponent = ((IProplistDeclaration)t).findComponent(node.name());
								if (proplistComponent != null)
									return proplistComponent;
							}
						} else {
							FindDeclarationInfo info = new FindDeclarationInfo(visitation.script().index());
							info.searchOrigin = scriptToLookIn;
							info.findGlobalVariables = predecessor == null;
							Declaration v = scriptToLookIn.findDeclaration(node.name(), info);
							if (v instanceof Definition)
								v = ((Definition)v).proxyVar();
							if (v != null) {
								Variable var = as(v, Variable.class);
								if (var != null && var.initializationExpression() != null) {
									Function p = var.initializationExpression().parentOfType(Function.class);
									if (p != null)
										visitation.visitFunction(p);
								}
								return v;
							}
						}
					}
					return null;
				}
				@Override
				protected Declaration obtainDeclaration(AccessVar node, Visitation visitation) {
					ASTNode p = node.predecessorInSequence();
					if (p == null && node.name().equals(Variable.THIS.name()))
						return Variable.THIS;
					IType type = visitation.script();
					if (p != null)
						type = ty(p, visitation);
					if (p == null) {
						Function f = node.parentOfType(Function.class);
						if (f != null) {
							Variable v = f.findVariable(node.name());
							if (v != null)
								return v;
						}
						Declaration v = visitation.base.variableMap.get(node.name());
						if (v == null && !visitation.base.variableMap.containsKey(node.name())) {
							v = findUsingType(visitation, node, null, type);
							visitation.base.variableMap.put(node.name(), v);
						}
						return v;
					}
					else
						return findUsingType(visitation, node, p, type);
				}
				@Override
				public IType type(AccessVar node, Visitation visitation) {
					Declaration d = internalObtainDeclaration(node, visitation);
					// declarationFromContext(context) ensures that declaration is not null (if there is actually a variable) which is needed for queryTypeOfExpression for example
					if (d == Variable.THIS)
						return visitation.base.thisType;
					IType stored = visitation.queryTypeOfExpression(node, null);
					if (stored != null)
						return stored;
					if (d instanceof Function)
						return new FunctionType((Function)d);
					else if (d instanceof Variable) {
						Variable v = (Variable)d;
						Map<Variable, IType> typesMap= null;
						if (v.scope() == Scope.LOCAL) {
							if (node.predecessorInSequence() == null)
								typesMap = visitation.base.variableTypes;
							else {
								IType targetType = ty(node.predecessorInSequence(), visitation);
								if (targetType instanceof Script) {
									ScriptProcessor other = visitation.base.shared.processors.get(targetType);
									if (other != null) {
										new Visitation(other).reportProblems();
										typesMap = other.variableTypes;
									} else
										typesMap = ((Script)targetType).variableTypes();
								}
							}
							IType type = typesMap != null ? typesMap.get(v) : v.type();
							if (type != null)
								return type;
						}
						return v.type();
					}
					else if (d instanceof ITypeable)
						return ((ITypeable) d).type();
					return PrimitiveType.UNKNOWN;
				}
				@Override
				public IType callerType(AccessVar node, Visitation visitation) {
					Variable v = as(node.declaration(), Variable.class);
					if (v != null) switch (v.scope()) {
					case CONST: case STATIC:
						return null;
					default:
						break;
					}
					return super.callerType(node, visitation);
				}
				@Override
				public boolean typingJudgement(AccessVar node, IType type, Visitation visitation, TypingJudgementMode mode) {
					if (node.declaration() == Variable.THIS)
						return true;
					return super.typingJudgement(node, type, visitation, mode);
				}
				@Override
				public void visit(AccessVar node, Visitation visitation) throws ParsingException {
					super.visit(node, visitation);
					ASTNode pred = node.predecessorInSequence();
					Declaration declaration = node.declaration();
					if (declaration == null && pred == null)
						visitation.markers().error(visitation, Problem.UndeclaredIdentifier, node, node, Markers.NO_THROW, node.name());
					// local variable used in global function
					else if (declaration instanceof Variable) {
						Variable var = (Variable) declaration;
						var.setUsed(true);
						switch (var.scope()) {
						case LOCAL:
							Declaration d = node.parentOfType(Declaration.class);
							if (d != null && pred == null) {
								Function f = d.topLevelParentDeclarationOfType(Function.class);
								Variable v = d.topLevelParentDeclarationOfType(Variable.class);
								if (
									(f != null && f.visibility() == FunctionScope.GLOBAL) ||
									(f == null && v != null && v.scope() != Scope.LOCAL)
								)
									visitation.markers().error(visitation, Problem.LocalUsedInGlobal, node, node, Markers.NO_THROW);
							}
							break;
						case STATIC: case CONST:
							visitation.script().addUsedScript(var.script());
							break;
						case VAR:
							Function currentFunction = node.parentOfType(Function.class);
							if (currentFunction != null && var.parentDeclaration() == currentFunction) {
								int locationUsed = currentFunction.bodyLocation().getOffset()+node.start();
								if (locationUsed < var.start())
									visitation.markers().warning(visitation, Problem.VarUsedBeforeItsDeclaration, node, node, 0, var.name());
							}
							break;
						case PARAMETER:
							break;
						}
					} else if (declaration instanceof Function)
						if (!visitation.script().engine().settings().supportsFunctionRefs)
							visitation.markers().error(visitation, Problem.FunctionRefNotAllowed, node, node, Markers.NO_THROW, visitation.script().engine().name());
				}
				public void initializeFromAssignment(Variable var, ASTNode referee, ASTNode expression, Visitation visitation) {
					IType type = ty(expression, visitation);
					var.expectedToBeOfType(type, TypingJudgementMode.Expect);
					var.setLocation(visitation.absoluteSourceLocationFromExpr(referee));
					var.forceType(type);
					var.setInitializationExpression(expression);
				}
				@Override
				public void assignment(AccessVar leftSide, ASTNode rightSide, Visitation visitation) {
					Declaration declaration = leftSide.declaration();
					if (declaration == Variable.THIS)
						return;
					if (declaration == null) {
						IType predType = predecessorType(leftSide, visitation);
						if (predType != null && predType.canBeAssignedFrom(PrimitiveType.PROPLIST))
							if (predType instanceof IProplistDeclaration) {
								IProplistDeclaration proplDecl = (IProplistDeclaration) predType;
								if (proplDecl.isAdHoc()) {
									Variable var = proplDecl.addComponent(
										new Variable(leftSide.name(), Variable.Scope.VAR),
										true
									);
									declaration = var;
									initializeFromAssignment(var, leftSide, rightSide, visitation);
								}
							} else for (IType t : predType)
								if (t == visitation.script()) {
									Variable var = new Variable(leftSide.name(), Variable.Scope.LOCAL);
									initializeFromAssignment(var, leftSide, rightSide, visitation);
									visitation.script().addDeclaration(var);
									declaration = var;
									break;
								}
					}
					super.assignment(leftSide, rightSide, visitation);
				}
				@Override
				public ITypeVariable createTypeVariable(AccessVar node, Visitation visitation) {
					if (node.declaration() instanceof Variable && node.predecessorInSequence() == null)
						return new VariableTypeVariable(node);
					else
						return super.createTypeVariable(node, visitation);
				}
				@Override
				public boolean isModifiable(AccessVar node, Visitation visitation) {
					Declaration declaration = node.declaration();
					ASTNode pred = node.predecessorInSequence();
					if (pred == null)
						return declaration == null || ((Variable)declaration).scope() != Scope.CONST;
					else
						return true; // you can never be so sure
				}
			},

			new Expert<InitializationFunction.VarInitializationAccess>(InitializationFunction.VarInitializationAccess.class) {
				@Override
				public void assignment(VarInitializationAccess leftSide, ASTNode rightSide, Visitation visitation) {
					supr.assignment(leftSide, rightSide, visitation);
					if (leftSide.declaration() instanceof Variable && ((Variable)leftSide.declaration()).scope() == Scope.CONST && !rightSide.isConstant())
						try {
							visitation.markers().error(visitation, Problem.NonConstGlobalVarAssignment, rightSide, rightSide, Markers.NO_THROW);
						} catch (ParsingException e) { }
				}
				@Override
				public boolean isModifiable(VarInitializationAccess node, Visitation visitation) { return true; /* sudo */ }
			},

			new Expert<ArrayExpression>(ArrayExpression.class) {
				@Override
				public IType type(ArrayExpression node, final Visitation visitation) {
					return new ArrayType(
						null,
						ArrayUtil.map(node.subElements(), IType.class, new IConverter<ASTNode, IType>() {
							@Override
							public IType convert(ASTNode from) {
								return from != null ? ty(from, visitation) : PrimitiveType.UNKNOWN;
							}
						})
					);
				}
				@Override
				public boolean isModifiable(ArrayExpression node, Visitation visitation) { return false; }
			},

			new Expert<ArrayElementExpression>(ArrayElementExpression.class) {
				@Override
				public IType type(ArrayElementExpression node, Visitation visitation) {
					IType t = supr.type(node, visitation);
					if (t != PrimitiveType.UNKNOWN && t != PrimitiveType.ANY)
						return t;
					ASTNode pred = node.predecessorInSequence();
					if (pred != null) {
						IType predTy = ty(pred, visitation);
						for (IType ty : predTy) {
							ArrayType at = as(ty, ArrayType.class);
							if (at != null)
								return at.typeForElementWithIndex(ASTNode.evaluateStatic(node.argument(), visitation));
						}
					}
					return PrimitiveType.ANY;
				}
				@Override
				public void assignment(ArrayElementExpression leftSide, ASTNode rightSide, Visitation visitation) {
					IType predType_ = predecessorType(leftSide, visitation);
					for (IType predType : predType_) {
						ArrayType arrayType = as(predType, ArrayType.class);
						IType rightSideType = ty(rightSide, visitation);
						ASTNode pred = leftSide.predecessorInSequence();
						if (arrayType != null) {
							Object argEv = ASTNode.evaluateStatic(leftSide.argument(), visitation);
							IType mutation;
							if (argEv instanceof Number)
								mutation = arrayType.modifiedBySliceAssignment(
									argEv,
									((Number)argEv).intValue()+1,
									new ArrayType(rightSideType, rightSideType)
								);
							else
								mutation = new ArrayType(
									TypeUnification.unify(rightSideType, arrayType.generalElementType()),
									ArrayType.NO_PRESUMED_LENGTH
								);
							visitation.storeType(pred, mutation);
							break;
						} else if (predType == PrimitiveType.UNKNOWN || predType == PrimitiveType.ARRAY)
							judgement(
								pred,
								new ArrayType(rightSideType, ArrayType.NO_PRESUMED_LENGTH),
								TypingJudgementMode.Force,
								visitation
							);
					}
				}
				@Override
				public void visit(ArrayElementExpression node, Visitation visitation) throws ParsingException {
					supr.visit(node, visitation);
					IType type = predecessorType(node, visitation);
					if (type == null)
						type = PrimitiveType.UNKNOWN;
					ASTNode arg = node.argument();
					if (arg == null)
						visitation.markers().warning(visitation, Problem.MissingExpression, node, node, 0);
					else if (PrimitiveType.UNKNOWN != type && PrimitiveType.ANY != type) {
						IType argType = ty(arg, visitation);
						ASTNode pred = node.predecessorInSequence();
						if (argType == PrimitiveType.STRING) {
							if (TypeUnification.unifyNoChoice(PrimitiveType.PROPLIST, type) == null)
								visitation.markers().warning(visitation, Problem.NotAProplist, node, pred, 0);
							else
								judgement(pred, PrimitiveType.PROPLIST, TypingJudgementMode.Unify, visitation);
						}
						else if (argType == PrimitiveType.INT)
							if (TypeUnification.unifyNoChoice(PrimitiveType.ARRAY, type) == null)
								visitation.markers().warning(visitation, Problem.NotAnArrayOrProplist, node, pred, 0);
							//else
							//	expert(pred).typingJudgement(pred, PrimitiveType.ARRAY, processor, TypingJudgementMode.Unify);
					}
				}
				@Override
				public boolean isModifiable(ArrayElementExpression node, Visitation visitation) { return true; }
			},

			new Expert<ArraySliceExpression>(ArraySliceExpression.class) {
				private void warnIfNotArray(ASTNode node, Visitation visitation, IType type) {
					if (type != null && type != PrimitiveType.UNKNOWN && type != PrimitiveType.ANY &&
						TypeUnification.unifyNoChoice(PrimitiveType.ARRAY, type) == null &&
						TypeUnification.unifyNoChoice(PrimitiveType.PROPLIST, type) == null)
						visitation.markers().warning(visitation, Problem.NotAnArrayOrProplist, node, node, 0);
				}
				@Override
				public void visit(ArraySliceExpression node, Visitation visitation) throws ParsingException {
					supr.visit(node, visitation);
					IType type = predecessorType(node, visitation);
					warnIfNotArray(node.predecessorInSequence(), visitation, type);
				}
				@Override
				public boolean isModifiable(ArraySliceExpression node, Visitation visitation) { return false; }
			},

			new Expert<OperatorExpression>(OperatorExpression.class) {
				@Override
				public IType type(OperatorExpression node, Visitation visitation) {
					return node.operator().resultType();
				}
				@Override
				public boolean isModifiable(OperatorExpression node, Visitation visitation) { return node.operator().returnsRef(); }
			},

			new Expert<BinaryOp>(BinaryOp.class) {
				@Override
				public IType type(BinaryOp node, Visitation visitation) {
					switch (node.operator()) {
					// &&/|| special: they return either the left or right side of the operator so the return type is the lowest common denominator of the argument types
					case And: case Or: case JumpNotNil:
						IType leftSideType = ty(node.leftSide(), visitation);
						IType rightSideType = ty(node.rightSide(), visitation);
						if (leftSideType == rightSideType)
							return leftSideType;
						else
							return TypeUnification.unify(leftSideType, rightSideType);
					case Assign:
						return ty(node.rightSide(), visitation);
					default:
						return supr.type(node, visitation);
					}
				}
				@Override
				public void visit(BinaryOp node, Visitation visitation) throws ParsingException {
					final Operator op = node.operator();
					// sanity
					ASTNode left = node.leftSide();
					ASTNode right = node.rightSide();
					node.setLocation(left.start(), right.end());
					// i'm an assignment operator and i can't modify my left side :C
					if (op.modifiesArgument() && !expert(left).isModifiable(left, visitation))
						visitation.markers().error(visitation, Problem.ExpressionNotModifiable, node, left, Markers.NO_THROW);
					// obsolete operators in #strict 2impor
					if ((op == Operator.StringEqual || op == Operator.ne) && (visitation.base.strictLevel >= 2))
						visitation.markers().warning(visitation, Problem.ObsoleteOperator, node, node, 0, op.operatorName());
					// wrong parameter types
					if (unifyDeclaredAndGiven(left, op.firstArgType(), visitation) == null)
						visitation.incompatibleTypes(node, left, op.firstArgType(), ty(left, visitation));
					if (unifyDeclaredAndGiven(right, op.secondArgType(), visitation) == null)
						visitation.incompatibleTypes(node, right, op.secondArgType(), ty(right, visitation));

					IType expectedLeft, expectedRight;
					switch (op) {
					case Assign: case Equal:
						expectedLeft = expectedRight = null;
						break;
					default:
						expectedLeft  = op.firstArgType();
						expectedRight = op.secondArgType();
					}

					switch (op) {
					case Assign: case AssignAdd: case AssignSubtract:
					case AssignMultiply: case AssignModulo: case AssignDivide:
						expert(left).assignment(left, right, visitation);
						break;
					default:
						break;
					}

					if (expectedLeft != null)
						judgement(left, expectedLeft, TypingJudgementMode.Unify, visitation);
					if (expectedRight != null)
						judgement(right, expectedRight, TypingJudgementMode.Unify, visitation);
				}
				@Override
				public ITypeVariable createTypeVariable(BinaryOp node, Visitation visitation) {
					ASTNode leftSide = node.leftSide();
					if (node.operator() == Operator.Assign && leftSide != null)
						return expert(leftSide).createTypeVariable(leftSide, visitation);
					return super.createTypeVariable(node, visitation);
				}
			},

			new Expert<UnaryOp>(UnaryOp.class) {
				@Override
				public void visit(UnaryOp node, Visitation visitation) throws ParsingException {
					supr.visit(node, visitation);
					ASTNode arg = node.argument();
					if (node.operator().modifiesArgument() && !expert(arg).isModifiable(arg, visitation))
						visitation.markers().error(visitation, Problem.ExpressionNotModifiable, node, arg, Markers.NO_THROW);
					Expert<? super ASTNode> rarg = expert(arg);
					PrimitiveType firstArgType = node.operator().firstArgType();
					if (rarg.unifyDeclaredAndGiven(arg, firstArgType, visitation) == null)
						visitation.incompatibleTypes(node, arg, firstArgType,
							ty(arg, rarg, visitation));
					if (firstArgType != PrimitiveType.ANY)
						rarg.typingJudgement(arg, firstArgType, visitation, TypingJudgementMode.Expect);
				}
				@Override
				public boolean isModifiable(UnaryOp node, Visitation visitation) {
					return node.placement() == Placement.Prefix && node.operator().returnsRef();
				}
			},

			new Expert<BoolLiteral>(BoolLiteral.class) {
				@Override
				public void visit(BoolLiteral node, Visitation visitation) throws ParsingException {
					supr.visit(node, visitation);
					if (node.parent() instanceof BinaryOp) {
						Operator op = ((BinaryOp) node.parent()).operator();
						if (op == Operator.And || op == Operator.Or)
							visitation.markers().warning(visitation, Problem.BoolLiteralAsOpArg, node, node, 0, this.toString());
					}
				}
			},

			new Expert<ContinueStatement>(ContinueStatement.class) {
				@Override
				public void visit(ContinueStatement node, Visitation visitation) throws ParsingException {
					if (node.parentOfType(ILoop.class) == null)
						visitation.markers().error(visitation, Problem.KeywordInWrongPlace, node, node, Markers.NO_THROW, node.keyword());
					supr.visit(node, visitation);
				}
			},

			new Expert<BreakStatement>(BreakStatement.class) {
				@Override
				public void visit(BreakStatement node, Visitation visitation) throws ParsingException {
					if (node.parentOfType(ILoop.class) == null)
						visitation.markers().error(visitation, Problem.KeywordInWrongPlace, node, node, Markers.NO_THROW, node.keyword());
					supr.visit(node, visitation);
				}
			},

			new Expert<ReturnStatement>(ReturnStatement.class) {
				private void warnAboutTupleInReturnExpr(Visitation visitation, ASTNode node, boolean tupleIsError) throws ParsingException {
					if (node == null)
						return;
					if (node instanceof Tuple)
						if (tupleIsError)
							visitation.markers().error(visitation, Problem.TuplesNotAllowed, node, node, Markers.NO_THROW);
						else if (visitation.base.strictLevel >= 2)
							visitation.markers().error(visitation, Problem.ReturnAsFunction, node, node, Markers.NO_THROW);
					ASTNode[] subElms = node.subElements();
					for (ASTNode e : subElms)
						warnAboutTupleInReturnExpr(visitation, e, true);
				}
				@Override
				public void visit(ReturnStatement node, Visitation visitation) throws ParsingException {
					supr.visit(node, visitation);
					ASTNode returnExpr = node.returnExpr();
					warnAboutTupleInReturnExpr(visitation, returnExpr, false);
					Function currentFunction = node.parentOfType(Function.class);
					if (currentFunction == null)
						visitation.markers().error(visitation, Problem.NotAllowedHere, node, node, Markers.NO_THROW, Keywords.Return);
					else if (returnExpr != null)
						if (visitation.base.typing == Typing.Static && currentFunction.staticallyTyped()) {
							if (expert(returnExpr).unifyDeclaredAndGiven(returnExpr, currentFunction.returnType(), visitation) == null)
								visitation.incompatibleTypes(node,
									returnExpr, currentFunction.returnType(), ty(returnExpr, visitation));
						}
						else {
							IType type = ty(returnExpr, visitation);
							judgement(node, type, TypingJudgementMode.Unify, visitation);
							//parser.linkTypesOf(dummy, returnExpr);
						}
				}
				@Override
				public ITypeVariable createTypeVariable(ReturnStatement node, Visitation visitation) {
					return new CurrentFunctionReturnTypeVariable(node.parentOfType(Function.class));
				}
			},

			new AccessDeclarationExpert<CallDeclaration>(CallDeclaration.class) {
				/**
				 * Find a {@link Function} for some hypothetical {@link CallDeclaration}, using contextual information such as the {@link ASTNode#type(ProblemReportingContext)} of the {@link ASTNode} preceding this {@link CallDeclaration} in the {@link Sequence}.
				 * @param processor Context to use for searching
				 * @param functionName Name of the function to look for. Would correspond to the hypothetical {@link CallDeclaration}'s {@link #name()}
				 * @param pred The predecessor of the hypothetical {@link CallDeclaration} ({@link ASTNode#predecessorInSequence()})
				 * @param listToAddPotentialDeclarationsTo When supplying a non-null value to this parameter, potential declarations will be added to the collection. Such potential declarations would be obtained by querying the {@link Index}'s {@link Index#declarationMap()}.
				 * @return The {@link Function} that is very likely to be the one actually intended to be referenced by the hypothetical {@link CallDeclaration}.
				 */
				private Declaration findUsingType(
					Visitation visitation,
					CallDeclaration node, String functionName,
					IType type
				) {
					IType lookIn = type != null ? type : visitation.script();
					if (lookIn != null) for (IType ty : lookIn) {
						Script script = as(ty, Script.class);
						if (script == null && ty instanceof MetaDefinition)
							script = ((MetaDefinition)ty).definition();
						if (script == null)
							continue;
						FindDeclarationInfo info = new FindDeclarationInfo(visitation.script().index());
						info.searchOrigin = visitation.script();
						info.contextFunction = node.parentOfType(Function.class);
						info.findGlobalVariables = type == null;
						Declaration dec = script.findDeclaration(functionName, info);
						// parse function before this one
						if (dec instanceof Function && node.parentOfType(Function.class) != null)
							visitation.visitFunction((Function)dec);
						if (dec != null)
							return dec;
					}
					if (type != null) {
						// find global function
						Declaration declaration;
						try {
							declaration = visitation.script().index().findGlobal(Declaration.class, functionName);
						} catch (Exception e) {
							e.printStackTrace();
							return null;
						}
						// find engine function
						if (declaration == null)
							declaration = visitation.script().index().engine().findFunction(functionName);

						List<Declaration> allFromLocalIndex = visitation.script().index().declarationMap().get(functionName);
						Declaration decl = visitation.script().engine().findLocalFunction(functionName, false);
						int numCandidates = 0;
						if (allFromLocalIndex != null)
							numCandidates += allFromLocalIndex.size();
						if (decl != null)
							numCandidates++;

						// only return found global function if it's the only choice
						if (declaration != null && numCandidates == 1)
							return declaration;
					}
					return null;
				}
				@Override
				protected Declaration obtainDeclaration(CallDeclaration node, Visitation visitation) {
					String declarationName = node.name();
					if (declarationName.equals(Keywords.Return))
						return null;
					ASTNode p = node.predecessorInSequence();
					if (p == null) {
						Declaration f = visitation.base.functionMap.get(node.name());
						if (f == null && !visitation.base.functionMap.containsKey(node.name())) {
							f = findUsingType(visitation, node, declarationName, visitation.script());
							visitation.base.functionMap.put(declarationName, f);
						}
						return f;
					}
					else
						return findUsingType(visitation, node, declarationName, ty(p, visitation));
				}
				private IType declarationType(CallDeclaration node, Visitation visitation) {
					Declaration d = internalObtainDeclaration(node, visitation);

					// look for gathered type information
					IType stored = visitation.queryTypeOfExpression(node, null);
					if (stored != null)
						return stored;

					// calling this() as function -> return object type belonging to script
					if (node.params().length == 0 && d != null && (d == visitation.cachedEngineDeclarations().This || d == Variable.THIS))
						return visitation.base.thisType;

					if (d instanceof Function) {
						// Some special rule applies and the return type is set accordingly
						SpecialFuncRule rule = node.specialRuleFromContext(visitation, SpecialEngineRules.RETURNTYPE_MODIFIER);
						if (rule != null) {
							IType type = rule.returnType(visitation, node);
							if (type != null)
								return type;
						}
						Function f = (Function)d;
						Map<String, IType> typesMap = null;
						if (f.visibility() != FunctionScope.GLOBAL)
							if (node.predecessorInSequence() == null)
								typesMap = visitation.base.functionReturnTypes;
							else {
								IType targetType = ty(node.predecessorInSequence(), visitation);
								if (targetType instanceof Script) {
									ScriptProcessor other = visitation.base.shared.processors.get(targetType);
									if (other != null) {
										new Visitation(other).reportProblems();
										typesMap = other.functionReturnTypes;
									} else
										typesMap = ((Script)targetType).functionReturnTypes();
								}
							}
						IType type = typesMap != null ? typesMap.get(d.name()) : null;
						if (type != null)
							return type;
						else
							return f.returnType();
					}
					if (d instanceof Variable)
						return ((Variable)d).type();

					return supr != null ? supr.type(node, visitation) : PrimitiveType.UNKNOWN;
				}
				private boolean unknownFunctionShouldBeError(CallDeclaration node, Visitation visitation) {
					ASTNode pred = node.predecessorInSequence();
					// stand-alone function? always bark!
					if (pred == null)
						return true;
					// not typed? weird
					IType predType = ty(pred, visitation);
					if (predType == null)
						return false;
					// called via ~? ok
					if (pred instanceof MemberOperator)
						if (((MemberOperator)pred).hasTilde())
							return false;
						else
							pred = pred.predecessorInSequence();
					// allow this->Unknown()
					AccessDeclaration ad = as(pred, AccessDeclaration.class);
					if (ad != null && (ad.declaration() == Variable.THIS || ad.declaration() == visitation.cachedEngineDeclarations().This))
						return false;
					boolean anyDefinitions = false;
					for (IType t : predType)
						if (t instanceof Definition)
							anyDefinitions = true;
					return anyDefinitions;
				}
				@Override
				public IType type(CallDeclaration node, Visitation visitation) {
					IType type = declarationType(node, visitation);
					if (type instanceof FunctionType)
						return ((FunctionType)type).prototype().returnType();
					else
						return type;
				}
				@Override
				public void visit(CallDeclaration node, Visitation visitation) throws ParsingException {
					super.visit(node, visitation);

					CachedEngineDeclarations cachedEngineDeclarations = visitation.cachedEngineDeclarations();
					String declarationName = node.name();
					Declaration declaration = node.declaration();
					ASTNode[] params = node.params();
					ASTNode predecessor = node.predecessorInSequence();

					// return as function
					if (declarationName.equals(Keywords.Return)) {
						if (visitation.base.strictLevel >= 2)
							visitation.markers().error(visitation, Problem.ReturnAsFunction, node, node, Markers.NO_THROW);
						else
							visitation.markers().warning(visitation, Problem.ReturnAsFunction, node, node, 0);
					} else if (declaration instanceof Variable) {
						// variable as function
						((Variable)declaration).setUsed(true);
						IType type = declarationType(node, visitation);
						// no warning when in #strict mode
						if (visitation.base.strictLevel >= 2)
							if (declaration != cachedEngineDeclarations.This && declaration != Variable.THIS && !PrimitiveType.FUNCTION.canBeAssignedFrom(type))
								visitation.markers().warning(visitation, Problem.VariableCalled, node, node, 0, declaration.name(), type.typeName(false));
					} else if (declaration instanceof Function) {
						Function f = (Function)declaration;
						if (f.visibility() == FunctionScope.GLOBAL || predecessor != null)
							visitation.script().addUsedScript(f.script());

						SpecialFuncRule rule = visitation.specialRuleFor(node, SpecialEngineRules.ARGUMENT_VALIDATOR);
						boolean specialCaseHandled =
							rule != null &&
							rule.validateArguments(node, params, visitation);

						// not a special case... check regular parameter types
						if (!specialCaseHandled) {
							int givenParam = 0;
							for (Variable parm : f.parameters()) {
								if (givenParam >= params.length)
									break;
								ASTNode given = params[givenParam++];
								if (given == null)
									continue;
								IType unified = unifyDeclaredAndGiven(given, parm.type(), visitation);
								if (unified == null)
									visitation.incompatibleTypes(node, given, parm.type(), ty(given, visitation));
								else
									judgement(given, unified, TypingJudgementMode.Unify, visitation);
							}
						}
					} else if (declaration == null && unknownFunctionShouldBeError(node, visitation)) {
						int start = node.start();
						visitation.markers().error(visitation, Problem.UndeclaredIdentifier, node, start, start+declarationName.length(), Markers.NO_THROW, declarationName);
					}
				}
				@Override
				public ITypeVariable createTypeVariable(CallDeclaration node, Visitation visitation) {
					Declaration d = node.declaration();
					CachedEngineDeclarations cache = visitation.cachedEngineDeclarations();
					if (isAnyOf(d, cache.VarAccessFunctions)) {
						Object ev;
						if (node.params().length == 1 && (ev = node.params()[0].evaluateStatic(node.parentOfType(Function.class))) != null)
							if (ev instanceof Number)
								// Var() with a sane constant number
								return new VarFunctionsTypeVariable(cache.Local == d ? null : node.parentOfType(Function.class), (Function) d, ((Number)ev).intValue());
					}
					else if (d instanceof Function) {
						Function f = (Function) d;
						if (f.staticallyTyped() || f.isEngineDeclaration() || f != node.parentOfType(Function.class))
							return null;
						return new FunctionReturnTypeVariable((Function)d);
					}
					else if (d != null)
						return new ExpressionTypeVariable(node, visitation);
					return super.createTypeVariable(node, visitation);
				}
				@Override
				public boolean isModifiable(CallDeclaration node, Visitation visitation) {
					Declaration declaration = node.declaration();
					IType t = declaration instanceof Function ? ((Function)declaration).returnType() : PrimitiveType.UNKNOWN;
					return t.canBeAssignedFrom(PrimitiveType.REFERENCE) || t.canBeAssignedFrom(PrimitiveType.UNKNOWN);
				}
			},

			new AccessDeclarationExpert<CallInherited>(CallInherited.class) {
				@Override
				public IType type(CallInherited node, Visitation visitation) {
					ITypeVariable tyVar = visitation.queryTypeInfo(node);
					if (tyVar != null)
						return tyVar.get();
					Function inherited = node.parentOfType(Function.class).inheritedFunction();
					if (inherited != null) {
						visitation.startRoaming();
						ITypeVariable ty = visitation.visitFunction(inherited);
						visitation.endRoaming();
						if (ty != null) {
							judgement(node, ty.get(), TypingJudgementMode.Force, visitation);
							return ty.get();
						}
					}
					return PrimitiveType.UNKNOWN;
				}
				@Override
				public void visit(CallInherited node, Visitation visitation) throws ParsingException {
					if (!visitation.roaming || node.parentOfType(Script.class) == visitation.script()) {
						// inherited/_inherited not allowed in non-strict mode
						if (visitation.base.strictLevel <= 0)
							visitation.markers().error(visitation, Problem.InheritedDisabledInStrict0, node, node, Markers.NO_THROW);

						node.setDeclaration(node.parentOfType(Function.class).inheritedFunction());
						if (node.declaration() == null && !node.failsafe())
							visitation.markers().error(visitation, Problem.NoInheritedFunction, node, node, Markers.NO_THROW, node.parentOfType(Function.class).name());
					}
				}
				@Override
				public ITypeVariable createTypeVariable(CallInherited node, Visitation visitation) {
					Function inherited = node.parentOfType(Function.class).inheritedFunction();
					return inherited != null ? new InheritedFunctionReturnTypeVariable(inherited) : null;
				}
			},

			new Expert<Sequence>(Sequence.class) {
				@Override
				public IType type(Sequence node, Visitation visitation) {
					ASTNode[] elements = node.subElements();
					return (elements == null || elements.length == 0)
						? PrimitiveType.UNKNOWN
						: ty(elements[elements.length-1], visitation);
				}
				@Override
				public void assignment(Sequence leftSide, ASTNode rightSide, Visitation visitation) {
					ASTNode lastElement = leftSide.lastElement();
					expert(lastElement).assignment(lastElement, rightSide, visitation);
				}
				@Override
				public void visit(Sequence node, Visitation visitation) throws ParsingException {
					supr.visit(node, visitation);
					ASTNode p = null;
					for (ASTNode e : node.subElements()) {
						if (
							(e != null && !e.isValidInSequence(p)) ||
							(p != null && !p.allowsSequenceSuccessor(e))
						)
							visitation.markers().error(visitation, Problem.NotAllowedHere, node, e, Markers.NO_THROW, e);
						p = e;
					}
					if (p != null && !p.isValidAtEndOfSequence())
						visitation.markers().error(visitation, Problem.NotFinished, node, node, Markers.NO_THROW, node.printed());
				}
				@Override
				public boolean isModifiable(Sequence node, Visitation visitation) {
					ASTNode[] elements = node.subElements();
					if (elements != null && elements.length > 0) {
						ASTNode last = elements[elements.length-1];
						return expert(last).isModifiable(last, visitation);
					} else
						return false;
				}
			},

			new Expert<ArraySliceExpression>(ArraySliceExpression.class) {
				@Override
				public IType type(ArraySliceExpression node, Visitation visitation) {
					ArrayType arrayType = predecessorTypeAs(node, ArrayType.class, visitation);
					if (arrayType != null)
						return node.lo() == null && node.hi() == null ? arrayType : arrayType.typeForSlice(
							ASTNode.evaluateStatic(node.lo(), visitation),
							ASTNode.evaluateStatic(node.hi(), visitation)
						);
					else
						return PrimitiveType.ARRAY;
				}
				@Override
				public void assignment(ArraySliceExpression leftSide, ASTNode rightSide, Visitation visitation) {
					ArrayType arrayType = predecessorTypeAs(leftSide, ArrayType.class, visitation);
					IType sliceType = ty(rightSide, visitation);
					if (arrayType != null)
						visitation.storeType(leftSide.predecessorInSequence(), arrayType.modifiedBySliceAssignment(
							ASTNode.evaluateStatic(leftSide.lo(), visitation),
							ASTNode.evaluateStatic(leftSide.hi(), visitation),
							sliceType
							));
				}
			},

			new Expert<Literal>(Literal.class) {
				@Override
				public boolean typingJudgement(Literal node, IType type, Visitation visitation, TypingJudgementMode mode) {
					// constantly steadfast do i resist the pressure of expectancy lied upon me
					return true;
				}
				@Override
				public void assignment(Literal leftSide, ASTNode rightSide, Visitation visitation) { /* don't care */ }
				@Override
				public ITypeVariable createTypeVariable(Literal node, Visitation visitation) { return null; /* nope */ }
				@Override
				public boolean isModifiable(Literal node, Visitation visitation) { return false; }
			},

			new Expert<Nil>(Nil.class) {
				@Override
				public IType type(Nil node, Visitation visitation) {
					return PrimitiveType.UNKNOWN;
				}
				@Override
				public void visit(Nil node, Visitation visitation) throws ParsingException {
					if (!visitation.script().engine().settings().supportsNil)
						visitation.markers().error(visitation, Problem.NotSupported, node, node, Markers.NO_THROW, Keywords.Nil, visitation.script().engine().name());
				}
			},

			new Expert<StringLiteral>(StringLiteral.class) {
				@Override
				public IType type(StringLiteral node, Visitation visitation) { return PrimitiveType.STRING; }
				@Override
				public void visit(StringLiteral node, Visitation visitation) throws ParsingException {
					// warn about overly long strings
					long max = visitation.script().index().engine().settings().maxStringLen;
					String lit = node.literal();
					if (max != 0 && lit.length() > max)
						visitation.markers().warning(visitation, Problem.StringTooLong, node, node, lit.length(), max);

					// stringtbl entries
					// don't warn in #appendto scripts because those will inherit their string tables from the scripts they are appended to
					// and checking for the existence of the table entries there is overkill
					if (visitation.base.hasAppendTo || visitation.script().resource() == null)
						return;
					String value = lit;
					int valueLen = value.length();
					// warn when using non-declared string tbl entries
					for (int i = 0; i < valueLen;) {
						if (i+1 < valueLen && value.charAt(i) == '$') {
							EntityRegion region = StringTbl.entryRegionInString(lit, node.start(), (i+1));
							if (region != null) {
								StringTbl.reportMissingStringTblEntries(visitation, region, node);
								i += region.region().getLength();
								continue;
							}
						}
						++i;
					}
				}
			},

			new Expert<IntegerLiteral>(IntegerLiteral.class) {
				@Override
				public IType type(IntegerLiteral node, Visitation visitation) {
					if (node.longValue() == 0 && visitation.script().engine().settings().zeroIsAny)
						return PrimitiveType.ANY;
					else
						return PrimitiveType.INT;
				}
			},

			new Expert<FloatLiteral>(FloatLiteral.class) {
				@Override
				public void visit(FloatLiteral node, Visitation visitation) throws ParsingException {
					if (!visitation.script().engine().settings().supportsFloats)
						visitation.markers().error(visitation, Problem.FloatNumbersNotSupported, node, node, Markers.NO_THROW);
					supr.visit(node, visitation);
				}
			},

			new Expert<IDLiteral>(IDLiteral.class) {
				@Override
				public IType type(IDLiteral node, Visitation visitation) {
					Definition obj = visitation.script().nearestDefinitionWithId(node.idValue());
					return obj != null ? obj.metaDefinition() : PrimitiveType.ID;
				}
			},

			new Expert<BoolLiteral>(BoolLiteral.class) {
				@Override
				public IType type(BoolLiteral node, Visitation visitation) {
					return PrimitiveType.BOOL;
				}
			},

			new Expert<CallExpr>(CallExpr.class) {
				@Override
				public IType type(CallExpr node, Visitation visitation) {
					ASTNode pred = node.predecessorInSequence();
					IType type = ty(pred, visitation);
					if (type instanceof FunctionType)
						return ((FunctionType)type).prototype().returnType();
					else
						return PrimitiveType.ANY;
				}
				@Override
				public void visit(CallExpr node, Visitation visitation) throws ParsingException {
					if (!visitation.script().engine().settings().supportsFunctionRefs)
						visitation.markers().error(visitation, Problem.FunctionRefNotAllowed, node, node, Markers.NO_THROW, visitation.script().engine().name());
					else {
						IType type = expert(node.predecessorInSequence()).type(node.predecessorInSequence(), visitation);
						if (!PrimitiveType.FUNCTION.canBeAssignedFrom(type))
							visitation.markers().error(visitation, Problem.CallingExpression, node, node, Markers.NO_THROW);
					}
				}
			},

			new Expert<Statement>(Statement.class) {
				@Override
				public IType type(Statement node, Visitation visitation) {
					return PrimitiveType.UNKNOWN;
				}
				/**
				 * Emit a warning if this expression is erroneously used at a place where only expressions with side effects are allowed.
				 * @param processor The processor
				 */
				public void warnIfNoSideEffects(Statement node, Visitation visitation) {
					if (node.parent() instanceof IterateArrayStatement && ((IterateArrayStatement)node.parent()).elementExpr() == node)
						return;
					if (!node.hasSideEffects())
						visitation.markers().warning(visitation, Problem.NoSideEffects, node, node, 0);
				}
				@Override
				public void visit(Statement node, Visitation visitation) throws ParsingException {
					supr.visit(node, visitation);
					warnIfNoSideEffects(node, visitation);
					if (visitation.controlFlow != ControlFlow.Continue)
						visitation.markers().warning(visitation, Problem.NeverReached, node, node, 0);
				}
			},

			new Expert<VarDeclarationStatement>(VarDeclarationStatement.class) {
				@Override
				public void visit(VarDeclarationStatement node, Visitation visitation) throws ParsingException {
					supr.visit(node, visitation);
					for (VarInitialization initialization : node.variableInitializations())
						if (initialization.variable != null)
							if (initialization.expression != null) {
								IType initializationType = ty(initialization.expression, visitation);
								if (
									initialization.variable.staticallyTyped() &&
									!initialization.variable.type().canBeAssignedFrom(initializationType)
								)
									visitation.incompatibleTypes(
										node,
										initialization.expression,
										initialization.variable.type(), initializationType
									);
								else {
									AccessVar av = new AccessVar(initialization.variable);
									judgement(av, initializationType, TypingJudgementMode.Unify, visitation);
								}
							}
				}
			},

			new Expert<PropListExpression>(PropListExpression.class) {
				@Override
				public IType type(PropListExpression node, Visitation visitation) {
					return node.definedDeclaration();
				}
				@Override
				public void visit(PropListExpression node, Visitation visitation) throws ParsingException {
					supr.visit(node, visitation);
					if (!visitation.script().engine().settings().supportsProplists)
						visitation.markers().error(visitation, Problem.NotSupported, node, node, Markers.NO_THROW,
							net.arctics.clonk.parser.c4script.ast.Messages.PropListExpression_ProplistsFeature,
							visitation.script().engine().name());
					for (Variable v : node.components())
						if (v.initializationExpression() != null)
							judgement(new AccessVar(v), ty(v.initializationExpression(), visitation), TypingJudgementMode.Unify, visitation);
				}
				@Override
				public boolean isModifiable(PropListExpression node, Visitation visitation) { return false; }
			},

			new Expert<Parenthesized>(Parenthesized.class) {
				@Override
				public IType type(Parenthesized node, Visitation visitation) {
					return ty(node.innerExpression(), visitation);
				}
				@Override
				public boolean isModifiable(Parenthesized node, Visitation visitation) {
					return expert(node.innerExpression()).isModifiable(node.innerExpression(), visitation);
				}
			},

			new Expert<MemberOperator>(MemberOperator.class) {
				@Override
				public IType type(MemberOperator node, Visitation visitation) {
					if (node.id() != null)
						return visitation.script().nearestDefinitionWithId(node.id());
					// stuff before -> decides
					ASTNode pred = node.predecessorInSequence();
					return pred != null ? ty(pred, visitation) : supr.type(node, visitation);
				}
				@Override
				public boolean typingJudgement(MemberOperator node, IType type, Visitation visitation, TypingJudgementMode mode) {
					ASTNode p = node.predecessorInSequence();
					return p != null ? expert(p).typingJudgement(p, type, visitation, mode) : false;
				}
				@Override
				public void visit(MemberOperator node, Visitation visitation) throws ParsingException {
					supr.visit(node, visitation);
					ASTNode pred = node.predecessorInSequence();
					EngineSettings settings = visitation.script().engine().settings();
					if (pred != null) {
						IType requiredType = node.dotNotation() ? PrimitiveType.PROPLIST : TypeChoice.make(PrimitiveType.OBJECT, PrimitiveType.ID);
						ASTNode sequenceTilMe = pred.sequenceTilMe();
						Expert<? super ASTNode> stmReporter = expert(sequenceTilMe);
						if (!stmReporter.typingJudgement(sequenceTilMe, requiredType, visitation, TypingJudgementMode.Hint))
							visitation.markers().warning(visitation, node.dotNotation() ? Problem.NotAProplist : Problem.CallingMethodOnNonObject, node, node, 0,
								ty(sequenceTilMe, stmReporter, visitation).typeName(false));
					}
					if (node.getLength() > 3 && !settings.spaceAllowedBetweenArrowAndTilde)
						visitation.markers().error(visitation, Problem.MemberOperatorWithTildeNoSpace, node, node, Markers.NO_THROW);
					if (node.dotNotation() && !settings.supportsProplists)
						visitation.markers().error(visitation, Problem.DotNotationNotSupported, node, node, Markers.NO_THROW, node);
				}
			},

			new Expert<IterateArrayStatement>(IterateArrayStatement.class) {
				@Override
				public boolean skipReportingProblemsForSubElements() { return true; }
				@Override
				public void visit(IterateArrayStatement node, Visitation visitation) throws ParsingException {
					ControlFlow t = visitation.controlFlow;
					visitation.controlFlow = ControlFlow.Continue;
					Variable loopVariable;
					AccessVar accessVar;
					ASTNode elementExpr = node.elementExpr();
					ASTNode arrayExpr = node.arrayExpr();
					if (elementExpr instanceof VarDeclarationStatement)
						loopVariable = ((VarDeclarationStatement)elementExpr).variableInitializations()[0].variable;
					else if ((accessVar = as(SimpleStatement.unwrap(elementExpr), AccessVar.class)) != null) {
						Declaration d = visitation.obtainDeclaration(accessVar);
						if (d == null) {
							// implicitly create loop variable declaration if not found
							SourceLocation varPos = visitation.absoluteSourceLocationFromExpr(accessVar);
							loopVariable = visitation.base.parser.createVarInScope(node.parentOfType(Function.class), accessVar.name(), Scope.VAR, varPos.start(), varPos.end(), null);
						} else
							loopVariable = as(d, Variable.class);
					} else
						loopVariable = null;

					visitation.visitNode(elementExpr, true);
					visitation.visitNode(arrayExpr, true);

					IType type = ty(arrayExpr, visitation);
					if (!type.canBeAssignedFrom(PrimitiveType.ARRAY))
						visitation.incompatibleTypes(node, arrayExpr, type, PrimitiveType.ARRAY);
					IType elmType = ArrayType.elementTypeSet(type);
					TypeEnvironment env = visitation.newTypeEnvironment();
					{
						if (loopVariable != null) {
							loopVariable.setUsed(true);
							judgement(new AccessVar(loopVariable), elmType, TypingJudgementMode.Unify, visitation);
						}
						visitation.visitNode(node.body(), true);
					}
					visitation.endTypeEnvironment(env, true, false);
					visitation.controlFlow = t;
				}
			},

			new Expert<SimpleStatement>(SimpleStatement.class) {
				@Override
				public void visit(SimpleStatement node, Visitation visitation) throws ParsingException {
					BinaryOp op = as(node.expression(), BinaryOp.class);
					if (op != null && !op.operator().modifiesArgument())
						visitation.markers().warning(visitation, Problem.NoAssignment, node, op, 0);
					supr.visit(node, visitation);
				}
			},

			new ConditionalStatementExpert<IfStatement>(IfStatement.class) {
				@Override
				public void visit(IfStatement node, Visitation visitation) throws ParsingException {
					ControlFlow old = visitation.controlFlow;
					ASTNode condition = node.condition();
					visitation.visitNode(condition, true);
					// use two separate type environments for if and else statement, merging
					// gathered information afterwards
					TypeEnvironment ifEnvironment = visitation.newTypeEnvironment();
					visitation.visitNode(node.body(), true);
					visitation.endTypeEnvironment(ifEnvironment, false, false);
					visitation.controlFlow = old;
					if (node.elseExpression() != null) {
						TypeEnvironment elseEnvironment = visitation.newTypeEnvironment();
						visitation.visitNode(node.elseExpression(), true);
						visitation.endTypeEnvironment(elseEnvironment, false, false);
						ifEnvironment.inject(elseEnvironment, false);
					}
					if (ifEnvironment.up != null)
						ifEnvironment.up.inject(ifEnvironment, false);
					visitation.controlFlow = old;

					if (!condition.containsConst()) {
						Object condEv = PrimitiveType.BOOL.convert(condition.evaluateStatic(node.parentOfType(Function.class)));
						if (condEv != null && condEv != ASTNode.EVALUATION_COMPLEX)
							visitation.markers().warning(visitation,
								condEv.equals(true) ? Problem.ConditionAlwaysTrue : Problem.ConditionAlwaysFalse,
								condition, condition, 0, condition);
					}
				};
			},

			new ConditionalStatementExpert<ForStatement>(ForStatement.class) {
				@Override
				public void visit(ForStatement node, Visitation visitation) throws ParsingException {
					if (node.initializer() != null)
						visitation.visitNode(node.initializer(), true);
					super.visit(node, visitation);
					if (node.increment() != null)
						visitation.visitNode(node.increment(), true);
				}
			},

			new ConditionalStatementExpert<WhileStatement>(WhileStatement.class),

			new Expert<NewProplist>(NewProplist.class) {
				@Override
				public void visit(NewProplist node, Visitation visitation) throws ParsingException {
					node.definedDeclaration().setPrototype(as(ty(node.prototype(), visitation), ProplistDeclaration.class));
				}
			},

			new Expert<Placeholder>(Placeholder.class) {
				@Override
				public void visit(Placeholder node, Visitation visitation) throws ParsingException {
					StringTbl.reportMissingStringTblEntries(visitation, new EntityRegion(null, node, node.entryName()), node);
				}
			},

			new Expert<MissingStatement>(MissingStatement.class) {
				@Override
				public void visit(MissingStatement node, Visitation visitation) throws ParsingException {
					visitation.markers().error(visitation, Problem.MissingStatement, node, node, Markers.NO_THROW);
				}
			},

			new Expert<GarbageStatement>(GarbageStatement.class) {
				@Override
				public void visit(GarbageStatement node, Visitation visitation) throws ParsingException {
					visitation.markers().error(visitation, Problem.Garbage, node, node, Markers.NO_THROW, node.garbage());
				}
			},

			new Expert<FunctionDescription>(FunctionDescription.class) {
				@Override
				public void visit(FunctionDescription node, Visitation visitation) throws ParsingException {
					if (visitation.base.hasAppendTo)
						return;
					int off = 1;
					for (String part : node.contents().split("\\|")) { //$NON-NLS-1$
						if (part.startsWith("$") && part.endsWith("$")) { //$NON-NLS-1$ //$NON-NLS-2$
							StringTbl stringTbl = visitation.script().localStringTblMatchingLanguagePref();
							String entryName = part.substring(1, part.length()-1);
							if (stringTbl == null || stringTbl.map().get(entryName) == null)
								visitation.markers().warning(visitation, Problem.UndeclaredIdentifier, node,
									new Region(node.start()+off, part.length()), 0, entryName);
						}
						off += part.length()+1;
					}
				}
			},

			new Expert<Comment>(Comment.class) {
				@Override
				public void visit(Comment node, Visitation visitation) throws ParsingException {
					if (!visitation.roaming || node.parentOfType(Script.class) == visitation.script()) {
						String s = node.text();
						int markerPriority;
						int searchStart = 0;
						do {
							markerPriority = IMarker.PRIORITY_LOW;
							int todoIndex = s.indexOf("TODO", searchStart);
							if (todoIndex != -1)
								markerPriority = IMarker.PRIORITY_NORMAL;
							else {
								todoIndex = s.indexOf("FIXME", searchStart);
								if (todoIndex != -1)
									markerPriority = IMarker.PRIORITY_HIGH;
							}
							if (todoIndex != -1) {
								int lineEnd = s.indexOf('\n', todoIndex);
								if (lineEnd == -1)
									lineEnd = s.length();
								searchStart = lineEnd;
								visitation.markers().todo(visitation.file(), node, s.substring(todoIndex, lineEnd), node.start()+2+todoIndex, node.start()+2+lineEnd, markerPriority);
							}
						} while (markerPriority > IMarker.PRIORITY_LOW);
					}
				}
			},

			new Expert<Unfinished>(Unfinished.class) {
				@Override
				public IType type(Unfinished node, Visitation visitation) {
					return ty(node.expression(), visitation);
				}
				@Override
				public void visit(Unfinished node, Visitation visitation) throws ParsingException {
					visitation.markers().error(visitation, Problem.NotFinished, node, node, Markers.NO_THROW, node);
				}
			}

		};
		for (Expert<?> expert : classes)
			committee.put(expert.cls(), expert);
		for (Expert<?> expert : classes)
			expert.findSuper();
	}

}
