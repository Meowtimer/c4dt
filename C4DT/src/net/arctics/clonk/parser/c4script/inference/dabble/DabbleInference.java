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
import net.arctics.clonk.parser.c4script.ast.ThisType;
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
		ClonkBuilder builder;
		IProgressMonitor monitor;
		final Map<Script, ScriptProcessor> processors = new HashMap<>();
	}
	private Shared shared;

	@Override
	public void initialize(Markers markers, ClonkBuilder builder) {
		super.initialize(markers, builder);
		shared = new Shared();
		shared.builder = builder;
		shared.monitor = builder.monitor();
	}

	@Override
	public void run() {
		threadPool(new Sink<ExecutorService>() {
			@Override
			public void receivedObject(ExecutorService pool) {
				Visitor[] visitors = new Visitor[shared.builder.parsers().size()];
				int i = 0;
				for (C4ScriptParser p : shared.builder.parsers())
					if (p != null) {
						ScriptProcessor processor = new ScriptProcessor(p.script(), p.fragmentOffset(), shared);
						shared.processors.put(p.script(), processor);
						Visitor v = new Visitor(processor);
						v.setMarkers(p.markers());
						visitors[i++] = v;
					}
				for (Visitor v : visitors)
					if (v != null)
						pool.execute(v);
			}
		}, 20);
		for (ScriptProcessor processor : shared.processors.values())
			dismissExperts(processor.script());
	}

	@Override
	public ProblemReportingContext localTypingContext(Script script, int fragmentOffset, ProblemReportingContext chain) {
		if (chain instanceof Visitor) {
			ScriptProcessor p = ((Visitor) chain).base.shared.processors.get(script);
			if (p != null)
				return new Visitor(p);
		}
		Shared shared = chain instanceof Visitor ? ((Visitor)chain).base.shared : new Shared();
		ScriptProcessor processor = new ScriptProcessor(script, fragmentOffset, shared);
		shared.processors.put(script, processor);
		return new Visitor(processor);
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
		public Thread thread;
		public CurrentFunctionReturnTypeVariable(Function function) {
			super(function);
			thread = Thread.currentThread();
		}
		@Override
		public boolean binds(ASTNode expr, Visitor visit) {
			return expr instanceof ReturnStatement && expr.parentOfType(Function.class) == function;
		}
		@Override
		public boolean same(ITypeVariable other) {
			return other instanceof CurrentFunctionReturnTypeVariable && ((CurrentFunctionReturnTypeVariable)other).function == function;
		}
		@Override
		public void apply(boolean soft, Visitor visit) { /* done by Dabble */ }
	}

	public static class InheritedFunctionReturnTypeVariable extends FunctionReturnTypeVariable {
		public InheritedFunctionReturnTypeVariable(Function function) { super(function); }
		@Override
		public boolean binds(ASTNode expr, Visitor visit) {
			return expr instanceof CallInherited && expr.parentOfType(Function.class) == function;
		}
		@Override
		public boolean same(ITypeVariable other) {
			return other instanceof InheritedFunctionReturnTypeVariable && ((InheritedFunctionReturnTypeVariable)other).function == function;
		}
	}

	protected final class ScriptProcessor {
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
		final SpecialEngineRules rules;
		final int fragmentOffset;
		final TypeEnvironment typeEnvironment = new TypeEnvironment();
		boolean finished = false;
		public Script script() { return script; }
		public ScriptProcessor(Script script, int sourceFragmentOffset, Shared shared) {
			this.shared = shared;
			this.script = script;
			this.index = script.index();
			this.typing = script.index() != null && script.index().nature() != null
				? script.index().nature().settings().typing
				: Typing.ParametersOptionallyTyped;
			this.rules = script.engine().specialRules();
			this.cachedEngineDeclarations = this.script.engine().cachedDeclarations();
			this.strictLevel = script.strictLevel();
			this.thisType = new ThisType(script);
			this.fragmentOffset = sourceFragmentOffset;
			boolean hasAppendTo = false;
			for (Directive d : script.directives())
				if (d.type() == DirectiveType.APPENDTO) {
					hasAppendTo = true;
					break;
				}
			this.hasAppendTo = hasAppendTo;
		}
	}

	public class Visitor implements Runnable, ProblemReportingContext, IEvaluationContext {

		static final int
			MAX_PAR = 10,
			MAX_NUMVAR = 20,
			UNKNOWN_PARAMETERNUM = MAX_PAR+1;
		static final boolean
			UNUSEDPARMWARNING = false;

		final ScriptProcessor base;

		private ControlFlow controlFlow;
		private Markers markers;
		private List<Script> visitees;
		private TypeEnvironment typeEnvironment;

		public Visitor(ScriptProcessor data) {
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
			if (base.rules != null)
				for (SpecialFuncRule funcRule : base.rules.defaultParmTypeAssignerRules())
					if (funcRule.assignDefaultParmTypes(this, function))
						return true;
			return false;
		}

		@Override
		public boolean triggersRevisit(Function function, Function called) { return called.typeFromCallsHint(); }

		private void initialParameterTypesFromCalls(Function function, Function baseFunction, IType[] callTypes) {
			//System.out.println(String.format("Typing '%s' from calls", function.qualifiedName()));
			List<CallDeclaration> calls = base.index.callsTo(function.name());
			if (calls != null)
				for (CallDeclaration call : calls) {
					Function f = call.parentOfType(Function.class);
					Script other = f.parentOfType(Script.class);
					ScriptProcessor processor = base.shared.processors.get(other);
					Visitor visitor;
					if (processor != null) {
						visitor = new Visitor(processor);
						visitor.visitFunction(f, function);
					} else
						visitor = null;
					Function ref = as(visitor != null ? visitor.obtainDeclaration(call) : call.declaration(), Function.class);
					if (ref != null) {
						ref = ref.baseFunction();
						if (ref != null)
							ref = (Function)ref.latestVersion();
						if (ref == baseFunction) {
							int parNum = Math.min(callTypes.length, call.params().length);
							for (int pa = 0; pa < parNum; pa++)
								if (!function.parameter(pa).staticallyTyped()) {
									ASTNode concretePar = call.params()[pa];
									if (concretePar != null) {
										IType concreteTy = visitor != null ? visitor.typeOf(concretePar) : concretePar.inferredType();
										IType unified = TypeUnification.unifyNoChoice(callTypes[pa], concreteTy);
										//System.out.println(String.format("%s: %s -> %s", function.parameter(pa).name(), concretePar.printed(), concreteTy.typeName(false)));
										if (unified == null) {
											if (visitor != null)
												visitor.incompatibleTypes(concretePar, concretePar, callTypes[pa], concreteTy);
										}
										else
											callTypes[pa] = unified;
									}
								}
						}
					}
				}
		}

		final boolean allParametersStaticallyTyped(Function function) {
			for (Variable p : function.parameters())
				if (!p.staticallyTyped())
					return false;
			return true;
		}

		@Override
		public ITypeVariable visitFunction(Function function) { return visitFunction(function, null); }

		private void assignExperts(ASTNode node) {
			node.traverse(new IASTVisitor<Void>() {
				@Override
				public TraversalContinuation visitNode(ASTNode node, Void parser) {
					if (!roaming && node instanceof AccessDeclaration)
						((AccessDeclaration)node).setDeclaration(null);
					node.temporaryProblemReportingObject = findExpert(node);
					return TraversalContinuation.Continue;
				}
			}, null);
		}

		public ITypeVariable visitFunction(Function function, Declaration hello) {
			if (function == null || function.body() == null)
				return null;
			Script funScript = function.script();
			if (roaming || (visitees != null && visitees.contains(funScript)) || script() == funScript) {
				CurrentFunctionReturnTypeVariable returnType;
				boolean kickOff = false;
				synchronized (base.finishedFunctions) {
					returnType = base.finishedFunctions.get(function);
					if (returnType == null) {
						base.finishedFunctions.put(function, returnType = new CurrentFunctionReturnTypeVariable(function));
						kickOff = true;
					}
				}
				if (!kickOff) synchronized (returnType) {
//					if (hello != null)
//						System.out.println(String.format("'%s' waiting for '%s'", hello.qualifiedName(), function.qualifiedName()));
					int i;
					for (i = 0; i < 3 && returnType.thread != null && returnType.thread != Thread.currentThread(); i++)
						try {
							returnType.wait(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					if (i == 3 && hello != null)
						System.out.println(String.format("'%s' gave up waiting for '%s'", hello.qualifiedName(), function.qualifiedName()));
					return returnType;
				}
				assignExperts(function);
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
							!(base.script instanceof Scenario) &&
							function.numParameters() > 0 &&
							(function.typeFromCallsHint() || !allParametersStaticallyTyped(function));
						function.setTypeFromCallsHint(typeFromCalls);
						IType[] callTypes = new IType[parameters.size()];
						if (typeFromCalls)
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
					synchronized (returnType) {
						function.assignType(returnType.get(), false);
						returnType.thread = null;
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
			if (inject)
				if (env.up != null)
					env.up.inject(env, ignoreLocals);
				else synchronized (base.typeEnvironment) {
					base.typeEnvironment.inject(env, ignoreLocals);
				}
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
				if (base.finished)
					return;
				else
					base.finished = true;
			}
			work();
		}

		private void work() {
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

			base.typeEnvironment.apply(this, false);
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
		public Definition definition() { return as(base.script, Definition.class); }
		@Override
		public SourceLocation absoluteSourceLocationFromExpr(ASTNode expression) {
			Function f = expression.parentOfType(Function.class);
			int bodyOffset = f != null ? f.bodyLocation().start() : 0;
			return new SourceLocation(
				base.fragmentOffset+bodyOffset+expression.start(),
				base.fragmentOffset+bodyOffset+expression.end()
			);
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
		public int fragmentOffset() { return base.fragmentOffset; }
		@Override
		public IType typeOf(ASTNode node) { return ty(node, this); }
		@Override
		public boolean isModifiable(ASTNode node) { return expert(node).isModifiable(node, this); }

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
		public void setMarkers(Markers markers) { this.markers = markers;}
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
	 * 	<li>typing ({@link Expert#type(ASTNode, Visitor)})</li>
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
		 * Returning true tells the {@link Visitor} to not recursively call {@link #visit(ASTNode, Visitor)} on {@link ASTNode#subElements()}
		 * @return Do you just show up, play the music,
		 */
		public boolean skipReportingProblemsForSubElements() {return false;}
		public void visit(T node, Visitor visit) throws ParsingException {}

		public IType type(T node, Visitor visit) { return visit.queryTypeOfExpression(node, PrimitiveType.UNKNOWN); }

		public IType callerType(T node, Visitor visit) {
			ASTNode pred = node.predecessorInSequence();
			if (pred != null)
				return ty(pred, visit);
			else
				return visit.script();
		}

		public final IType predecessorType(ASTNode node, Visitor visit) {
			ASTNode p = node.predecessorInSequence();
			return p != null ? ty(p, visit) : null;
		}

		public final <X extends IType> X predecessorTypeAs(ASTNode node, Class<X> cls, Visitor visit) {
			return as(predecessorType(node, visit), cls);
		}

		/**
		 * Return whether this expression is valid as a value of the specified type.
		 * @param type The type to test against
		 * @param context Script parser context
		 * @return True if valid, false if not.
		 */
		public final IType unifyDeclaredAndGiven(ASTNode node, IType type, Visitor visit) {
			IType myType = ty(node, visit);
			if (type == null)
				return myType;
			return TypeUnification.unifyNoChoice(type, myType);
		}

		public boolean typingJudgement(T node, IType type, Visitor visit, TypingJudgementMode mode) {
			ITypeVariable info;
			switch (mode) {
			case Expect:
				info = visit.requestTypeInfo(node);
				if (info != null)
					if (info.get() == PrimitiveType.UNKNOWN || info.get() == PrimitiveType.ANY) {
						info.set(type);
						return true;
					} else
						return false;
				return true;
			case Force:
				info = visit.requestTypeInfo(node);
				if (info != null) {
					info.set(type);
					return true;
				} else
					return false;
			case Hint:
				info = visit.queryTypeInfo(node);
				return info == null || info.hint(type);
			case Unify:
				info = visit.requestTypeInfo(node);
				if (info != null) {
					info.set(TypeUnification.unify(info.get(), type));
					return true;
				} else
					return false;
			default:
				return false;
			}
		}

		public void assignment(T leftSide, ASTNode rightSide, Visitor visit) {
			if (visit.base.typing == Typing.Static) {
				IType leftTy = ty(leftSide, this, visit);
				IType rightTy = ty(rightSide, visit);
				if (!leftTy.canBeAssignedFrom(rightTy))
					visit.incompatibleTypes(rightSide, rightSide, leftTy, rightTy);
			} else
				judgement(leftSide, ty(rightSide, visit), TypingJudgementMode.Force, visit);
		}

		public ITypeVariable createTypeVariable(T node, Visitor visit) {
			ITypeable d = ExpressionTypeVariable.typeableFromExpression(node, visit);
			if (d != null && !d.staticallyTyped())
				return new ExpressionTypeVariable(node, visit);
			return null;
		}

		@Override
		public String toString() { return String.format("Expert<%s>", cls.getSimpleName()); }

		public boolean isModifiable(T node, Visitor visit) { return true; }
	}

	class AccessDeclarationExpert<T extends AccessDeclaration> extends Expert<T> {
		public AccessDeclarationExpert(Class<T> cls) { super(cls); }
		protected Declaration obtainDeclaration(T node, Visitor visit) { return null; }
		@Override
		public void visit(T node, Visitor visit) throws ParsingException {
			super.visit(node, visit);
			internalObtainDeclaration(node, visit);
		}
		protected final Declaration internalObtainDeclaration(T node, Visitor visit) {
			if (!visit.roaming || visit.script() == node.parentOfType(Script.class)) {
				if (node.declaration() == null)
					node.setDeclaration(obtainDeclaration(node, visit));
				if (node.declaration() == null) {
					visit.script().index().loadScriptsContainingDeclarationsNamed(node.name());
					node.setDeclaration(obtainDeclaration(node, visit));
				}
				return node.declaration();
			} else
				return obtainDeclaration(node, visit);
		}
		@Override
		public ITypeVariable createTypeVariable(T node, Visitor visit) {
			if (node.declaration() instanceof ITypeable && ((ITypeable)node.declaration()).staticallyTyped())
				return null;
			else
				return super.createTypeVariable(node, visit);
		}
	}

	class ConditionalStatementExpert<T extends ConditionalStatement> extends Expert<T> {
		public ConditionalStatementExpert(Class<T> cls) { super(cls); }
		@Override
		public boolean skipReportingProblemsForSubElements() {return true;}
		@Override
		public void visit(ConditionalStatement node, Visitor visit) throws ParsingException {
			ControlFlow t = visit.controlFlow;
			visit.controlFlow = ControlFlow.Continue;
			TypeEnvironment env = visit.newTypeEnvironment();
			visit.visitNode(node.condition(), true);
			visit.endTypeEnvironment(env, true, false);
			env = visit.newTypeEnvironment();
			visit.visitNode(node.body(), true);
			visit.endTypeEnvironment(env, true, false);
			loopConditionWarnings(node, visit);
			visit.controlFlow = t;
		}
		/**
		 * Emit warnings about loop conditions that could result in loops never executing or never ending.
		 * @param body The loop body. If the condition looks like it will always be true, checks are performed whether the body contains loop control flow statements.
		 * @param condition The loop condition to check
		 */
		protected void loopConditionWarnings(ConditionalStatement node, Visitor visit) {
			ASTNode condition = node.condition();
			if (node.body() == null || condition == null || !(node instanceof ILoop))
				return;
			Object condEv = PrimitiveType.BOOL.convert(condition == null ? true : condition.evaluateStatic(node.parentOfType(Function.class)));
			if (Boolean.FALSE.equals(condEv))
				visit.markers().warning(visit, Problem.ConditionAlwaysFalse, condition, condition, Markers.NO_THROW, condition);
			else if (Boolean.TRUE.equals(condEv)) {
				EnumSet<ControlFlow> flows = node.body().possibleControlFlows();
				if (!(flows.contains(ControlFlow.BreakLoop) || flows.contains(ControlFlow.Return)))
					visit.markers().warning(visit, Problem.InfiniteLoop, node, node, Markers.NO_THROW);
			}
		}
	}

	private final Expert<ASTNode> MASTER_OF_NONE = new Expert<ASTNode>(ASTNode.class) {
		@Override
		public IType type(ASTNode node, Visitor visit) {
			return PrimitiveType.UNKNOWN;
		}
		@Override
		public IType callerType(ASTNode node, Visitor visit) {
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

	@SuppressWarnings("unchecked")
	private final <T extends ASTNode> Expert<? super T> expert(T node) {
		if (node.temporaryProblemReportingObject != null)
			return (Expert<? super T>)node.temporaryProblemReportingObject;
		else
			return findExpert(node);
	}

	public final IType ty(ASTNode node, Visitor visit) {
		return node != null ? ty(node, expert(node), visit) : null;
	}

	public final <T extends ASTNode> IType ty(T node, Expert<T> expert, Visitor visit) {
		IType type = expert.type(node, visit);
		node.inferredType(type);
		return type;
	}

	public final void judgement(ASTNode node, IType type, TypingJudgementMode mode, Visitor visit) {
		expert(node).typingJudgement(node, type, visit, mode);
	}

	private final Map<Class<? extends ASTNode>, Expert<? extends ASTNode>> committee = new HashMap<Class<? extends ASTNode>, Expert<?>>();
	{
		@SuppressWarnings("rawtypes")
		Expert<?>[] classes = new Expert[] {

			new AccessDeclarationExpert<AccessDeclaration>(AccessDeclaration.class),

			new AccessDeclarationExpert<AccessVar>(AccessVar.class) {
				private Declaration findUsingType(Visitor visit, AccessVar node, ASTNode predecessor, IType type) {
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
							FindDeclarationInfo info = new FindDeclarationInfo(visit.script().index());
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
										visit.visitFunction(p, node.parentOfType(Function.class));
								}
								return v;
							}
						}
					}
					return null;
				}
				@Override
				protected Declaration obtainDeclaration(AccessVar node, Visitor visit) {
					ASTNode p = node.predecessorInSequence();
					if (p == null && node.name().equals(Variable.THIS.name()))
						return Variable.THIS;
					IType type = visit.script();
					if (p != null)
						type = ty(p, visit);
					if (p == null) {
						Function f = node.parentOfType(Function.class);
						if (f != null) {
							Variable v = f.findVariable(node.name());
							if (v != null)
								return v;
						}
						Declaration v = visit.base.variableMap.get(node.name());
						if (v == null && !visit.base.variableMap.containsKey(node.name())) {
							v = findUsingType(visit, node, null, type);
							visit.base.variableMap.put(node.name(), v);
						}
						return v;
					}
					else
						return findUsingType(visit, node, p, type);
				}
				@Override
				public IType type(AccessVar node, Visitor visit) {
					Declaration d = internalObtainDeclaration(node, visit);
					// declarationFromContext(context) ensures that declaration is not null (if there is actually a variable) which is needed for queryTypeOfExpression for example
					if (d == Variable.THIS)
						return visit.base.thisType;
					IType stored = visit.queryTypeOfExpression(node, null);
					if (stored != null)
						return stored;
					if (d instanceof Function)
						return new FunctionType((Function)d);
					else if (d instanceof Variable) {
						Variable v = (Variable)d;
						Map<Variable, IType> typesMap= null;
						if (v.scope() == Scope.LOCAL) {
							if (node.predecessorInSequence() == null)
								typesMap = visit.base.variableTypes;
							else {
								IType targetType = ty(node.predecessorInSequence(), visit);
								if (targetType instanceof Script) {
									ScriptProcessor other = visit.base.shared.processors.get(targetType);
									if (other != null) {
										new Visitor(other).reportProblems();
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
				public IType callerType(AccessVar node, Visitor visit) {
					Variable v = as(node.declaration(), Variable.class);
					if (v != null) switch (v.scope()) {
					case CONST: case STATIC:
						return null;
					default:
						break;
					}
					return super.callerType(node, visit);
				}
				@Override
				public boolean typingJudgement(AccessVar node, IType type, Visitor visit, TypingJudgementMode mode) {
					if (node.declaration() == Variable.THIS)
						return true;
					return super.typingJudgement(node, type, visit, mode);
				}
				@Override
				public void visit(AccessVar node, Visitor visit) throws ParsingException {
					super.visit(node, visit);
					ASTNode pred = node.predecessorInSequence();
					Declaration declaration = node.declaration();
					if (declaration == null && pred == null)
						visit.markers().error(visit, Problem.UndeclaredIdentifier, node, node, Markers.NO_THROW, node.name());
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
									visit.markers().error(visit, Problem.LocalUsedInGlobal, node, node, Markers.NO_THROW);
							}
							break;
						case STATIC: case CONST:
							visit.script().addUsedScript(var.script());
							break;
						case VAR:
							Function currentFunction = node.parentOfType(Function.class);
							if (currentFunction != null && var.parentDeclaration() == currentFunction) {
								int locationUsed = currentFunction.bodyLocation().getOffset()+node.start();
								if (locationUsed < var.start())
									visit.markers().warning(visit, Problem.VarUsedBeforeItsDeclaration, node, node, 0, var.name());
							}
							break;
						case PARAMETER:
							break;
						}
					} else if (declaration instanceof Function)
						if (!visit.script().engine().settings().supportsFunctionRefs)
							visit.markers().error(visit, Problem.FunctionRefNotAllowed, node, node, Markers.NO_THROW, visit.script().engine().name());
				}
				public void initializeFromAssignment(Variable var, ASTNode referee, ASTNode expression, Visitor visit) {
					IType type = ty(expression, visit);
					var.expectedToBeOfType(type, TypingJudgementMode.Expect);
					var.setLocation(visit.absoluteSourceLocationFromExpr(referee));
					var.forceType(type);
					var.setInitializationExpression(expression);
				}
				@Override
				public void assignment(AccessVar leftSide, ASTNode rightSide, Visitor visit) {
					Declaration declaration = leftSide.declaration();
					if (declaration == Variable.THIS)
						return;
					if (declaration == null) {
						IType predType = predecessorType(leftSide, visit);
						if (predType != null && predType.canBeAssignedFrom(PrimitiveType.PROPLIST))
							if (predType instanceof IProplistDeclaration) {
								IProplistDeclaration proplDecl = (IProplistDeclaration) predType;
								if (proplDecl.isAdHoc()) {
									Variable var = proplDecl.addComponent(
										new Variable(leftSide.name(), Variable.Scope.VAR),
										true
									);
									declaration = var;
									initializeFromAssignment(var, leftSide, rightSide, visit);
								}
							} else for (IType t : predType)
								if (t == visit.script()) {
									Variable var = new Variable(leftSide.name(), Variable.Scope.LOCAL);
									initializeFromAssignment(var, leftSide, rightSide, visit);
									visit.script().addDeclaration(var);
									declaration = var;
									break;
								}
					}
					super.assignment(leftSide, rightSide, visit);
				}
				@Override
				public ITypeVariable createTypeVariable(AccessVar node, Visitor visit) {
					if (node.declaration() instanceof Variable && node.predecessorInSequence() == null)
						return new VariableTypeVariable(node);
					else
						return super.createTypeVariable(node, visit);
				}
				@Override
				public boolean isModifiable(AccessVar node, Visitor visit) {
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
				public void assignment(VarInitializationAccess leftSide, ASTNode rightSide, Visitor visit) {
					supr.assignment(leftSide, rightSide, visit);
					if (leftSide.declaration() instanceof Variable && ((Variable)leftSide.declaration()).scope() == Scope.CONST && !rightSide.isConstant())
						try {
							visit.markers().error(visit, Problem.NonConstGlobalVarAssignment, rightSide, rightSide, Markers.NO_THROW);
						} catch (ParsingException e) { }
				}
				@Override
				public boolean isModifiable(VarInitializationAccess node, Visitor visit) { return true; /* sudo */ }
			},

			new Expert<ArrayExpression>(ArrayExpression.class) {
				@Override
				public IType type(ArrayExpression node, final Visitor visit) {
					return new ArrayType(
						null,
						ArrayUtil.map(node.subElements(), IType.class, new IConverter<ASTNode, IType>() {
							@Override
							public IType convert(ASTNode from) {
								return from != null ? ty(from, visit) : PrimitiveType.UNKNOWN;
							}
						})
					);
				}
				@Override
				public boolean isModifiable(ArrayExpression node, Visitor visit) { return false; }
			},

			new Expert<ArrayElementExpression>(ArrayElementExpression.class) {
				@Override
				public IType type(ArrayElementExpression node, Visitor visit) {
					IType t = supr.type(node, visit);
					if (t != PrimitiveType.UNKNOWN && t != PrimitiveType.ANY)
						return t;
					ASTNode pred = node.predecessorInSequence();
					if (pred != null) {
						IType predTy = ty(pred, visit);
						for (IType ty : predTy) {
							ArrayType at = as(ty, ArrayType.class);
							if (at != null)
								return at.typeForElementWithIndex(ASTNode.evaluateStatic(node.argument(), visit));
						}
					}
					return PrimitiveType.ANY;
				}
				@Override
				public void assignment(ArrayElementExpression leftSide, ASTNode rightSide, Visitor visit) {
					IType predType_ = predecessorType(leftSide, visit);
					for (IType predType : predType_) {
						ArrayType arrayType = as(predType, ArrayType.class);
						IType rightSideType = ty(rightSide, visit);
						ASTNode pred = leftSide.predecessorInSequence();
						if (arrayType != null) {
							Object argEv = ASTNode.evaluateStatic(leftSide.argument(), visit);
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
							visit.storeType(pred, mutation);
							break;
						} else if (predType == PrimitiveType.UNKNOWN || predType == PrimitiveType.ARRAY)
							judgement(
								pred,
								new ArrayType(rightSideType, ArrayType.NO_PRESUMED_LENGTH),
								TypingJudgementMode.Force,
								visit
							);
					}
				}
				@Override
				public void visit(ArrayElementExpression node, Visitor visit) throws ParsingException {
					supr.visit(node, visit);
					IType type = predecessorType(node, visit);
					if (type == null)
						type = PrimitiveType.UNKNOWN;
					ASTNode arg = node.argument();
					if (arg == null)
						visit.markers().warning(visit, Problem.MissingExpression, node, node, 0);
					else if (PrimitiveType.UNKNOWN != type && PrimitiveType.ANY != type) {
						IType argType = ty(arg, visit);
						ASTNode pred = node.predecessorInSequence();
						if (argType == PrimitiveType.STRING) {
							if (TypeUnification.unifyNoChoice(PrimitiveType.PROPLIST, type) == null)
								visit.markers().warning(visit, Problem.NotAProplist, node, pred, 0);
							else
								judgement(pred, PrimitiveType.PROPLIST, TypingJudgementMode.Unify, visit);
						}
						else if (argType == PrimitiveType.INT)
							if (TypeUnification.unifyNoChoice(PrimitiveType.ARRAY, type) == null)
								visit.markers().warning(visit, Problem.NotAnArrayOrProplist, node, pred, 0);
							//else
							//	expert(pred).typingJudgement(pred, PrimitiveType.ARRAY, processor, TypingJudgementMode.Unify);
					}
				}
				@Override
				public boolean isModifiable(ArrayElementExpression node, Visitor visit) { return true; }
			},

			new Expert<ArraySliceExpression>(ArraySliceExpression.class) {
				private void warnIfNotArray(ASTNode node, Visitor visit, IType type) {
					if (type != null && type != PrimitiveType.UNKNOWN && type != PrimitiveType.ANY &&
						TypeUnification.unifyNoChoice(PrimitiveType.ARRAY, type) == null &&
						TypeUnification.unifyNoChoice(PrimitiveType.PROPLIST, type) == null)
						visit.markers().warning(visit, Problem.NotAnArrayOrProplist, node, node, 0);
				}
				@Override
				public void visit(ArraySliceExpression node, Visitor visit) throws ParsingException {
					supr.visit(node, visit);
					IType type = predecessorType(node, visit);
					warnIfNotArray(node.predecessorInSequence(), visit, type);
				}
				@Override
				public boolean isModifiable(ArraySliceExpression node, Visitor visit) { return false; }
			},

			new Expert<OperatorExpression>(OperatorExpression.class) {
				@Override
				public IType type(OperatorExpression node, Visitor visit) {
					return node.operator().resultType();
				}
				@Override
				public boolean isModifiable(OperatorExpression node, Visitor visit) { return node.operator().returnsRef(); }
			},

			new Expert<BinaryOp>(BinaryOp.class) {
				@Override
				public IType type(BinaryOp node, Visitor visit) {
					switch (node.operator()) {
					// &&/|| special: they return either the left or right side of the operator so the return type is the lowest common denominator of the argument types
					case And: case Or: case JumpNotNil:
						IType leftSideType = ty(node.leftSide(), visit);
						IType rightSideType = ty(node.rightSide(), visit);
						if (leftSideType == rightSideType)
							return leftSideType;
						else
							return TypeUnification.unify(leftSideType, rightSideType);
					case Assign:
						return ty(node.rightSide(), visit);
					default:
						return supr.type(node, visit);
					}
				}
				@Override
				public void visit(BinaryOp node, Visitor visit) throws ParsingException {
					final Operator op = node.operator();
					// sanity
					ASTNode left = node.leftSide();
					ASTNode right = node.rightSide();
					node.setLocation(left.start(), right.end());
					// i'm an assignment operator and i can't modify my left side :C
					if (op.modifiesArgument() && !expert(left).isModifiable(left, visit))
						visit.markers().error(visit, Problem.ExpressionNotModifiable, node, left, Markers.NO_THROW);
					// obsolete operators in #strict 2impor
					if ((op == Operator.StringEqual || op == Operator.ne) && (visit.base.strictLevel >= 2))
						visit.markers().warning(visit, Problem.ObsoleteOperator, node, node, 0, op.operatorName());
					// wrong parameter types
					if (unifyDeclaredAndGiven(left, op.firstArgType(), visit) == null)
						visit.incompatibleTypes(node, left, op.firstArgType(), ty(left, visit));
					if (unifyDeclaredAndGiven(right, op.secondArgType(), visit) == null)
						visit.incompatibleTypes(node, right, op.secondArgType(), ty(right, visit));

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
						expert(left).assignment(left, right, visit);
						break;
					default:
						break;
					}

					if (expectedLeft != null)
						judgement(left, expectedLeft, TypingJudgementMode.Unify, visit);
					if (expectedRight != null)
						judgement(right, expectedRight, TypingJudgementMode.Unify, visit);
				}
				@Override
				public ITypeVariable createTypeVariable(BinaryOp node, Visitor visit) {
					ASTNode leftSide = node.leftSide();
					if (node.operator() == Operator.Assign && leftSide != null)
						return expert(leftSide).createTypeVariable(leftSide, visit);
					return super.createTypeVariable(node, visit);
				}
			},

			new Expert<UnaryOp>(UnaryOp.class) {
				@Override
				public void visit(UnaryOp node, Visitor visit) throws ParsingException {
					supr.visit(node, visit);
					ASTNode arg = node.argument();
					if (node.operator().modifiesArgument() && !expert(arg).isModifiable(arg, visit))
						visit.markers().error(visit, Problem.ExpressionNotModifiable, node, arg, Markers.NO_THROW);
					Expert<? super ASTNode> rarg = expert(arg);
					PrimitiveType firstArgType = node.operator().firstArgType();
					if (rarg.unifyDeclaredAndGiven(arg, firstArgType, visit) == null)
						visit.incompatibleTypes(node, arg, firstArgType,
							ty(arg, rarg, visit));
					if (firstArgType != PrimitiveType.ANY)
						rarg.typingJudgement(arg, firstArgType, visit, TypingJudgementMode.Expect);
				}
				@Override
				public boolean isModifiable(UnaryOp node, Visitor visit) {
					return node.placement() == Placement.Prefix && node.operator().returnsRef();
				}
			},

			new Expert<BoolLiteral>(BoolLiteral.class) {
				@Override
				public void visit(BoolLiteral node, Visitor visit) throws ParsingException {
					supr.visit(node, visit);
					if (node.parent() instanceof BinaryOp) {
						Operator op = ((BinaryOp) node.parent()).operator();
						if (op == Operator.And || op == Operator.Or)
							visit.markers().warning(visit, Problem.BoolLiteralAsOpArg, node, node, 0, this.toString());
					}
				}
			},

			new Expert<ContinueStatement>(ContinueStatement.class) {
				@Override
				public void visit(ContinueStatement node, Visitor visit) throws ParsingException {
					if (node.parentOfType(ILoop.class) == null)
						visit.markers().error(visit, Problem.KeywordInWrongPlace, node, node, Markers.NO_THROW, node.keyword());
					supr.visit(node, visit);
				}
			},

			new Expert<BreakStatement>(BreakStatement.class) {
				@Override
				public void visit(BreakStatement node, Visitor visit) throws ParsingException {
					if (node.parentOfType(ILoop.class) == null)
						visit.markers().error(visit, Problem.KeywordInWrongPlace, node, node, Markers.NO_THROW, node.keyword());
					supr.visit(node, visit);
				}
			},

			new Expert<ReturnStatement>(ReturnStatement.class) {
				private void warnAboutTupleInReturnExpr(Visitor visit, ASTNode node, boolean tupleIsError) throws ParsingException {
					if (node == null)
						return;
					if (node instanceof Tuple)
						if (tupleIsError)
							visit.markers().error(visit, Problem.TuplesNotAllowed, node, node, Markers.NO_THROW);
						else if (visit.base.strictLevel >= 2)
							visit.markers().error(visit, Problem.ReturnAsFunction, node, node, Markers.NO_THROW);
					ASTNode[] subElms = node.subElements();
					for (ASTNode e : subElms)
						warnAboutTupleInReturnExpr(visit, e, true);
				}
				@Override
				public void visit(ReturnStatement node, Visitor visit) throws ParsingException {
					supr.visit(node, visit);
					ASTNode returnExpr = node.returnExpr();
					warnAboutTupleInReturnExpr(visit, returnExpr, false);
					Function currentFunction = node.parentOfType(Function.class);
					if (currentFunction == null)
						visit.markers().error(visit, Problem.NotAllowedHere, node, node, Markers.NO_THROW, Keywords.Return);
					else if (returnExpr != null)
						if (visit.base.typing == Typing.Static && currentFunction.staticallyTyped()) {
							if (expert(returnExpr).unifyDeclaredAndGiven(returnExpr, currentFunction.returnType(), visit) == null)
								visit.incompatibleTypes(node,
									returnExpr, currentFunction.returnType(), ty(returnExpr, visit));
						}
						else {
							IType type = ty(returnExpr, visit);
							judgement(node, type, TypingJudgementMode.Unify, visit);
							//parser.linkTypesOf(dummy, returnExpr);
						}
				}
				@Override
				public ITypeVariable createTypeVariable(ReturnStatement node, Visitor visit) {
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
					Visitor visit,
					CallDeclaration node, String functionName,
					IType type
				) {
					IType lookIn = type != null ? type : visit.script();
					if (lookIn != null) for (IType ty : lookIn) {
						Script script = as(ty, Script.class);
						if (script == null && ty instanceof MetaDefinition)
							script = ((MetaDefinition)ty).definition();
						if (script == null)
							continue;
						FindDeclarationInfo info = new FindDeclarationInfo(visit.script().index());
						info.searchOrigin = visit.script();
						info.contextFunction = node.parentOfType(Function.class);
						info.findGlobalVariables = type == null;
						Declaration dec = script.findDeclaration(functionName, info);
						// parse function before this one
						if (dec instanceof Function && node.parentOfType(Function.class) != null)
							visit.visitFunction((Function)dec, node.parentOfType(Function.class));
						if (dec != null)
							return dec;
					}
					if (type != null) {
						// find global function
						Declaration declaration;
						try {
							declaration = visit.script().index().findGlobal(Declaration.class, functionName);
						} catch (Exception e) {
							e.printStackTrace();
							return null;
						}
						// find engine function
						if (declaration == null)
							declaration = visit.script().index().engine().findFunction(functionName);

						List<Declaration> allFromLocalIndex = visit.script().index().declarationMap().get(functionName);
						Declaration decl = visit.script().engine().findLocalFunction(functionName, false);
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
				protected Declaration obtainDeclaration(CallDeclaration node, Visitor visit) {
					String declarationName = node.name();
					if (declarationName.equals(Keywords.Return))
						return null;
					ASTNode p = node.predecessorInSequence();
					if (p == null) {
						Declaration f = visit.base.functionMap.get(node.name());
						if (f == null && !visit.base.functionMap.containsKey(node.name())) {
							f = findUsingType(visit, node, declarationName, visit.script());
							visit.base.functionMap.put(declarationName, f);
						}
						return f;
					}
					else
						return findUsingType(visit, node, declarationName, ty(p, visit));
				}
				private IType declarationType(CallDeclaration node, Visitor visit) {
					Declaration d = internalObtainDeclaration(node, visit);

					// look for gathered type information
					IType stored = visit.queryTypeOfExpression(node, null);
					if (stored != null)
						return stored;

					// calling this() as function -> return object type belonging to script
					if (node.params().length == 0 && d != null && (d == visit.cachedEngineDeclarations().This || d == Variable.THIS))
						return visit.base.thisType;

					if (d instanceof Function) {
						// Some special rule applies and the return type is set accordingly
						SpecialFuncRule rule = node.specialRuleFromContext(visit, SpecialEngineRules.RETURNTYPE_MODIFIER);
						if (rule != null) {
							IType type = rule.returnType(visit, node);
							if (type != null)
								return type;
						}
						Function f = (Function)d;
						Map<String, IType> typesMap = null;
						if (f.visibility() != FunctionScope.GLOBAL)
							if (node.predecessorInSequence() == null)
								typesMap = visit.base.functionReturnTypes;
							else {
								IType targetType = ty(node.predecessorInSequence(), visit);
								if (targetType instanceof Script) {
									ScriptProcessor other = visit.base.shared.processors.get(targetType);
									if (other != null) {
										new Visitor(other).reportProblems();
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

					return supr != null ? supr.type(node, visit) : PrimitiveType.UNKNOWN;
				}
				private boolean unknownFunctionShouldBeError(CallDeclaration node, Visitor visit) {
					ASTNode pred = node.predecessorInSequence();
					// stand-alone function? always bark!
					if (pred == null)
						return true;
					// not typed? weird
					IType predType = ty(pred, visit);
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
					if (ad != null && (ad.declaration() == Variable.THIS || ad.declaration() == visit.cachedEngineDeclarations().This))
						return false;
					boolean anyDefinitions = false;
					for (IType t : predType)
						if (t instanceof Definition)
							anyDefinitions = true;
					return anyDefinitions;
				}
				@Override
				public IType type(CallDeclaration node, Visitor visit) {
					IType type = declarationType(node, visit);
					if (type instanceof FunctionType)
						return ((FunctionType)type).prototype().returnType();
					else
						return type;
				}
				@Override
				public void visit(CallDeclaration node, Visitor visit) throws ParsingException {
					super.visit(node, visit);

					CachedEngineDeclarations cachedEngineDeclarations = visit.cachedEngineDeclarations();
					String declarationName = node.name();
					Declaration declaration = node.declaration();
					ASTNode[] params = node.params();
					ASTNode predecessor = node.predecessorInSequence();

					// return as function
					if (declarationName.equals(Keywords.Return)) {
						if (visit.base.strictLevel >= 2)
							visit.markers().error(visit, Problem.ReturnAsFunction, node, node, Markers.NO_THROW);
						else
							visit.markers().warning(visit, Problem.ReturnAsFunction, node, node, 0);
					} else if (declaration instanceof Variable) {
						// variable as function
						((Variable)declaration).setUsed(true);
						IType type = declarationType(node, visit);
						// no warning when in #strict mode
						if (visit.base.strictLevel >= 2)
							if (declaration != cachedEngineDeclarations.This && declaration != Variable.THIS && !PrimitiveType.FUNCTION.canBeAssignedFrom(type))
								visit.markers().warning(visit, Problem.VariableCalled, node, node, 0, declaration.name(), type.typeName(false));
					} else if (declaration instanceof Function) {
						Function f = (Function)declaration;
						if (f.visibility() == FunctionScope.GLOBAL || predecessor != null)
							visit.script().addUsedScript(f.script());

						SpecialFuncRule rule = visit.specialRuleFor(node, SpecialEngineRules.ARGUMENT_VALIDATOR);
						boolean specialCaseHandled =
							rule != null &&
							rule.validateArguments(node, params, visit);

						// not a special case... check regular parameter types
						if (!specialCaseHandled) {
							int givenParam = 0;
							for (Variable parm : f.parameters()) {
								if (givenParam >= params.length)
									break;
								ASTNode given = params[givenParam++];
								if (given == null)
									continue;
								IType unified = unifyDeclaredAndGiven(given, parm.type(), visit);
								if (unified == null)
									visit.incompatibleTypes(node, given, parm.type(), ty(given, visit));
								else
									judgement(given, unified, TypingJudgementMode.Unify, visit);
							}
						}
					} else if (declaration == null && unknownFunctionShouldBeError(node, visit)) {
						int start = node.start();
						visit.markers().error(visit, Problem.UndeclaredIdentifier, node, start, start+declarationName.length(), Markers.NO_THROW, declarationName);
					}
				}
				@Override
				public ITypeVariable createTypeVariable(CallDeclaration node, Visitor visit) {
					Declaration d = node.declaration();
					CachedEngineDeclarations cache = visit.cachedEngineDeclarations();
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
						return new ExpressionTypeVariable(node, visit);
					return super.createTypeVariable(node, visit);
				}
				@Override
				public boolean isModifiable(CallDeclaration node, Visitor visit) {
					Declaration declaration = node.declaration();
					IType t = declaration instanceof Function ? ((Function)declaration).returnType() : PrimitiveType.UNKNOWN;
					return t.canBeAssignedFrom(PrimitiveType.REFERENCE) || t.canBeAssignedFrom(PrimitiveType.UNKNOWN);
				}
			},

			new AccessDeclarationExpert<CallInherited>(CallInherited.class) {
				@Override
				public IType type(CallInherited node, Visitor visit) {
					ITypeVariable tyVar = visit.queryTypeInfo(node);
					if (tyVar != null)
						return tyVar.get();
					Function inherited = node.parentOfType(Function.class).inheritedFunction();
					if (inherited != null) {
						visit.startRoaming();
						ITypeVariable ty = visit.visitFunction(inherited);
						visit.endRoaming();
						if (ty != null) {
							judgement(node, ty.get(), TypingJudgementMode.Force, visit);
							return ty.get();
						}
					}
					return PrimitiveType.UNKNOWN;
				}
				@Override
				public void visit(CallInherited node, Visitor visit) throws ParsingException {
					if (!visit.roaming || node.parentOfType(Script.class) == visit.script()) {
						// inherited/_inherited not allowed in non-strict mode
						if (visit.base.strictLevel <= 0)
							visit.markers().error(visit, Problem.InheritedDisabledInStrict0, node, node, Markers.NO_THROW);

						node.setDeclaration(node.parentOfType(Function.class).inheritedFunction());
						if (node.declaration() == null && !node.failsafe())
							visit.markers().error(visit, Problem.NoInheritedFunction, node, node, Markers.NO_THROW, node.parentOfType(Function.class).name());
					}
				}
				@Override
				public ITypeVariable createTypeVariable(CallInherited node, Visitor visit) {
					Function inherited = node.parentOfType(Function.class).inheritedFunction();
					return inherited != null ? new InheritedFunctionReturnTypeVariable(inherited) : null;
				}
			},

			new Expert<Sequence>(Sequence.class) {
				@Override
				public IType type(Sequence node, Visitor visit) {
					ASTNode[] elements = node.subElements();
					return (elements == null || elements.length == 0)
						? PrimitiveType.UNKNOWN
						: ty(elements[elements.length-1], visit);
				}
				@Override
				public void assignment(Sequence leftSide, ASTNode rightSide, Visitor visit) {
					ASTNode lastElement = leftSide.lastElement();
					expert(lastElement).assignment(lastElement, rightSide, visit);
				}
				@Override
				public void visit(Sequence node, Visitor visit) throws ParsingException {
					supr.visit(node, visit);
					ASTNode p = null;
					for (ASTNode e : node.subElements()) {
						if (
							(e != null && !e.isValidInSequence(p)) ||
							(p != null && !p.allowsSequenceSuccessor(e))
						)
							visit.markers().error(visit, Problem.NotAllowedHere, node, e, Markers.NO_THROW, e);
						p = e;
					}
					if (p != null && !p.isValidAtEndOfSequence())
						visit.markers().error(visit, Problem.NotFinished, node, node, Markers.NO_THROW, node.printed());
				}
				@Override
				public boolean isModifiable(Sequence node, Visitor visit) {
					ASTNode[] elements = node.subElements();
					if (elements != null && elements.length > 0) {
						ASTNode last = elements[elements.length-1];
						return expert(last).isModifiable(last, visit);
					} else
						return false;
				}
			},

			new Expert<ArraySliceExpression>(ArraySliceExpression.class) {
				@Override
				public IType type(ArraySliceExpression node, Visitor visit) {
					ArrayType arrayType = predecessorTypeAs(node, ArrayType.class, visit);
					if (arrayType != null)
						return node.lo() == null && node.hi() == null ? arrayType : arrayType.typeForSlice(
							ASTNode.evaluateStatic(node.lo(), visit),
							ASTNode.evaluateStatic(node.hi(), visit)
						);
					else
						return PrimitiveType.ARRAY;
				}
				@Override
				public void assignment(ArraySliceExpression leftSide, ASTNode rightSide, Visitor visit) {
					ArrayType arrayType = predecessorTypeAs(leftSide, ArrayType.class, visit);
					IType sliceType = ty(rightSide, visit);
					if (arrayType != null)
						visit.storeType(leftSide.predecessorInSequence(), arrayType.modifiedBySliceAssignment(
							ASTNode.evaluateStatic(leftSide.lo(), visit),
							ASTNode.evaluateStatic(leftSide.hi(), visit),
							sliceType
							));
				}
			},

			new Expert<Literal>(Literal.class) {
				@Override
				public boolean typingJudgement(Literal node, IType type, Visitor visit, TypingJudgementMode mode) {
					// constantly steadfast do i resist the pressure of expectancy lied upon me
					return true;
				}
				@Override
				public void assignment(Literal leftSide, ASTNode rightSide, Visitor visit) { /* don't care */ }
				@Override
				public ITypeVariable createTypeVariable(Literal node, Visitor visit) { return null; /* nope */ }
				@Override
				public boolean isModifiable(Literal node, Visitor visit) { return false; }
			},

			new Expert<Nil>(Nil.class) {
				@Override
				public IType type(Nil node, Visitor visit) {
					return PrimitiveType.UNKNOWN;
				}
				@Override
				public void visit(Nil node, Visitor visit) throws ParsingException {
					if (!visit.script().engine().settings().supportsNil)
						visit.markers().error(visit, Problem.NotSupported, node, node, Markers.NO_THROW, Keywords.Nil, visit.script().engine().name());
				}
			},

			new Expert<StringLiteral>(StringLiteral.class) {
				@Override
				public IType type(StringLiteral node, Visitor visit) { return PrimitiveType.STRING; }
				@Override
				public void visit(StringLiteral node, Visitor visit) throws ParsingException {
					// warn about overly long strings
					long max = visit.script().index().engine().settings().maxStringLen;
					String lit = node.literal();
					if (max != 0 && lit.length() > max)
						visit.markers().warning(visit, Problem.StringTooLong, node, node, lit.length(), max);

					// stringtbl entries
					// don't warn in #appendto scripts because those will inherit their string tables from the scripts they are appended to
					// and checking for the existence of the table entries there is overkill
					if (visit.base.hasAppendTo || visit.script().resource() == null)
						return;
					String value = lit;
					int valueLen = value.length();
					// warn when using non-declared string tbl entries
					for (int i = 0; i < valueLen;) {
						if (i+1 < valueLen && value.charAt(i) == '$') {
							EntityRegion region = StringTbl.entryRegionInString(lit, node.start(), (i+1));
							if (region != null) {
								StringTbl.reportMissingStringTblEntries(visit, region, node);
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
				public IType type(IntegerLiteral node, Visitor visit) {
					if (node.longValue() == 0 && visit.script().engine().settings().zeroIsAny)
						return PrimitiveType.ANY;
					else
						return PrimitiveType.INT;
				}
			},

			new Expert<FloatLiteral>(FloatLiteral.class) {
				@Override
				public void visit(FloatLiteral node, Visitor visit) throws ParsingException {
					if (!visit.script().engine().settings().supportsFloats)
						visit.markers().error(visit, Problem.FloatNumbersNotSupported, node, node, Markers.NO_THROW);
					supr.visit(node, visit);
				}
			},

			new Expert<IDLiteral>(IDLiteral.class) {
				@Override
				public IType type(IDLiteral node, Visitor visit) {
					Definition obj = visit.script().nearestDefinitionWithId(node.idValue());
					return obj != null ? obj.metaDefinition() : PrimitiveType.ID;
				}
			},

			new Expert<BoolLiteral>(BoolLiteral.class) {
				@Override
				public IType type(BoolLiteral node, Visitor visit) {
					return PrimitiveType.BOOL;
				}
			},

			new Expert<CallExpr>(CallExpr.class) {
				@Override
				public IType type(CallExpr node, Visitor visit) {
					ASTNode pred = node.predecessorInSequence();
					IType type = ty(pred, visit);
					if (type instanceof FunctionType)
						return ((FunctionType)type).prototype().returnType();
					else
						return PrimitiveType.ANY;
				}
				@Override
				public void visit(CallExpr node, Visitor visit) throws ParsingException {
					if (!visit.script().engine().settings().supportsFunctionRefs)
						visit.markers().error(visit, Problem.FunctionRefNotAllowed, node, node, Markers.NO_THROW, visit.script().engine().name());
					else {
						IType type = expert(node.predecessorInSequence()).type(node.predecessorInSequence(), visit);
						if (!PrimitiveType.FUNCTION.canBeAssignedFrom(type))
							visit.markers().error(visit, Problem.CallingExpression, node, node, Markers.NO_THROW);
					}
				}
			},

			new Expert<Statement>(Statement.class) {
				@Override
				public IType type(Statement node, Visitor visit) {
					return PrimitiveType.UNKNOWN;
				}
				/**
				 * Emit a warning if this expression is erroneously used at a place where only expressions with side effects are allowed.
				 * @param processor The processor
				 */
				public void warnIfNoSideEffects(Statement node, Visitor visit) {
					if (node.parent() instanceof IterateArrayStatement && ((IterateArrayStatement)node.parent()).elementExpr() == node)
						return;
					if (!node.hasSideEffects())
						visit.markers().warning(visit, Problem.NoSideEffects, node, node, 0);
				}
				@Override
				public void visit(Statement node, Visitor visit) throws ParsingException {
					supr.visit(node, visit);
					warnIfNoSideEffects(node, visit);
					if (visit.controlFlow != ControlFlow.Continue)
						visit.markers().warning(visit, Problem.NeverReached, node, node, 0);
				}
			},

			new Expert<VarDeclarationStatement>(VarDeclarationStatement.class) {
				@Override
				public void visit(VarDeclarationStatement node, Visitor visit) throws ParsingException {
					supr.visit(node, visit);
					for (VarInitialization initialization : node.variableInitializations())
						if (initialization.variable != null)
							if (initialization.expression != null) {
								IType initializationType = ty(initialization.expression, visit);
								if (
									initialization.variable.staticallyTyped() &&
									!initialization.variable.type().canBeAssignedFrom(initializationType)
								)
									visit.incompatibleTypes(
										node,
										initialization.expression,
										initialization.variable.type(), initializationType
									);
								else {
									AccessVar av = new AccessVar(initialization.variable);
									judgement(av, initializationType, TypingJudgementMode.Unify, visit);
								}
							}
				}
			},

			new Expert<PropListExpression>(PropListExpression.class) {
				@Override
				public IType type(PropListExpression node, Visitor visit) {
					return node.definedDeclaration();
				}
				@Override
				public void visit(PropListExpression node, Visitor visit) throws ParsingException {
					supr.visit(node, visit);
					if (!visit.script().engine().settings().supportsProplists)
						visit.markers().error(visit, Problem.NotSupported, node, node, Markers.NO_THROW,
							net.arctics.clonk.parser.c4script.ast.Messages.PropListExpression_ProplistsFeature,
							visit.script().engine().name());
					for (Variable v : node.components())
						if (v.initializationExpression() != null)
							judgement(new AccessVar(v), ty(v.initializationExpression(), visit), TypingJudgementMode.Unify, visit);
				}
				@Override
				public boolean isModifiable(PropListExpression node, Visitor visit) { return false; }
			},

			new Expert<Parenthesized>(Parenthesized.class) {
				@Override
				public IType type(Parenthesized node, Visitor visit) {
					return ty(node.innerExpression(), visit);
				}
				@Override
				public boolean isModifiable(Parenthesized node, Visitor visit) {
					return expert(node.innerExpression()).isModifiable(node.innerExpression(), visit);
				}
			},

			new Expert<MemberOperator>(MemberOperator.class) {
				@Override
				public IType type(MemberOperator node, Visitor visit) {
					if (node.id() != null)
						return visit.script().nearestDefinitionWithId(node.id());
					// stuff before -> decides
					ASTNode pred = node.predecessorInSequence();
					return pred != null ? ty(pred, visit) : supr.type(node, visit);
				}
				@Override
				public boolean typingJudgement(MemberOperator node, IType type, Visitor visit, TypingJudgementMode mode) {
					ASTNode p = node.predecessorInSequence();
					return p != null ? expert(p).typingJudgement(p, type, visit, mode) : false;
				}
				@Override
				public void visit(MemberOperator node, Visitor visit) throws ParsingException {
					supr.visit(node, visit);
					ASTNode pred = node.predecessorInSequence();
					EngineSettings settings = visit.script().engine().settings();
					if (pred != null) {
						IType requiredType = node.dotNotation() ? PrimitiveType.PROPLIST : TypeChoice.make(PrimitiveType.OBJECT, PrimitiveType.ID);
						ASTNode sequenceTilMe = pred.sequenceTilMe();
						Expert<? super ASTNode> stmReporter = expert(sequenceTilMe);
						if (!stmReporter.typingJudgement(sequenceTilMe, requiredType, visit, TypingJudgementMode.Hint))
							visit.markers().warning(visit, node.dotNotation() ? Problem.NotAProplist : Problem.CallingMethodOnNonObject, node, node, 0,
								ty(sequenceTilMe, stmReporter, visit).typeName(false));
					}
					if (node.getLength() > 3 && !settings.spaceAllowedBetweenArrowAndTilde)
						visit.markers().error(visit, Problem.MemberOperatorWithTildeNoSpace, node, node, Markers.NO_THROW);
					if (node.dotNotation() && !settings.supportsProplists)
						visit.markers().error(visit, Problem.DotNotationNotSupported, node, node, Markers.NO_THROW, node);
				}
			},

			new Expert<IterateArrayStatement>(IterateArrayStatement.class) {
				@Override
				public boolean skipReportingProblemsForSubElements() { return true; }
				@Override
				public void visit(IterateArrayStatement node, Visitor visit) throws ParsingException {
					ControlFlow t = visit.controlFlow;
					visit.controlFlow = ControlFlow.Continue;
					Variable loopVariable;
					AccessVar accessVar;
					ASTNode elementExpr = node.elementExpr();
					ASTNode arrayExpr = node.arrayExpr();
					if (elementExpr instanceof VarDeclarationStatement)
						loopVariable = ((VarDeclarationStatement)elementExpr).variableInitializations()[0].variable;
					else if ((accessVar = as(SimpleStatement.unwrap(elementExpr), AccessVar.class)) != null) {
						Declaration d = visit.obtainDeclaration(accessVar);
						if (d == null) {
							// implicitly create loop variable declaration if not found
							SourceLocation varPos = visit.absoluteSourceLocationFromExpr(accessVar);
							loopVariable = visit.base.script.createVarInScope(Variable.DEFAULT_VARIABLE_FACTORY, node.parentOfType(Function.class), accessVar.name(), Scope.VAR, varPos.start(), varPos.end(), null);
						} else
							loopVariable = as(d, Variable.class);
					} else
						loopVariable = null;

					visit.visitNode(elementExpr, true);
					visit.visitNode(arrayExpr, true);

					IType type = ty(arrayExpr, visit);
					if (!type.canBeAssignedFrom(PrimitiveType.ARRAY))
						visit.incompatibleTypes(node, arrayExpr, type, PrimitiveType.ARRAY);
					IType elmType = ArrayType.elementTypeSet(type);
					TypeEnvironment env = visit.newTypeEnvironment();
					{
						if (loopVariable != null) {
							loopVariable.setUsed(true);
							judgement(new AccessVar(loopVariable), elmType, TypingJudgementMode.Unify, visit);
						}
						visit.visitNode(node.body(), true);
					}
					visit.endTypeEnvironment(env, true, false);
					visit.controlFlow = t;
				}
			},

			new Expert<SimpleStatement>(SimpleStatement.class) {
				@Override
				public void visit(SimpleStatement node, Visitor visit) throws ParsingException {
					BinaryOp op = as(node.expression(), BinaryOp.class);
					if (op != null && !op.operator().modifiesArgument())
						visit.markers().warning(visit, Problem.NoAssignment, node, op, 0);
					supr.visit(node, visit);
				}
			},

			new ConditionalStatementExpert<IfStatement>(IfStatement.class) {
				@Override
				public void visit(IfStatement node, Visitor visit) throws ParsingException {
					ControlFlow old = visit.controlFlow;
					ASTNode condition = node.condition();
					visit.visitNode(condition, true);
					// use two separate type environments for if and else statement, merging
					// gathered information afterwards
					TypeEnvironment ifEnvironment = visit.newTypeEnvironment();
					visit.visitNode(node.body(), true);
					visit.endTypeEnvironment(ifEnvironment, false, false);
					visit.controlFlow = old;
					if (node.elseExpression() != null) {
						TypeEnvironment elseEnvironment = visit.newTypeEnvironment();
						visit.visitNode(node.elseExpression(), true);
						visit.endTypeEnvironment(elseEnvironment, false, false);
						ifEnvironment.inject(elseEnvironment, false);
					}
					if (ifEnvironment.up != null)
						ifEnvironment.up.inject(ifEnvironment, false);
					visit.controlFlow = old;

					if (!condition.containsConst()) {
						Object condEv = PrimitiveType.BOOL.convert(condition.evaluateStatic(node.parentOfType(Function.class)));
						if (condEv != null && condEv != ASTNode.EVALUATION_COMPLEX)
							visit.markers().warning(visit,
								condEv.equals(true) ? Problem.ConditionAlwaysTrue : Problem.ConditionAlwaysFalse,
								condition, condition, 0, condition);
					}
				};
			},

			new ConditionalStatementExpert<ForStatement>(ForStatement.class) {
				@Override
				public void visit(ForStatement node, Visitor visit) throws ParsingException {
					if (node.initializer() != null)
						visit.visitNode(node.initializer(), true);
					super.visit(node, visit);
					if (node.increment() != null)
						visit.visitNode(node.increment(), true);
				}
			},

			new ConditionalStatementExpert<WhileStatement>(WhileStatement.class),

			new Expert<NewProplist>(NewProplist.class) {
				@Override
				public void visit(NewProplist node, Visitor visit) throws ParsingException {
					node.definedDeclaration().setPrototype(as(ty(node.prototype(), visit), ProplistDeclaration.class));
				}
			},

			new Expert<Placeholder>(Placeholder.class) {
				@Override
				public void visit(Placeholder node, Visitor visit) throws ParsingException {
					StringTbl.reportMissingStringTblEntries(visit, new EntityRegion(null, node, node.entryName()), node);
				}
			},

			new Expert<MissingStatement>(MissingStatement.class) {
				@Override
				public void visit(MissingStatement node, Visitor visit) throws ParsingException {
					visit.markers().error(visit, Problem.MissingStatement, node, node, Markers.NO_THROW);
				}
			},

			new Expert<GarbageStatement>(GarbageStatement.class) {
				@Override
				public void visit(GarbageStatement node, Visitor visit) throws ParsingException {
					visit.markers().error(visit, Problem.Garbage, node, node, Markers.NO_THROW, node.garbage());
				}
			},

			new Expert<FunctionDescription>(FunctionDescription.class) {
				@Override
				public void visit(FunctionDescription node, Visitor visit) throws ParsingException {
					if (visit.base.hasAppendTo)
						return;
					int off = 1;
					for (String part : node.contents().split("\\|")) { //$NON-NLS-1$
						if (part.startsWith("$") && part.endsWith("$")) { //$NON-NLS-1$ //$NON-NLS-2$
							StringTbl stringTbl = visit.script().localStringTblMatchingLanguagePref();
							String entryName = part.substring(1, part.length()-1);
							if (stringTbl == null || stringTbl.map().get(entryName) == null)
								visit.markers().warning(visit, Problem.UndeclaredIdentifier, node,
									new Region(node.start()+off, part.length()), 0, entryName);
						}
						off += part.length()+1;
					}
				}
			},

			new Expert<Comment>(Comment.class) {
				@Override
				public void visit(Comment node, Visitor visit) throws ParsingException {
					if (!visit.roaming || node.parentOfType(Script.class) == visit.script()) {
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
								visit.markers().todo(visit.file(), node, s.substring(todoIndex, lineEnd), node.start()+2+todoIndex, node.start()+2+lineEnd, markerPriority);
							}
						} while (markerPriority > IMarker.PRIORITY_LOW);
					}
				}
			},

			new Expert<Unfinished>(Unfinished.class) {
				@Override
				public IType type(Unfinished node, Visitor visit) {
					return ty(node.expression(), visit);
				}
				@Override
				public void visit(Unfinished node, Visitor visit) throws ParsingException {
					visit.markers().error(visit, Problem.NotFinished, node, node, Markers.NO_THROW, node);
				}
			}

		};
		for (Expert<?> expert : classes)
			committee.put(expert.cls(), expert);
		for (Expert<?> expert : classes)
			expert.findSuper();
	}

}
