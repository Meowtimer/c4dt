package net.arctics.clonk.parser.c4script.inference.dabble;

import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.defaulting;
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

	static final boolean UNUSEDPARMWARNING = false;

	private static class Shared {
		ClonkBuilder builder;
		IProgressMonitor monitor;
		final Map<Script, ScriptInfo> infos = new HashMap<>();
	}
	private Shared shared;

	@Override
	public void initialize(Markers markers, ClonkBuilder builder) {
		super.initialize(markers, builder);
		shared = new Shared();
		shared.builder = builder;
		shared.monitor = builder.monitor();
		assembleCommittee();
	}

	@Override
	public void run() { work(); }

	@Profiled
	final void work() {
		threadPool(new Sink<ExecutorService>() {
			@Override
			public void receivedObject(ExecutorService pool) {
				final Visitor[] visitors = new Visitor[shared.builder.parsers().size()];
				int i = 0;
				for (final C4ScriptParser p : shared.builder.parsers())
					if (p != null) {
						final ScriptInfo info = new ScriptInfo(p.script(), p.fragmentOffset(), shared);
						shared.infos.put(p.script(), info);
						final Visitor v = new Visitor(info);
						v.setMarkers(shared.builder.markers());
						visitors[i++] = v;
					}
				for (final Visitor v : visitors)
					if (v != null)
						pool.execute(v);
			}
		}, 20);
		for (final ScriptInfo info : shared.infos.values())
			dismissExperts(info.script());
	}

	@Override
	public ProblemReportingContext localTypingContext(Script script, int fragmentOffset, ProblemReportingContext chain) {
		if (chain instanceof Visitor) {
			final ScriptInfo p = ((Visitor) chain).info.shared.infos.get(script);
			if (p != null)
				return new Visitor(p);
		}
		if (!(chain instanceof Visitor)) {
			shared = new Shared();
			assembleCommittee();
		}
		final ScriptInfo info = new ScriptInfo(script, fragmentOffset, shared);
		shared.infos.put(script, info);
		return new Visitor(info);
	}

	public static class CurrentFunctionReturnTypeVariable extends FunctionReturnTypeVariable {
		public Thread thread;
		public CurrentFunctionReturnTypeVariable(Function function) {
			super(function);
			thread = Thread.currentThread();
		}
		@Override
		public void apply(boolean soft, Visitor visitor) { /* done by Dabble */ }
	}

	protected final class ScriptInfo {
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
		public ScriptInfo(Script script, int sourceFragmentOffset, Shared shared) {
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
			for (final Directive d : script.directives())
				if (d.type() == DirectiveType.APPENDTO) {
					hasAppendTo = true;
					break;
				}
			this.hasAppendTo = hasAppendTo;
		}
	}

	private static final IASTVisitor<Function> ACCESSDECLARATIONCLEAR = new IASTVisitor<Function>() {
		@Override
		public TraversalContinuation visitNode(ASTNode node, Function parser) {
			if (node instanceof AccessDeclaration)
				((AccessDeclaration)node).setDeclaration(null);
			return TraversalContinuation.Continue;
		}
	};

	private static final PrimitiveType[] callTypingSeeds = new PrimitiveType[] {
		PrimitiveType.UNKNOWN,
		PrimitiveType.INT,
		PrimitiveType.ID,
		PrimitiveType.OBJECT,
		PrimitiveType.STRING,
		PrimitiveType.PROPLIST
	};

	public class Visitor implements Runnable, ProblemReportingContext, IEvaluationContext {

		final ScriptInfo info;

		private ControlFlow controlFlow;
		private Markers markers;
		private List<Script> conglomerate;
		private TypeEnvironment typeEnvironment;
		private Function preliminary;

		public Visitor(ScriptInfo data, Script... scope) {
			this.markers = DabbleInference.this.markers();
			this.info = data;
			this.conglomerate = Arrays.asList(scope);
		}

		public Visitor(ScriptInfo data) { this(data, data.script); }

		public final SpecialFuncRule specialRuleFor(CallDeclaration node, int role) {
			final Engine engine = script().engine();
			if (engine != null && engine.specialRules() != null)
				return engine.specialRules().funcRuleFor(node.name(), role);
			else
				return null;
		}

		private boolean assignDefaultParmTypesToFunction(Function function) {
			if (info.rules != null)
				for (final SpecialFuncRule funcRule : info.rules.defaultParmTypeAssignerRules())
					if (funcRule.assignDefaultParmTypes(this, function))
						return true;
			return false;
		}

		@Override
		public boolean triggersRevisit(Function function, Function called) { return called.typeFromCallsHint(); }

		private void initialParameterTypesFromCalls(Function function, Function baseFunction, TypeVariable[] callTypes) {
			final List<CallDeclaration> calls = info.index.callsTo(function.name());
			if (calls != null) {
				final IType[][] types = new IType[calls.size()][callTypes.length];
				final Visitor[] visitors = new Visitor[calls.size()];
				for (int ci = 0; ci < calls.size(); ci++) {
					final CallDeclaration call = calls.get(ci);
					final Function f = call.parentOfType(Function.class);
					final Script other = f.parentOfType(Script.class);
					final ScriptInfo info = this.info.shared.infos.get(other);
					Visitor visitor;
					if (info != null) {
						visitor = info == this.info ? this : new Visitor(info);
						visitFunction(f, function);
					} else
						visitor = null;
					visitors[ci] = visitor;
					Function ref = as(visitor != null ? visitor.obtainDeclaration(call) : call.declaration(), Function.class);
					if (ref != null) {
						ref = ref.baseFunction();
						if (ref != null)
							ref = (Function)ref.latestVersion();
						if (ref == baseFunction) {
							final int parNum = Math.min(callTypes.length, call.params().length);
							for (int pa = 0; pa < parNum; pa++)
								if (!function.parameter(pa).staticallyTyped()) {
									final ASTNode concretePar = call.params()[pa];
									if (concretePar != null) {
										script().addUsedScript(other);
										types[ci][pa] = visitor != null ? visitor.typeOf(concretePar) : concretePar.inferredType();
									}
								}
						}
					}
				}

				for (int pa = 0; pa < callTypes.length; pa++) {
					final Variable p = function.parameter(pa);
					if (p.staticallyTyped())
						continue;
					IType result;
					final boolean lenient = callTypes[pa].get() == PrimitiveType.UNKNOWN;
					// if there are concrete parameter types not unifying seed the unification chain with some primitive type
					// and look which seed results in least disagreement. Then place warning markers at the concrete parameters
					// not unifying with that consensus.
					// Only seeds are considered which unify with parameter usage in the function body in the first place.
					Seeding: {

						int leastDiscord = Integer.MAX_VALUE;
						PrimitiveType bestSeed = null;
						for (final PrimitiveType seed : callTypingSeeds) {
							result = TypeUnification.unifyNoChoice(callTypes[pa].get(), seed);
							if (result == null)
								continue; // disagreement with usage inside body - ignore
							int discord = 0;
							for (int ci = 0; ci < calls.size(); ci++) {
								final IType concreteTy = types[ci][pa];
								if (concreteTy == null)
									continue;
								final IType unified = TypeUnification.unifyNoChoice(result, concreteTy);
								if (unified == null)
									discord++;
								else
									result = unified;
							}
							if (discord == 0)
								// no disagreement - type away
								break Seeding;
							else if (discord < leastDiscord) {
								bestSeed = seed;
								leastDiscord = discord;
							}
						}

						if (bestSeed != null) {
							result = TypeUnification.unify(callTypes[pa].get(), bestSeed);
							for (int ci = 0; ci < calls.size(); ci++) {
								final IType concreteTy = types[ci][pa];
								if (concreteTy == null)
									continue;
								final IType unified = TypeUnification.unifyNoChoice(result, concreteTy);
								if (unified == null) {
									final Visitor visitor = visitors[ci];
									final ASTNode concretePar = calls.get(ci).params()[pa];
									if (visitor != null && !lenient)
										visitor.concreteArgumentMismatch(concretePar, p, function, result, concreteTy);
								}
								else
									result = unified;
							}
						} else if (!lenient) {
							// no consensus at all - warnings at all call sides
							result = callTypes[pa].get();
							for (int ci = 0; ci < calls.size(); ci++) {
								final IType concreteTy = types[ci][pa];
								if (concreteTy == null)
									continue;
								final Visitor visitor = visitors[ci];
								final ASTNode concretePar = calls.get(ci).params()[pa];
								if (visitor != null)
									visitor.concreteArgumentMismatch(concretePar, p, function, result, concreteTy);
							}
						} else
							result = PrimitiveType.ANY;

					}
					if (lenient)
						result = TypeUnification.unify(result, PrimitiveType.ANY);
					callTypes[pa].set(result);
				}
			}
		}

		final boolean allParametersStaticallyTyped(Function function) {
			for (final Variable p : function.parameters())
				if (!p.staticallyTyped())
					return false;
			return true;
		}

		@Override
		public TypeVariable visitFunction(Function function) { return visitFunction(function, null); }

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

		public TypeVariable visitFunction(Function function, Declaration hello) {
			if (function == null || function.body() == null)
				return null;
			if (function == hello)
				return null;
			final Script funScript = function.script();
			if (conglomerate != null && conglomerate.contains(funScript)) {
				CurrentFunctionReturnTypeVariable returnType;
				boolean kickOff = false;
				synchronized (info.finishedFunctions) {
					returnType = info.finishedFunctions.get(function);
					if (returnType == null) {
						info.finishedFunctions.put(function, returnType = new CurrentFunctionReturnTypeVariable(function));
						kickOff = true;
					}
				}
				if (!kickOff) synchronized (returnType) {
					if (returnType.thread != Thread.currentThread()) {
						//if (hello != null)
						//	System.out.println(String.format("'%s' waiting for '%s'", hello.qualifiedName(), function.qualifiedName()));
						int i;
						for (i = 0; i < 3 && returnType.thread != null; i++)
							try {
								returnType.wait(100);
							} catch (final InterruptedException e) {
								e.printStackTrace();
							}
						if (i == 3 && hello != null)
							System.out.println(String.format("'%s' gave up waiting for '%s'", hello.qualifiedName(), function.qualifiedName()));
					}
					return returnType;
				}
				assignExperts(function);
				try {
					final ASTNode[] statements = function.body().statements();
					final TypeEnvironment env = newTypeEnvironment();
					{
						env.add(returnType);
						final boolean ownedFunction = !roaming || funScript == script();
						if (!ownedFunction)
							for (final Variable l : function.locals()) {
								final AccessVar av = AccessVar.temp(l, function.body());
								final TypeVariable ti = expert(av).requestTypeVariable(av, this);
								if (ti != null)
									ti.set(PrimitiveType.UNKNOWN);
							}
						final List<Variable> parameters = function.parameters();
						final Function baseFunction = function.baseFunction();
						final boolean typeFromCalls =
							ownedFunction && !assignDefaultParmTypesToFunction(function) &&
							info.typing == Typing.ParametersOptionallyTyped &&
						//	baseFunction.visibility() != FunctionScope.GLOBAL &&
							info.script instanceof Definition &&
							function.numParameters() > 0 &&
							(function.typeFromCallsHint() || !allParametersStaticallyTyped(function));
						function.setTypeFromCallsHint(typeFromCalls);

						// create type variables for parameters
						final TypeVariable[] callTypes = new TypeVariable[parameters.size()];
						for (int i = 0; i < callTypes.length; i++) {
							final Variable p = function.parameter(i);
							final TypeVariable tyvar = new VariableTypeVariable(p);
							tyvar.set(p.type());
							typeEnvironment.add(tyvar);
							callTypes[i] = tyvar;
						}

						if (typeFromCalls) {
							// when taking parameter types from calls to the function visit the function body an additional time
							// before visiting the functions the calls are in.
							// Merge insights about types from visiting the body with the types of concrete parameters.
							// This way, the functions calling this function - which also call visitFunction - also will have
							// some preliminary return type for this function.
							// Also, merging call types with how the parameter is actually used inside the body improves
							// the chance of correctly deciding which kind of parameters are the 'right' ones to pass to the function.
							final Function oldp = preliminary; preliminary = function;
							startRoaming();
							{
								final ControlFlow old = controlFlow;
								controlFlow = ControlFlow.Continue;
								for (final ASTNode s : statements)
									visitNode(s, true);
								controlFlow = old;
							}
							endRoaming();
							preliminary = oldp;
							function.traverse(ACCESSDECLARATIONCLEAR, function);
							initialParameterTypesFromCalls(function, baseFunction, callTypes);
						}
						if (ownedFunction)
							for (int i = 0; i < callTypes.length; i++)
								callTypes[i].apply(false, this);
						final ControlFlow old = controlFlow;
						controlFlow = ControlFlow.Continue;
						for (final ASTNode s : statements)
							visitNode(s, true);
						controlFlow = old;
					}
					if (!roaming)
						env.apply(this, false);
					endTypeEnvironment(env, true, true);
					warnAboutPossibleProblemsWithFunctionLocalVariables(function, statements);
					dismissExperts(function);
				}
				catch (final ParsingException e) { return null; }
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

		public void concreteArgumentMismatch(ASTNode argument, Variable parameter, Function callee, IType expected, IType got) {
			try {
				this.markers().marker(this,
					Problem.ConcreteArgumentMismatch,
					argument, argument.start(), argument.end(),
					Markers.NO_THROW, IMarker.SEVERITY_WARNING,
					argument, parameter.name(), callee.qualifiedName(), expected.typeName(true), got.typeName(true)
				);
			} catch (final ParsingException e) {}
		}

		@Override
		public void incompatibleTypesMarker(ASTNode node, IRegion region, IType left, IType right) {
			try {
				if (left == null)
					left = PrimitiveType.ANY;
				if (right == null)
					right = PrimitiveType.ANY;
				this.markers().marker(this, Problem.IncompatibleTypes, node, region.getOffset(), region.getOffset()+region.getLength(), Markers.NO_THROW,
					info.typing == Typing.Static ? IMarker.SEVERITY_ERROR : IMarker.SEVERITY_WARNING,
					left.typeName(true), right.typeName(true)
				);
			} catch (final ParsingException e) {}
		}

		public final <T extends ASTNode> T visitNode(T expression, boolean recursive) throws ParsingException {
			if (expression == null)
				return null;
			final Expert<? super T> expert = expert(expression);
			final ControlFlow old = controlFlow;
			if (recursive && !expert.skipReportingProblemsForSubElements())
				for (final ASTNode e : expression.subElements())
					if (e != null)
						visitNode(e, true);
			controlFlow = old;
			expert.visit(expression, this);
			if (controlFlow == ControlFlow.Continue)
				controlFlow = expression.controlFlow();
			return expression;
		}

		public TypeEnvironment newTypeEnvironment() {
			final TypeEnvironment l = new TypeEnvironment();
			l.up = typeEnvironment;
			return typeEnvironment = l;
		}

		public void endTypeEnvironment(TypeEnvironment env, boolean inject, boolean ignoreLocals) {
			if (inject)
				if (env.up != null)
					env.up.inject(env, ignoreLocals);
				else synchronized (info.typeEnvironment) {
					info.typeEnvironment.inject(env, ignoreLocals);
				}
			typeEnvironment = env.up;
		}

		private boolean createWarningAtDeclarationOfVariable(
			ASTNode[] statements,
			Variable variable,
			Problem code,
			Object... format
		) {
			for (final ASTNode s : statements)
				for (final VarDeclarationStatement decl : s.collectionExpressionsOfType(VarDeclarationStatement.class))
					for (final VarInitialization initialization : decl.variableInitializations())
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
				for (final Variable p : func.parameters())
					if (!p.isUsed())
						this.markers().warning(this, Problem.UnusedParameter, null, p, Markers.ABSOLUTE_MARKER_LOCATION, p.name());
			if (func.locals() != null)
				for (final Variable v : func.locals()) {
					if (!v.isUsed())
						createWarningAtDeclarationOfVariable(statements, v, Problem.Unused, v.name());
					final Variable shadowed = script().findVariable(v.name());
					// ignore those pesky static variables from scenario scripts
					if (shadowed != null && !(shadowed.parentDeclaration() instanceof Scenario))
						createWarningAtDeclarationOfVariable(statements, v, Problem.IdentShadowed, v.qualifiedName(), shadowed.qualifiedName());
				}
		}

		private final class RoamingMarkers extends Markers {
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
				if (node == null || node.parentOfType(Script.class) != origin || (preliminary != null && node.containedIn(preliminary)))
					return;
				else
					oldMarkers.marker(positionProvider, code, node, markerStart, markerEnd, flags, severity, args);
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
			for (final Function f : script.functions()) {
				// skip function that have been overridden
				if (roaming && !script().seesFunction(f))
					continue;
				visitFunction(f);
			}
			if (roaming)
				endRoaming();
		}

		@Override
		public void reportProblems() {
			synchronized (info) {
				if (info.finished)
					return;
				else
					info.finished = true;
			}
			work();
		}

		private void work() {
			// revisit all inherited scripts since that is the only way to
			// accurately type inherited functions with respect to added things from this script
			final TypeEnvironment env1 = newTypeEnvironment();
			{
				conglomerate = info.script.conglomerate();
				for (final Script include : conglomerate)
					if (include != info.script)
						visit(include, true);
				conglomerate = Arrays.asList(script());
				storeTypings(env1);
			}
			endTypeEnvironment(env1, false, false);

			final TypeEnvironment env2 = newTypeEnvironment();
			{
				visit(script(), false);
				storeTypings(env2);
				script().setTypings(info.variableTypes, info.functionReturnTypes);
				env2.apply(this, false);
			}
			endTypeEnvironment(env2, true, false);

			info.typeEnvironment.apply(this, false);
		}

		private void storeTypings(TypeEnvironment typeEnvironment) {
			for (final TypeVariable tyvar : typeEnvironment.values()) {
				final VariableTypeVariable vti = as(tyvar, VariableTypeVariable.class);
				if (vti != null && vti.variable().scope() == Scope.LOCAL)
					for (final Script s : conglomerate)
						if (vti.variable().containedIn(s)) {
							synchronized (info.variableTypes) {
								info.variableTypes.put(vti.variable(), vti.get());
							}
							break;
						}
				final FunctionReturnTypeVariable ftri = as(tyvar, FunctionReturnTypeVariable.class);
				if (ftri != null && ftri.function().visibility() != FunctionScope.GLOBAL && info.script.seesFunction(ftri.function()))
					synchronized (info.functionReturnTypes) {
						info.functionReturnTypes.put(ftri.function().name(), ftri.get());
					}
				final Declaration d = tyvar.declaration();
				if (d != null && d.containedIn(script()))
					tyvar.apply(false, this);
			}
		}

		@Override
		public void run() {
			if (info.finished)
				return;
			if (info.shared.monitor.isCanceled())
				return;
			info.shared.monitor.subTask(String.format("Reporting problems for '%s'", script().name()));
			reportProblems();
			info.shared.monitor.worked(1);
		}

		@Override
		public Definition definition() { return as(info.script, Definition.class); }
		@Override
		public SourceLocation absoluteSourceLocationFromExpr(ASTNode expression) {
			final Function f = expression.parentOfType(Function.class);
			final int bodyOffset = f != null ? f.bodyLocation().start() : 0;
			return new SourceLocation(
				info.fragmentOffset+bodyOffset+expression.start(),
				info.fragmentOffset+bodyOffset+expression.end()
			);
		}
		@Override
		public CachedEngineDeclarations cachedEngineDeclarations() { return info.cachedEngineDeclarations; }
		@Override
		public Script script() { return info.script; }
		@Override
		public IFile file() { return script().scriptFile(); }
		@Override
		public Declaration container() { return script(); }
		@Override
		public int fragmentOffset() { return info.fragmentOffset; }
		@Override
		public IType typeOf(ASTNode node) { return ty(node, this); }
		@Override
		public boolean isModifiable(ASTNode node) { return expert(node).isModifiable(node, this); }

		@Override
		public <T extends AccessDeclaration> Declaration obtainDeclaration(T access) {
			@SuppressWarnings("unchecked")
			final AccessDeclarationExpert<T> expert = (AccessDeclarationExpert<T>)expert(access);
			return expert.obtainDeclaration(access, this);
		}

		public boolean validForType(ASTNode node, IType type) {
			return expert(node).unifyDeclaredAndGiven(node, type, this) != null;
		}

		@Override
		public void judgement(ASTNode node, IType type, TypingJudgementMode mode) {
			expert(node).typingJudgement(node, type, null, this, mode);
		}

		@Override
		@SuppressWarnings("unchecked")
		public final <T extends IType> T typeOf(ASTNode node, Class<T> cls) {
			for (final IType t : typeOf(node))
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
				final
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
		public void visit(T node, Visitor visitor) throws ParsingException {}
		public IType type(T node, Visitor visitor) { return PrimitiveType.UNKNOWN; }
		public Declaration typeEnvironmentKey(T node, Visitor visitor) { return null; }
		public TypeVariable createTypeVariable(T node, Visitor visitor) { return null; }
		@Override
		public String toString() { return String.format("Expert<%s>", cls.getSimpleName()); }
		public boolean isModifiable(T node, Visitor visitor) { return true; }

		public final IType predecessorType(ASTNode node, Visitor visitor) {
			final ASTNode p = node.predecessorInSequence();
			return p != null ? ty(p, visitor) : null;
		}

		/**
		 * Return whether this expression is valid as a value of the specified type.
		 * @param type The type to test against
		 * @param context Script parser context
		 * @return True if valid, false if not.
		 */
		public final IType unifyDeclaredAndGiven(ASTNode node, IType type, Visitor visitor) {
			final IType myType = ty(node, visitor);
			if (type == null)
				return myType;
			return TypeUnification.unifyNoChoice(type, myType);
		}

		public TypeVariable findTypeVariable(T node, Visitor visitor) {
			final Declaration key = typeEnvironmentKey(node, visitor);
			return findTypeVariable(key, visitor);
		}

		/**
		 * Query the type variable of an arbitrary expression. With some luck the inference engine will be able to give an answer.
		 * @param node the expression to query the type of
		 * @return The {@link TypeVariable} or null if nothing was found
		 */
		public TypeVariable findTypeVariable(Declaration key, Visitor visitor) {
			if (key == null)
				return null;
			for (TypeEnvironment e = visitor.typeEnvironment; e != null; e = e.up) {
				final TypeVariable info = e.find(key);
				if (info != null)
					return info;
			}
			return null;
		}

		/**
		 * Requests type information for an expression
		 * @param expression the expression
		 * @return the type information or null if none has been stored
		 */
		public final TypeVariable requestTypeVariable(T node, Visitor visitor) {
			final Declaration key = typeEnvironmentKey(node, visitor);
			if (key == null)
				return null;
			final TypeEnvironment env = visitor.typeEnvironment;
			if (env == null || visitor.info.typing == Typing.Static || visitor.info.typing == Typing.Dynamic)
				return null;
			boolean topMostLayer = true;
			TypeVariable base = null;
			for (TypeEnvironment list = env; list != null; list = list.up) {
				final TypeVariable tyvar = list.find(key);
				if (tyvar != null)
					if (!topMostLayer) {
						base = tyvar;
						break;
					}
					else
						return tyvar;
				topMostLayer = false;
			}
			final TypeVariable newtyvar = createTypeVariable(node, visitor);
			if (newtyvar != null) {
				if (base != null)
					newtyvar.set(base.get());
				env.add(newtyvar);
			}
			return newtyvar;
		}

		public boolean typingJudgement(T node, IType type, ASTNode origin, Visitor visitor, TypingJudgementMode mode) {
			final TypeVariable tyvar = requestTypeVariable(node, visitor);
			if (tyvar != null) {
				switch (mode) {
				case OVERWRITE:
					tyvar.set(type);
					break;
				case UNIFY:
					tyvar.set(TypeUnification.unify(tyvar.get(), type));
					break;
				}
				return true;
			} else
				return false;
		}

		public final void assignment(T leftSide, ASTNode rightSide, Visitor visitor) {
			switch (visitor.info.typing) {
			case Static:
				final IType leftTy = ty(leftSide, this, visitor);
				final IType rightTy = ty(rightSide, visitor);
				if (!TypeUnification.compatible(leftTy, rightTy))
					visitor.incompatibleTypesMarker(rightSide, rightSide, leftTy, rightTy);
				break;
			case ParametersOptionallyTyped:
				judgement(leftSide, ty(rightSide, visitor), rightSide, TypingJudgementMode.OVERWRITE, visitor);
				break;
			case Dynamic:
				break;
			}
		}
	}

	private final Expert<ASTNode> MASTER_OF_NONE = new Expert<ASTNode>(ASTNode.class) {
		@Override
		public IType type(ASTNode node, Visitor visitor) { return PrimitiveType.UNKNOWN; }
	};

	private final <T extends ASTNode> Expert<? super T> findExpert(T node) {
		for (Class<?> cls = node.getClass(); cls != null; cls = cls.getSuperclass()) {
			@SuppressWarnings("unchecked")
			final
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

	public final IType ty(ASTNode node, Visitor visitor) {
		return node != null ? ty(node, expert(node), visitor) : null;
	}

	public final <T extends ASTNode> IType ty(T node, Expert<T> expert, Visitor visitor) {
		final IType type = expert.type(node, visitor);
		node.inferredType(type);
		return type;
	}

	public final boolean judgement(ASTNode node, IType type, TypingJudgementMode mode, Visitor visitor) {
		return expert(node).typingJudgement(node, type, null, visitor, mode);
	}

	public final boolean judgement(ASTNode node, IType type, ASTNode origin, TypingJudgementMode mode, Visitor visitor) {
		return expert(node).typingJudgement(node, type, origin, visitor, mode);
	}

	private final Map<Class<? extends ASTNode>, Expert<? extends ASTNode>> committee = new HashMap<Class<? extends ASTNode>, Expert<?>>();

	class AccessDeclarationExpert<T extends AccessDeclaration> extends Expert<T> {
		public AccessDeclarationExpert(Class<T> cls) { super(cls); }
		protected Declaration obtainDeclaration(T node, Visitor visitor) { return null; }
		@Override
		public void visit(T node, Visitor visitor) throws ParsingException {
			super.visit(node, visitor);
			internalObtainDeclaration(node, visitor);
		}
		protected final Declaration internalObtainDeclaration(T node, Visitor visitor) {
			if (!visitor.roaming || visitor.script() == node.parentOfType(Script.class)) {
				if (node.declaration() == null)
					node.setDeclaration(obtainDeclaration(node, visitor));
				if (node.declaration() == null) {
					visitor.script().index().loadScriptsContainingDeclarationsNamed(node.name());
					node.setDeclaration(obtainDeclaration(node, visitor));
				}
				return node.declaration();
			} else
				return obtainDeclaration(node, visitor);
		}
		@Override
		public TypeVariable createTypeVariable(T node, Visitor visitor) {
			if (node.declaration() instanceof ITypeable && ((ITypeable)node.declaration()).staticallyTyped())
				return null;
			else
				return super.createTypeVariable(node, visitor);
		}
		@Override
		public IType type(T node, Visitor visitor) {
			final TypeVariable tyvar = findTypeVariable(node, visitor);
			return tyvar != null ? tyvar.get() : PrimitiveType.UNKNOWN;
		}
		@Override
		public Declaration typeEnvironmentKey(T node, Visitor visitor) {
			return internalObtainDeclaration(node, visitor);
		}
	}

	private void assembleCommittee() {

		class AccessVarExpert<T extends AccessVar> extends AccessDeclarationExpert<T> {
			private AccessVarExpert(Class<T> cls) {
				super(cls);
			}

			private Declaration findUsingType(Visitor visitor, AccessVar node, ASTNode predecessor, IType type) {
				if (type == null)
					return null;
				for (final IType t : type) {
					Script scriptToLookIn;
					if ((scriptToLookIn = Definition.scriptFrom(t)) == null) {
						// find pseudo-variable from proplist expression
						if (t instanceof IProplistDeclaration) {
							final Variable proplistComponent = ((IProplistDeclaration)t).findComponent(node.name());
							if (proplistComponent != null)
								return proplistComponent;
						}
					} else {
						final FindDeclarationInfo info = new FindDeclarationInfo(visitor.script().index());
						info.searchOrigin = scriptToLookIn;
						info.findGlobalVariables = predecessor == null;
						Declaration v = scriptToLookIn.findDeclaration(node.name(), info);
						if (v instanceof Definition)
							v = ((Definition)v).proxyVar();
						if (v != null) {
							final Variable var = as(v, Variable.class);
							if (var != null && var.initializationExpression() != null) {
								final Function p = var.initializationExpression().parentOfType(Function.class);
								if (p != null)
									visitor.visitFunction(p, node.parentOfType(Function.class));
							}
							return v;
						}
					}
				}
				return null;
			}

			@Override
			protected Declaration obtainDeclaration(AccessVar node, Visitor visitor) {
				final ASTNode p = node.predecessorInSequence();
				if (p == null && node.name().equals(Variable.THIS.name()))
					return Variable.THIS;
				IType type = visitor.script();
				if (p != null)
					type = ty(p, visitor);
				if (p == null) {
					final Function f = node.parentOfType(Function.class);
					if (f != null) {
						final Variable v = f.findVariable(node.name());
						if (v != null)
							return v;
					}
					Declaration v = visitor.info.variableMap.get(node.name());
					if (v == null && !visitor.info.variableMap.containsKey(node.name())) {
						v = findUsingType(visitor, node, null, type);
						visitor.info.variableMap.put(node.name(), v);
					}
					return v;
				}
				else
					return findUsingType(visitor, node, p, type);
			}

			@Override
			public IType type(T node, Visitor visitor) {
				final Declaration d = internalObtainDeclaration(node, visitor);
				// declarationFromContext(context) ensures that declaration is not null (if there is actually a variable) which is needed for queryTypeOfExpression for example
				if (d == Variable.THIS)
					return visitor.info.thisType;
				final TypeVariable stored = findTypeVariable(node, visitor);
				if (stored != null)
					return stored.get();
				if (d instanceof Function)
					return new FunctionType((Function)d);
				else if (d instanceof Variable) {
					final Variable v = (Variable)d;
					Map<Variable, IType> typesMap= null;
					if (v.scope() == Scope.LOCAL) {
						if (node.predecessorInSequence() == null)
							typesMap = visitor.info.variableTypes;
						else {
							final IType targetType = ty(node.predecessorInSequence(), visitor);
							if (targetType instanceof Script) {
								final ScriptInfo other = visitor.info.shared.infos.get(targetType);
								if (other != null) {
									new Visitor(other).reportProblems();
									typesMap = other.variableTypes;
								} else
									typesMap = ((Script)targetType).variableTypes();
							}
						}
						final IType type = typesMap != null ? typesMap.get(v) : v.type();
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
			public void visit(T node, Visitor visitor) throws ParsingException {
				super.visit(node, visitor);
				final ASTNode pred = node.predecessorInSequence();
				final Declaration declaration = node.declaration();
				if (declaration == null && pred == null)
					visitor.markers().error(visitor, Problem.UndeclaredIdentifier, node, node, Markers.NO_THROW, node.name());
				// local variable used in global function
				else if (declaration instanceof Variable) {
					final Variable var = (Variable) declaration;
					var.setUsed(true);
					switch (var.scope()) {
					case LOCAL:
						final Declaration d = node.parentOfType(Declaration.class);
						if (d != null && pred == null) {
							final Function f = d.topLevelParentDeclarationOfType(Function.class);
							final Variable v = d.topLevelParentDeclarationOfType(Variable.class);
							if (
								(f != null && f.visibility() == FunctionScope.GLOBAL) ||
								(f == null && v != null && v.scope() != Scope.LOCAL)
							)
								visitor.markers().error(visitor, Problem.LocalUsedInGlobal, node, node, Markers.NO_THROW);
						}
						break;
					case STATIC: case CONST:
						visitor.script().addUsedScript(var.script());
						break;
					case VAR:
						final Function currentFunction = node.parentOfType(Function.class);
						if (currentFunction != null && var.parentDeclaration() == currentFunction) {
							final int locationUsed = currentFunction.bodyLocation().getOffset()+node.start();
							if (locationUsed < var.start())
								visitor.markers().warning(visitor, Problem.VarUsedBeforeItsDeclaration, node, node, 0, var.name());
						}
						break;
					case PARAMETER:
						break;
					}
				} else if (declaration instanceof Function)
					if (!visitor.script().engine().settings().supportsFunctionRefs)
						visitor.markers().error(visitor, Problem.FunctionRefNotAllowed, node, node, Markers.NO_THROW, visitor.script().engine().name());
			}

			@Override
			public boolean typingJudgement(T node, IType type, ASTNode origin, Visitor visitor, TypingJudgementMode mode) {
				Declaration declaration = internalObtainDeclaration(node, visitor);
				if (declaration == Variable.THIS)
					return true;
				if (declaration == null && origin != null) {
					final IType predType = predecessorType(node, visitor);
					if (predType != null && TypeUnification.compatible(predType, PrimitiveType.PROPLIST))
						if (predType instanceof IProplistDeclaration) {
							final IProplistDeclaration proplDecl = (IProplistDeclaration) predType;
							if (proplDecl.isAdHoc()) {
								final Variable var = proplDecl.addComponent(
									new Variable(node.name(), Variable.Scope.VAR),
									true
								);
								initializeFromAssignment(node, type, origin, visitor, var);
								node.setDeclaration(declaration = var);
							}
						} else for (final IType t : predType)
							if (t == visitor.script()) {
								final Variable var = new Variable(node.name(), Variable.Scope.LOCAL);
								initializeFromAssignment(node, type, origin, visitor, var);
								visitor.script().addDeclaration(var);
								visitor.script().generateFindDeclarationCache();
								node.setDeclaration(declaration = var);
								break;
							}
				}
				return super.typingJudgement(node, type, origin, visitor, mode);
			}

			private void initializeFromAssignment(T node, IType type, ASTNode origin, Visitor visitor, final Variable var) {
				var.assignType(type);
				var.setLocation(visitor.absoluteSourceLocationFromExpr(node));
				var.forceType(type);
				var.setInitializationExpression(origin);
			}

			@Override
			public TypeVariable findTypeVariable(T node, Visitor visitor) {
				final TypeVariable r = super.findTypeVariable(node, visitor);
				if (r != null)
					return r;
				final Declaration d = node.declaration();
				if (
					d != null && d.parent() == visitor.script() &&
					!(node.parent() instanceof BinaryOp && ((BinaryOp)node.parent()).operator().isAssignment())
				) {
					final List<BinaryOp> assignments = visitor.script().varAssignments().get(node.name());
					if (assignments != null)
						for (final BinaryOp a : assignments)
							visitor.visitFunction(a.parentOfType(Function.class), node.parentOfType(Function.class));
				}
				return super.findTypeVariable(node, visitor);
			}

			@Override
			public TypeVariable createTypeVariable(T node, Visitor visitor) {
				if (node.declaration() instanceof Variable)
					return new VariableTypeVariable(node);
				else
					return null;
			}

			@Override
			public boolean isModifiable(AccessVar node, Visitor visitor) {
				final Declaration declaration = node.declaration();
				final ASTNode pred = node.predecessorInSequence();
				if (pred == null)
					return declaration == null || ((Variable)declaration).scope() != Scope.CONST;
				else
					return true; // you can never be so sure
			}
		}

		class LiteralExpert<T extends Literal<?>> extends Expert<T> {
			public LiteralExpert(Class<T> cls) { super(cls); }
			@Override
			public boolean typingJudgement(T node, IType type, ASTNode origin, Visitor visitor, TypingJudgementMode mode) {
				// constantly steadfast do i resist the pressure of expectancy lied upon me
				return true;
			}
			@Override
			public boolean isModifiable(T node, Visitor visitor) { return false; }
		}

		class ConditionalStatementExpert<T extends ConditionalStatement> extends Expert<T> {
			public ConditionalStatementExpert(Class<T> cls) { super(cls); }
			@Override
			public boolean skipReportingProblemsForSubElements() {return true;}
			@Override
			public void visit(ConditionalStatement node, Visitor visitor) throws ParsingException {
				final ControlFlow t = visitor.controlFlow;
				visitor.controlFlow = ControlFlow.Continue;
				TypeEnvironment env = visitor.newTypeEnvironment();
				visitor.visitNode(node.condition(), true);
				visitor.endTypeEnvironment(env, true, false);
				env = visitor.newTypeEnvironment();
				visitor.visitNode(node.body(), true);
				visitor.endTypeEnvironment(env, true, false);
				loopConditionWarnings(node, visitor);
				visitor.controlFlow = t;
			}
			/**
			 * Emit warnings about loop conditions that could result in loops never executing or never ending.
			 * @param body The loop body. If the condition looks like it will always be true, checks are performed whether the body contains loop control flow statements.
			 * @param condition The loop condition to check
			 */
			protected void loopConditionWarnings(ConditionalStatement node, Visitor visitor) {
				final ASTNode condition = node.condition();
				if (node.body() == null || condition == null || !(node instanceof ILoop))
					return;
				final Object condEv = PrimitiveType.BOOL.convert(condition == null ? true : condition.evaluateStatic(node.parentOfType(Function.class)));
				if (Boolean.FALSE.equals(condEv))
					visitor.markers().warning(visitor, Problem.ConditionAlwaysFalse, condition, condition, Markers.NO_THROW, condition);
				else if (Boolean.TRUE.equals(condEv)) {
					final EnumSet<ControlFlow> flows = node.body().possibleControlFlows();
					if (!(flows.contains(ControlFlow.BreakLoop) || flows.contains(ControlFlow.Return)))
						visitor.markers().warning(visitor, Problem.InfiniteLoop, node, node, Markers.NO_THROW);
				}
			}
		}

		final Expert<?>[] classes = new Expert[] {

			new AccessDeclarationExpert<AccessDeclaration>(AccessDeclaration.class),

			new AccessVarExpert<AccessVar>(AccessVar.class),

			new AccessVarExpert<InitializationFunction.VarInitializationAccess>(InitializationFunction.VarInitializationAccess.class) {
				@Override
				public boolean typingJudgement(VarInitializationAccess leftSide, IType type, ASTNode origin, Visitor visitor, TypingJudgementMode mode) {
					super.typingJudgement(leftSide, type, origin, visitor, mode);
					if (origin != null)
						if (leftSide.declaration() instanceof Variable && ((Variable)leftSide.declaration()).scope() == Scope.CONST && !origin.isConstant())
							try {
								visitor.markers().error(visitor, Problem.NonConstGlobalVarAssignment, origin, origin, Markers.NO_THROW);
							} catch (final ParsingException e) { }
					return true;
				}
				@Override
				public boolean isModifiable(VarInitializationAccess node, Visitor visitor) { return true; /* sudo */ }
			},

			new Expert<ArrayExpression>(ArrayExpression.class) {
				@Override
				public IType type(ArrayExpression node, final Visitor visitor) {
					IType elmType = PrimitiveType.UNKNOWN;
					for (final ASTNode e : node.subElements())
						if (e != null)
							elmType = TypeUnification.unify(elmType, ty(e, visitor));
					return new ArrayType(elmType);
				}
				@Override
				public boolean isModifiable(ArrayExpression node, Visitor visitor) { return false; }
			},

			new Expert<ArrayElementExpression>(ArrayElementExpression.class) {
				@Override
				public IType type(ArrayElementExpression node, Visitor visitor) {
					final IType t = supr.type(node, visitor);
					if (t != PrimitiveType.UNKNOWN && t != PrimitiveType.ANY)
						return t;
					final ASTNode pred = node.predecessorInSequence();
					if (pred != null) {
						final IType predTy = ty(pred, visitor);
						if (predTy != null)
							for (final IType ty : predTy) {
								final ArrayType at = as(ty, ArrayType.class);
								if (at != null)
									return at.elementType();
							}
					}
					return PrimitiveType.ANY;
				}
				@Override
				public boolean typingJudgement(ArrayElementExpression leftSide, IType rightSideType, ASTNode origin, Visitor visitor, TypingJudgementMode mode) {
					final IType predType_ = predecessorType(leftSide, visitor);
					final ASTNode pred = leftSide.predecessorInSequence();
					if (predType_ == null)
						return false;
					if (origin != null) {
						final IType elmTy = ty(leftSide.argument(), visitor);
						for (final IType e : elmTy)
							if (e == PrimitiveType.STRING)
								return judgement(pred, PrimitiveType.PROPLIST, mode, visitor);
						for (final IType predType : predType_) {
							final ArrayType arrayType = as(predType, ArrayType.class);
							if (arrayType != null) {
								if (judgement(pred, new ArrayType(TypeUnification.unify(rightSideType, arrayType.elementType())), mode, visitor))
									return true;
							} else if (predType == PrimitiveType.UNKNOWN || predType == PrimitiveType.ARRAY)
								if (judgement(
									pred,
									new ArrayType(rightSideType),
									mode,
									visitor
								))
									return true;
						}
					}
					return true;
				}
				@Override
				public void visit(ArrayElementExpression node, Visitor visitor) throws ParsingException {
					supr.visit(node, visitor);
					IType type = predecessorType(node, visitor);
					if (type == null)
						type = PrimitiveType.UNKNOWN;
					final ASTNode arg = node.argument();
					if (arg == null)
						visitor.markers().warning(visitor, Problem.MissingExpression, node, node, 0);
					else if (PrimitiveType.UNKNOWN != type && PrimitiveType.ANY != type) {
						final IType argType = ty(arg, visitor);
						final ASTNode pred = node.predecessorInSequence();
						if (argType == PrimitiveType.STRING) {
							if (TypeUnification.unifyNoChoice(PrimitiveType.PROPLIST, type) == null)
								visitor.markers().warning(visitor, Problem.NotAProplist, node, pred, 0);
							else
								judgement(pred, PrimitiveType.PROPLIST, TypingJudgementMode.UNIFY, visitor);
						}
						else if (argType == PrimitiveType.INT)
							if (TypeUnification.unifyNoChoice(PrimitiveType.ARRAY, type) == null)
								visitor.markers().warning(visitor, Problem.NotAnArrayOrProplist, node, pred, 0);
							//else
							//	expert(pred).typingJudgement(pred, PrimitiveType.ARRAY, info, TypingJudgementMode.Unify);
					}
				}
				@Override
				public boolean isModifiable(ArrayElementExpression node, Visitor visitor) { return true; }
			},

			new Expert<ArraySliceExpression>(ArraySliceExpression.class) {
				private void warnIfNotArray(ASTNode node, Visitor visitor, IType type) {
					if (type != null && type != PrimitiveType.UNKNOWN && type != PrimitiveType.ANY &&
						TypeUnification.unifyNoChoice(PrimitiveType.ARRAY, type) == null &&
						TypeUnification.unifyNoChoice(PrimitiveType.PROPLIST, type) == null)
						visitor.markers().warning(visitor, Problem.NotAnArrayOrProplist, node, node, 0);
				}
				@Override
				public void visit(ArraySliceExpression node, Visitor visitor) throws ParsingException {
					supr.visit(node, visitor);
					final IType type = predecessorType(node, visitor);
					warnIfNotArray(node.predecessorInSequence(), visitor, type);
				}
				@Override
				public boolean isModifiable(ArraySliceExpression node, Visitor visitor) { return false; }
			},

			new Expert<OperatorExpression>(OperatorExpression.class) {
				@Override
				public IType type(OperatorExpression node, Visitor visitor) {
					return node.operator().resultType();
				}
				@Override
				public boolean isModifiable(OperatorExpression node, Visitor visitor) { return node.operator().returnsRef(); }
			},

			new Expert<BinaryOp>(BinaryOp.class) {
				@Override
				public IType type(BinaryOp node, Visitor visitor) {
					switch (node.operator()) {
					// &&/|| special: they return either the left or right side of the operator so the return type is the lowest common denominator of the argument types
					case And: case Or: case JumpNotNil:
						final IType leftSideType = ty(node.leftSide(), visitor);
						final IType rightSideType = ty(node.rightSide(), visitor);
						if (leftSideType == rightSideType)
							return leftSideType;
						else
							return TypeUnification.unify(leftSideType, rightSideType);
					case Assign:
						return ty(node.rightSide(), visitor);
					default:
						return supr.type(node, visitor);
					}
				}
				@Override
				public void visit(BinaryOp node, Visitor visitor) throws ParsingException {
					final Operator op = node.operator();
					// sanity
					final ASTNode left = node.leftSide();
					final ASTNode right = node.rightSide();
					node.setLocation(left.start(), right.end());
					// i'm an assignment operator and i can't modify my left side :C
					if (op.modifiesArgument() && !expert(left).isModifiable(left, visitor))
						visitor.markers().error(visitor, Problem.ExpressionNotModifiable, node, left, Markers.NO_THROW);
					// obsolete operators in #strict 2impor
					if ((op == Operator.StringEqual || op == Operator.ne) && (visitor.info.strictLevel >= 2))
						visitor.markers().warning(visitor, Problem.ObsoleteOperator, node, node, 0, op.operatorName());
					// wrong parameter types
					if (unifyDeclaredAndGiven(left, op.firstArgType(), visitor) == null)
						visitor.incompatibleTypesMarker(node, left, op.firstArgType(), ty(left, visitor));
					if (unifyDeclaredAndGiven(right, op.secondArgType(), visitor) == null)
						visitor.incompatibleTypesMarker(node, right, op.secondArgType(), ty(right, visitor));

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
						expert(left).assignment(left, right, visitor);
						break;
					case Equal:
						if (runtimeTypeCheck(visitor, left, right, true))
							return;
						break;
					default:
						break;
					}

					if (expectedLeft != null)
						judgement(left, expectedLeft, TypingJudgementMode.UNIFY, visitor);
					if (expectedRight != null)
						judgement(right, expectedRight, TypingJudgementMode.UNIFY, visitor);
				}
				private boolean runtimeTypeCheck(Visitor visitor, ASTNode left, ASTNode right, boolean checkReverse) {
					if (
						left instanceof CallDeclaration &&
						((CallDeclaration)left).params().length >= 1 &&
						((CallDeclaration)left).name().equals("GetType") &&
						right instanceof AccessVar &&
						((AccessVar)right).name().startsWith("C4V_")
					) {
						final IType type = PrimitiveType.fromString(((AccessVar)right).name().substring(4).toLowerCase());
						if (type != null) {
							judgement(
								((CallDeclaration)left).params()[0],
								TypeChoice.make(PrimitiveType.ANY, type),
								TypingJudgementMode.UNIFY, visitor
							);
							return true;
						}
					}
					return checkReverse && runtimeTypeCheck(visitor, right, left, false);
				}
				@Override
				public TypeVariable createTypeVariable(BinaryOp node, Visitor visitor) {
					final ASTNode leftSide = node.leftSide();
					if (node.operator().isAssignment() && leftSide != null)
						return expert(leftSide).createTypeVariable(leftSide, visitor);
					return super.createTypeVariable(node, visitor);
				}
				@Override
				public Declaration typeEnvironmentKey(BinaryOp node, Visitor visitor) {
					final ASTNode leftSide = node.leftSide();
					if (node.operator().isAssignment() && leftSide != null)
						return expert(leftSide).typeEnvironmentKey(leftSide, visitor);
					else
						return null;
				}
			},

			new Expert<UnaryOp>(UnaryOp.class) {
				@Override
				public void visit(UnaryOp node, Visitor visitor) throws ParsingException {
					supr.visit(node, visitor);
					final ASTNode arg = node.argument();
					if (node.operator().modifiesArgument() && !expert(arg).isModifiable(arg, visitor))
						visitor.markers().error(visitor, Problem.ExpressionNotModifiable, node, arg, Markers.NO_THROW);
					final Expert<? super ASTNode> rarg = expert(arg);
					final PrimitiveType firstArgType = node.operator().firstArgType();
					if (rarg.unifyDeclaredAndGiven(arg, firstArgType, visitor) == null)
						visitor.incompatibleTypesMarker(node, arg, firstArgType,
							ty(arg, rarg, visitor));
					if (firstArgType != PrimitiveType.UNKNOWN)
						rarg.typingJudgement(arg, firstArgType, null, visitor, TypingJudgementMode.UNIFY);
				}
				@Override
				public boolean isModifiable(UnaryOp node, Visitor visitor) {
					return node.placement() == Placement.Prefix && node.operator().returnsRef();
				}
			},

			new Expert<BoolLiteral>(BoolLiteral.class) {
				@Override
				public void visit(BoolLiteral node, Visitor visitor) throws ParsingException {
					supr.visit(node, visitor);
					if (node.parent() instanceof BinaryOp) {
						final Operator op = ((BinaryOp) node.parent()).operator();
						if (op == Operator.And || op == Operator.Or)
							visitor.markers().warning(visitor, Problem.BoolLiteralAsOpArg, node, node, 0, this.toString());
					}
				}
			},

			new Expert<ContinueStatement>(ContinueStatement.class) {
				@Override
				public void visit(ContinueStatement node, Visitor visitor) throws ParsingException {
					if (node.parentOfType(ILoop.class) == null)
						visitor.markers().error(visitor, Problem.KeywordInWrongPlace, node, node, Markers.NO_THROW, node.keyword());
					supr.visit(node, visitor);
				}
			},

			new Expert<BreakStatement>(BreakStatement.class) {
				@Override
				public void visit(BreakStatement node, Visitor visitor) throws ParsingException {
					if (node.parentOfType(ILoop.class) == null)
						visitor.markers().error(visitor, Problem.KeywordInWrongPlace, node, node, Markers.NO_THROW, node.keyword());
					supr.visit(node, visitor);
				}
			},

			new Expert<ReturnStatement>(ReturnStatement.class) {
				private void warnAboutTupleInReturnExpr(Visitor visitor, ASTNode node, boolean tupleIsError) throws ParsingException {
					if (node == null)
						return;
					if (node instanceof Tuple)
						if (tupleIsError)
							visitor.markers().error(visitor, Problem.TuplesNotAllowed, node, node, Markers.NO_THROW);
						else if (visitor.info.strictLevel >= 2)
							visitor.markers().error(visitor, Problem.ReturnAsFunction, node, node, Markers.NO_THROW);
					final ASTNode[] subElms = node.subElements();
					for (final ASTNode e : subElms)
						warnAboutTupleInReturnExpr(visitor, e, true);
				}
				@Override
				public void visit(ReturnStatement node, Visitor visitor) throws ParsingException {
					supr.visit(node, visitor);
					final ASTNode returnExpr = node.returnExpr();
					warnAboutTupleInReturnExpr(visitor, returnExpr, false);
					final Function currentFunction = node.parentOfType(Function.class);
					if (currentFunction == null)
						visitor.markers().error(visitor, Problem.NotAllowedHere, node, node, Markers.NO_THROW, Keywords.Return);
					else if (returnExpr != null)
						if (visitor.info.typing == Typing.Static && currentFunction.staticallyTyped()) {
							if (expert(returnExpr).unifyDeclaredAndGiven(returnExpr, currentFunction.returnType(), visitor) == null)
								visitor.incompatibleTypesMarker(node,
									returnExpr, currentFunction.returnType(), ty(returnExpr, visitor));
						}
						else {
							final IType type = ty(returnExpr, visitor);
							judgement(node, type, TypingJudgementMode.UNIFY, visitor);
						}
				}
				@Override
				public TypeVariable createTypeVariable(ReturnStatement node, Visitor visitor) {
					return new CurrentFunctionReturnTypeVariable(node.parentOfType(Function.class));
				}
				@Override
				public Declaration typeEnvironmentKey(ReturnStatement node, Visitor visitor) {
					return node.parentOfType(Function.class);
				}
			},

			new AccessDeclarationExpert<CallDeclaration>(CallDeclaration.class) {
				/**
				 * Find a {@link Function} for some hypothetical {@link CallDeclaration}, using contextual information such as the {@link ASTNode#type(ProblemReportingContext)} of the {@link ASTNode} preceding this {@link CallDeclaration} in the {@link Sequence}.
				 * @param info Context to use for searching
				 * @param functionName Name of the function to look for. Would correspond to the hypothetical {@link CallDeclaration}'s {@link #name()}
				 * @param pred The predecessor of the hypothetical {@link CallDeclaration} ({@link ASTNode#predecessorInSequence()})
				 * @param listToAddPotentialDeclarationsTo When supplying a non-null value to this parameter, potential declarations will be added to the collection. Such potential declarations would be obtained by querying the {@link Index}'s {@link Index#declarationMap()}.
				 * @return The {@link Function} that is very likely to be the one actually intended to be referenced by the hypothetical {@link CallDeclaration}.
				 */
				private Declaration findUsingType(
					Visitor visitor,
					CallDeclaration node, String functionName,
					IType type
				) {
					final IType lookIn = type != null ? type : visitor.script();
					if (lookIn != null) for (final IType ty : lookIn) {
						Script script = as(ty, Script.class);
						if (script == null && ty instanceof MetaDefinition)
							script = ((MetaDefinition)ty).definition();
						if (script == null)
							continue;
						final FindDeclarationInfo info = new FindDeclarationInfo(visitor.script().index());
						info.searchOrigin = visitor.script();
						info.contextFunction = node.parentOfType(Function.class);
						info.findGlobalVariables = type == null;
						final Declaration dec = script.findDeclaration(functionName, info);
						if (dec instanceof Function)
							// try to visit with this visitor first in case of roaming in included scripts
							if (visitor.visitFunction((Function) dec, info.contextFunction) == null) {
								// if that does not apply use another visitor
								final ScriptInfo other = visitor.info.shared.infos.get(dec.parentOfType(Script.class));
								if (other != null)
									new Visitor(other).visitFunction((Function)dec, info.contextFunction);
							}
						if (dec != null)
							return dec;
					}
					if (type != null) {
						// find global function
						Declaration declaration;
						try {
							declaration = visitor.script().index().findGlobal(Declaration.class, functionName);
						} catch (final Exception e) {
							e.printStackTrace();
							return null;
						}
						// find engine function
						if (declaration == null)
							declaration = visitor.script().index().engine().findFunction(functionName);

						final List<Declaration> allFromLocalIndex = visitor.script().index().declarationMap().get(functionName);
						final Declaration decl = visitor.script().engine().findLocalFunction(functionName, false);
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
				protected Declaration obtainDeclaration(CallDeclaration node, Visitor visitor) {
					final String declarationName = node.name();
					if (declarationName.equals(Keywords.Return))
						return null;
					final ASTNode p = node.predecessorInSequence();
					if (p == null) {
						Declaration f = visitor.info.functionMap.get(node.name());
						if (f == null && !visitor.info.functionMap.containsKey(node.name())) {
							f = findUsingType(visitor, node, declarationName, visitor.script());
							visitor.info.functionMap.put(declarationName, f);
						}
						return f;
					}
					else
						return findUsingType(visitor, node, declarationName, ty(p, visitor));
				}
				private IType declarationType(CallDeclaration node, Visitor visitor) {
					final Declaration d = internalObtainDeclaration(node, visitor);

					// look for gathered type information
					final TypeVariable tyvar = findTypeVariable(node, visitor);
					if (tyvar != null)
						return tyvar.get();

					// calling this() as function -> return object type belonging to script
					if (node.params().length == 0 && d != null && (d == visitor.cachedEngineDeclarations().This || d == Variable.THIS))
						return visitor.info.thisType;

					if (d instanceof Function) {
						// Some special rule applies and the return type is set accordingly
						final SpecialFuncRule rule = node.specialRuleFromContext(visitor, SpecialEngineRules.RETURNTYPE_MODIFIER);
						if (rule != null) {
							final IType type = rule.returnType(visitor, node);
							if (type != null)
								return type;
						}
						final Function f = (Function)d;
						Map<String, IType> typesMap = null;
						if (f.visibility() != FunctionScope.GLOBAL)
							if (node.predecessorInSequence() == null)
								typesMap = visitor.info.functionReturnTypes;
							else {
								final IType targetType = ty(node.predecessorInSequence(), visitor);
								if (targetType instanceof Script) {
									final ScriptInfo other = visitor.info.shared.infos.get(targetType);
									if (other != null) {
										new Visitor(other).visitFunction((Function)d, node.parentOfType(Function.class));
										typesMap = other.functionReturnTypes;
									} else
										typesMap = ((Script)targetType).functionReturnTypes();
								}
							}
						final IType type = typesMap != null ? typesMap.get(d.name()) : null;
						if (type != null)
							return type;
						else
							return f.returnType();
					}
					if (d instanceof Variable)
						return ((Variable)d).type();

					return supr != null ? supr.type(node, visitor) : PrimitiveType.UNKNOWN;
				}
				private boolean unknownFunctionShouldBeError(CallDeclaration node, Visitor visitor) {
					ASTNode pred = node.predecessorInSequence();
					// stand-alone function? always bark!
					if (pred == null)
						return true;
					// not typed? weird
					final IType predType = ty(pred, visitor);
					if (predType == null)
						return false;
					// called via ~? ok
					if (pred instanceof MemberOperator)
						if (((MemberOperator)pred).hasTilde())
							return false;
						else
							pred = pred.predecessorInSequence();
					// allow this->Unknown()
					final AccessDeclaration ad = as(pred, AccessDeclaration.class);
					if (ad != null && (ad.declaration() == Variable.THIS || ad.declaration() == visitor.cachedEngineDeclarations().This))
						return false;
					boolean anyDefinitions = false;
					for (final IType t : predType)
						if (t instanceof Definition)
							anyDefinitions = true;
					return anyDefinitions;
				}
				@Override
				public IType type(CallDeclaration node, Visitor visitor) {
					final IType type = declarationType(node, visitor);
					if (type instanceof FunctionType)
						return ((FunctionType)type).prototype().returnType();
					else
						return type;
				}
				@Override
				public void visit(CallDeclaration node, Visitor visitor) throws ParsingException {
					super.visit(node, visitor);

					final CachedEngineDeclarations cachedEngineDeclarations = visitor.cachedEngineDeclarations();
					final String declarationName = node.name();
					final Declaration declaration = node.declaration();
					final ASTNode[] params = node.params();
					final ASTNode predecessor = node.predecessorInSequence();

					// return as function
					if (declarationName.equals(Keywords.Return)) {
						if (visitor.info.strictLevel >= 2)
							visitor.markers().error(visitor, Problem.ReturnAsFunction, node, node, Markers.NO_THROW);
						else
							visitor.markers().warning(visitor, Problem.ReturnAsFunction, node, node, 0);
					} else if (declaration instanceof Variable) {
						// variable as function
						((Variable)declaration).setUsed(true);
						final IType type = declarationType(node, visitor);
						// no warning when in #strict mode
						if (visitor.info.strictLevel >= 2)
							if (declaration != cachedEngineDeclarations.This && declaration != Variable.THIS && !TypeUnification.compatible(PrimitiveType.FUNCTION, type))
								visitor.markers().warning(visitor, Problem.VariableCalled, node, node, 0, declaration.name(), type.typeName(false));
					} else if (declaration instanceof Function) {
						final Function f = (Function)declaration;
						if (f.visibility() == FunctionScope.GLOBAL || predecessor != null)
							visitor.script().addUsedScript(f.script());

						final SpecialFuncRule rule = visitor.specialRuleFor(node, SpecialEngineRules.ARGUMENT_VALIDATOR);
						final boolean specialCaseHandled =
							rule != null &&
							rule.validateArguments(node, params, visitor);

						// not a special case... check regular parameter types
						if (!specialCaseHandled) {
							int givenParam = 0;
							for (final Variable parm : f.parameters()) {
								if (givenParam >= params.length)
									break;
								final ASTNode given = params[givenParam++];
								if (given == null)
									continue;
								final TypeVariable parmTyVar = findTypeVariable(parm, visitor);
								final IType parmTy = parmTyVar != null ? parmTyVar.get() : parm.type();
								final IType unified = unifyDeclaredAndGiven(given, parmTy, visitor);
								if (unified == null)
									visitor.incompatibleTypesMarker(node, given, parmTy, ty(given, visitor));
								else
									judgement(given, unified, TypingJudgementMode.UNIFY, visitor);
							}
						}
					} else if (declaration == null && unknownFunctionShouldBeError(node, visitor)) {
						final int start = node.start();
						visitor.markers().error(visitor, Problem.UndeclaredIdentifier, node, start, start+declarationName.length(), Markers.NO_THROW, declarationName);
					}
				}
				@Override
				public TypeVariable createTypeVariable(CallDeclaration node, Visitor visitor) {
					final Declaration d = node.declaration();
					if (d instanceof Function) {
						final Function f = (Function) d;
						if (f.staticallyTyped() || f.isEngineDeclaration() || f != node.parentOfType(Function.class))
							return null;
						return new FunctionReturnTypeVariable((Function)d);
					} else
						return null;
				}
				@Override
				public Declaration typeEnvironmentKey(CallDeclaration node, Visitor visitor) {
					return internalObtainDeclaration(node, visitor);
				}
				@Override
				public boolean isModifiable(CallDeclaration node, Visitor visitor) {
					final Declaration declaration = node.declaration();
					final IType t = declaration instanceof Function ? ((Function)declaration).returnType() : PrimitiveType.UNKNOWN;
					return
						TypeUnification.compatible(t, PrimitiveType.REFERENCE) ||
						TypeUnification.compatible(t, PrimitiveType.UNKNOWN);
				}
			},

			new AccessDeclarationExpert<CallInherited>(CallInherited.class) {
				@Override
				public IType type(CallInherited node, Visitor visitor) {
					final TypeVariable tyVar = findTypeVariable(node, visitor);
					if (tyVar != null)
						return tyVar.get();
					final Function inherited = node.parentOfType(Function.class).inheritedFunction();
					if (inherited != null) {
						final Visitor inheritedVisitor = new Visitor(visitor.info, inherited.script());
						inheritedVisitor.startRoaming();
						final TypeVariable ty = inheritedVisitor.visitFunction(inherited);
						if (ty != null) {
							judgement(node, ty.get(), TypingJudgementMode.OVERWRITE, visitor);
							return ty.get();
						}
					}
					return PrimitiveType.UNKNOWN;
				}
				@Override
				public void visit(CallInherited node, Visitor visitor) throws ParsingException {
					if (!visitor.roaming || node.parentOfType(Script.class) == visitor.script()) {
						// inherited/_inherited not allowed in non-strict mode
						if (visitor.info.strictLevel <= 0)
							visitor.markers().error(visitor, Problem.InheritedDisabledInStrict0, node, node, Markers.NO_THROW);

						node.setDeclaration(node.parentOfType(Function.class).inheritedFunction());
						if (node.declaration() == null && !node.failsafe())
							visitor.markers().error(visitor, Problem.NoInheritedFunction, node, node, Markers.NO_THROW, node.parentOfType(Function.class).name());
					}
				}
				@Override
				public Declaration typeEnvironmentKey(CallInherited node, Visitor visitor) {
					return node.parentOfType(Function.class).inheritedFunction();
				}
			},

			new Expert<Sequence>(Sequence.class) {
				@Override
				public IType type(Sequence node, Visitor visitor) {
					final ASTNode[] elements = node.subElements();
					return (elements == null || elements.length == 0)
						? PrimitiveType.UNKNOWN
						: ty(elements[elements.length-1], visitor);
				}
				@Override
				public boolean typingJudgement(Sequence node, IType type, ASTNode origin, Visitor visitor, TypingJudgementMode mode) {
					return judgement(node.lastElement(), type, origin, mode, visitor);
				}
				@Override
				public void visit(Sequence node, Visitor visitor) throws ParsingException {
					supr.visit(node, visitor);
					ASTNode p = null;
					for (final ASTNode e : node.subElements()) {
						if (
							(e != null && !e.isValidInSequence(p)) ||
							(p != null && !p.allowsSequenceSuccessor(e))
						)
							visitor.markers().error(visitor, Problem.NotAllowedHere, node, e, Markers.NO_THROW, e);
						p = e;
					}
					if (p != null && !p.isValidAtEndOfSequence())
						visitor.markers().error(visitor, Problem.NotFinished, node, node, Markers.NO_THROW, node.printed());
				}
				@Override
				public boolean isModifiable(Sequence node, Visitor visitor) {
					final ASTNode[] elements = node.subElements();
					if (elements != null && elements.length > 0) {
						final ASTNode last = elements[elements.length-1];
						return expert(last).isModifiable(last, visitor);
					} else
						return false;
				}
			},

			new Expert<ArraySliceExpression>(ArraySliceExpression.class) {
				@Override
				public IType type(ArraySliceExpression node, Visitor visitor) {
					final ArrayType arrayType = as(predecessorType(node, visitor), ArrayType.class);
					return defaulting(arrayType, PrimitiveType.ARRAY);
				}
			},

			new Expert<Nil>(Nil.class) {
				@Override
				public IType type(Nil node, Visitor visitor) {
					return PrimitiveType.UNKNOWN;
				}
				@Override
				public void visit(Nil node, Visitor visitor) throws ParsingException {
					if (!visitor.script().engine().settings().supportsNil)
						visitor.markers().error(visitor, Problem.NotSupported, node, node, Markers.NO_THROW, Keywords.Nil, visitor.script().engine().name());
				}
			},

			new LiteralExpert<StringLiteral>(StringLiteral.class) {
				@Override
				public IType type(StringLiteral node, Visitor visitor) { return PrimitiveType.STRING; }
				@Override
				public void visit(StringLiteral node, Visitor visitor) throws ParsingException {
					// warn about overly long strings
					final long max = visitor.script().index().engine().settings().maxStringLen;
					final String lit = node.literal();
					if (max != 0 && lit.length() > max)
						visitor.markers().warning(visitor, Problem.StringTooLong, node, node, lit.length(), max);

					// stringtbl entries
					// don't warn in #appendto scripts because those will inherit their string tables from the scripts they are appended to
					// and checking for the existence of the table entries there is overkill
					if (visitor.info.hasAppendTo || visitor.script().resource() == null)
						return;
					final String value = lit;
					final int valueLen = value.length();
					// warn when using non-declared string tbl entries
					for (int i = 0; i < valueLen;) {
						if (i+1 < valueLen && value.charAt(i) == '$') {
							final EntityRegion region = StringTbl.entryRegionInString(lit, node.start(), (i+1));
							if (region != null) {
								StringTbl.reportMissingStringTblEntries(visitor, region, node);
								i += region.region().getLength();
								continue;
							}
						}
						++i;
					}
				}
			},

			new LiteralExpert<IntegerLiteral>(IntegerLiteral.class) {
				@Override
				public IType type(IntegerLiteral node, Visitor visitor) {
					if (node.longValue() == 0 && visitor.script().engine().settings().zeroIsAny)
						return PrimitiveType.ANY;
					else
						return PrimitiveType.INT;
				}
			},

			new LiteralExpert<FloatLiteral>(FloatLiteral.class) {
				@Override
				public void visit(FloatLiteral node, Visitor visitor) throws ParsingException {
					if (!visitor.script().engine().settings().supportsFloats)
						visitor.markers().error(visitor, Problem.FloatNumbersNotSupported, node, node, Markers.NO_THROW);
					supr.visit(node, visitor);
				}
			},

			new LiteralExpert<IDLiteral>(IDLiteral.class) {
				@Override
				public IType type(IDLiteral node, Visitor visitor) {
					final Definition obj = visitor.script().nearestDefinitionWithId(node.idValue());
					return obj != null ? obj.metaDefinition() : PrimitiveType.ID;
				}
			},

			new LiteralExpert<BoolLiteral>(BoolLiteral.class) {
				@Override
				public IType type(BoolLiteral node, Visitor visitor) {
					return PrimitiveType.BOOL;
				}
			},

			new Expert<CallExpr>(CallExpr.class) {
				@Override
				public IType type(CallExpr node, Visitor visitor) {
					final ASTNode pred = node.predecessorInSequence();
					final IType type = ty(pred, visitor);
					if (type instanceof FunctionType)
						return ((FunctionType)type).prototype().returnType();
					else
						return PrimitiveType.ANY;
				}
				@Override
				public void visit(CallExpr node, Visitor visitor) throws ParsingException {
					if (!visitor.script().engine().settings().supportsFunctionRefs)
						visitor.markers().error(visitor, Problem.FunctionRefNotAllowed, node, node, Markers.NO_THROW, visitor.script().engine().name());
					else {
						final IType type = expert(node.predecessorInSequence()).type(node.predecessorInSequence(), visitor);
						if (!TypeUnification.compatible(PrimitiveType.FUNCTION, type))
							visitor.markers().error(visitor, Problem.CallingExpression, node, node, Markers.NO_THROW);
					}
				}
			},

			new Expert<Statement>(Statement.class) {
				@Override
				public IType type(Statement node, Visitor visitor) {
					return PrimitiveType.UNKNOWN;
				}
				/**
				 * Emit a warning if this expression is erroneously used at a place where only expressions with side effects are allowed.
				 * @param info The info
				 */
				public void warnIfNoSideEffects(Statement node, Visitor visitor) {
					if (node.parent() instanceof IterateArrayStatement && ((IterateArrayStatement)node.parent()).elementExpr() == node)
						return;
					if (!node.hasSideEffects())
						visitor.markers().warning(visitor, Problem.NoSideEffects, node, node, 0);
				}
				@Override
				public void visit(Statement node, Visitor visitor) throws ParsingException {
					supr.visit(node, visitor);
					warnIfNoSideEffects(node, visitor);
					if (visitor.controlFlow != ControlFlow.Continue)
						visitor.markers().warning(visitor, Problem.NeverReached, node, node, 0);
				}
			},

			new Expert<VarDeclarationStatement>(VarDeclarationStatement.class) {
				@Override
				public void visit(VarDeclarationStatement node, Visitor visitor) throws ParsingException {
					supr.visit(node, visitor);
					for (final VarInitialization initialization : node.variableInitializations())
						if (initialization.variable != null)
							if (initialization.expression != null) {
								final IType initializationType = ty(initialization.expression, visitor);
								if (
									initialization.variable.staticallyTyped() &&
									!TypeUnification.compatible(
										initialization.variable.type(),
										initializationType
									)
								)
									visitor.incompatibleTypesMarker(
										node,
										initialization.expression,
										initialization.variable.type(), initializationType
									);
								else {
									final AccessVar av = AccessVar.temp(initialization.variable, initialization);
									judgement(av, initializationType, TypingJudgementMode.OVERWRITE, visitor);
								}
							}
				}
			},

			new Expert<PropListExpression>(PropListExpression.class) {
				@Override
				public IType type(PropListExpression node, Visitor visitor) {
					return node.definedDeclaration();
				}
				@Override
				public void visit(PropListExpression node, Visitor visitor) throws ParsingException {
					supr.visit(node, visitor);
					if (!visitor.script().engine().settings().supportsProplists)
						visitor.markers().error(visitor, Problem.NotSupported, node, node, Markers.NO_THROW,
							net.arctics.clonk.parser.c4script.ast.Messages.PropListExpression_ProplistsFeature,
							visitor.script().engine().name());
					for (final Variable v : node.components())
						if (v.initializationExpression() != null)
							judgement(AccessVar.temp(v, node), ty(v.initializationExpression(), visitor), TypingJudgementMode.UNIFY, visitor);
				}
				@Override
				public boolean isModifiable(PropListExpression node, Visitor visitor) { return false; }
			},

			new Expert<Parenthesized>(Parenthesized.class) {
				@Override
				public IType type(Parenthesized node, Visitor visitor) {
					return ty(node.innerExpression(), visitor);
				}
				@Override
				public boolean isModifiable(Parenthesized node, Visitor visitor) {
					return expert(node.innerExpression()).isModifiable(node.innerExpression(), visitor);
				}
			},

			new Expert<MemberOperator>(MemberOperator.class) {
				final IType OBJECTISH = TypeChoice.make(Arrays.asList(PrimitiveType.OBJECT, PrimitiveType.ID, PrimitiveType.PROPLIST));
				@Override
				public IType type(MemberOperator node, Visitor visitor) {
					if (node.id() != null)
						return visitor.script().nearestDefinitionWithId(node.id());
					// stuff before -> decides
					final ASTNode pred = node.predecessorInSequence();
					return pred != null ? ty(pred, visitor) : supr.type(node, visitor);
				}
				@Override
				public boolean typingJudgement(MemberOperator node, IType type, ASTNode origin, Visitor visitor, TypingJudgementMode mode) {
					final ASTNode p = node.predecessorInSequence();
					return p != null ? expert(p).typingJudgement(p, type, origin, visitor, mode) : false;
				}
				@Override
				public void visit(MemberOperator node, Visitor visitor) throws ParsingException {
					supr.visit(node, visitor);
					final ASTNode pred = node.predecessorInSequence();
					final EngineSettings settings = visitor.script().engine().settings();
					if (pred != null) {
						final IType requiredType = node.dotNotation() ? PrimitiveType.PROPLIST : OBJECTISH;
						final Expert<? super ASTNode> stmReporter = expert(pred);
						if (!TypeUnification.compatible(requiredType, ty(pred, visitor)))
							visitor.markers().warning(visitor, node.dotNotation() ? Problem.NotAProplist : Problem.CallingMethodOnNonObject, node, node, 0,
								ty(pred, stmReporter, visitor).typeName(false));
						else
							judgement(pred, OBJECTISH, TypingJudgementMode.UNIFY, visitor);
					}
					if (node.getLength() > 3 && !settings.spaceAllowedBetweenArrowAndTilde)
						visitor.markers().error(visitor, Problem.MemberOperatorWithTildeNoSpace, node, node, Markers.NO_THROW);
					if (node.dotNotation() && !settings.supportsProplists)
						visitor.markers().error(visitor, Problem.DotNotationNotSupported, node, node, Markers.NO_THROW, node);
				}
			},

			new Expert<IterateArrayStatement>(IterateArrayStatement.class) {
				@Override
				public boolean skipReportingProblemsForSubElements() { return true; }
				@Override
				public void visit(IterateArrayStatement node, Visitor visitor) throws ParsingException {
					final ControlFlow t = visitor.controlFlow;
					visitor.controlFlow = ControlFlow.Continue;
					Variable loopVariable;
					AccessVar accessVar;
					final ASTNode elementExpr = node.elementExpr();
					final ASTNode arrayExpr = node.arrayExpr();
					if (elementExpr instanceof VarDeclarationStatement)
						loopVariable = ((VarDeclarationStatement)elementExpr).variableInitializations()[0].variable;
					else if ((accessVar = as(SimpleStatement.unwrap(elementExpr), AccessVar.class)) != null) {
						final Declaration d = visitor.obtainDeclaration(accessVar);
						if (d == null) {
							// implicitly create loop variable declaration if not found
							final SourceLocation varPos = visitor.absoluteSourceLocationFromExpr(accessVar);
							loopVariable = visitor.info.script.createVarInScope(Variable.DEFAULT_VARIABLE_FACTORY, node.parentOfType(Function.class), accessVar.name(), Scope.VAR, varPos.start(), varPos.end(), null);
						} else
							loopVariable = as(d, Variable.class);
					} else
						loopVariable = null;

					visitor.visitNode(elementExpr, true);
					visitor.visitNode(arrayExpr, true);

					final IType type = ty(arrayExpr, visitor);
					if (!TypeUnification.compatible(type, PrimitiveType.ARRAY))
						visitor.incompatibleTypesMarker(node, arrayExpr, type, PrimitiveType.ARRAY);
					final IType elmType = ArrayType.elementTypeSet(type);
					final TypeEnvironment env = visitor.newTypeEnvironment();
					{
						if (loopVariable != null) {
							loopVariable.setUsed(true);
							judgement(AccessVar.temp(loopVariable, node), elmType, TypingJudgementMode.UNIFY, visitor);
						}
						visitor.visitNode(node.body(), true);
					}
					visitor.endTypeEnvironment(env, true, false);
					visitor.controlFlow = t;
				}
			},

			new Expert<SimpleStatement>(SimpleStatement.class) {
				@Override
				public void visit(SimpleStatement node, Visitor visitor) throws ParsingException {
					final BinaryOp op = as(node.expression(), BinaryOp.class);
					if (op != null && !op.operator().modifiesArgument())
						visitor.markers().warning(visitor, Problem.NoAssignment, node, op, 0);
					supr.visit(node, visitor);
				}
			},

			new ConditionalStatementExpert<IfStatement>(IfStatement.class) {
				@Override
				public void visit(IfStatement node, Visitor visitor) throws ParsingException {
					final ControlFlow old = visitor.controlFlow;
					final ASTNode condition = node.condition();
					visitor.visitNode(condition, true);
					// use two separate type environments for if and else statement, merging
					// gathered information afterwards
					final TypeEnvironment ifEnvironment = visitor.newTypeEnvironment();
					visitor.visitNode(node.body(), true);
					visitor.endTypeEnvironment(ifEnvironment, false, false);
					visitor.controlFlow = old;
					if (node.elseExpression() != null) {
						final TypeEnvironment elseEnvironment = visitor.newTypeEnvironment();
						visitor.visitNode(node.elseExpression(), true);
						visitor.endTypeEnvironment(elseEnvironment, false, false);
						ifEnvironment.inject(elseEnvironment, false);
					}
					if (ifEnvironment.up != null)
						ifEnvironment.up.inject(ifEnvironment, false);
					visitor.controlFlow = old;

					if (!condition.containsConst()) {
						final Object condEv = PrimitiveType.BOOL.convert(condition.evaluateStatic(node.parentOfType(Function.class)));
						if (condEv != null && condEv != ASTNode.EVALUATION_COMPLEX)
							visitor.markers().warning(visitor,
								condEv.equals(true) ? Problem.ConditionAlwaysTrue : Problem.ConditionAlwaysFalse,
								condition, condition, 0, condition);
					}
				};
			},

			new ConditionalStatementExpert<ForStatement>(ForStatement.class) {
				@Override
				public void visit(ForStatement node, Visitor visitor) throws ParsingException {
					if (node.initializer() != null)
						visitor.visitNode(node.initializer(), true);
					super.visit(node, visitor);
					if (node.increment() != null)
						visitor.visitNode(node.increment(), true);
				}
			},

			new ConditionalStatementExpert<WhileStatement>(WhileStatement.class),

			new Expert<NewProplist>(NewProplist.class) {
				@Override
				public void visit(NewProplist node, Visitor visitor) throws ParsingException {
					node.definedDeclaration().setPrototype(as(ty(node.prototype(), visitor), ProplistDeclaration.class));
				}
			},

			new Expert<Placeholder>(Placeholder.class) {
				@Override
				public void visit(Placeholder node, Visitor visitor) throws ParsingException {
					StringTbl.reportMissingStringTblEntries(visitor, new EntityRegion(null, node, node.entryName()), node);
				}
			},

			new Expert<MissingStatement>(MissingStatement.class) {
				@Override
				public void visit(MissingStatement node, Visitor visitor) throws ParsingException {
					visitor.markers().error(visitor, Problem.MissingStatement, node, node, Markers.NO_THROW);
				}
			},

			new Expert<GarbageStatement>(GarbageStatement.class) {
				@Override
				public void visit(GarbageStatement node, Visitor visitor) throws ParsingException {
					visitor.markers().error(visitor, Problem.Garbage, node, node, Markers.NO_THROW, node.garbage());
				}
			},

			new Expert<FunctionDescription>(FunctionDescription.class) {
				@Override
				public void visit(FunctionDescription node, Visitor visitor) throws ParsingException {
					if (visitor.info.hasAppendTo)
						return;
					int off = 1;
					for (final String part : node.contents().split("\\|")) { //$NON-NLS-1$
						if (part.startsWith("$") && part.endsWith("$")) { //$NON-NLS-1$ //$NON-NLS-2$
							final StringTbl stringTbl = visitor.script().localStringTblMatchingLanguagePref();
							final String entryName = part.substring(1, part.length()-1);
							if (stringTbl == null || stringTbl.map().get(entryName) == null)
								visitor.markers().warning(visitor, Problem.UndeclaredIdentifier, node,
									new Region(node.start()+off, part.length()), 0, entryName);
						}
						off += part.length()+1;
					}
				}
			},

			new Expert<Comment>(Comment.class) {
				@Override
				public void visit(Comment node, Visitor visitor) throws ParsingException {
					if (!visitor.roaming || node.parentOfType(Script.class) == visitor.script()) {
						final String s = node.text();
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
								visitor.markers().todo(visitor.file(), node, s.substring(todoIndex, lineEnd), node.start()+2+todoIndex, node.start()+2+lineEnd, markerPriority);
							}
						} while (markerPriority > IMarker.PRIORITY_LOW);
					}
				}
			},

			new Expert<Unfinished>(Unfinished.class) {
				@Override
				public IType type(Unfinished node, Visitor visitor) {
					return ty(node.expression(), visitor);
				}
				@Override
				public void visit(Unfinished node, Visitor visitor) throws ParsingException {
					visitor.markers().error(visitor, Problem.NotFinished, node, node, Markers.NO_THROW, node);
				}
			}

		};
		for (final Expert<?> expert : classes)
			committee.put(expert.cls(), expert);
		for (final Expert<?> expert : classes)
			expert.findSuper();
	}

}
