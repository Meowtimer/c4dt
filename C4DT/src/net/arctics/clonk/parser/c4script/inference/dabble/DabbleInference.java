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
	public void run() { work(); }

	@Profiled
	final void work() {
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
						v.setMarkers(shared.builder.markers());
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
			ScriptProcessor p = ((Visitor) chain).processor.shared.processors.get(script);
			if (p != null)
				return new Visitor(p);
		}
		Shared shared = chain instanceof Visitor ? ((Visitor)chain).processor.shared : new Shared();
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
		public boolean binds(ASTNode expr, Visitor visitor) {
			return expr instanceof ReturnStatement && expr.parentOfType(Function.class) == function;
		}
		@Override
		public boolean same(ITypeVariable other) {
			return other instanceof CurrentFunctionReturnTypeVariable && ((CurrentFunctionReturnTypeVariable)other).function == function;
		}
		@Override
		public void apply(boolean soft, Visitor visitor) { /* done by Dabble */ }
	}

	public static class InheritedFunctionReturnTypeVariable extends FunctionReturnTypeVariable {
		public InheritedFunctionReturnTypeVariable(Function function) { super(function); }
		@Override
		public boolean binds(ASTNode expr, Visitor visitor) {
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

		final ScriptProcessor processor;

		private ControlFlow controlFlow;
		private Markers markers;
		private List<Script> conglomerate;
		private TypeEnvironment typeEnvironment;

		public Visitor(ScriptProcessor data) {
			this.markers = DabbleInference.this.markers();
			this.processor = data;
		}

		public final SpecialFuncRule specialRuleFor(CallDeclaration node, int role) {
			Engine engine = script().engine();
			if (engine != null && engine.specialRules() != null)
				return engine.specialRules().funcRuleFor(node.name(), role);
			else
				return null;
		}

		private boolean assignDefaultParmTypesToFunction(Function function) {
			if (processor.rules != null)
				for (SpecialFuncRule funcRule : processor.rules.defaultParmTypeAssignerRules())
					if (funcRule.assignDefaultParmTypes(this, function))
						return true;
			return false;
		}

		@Override
		public boolean triggersRevisit(Function function, Function called) { return called.typeFromCallsHint(); }

		private void initialParameterTypesFromCalls(Function function, Function baseFunction, IType[] callTypes) {
			//System.out.println(String.format("Typing '%s' from calls", function.qualifiedName()));
			List<CallDeclaration> calls = processor.index.callsTo(function.name());
			if (calls != null)
				for (CallDeclaration call : calls) {
					Function f = call.parentOfType(Function.class);
					Script other = f.parentOfType(Script.class);
					ScriptProcessor processor = this.processor.shared.processors.get(other);
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
												visitor.incompatibleTypesMarker(concretePar, concretePar, callTypes[pa], concreteTy);
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
			if (roaming || (conglomerate != null && conglomerate.contains(funScript)) || script() == funScript) {
				CurrentFunctionReturnTypeVariable returnType;
				boolean kickOff = false;
				synchronized (processor.finishedFunctions) {
					returnType = processor.finishedFunctions.get(function);
					if (returnType == null) {
						processor.finishedFunctions.put(function, returnType = new CurrentFunctionReturnTypeVariable(function));
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
								ITypeVariable ti = requestTypeVariable(new AccessVar(l));
								if (ti != null)
									ti.set(PrimitiveType.UNKNOWN);
							}
						List<Variable> parameters = function.parameters();
						Function baseFunction = function.baseFunction();
						boolean typeFromCalls =
							ownedFunction && !assignDefaultParmTypesToFunction(function) &&
							processor.typing == Typing.ParametersOptionallyTyped &&
							baseFunction.visibility() != FunctionScope.GLOBAL &&
							processor.script instanceof Definition &&
							!(processor.script instanceof Scenario) &&
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
							ITypeVariable varTypeInfo = requestTypeVariable(new AccessVar(p));
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
		public void incompatibleTypesMarker(ASTNode node, IRegion region, IType left, IType right) {
			try {
				if (left == null)
					left = PrimitiveType.ANY;
				if (right == null)
					right = PrimitiveType.ANY;
				this.markers().marker(this, Problem.IncompatibleTypes, node, region.getOffset(), region.getOffset()+region.getLength(), Markers.NO_THROW,
					processor.typing == Typing.Static ? IMarker.SEVERITY_ERROR : IMarker.SEVERITY_WARNING,
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
			expert.visitor(expression, this);
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
				else synchronized (processor.typeEnvironment) {
					processor.typeEnvironment.inject(env, ignoreLocals);
				}
			typeEnvironment = env.up;
		}

		/**
		 * Requests type information for an expression
		 * @param expression the expression
		 * @return the type information or null if none has been stored
		 */
		public ITypeVariable requestTypeVariable(ASTNode expression) {
			TypeEnvironment env = typeEnvironment;
			if (env == null || processor.typing == Typing.Static || processor.typing == Typing.Dynamic)
				return null;
			boolean topMostLayer = true;
			ITypeVariable base = null;
			for (TypeEnvironment list = env; list != null; list = list.up) {
				ITypeVariable tyvar = list.find(expression, this);
				if (tyvar != null)
					if (!topMostLayer) {
						base = tyvar;
						break;
					}
					else
						return tyvar;
				topMostLayer = false;
			}
			ITypeVariable newtyvar = expert(expression).createTypeVariable(expression, this);
			if (newtyvar != null) {
				if (base != null)
					newtyvar.merge(base);
				env.add(newtyvar);
			}
			return newtyvar;
		}

		/**
		 * Query the type variable of an arbitrary expression. With some luck the inference engine will be able to give an answer.
		 * @param node the expression to query the type of
		 * @return The {@link ITypeVariable} or null if nothing was found
		 */
		public final ITypeVariable findTypeVariable(ASTNode node) {
			for (TypeEnvironment e = typeEnvironment; e != null; e = e.up) {
				ITypeVariable info = e.find(node, this);
				if (info != null)
					return info;
			}
			return null;
		}

		public final ITypeVariable findTypeVariable(AccessVar node) {
			ITypeVariable r = findTypeVariable((ASTNode)node);
			if (r != null)
				return r;
			Declaration d = node.declaration();
			if (
				d != null && d.parent() == script() &&
				!(node.parent() instanceof BinaryOp && ((BinaryOp)node.parent()).operator().isAssignment())
			) {
				List<BinaryOp> assignments = processor.script.varAssignments().get(node.name());
				if (assignments != null)
					for (BinaryOp a : assignments)
						visitFunction(a.parentOfType(Function.class));
			}
			return findTypeVariable((ASTNode)node);
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
		public void reportProblems() {
			synchronized (processor) {
				if (processor.finished)
					return;
				else
					processor.finished = true;
			}
			work();
		}

		private void work() {
			// revisit all inherited scripts since that is the only way to
			// accurately type inherited functions with respect to added things from this script
			TypeEnvironment env1 = newTypeEnvironment();
			{
				conglomerate = processor.script.conglomerate();
				for (Script include : conglomerate)
					visit(include, true);
				conglomerate = Arrays.asList(script());
				storeTypings(env1);
			}
			endTypeEnvironment(env1, false, false);

			TypeEnvironment env2 = newTypeEnvironment();
			{
				visit(script(), false);
				storeTypings(env2);
				script().setTypings(processor.variableTypes, processor.functionReturnTypes);
				env2.apply(this, false);
			}
			endTypeEnvironment(env2, true, false);

			processor.typeEnvironment.apply(this, false);
		}

		private void storeTypings(TypeEnvironment typeEnvironment) {
			for (ITypeVariable tyvar : typeEnvironment) {
				VariableTypeVariable vti = as(tyvar, VariableTypeVariable.class);
				if (vti != null && vti.variable().scope() == Scope.LOCAL)
					synchronized (processor.variableTypes) {
						processor.variableTypes.put(vti.variable(), vti.get());
					}
				FunctionReturnTypeVariable ftri = as(tyvar, FunctionReturnTypeVariable.class);
				if (ftri != null && ftri.function().visibility() != FunctionScope.GLOBAL && processor.script.seesFunction(ftri.function()))
					synchronized (processor.functionReturnTypes) {
						processor.functionReturnTypes.put(ftri.function().name(), ftri.get());
					}
				Declaration d = tyvar.declaration(this);
				if (d != null && d.containedIn(script()))
					tyvar.apply(false, this);
			}
		}

		@Override
		public void run() {
			if (processor.finished)
				return;
			if (processor.shared.monitor.isCanceled())
				return;
			processor.shared.monitor.subTask(String.format("Reporting problems for '%s'", script().name()));
			reportProblems();
			processor.shared.monitor.worked(1);
		}

		@Override
		public Definition definition() { return as(processor.script, Definition.class); }
		@Override
		public SourceLocation absoluteSourceLocationFromExpr(ASTNode expression) {
			Function f = expression.parentOfType(Function.class);
			int bodyOffset = f != null ? f.bodyLocation().start() : 0;
			return new SourceLocation(
				processor.fragmentOffset+bodyOffset+expression.start(),
				processor.fragmentOffset+bodyOffset+expression.end()
			);
		}
		@Override
		public CachedEngineDeclarations cachedEngineDeclarations() { return processor.cachedEngineDeclarations; }
		@Override
		public Script script() { return processor.script; }
		@Override
		public IFile file() { return script().scriptFile(); }
		@Override
		public Declaration container() { return script(); }
		@Override
		public int fragmentOffset() { return processor.fragmentOffset; }
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

		public boolean validForType(ASTNode node, IType type) {
			return expert(node).unifyDeclaredAndGiven(node, type, this) != null;
		}

		@Override
		public void assignment(ASTNode leftSide, ASTNode rightSide) {
			expert(leftSide).assignment(leftSide, rightSide, this);
		}

		@Override
		public void judgement(ASTNode node, IType type, TypingJudgementMode mode) {
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
		 * Returning true tells the {@link Visitor} to not recursively call {@link #visitor(ASTNode, Visitor)} on {@link ASTNode#subElements()}
		 * @return Do you just show up, play the music,
		 */
		public boolean skipReportingProblemsForSubElements() {return false;}
		public void visitor(T node, Visitor visitor) throws ParsingException {}

		public IType type(T node, Visitor visitor) {
			ITypeVariable tyvar = visitor.findTypeVariable(node);
			return tyvar != null ? tyvar.get() : PrimitiveType.UNKNOWN;
		}

		public final IType predecessorType(ASTNode node, Visitor visitor) {
			ASTNode p = node.predecessorInSequence();
			return p != null ? ty(p, visitor) : null;
		}

		/**
		 * Return whether this expression is valid as a value of the specified type.
		 * @param type The type to test against
		 * @param context Script parser context
		 * @return True if valid, false if not.
		 */
		public final IType unifyDeclaredAndGiven(ASTNode node, IType type, Visitor visitor) {
			IType myType = ty(node, visitor);
			if (type == null)
				return myType;
			return TypeUnification.unifyNoChoice(type, myType);
		}

		public boolean typingJudgement(T node, IType type, Visitor visitor, TypingJudgementMode mode) {
			ITypeVariable tyvar;
			switch (mode) {
			case Expect:
				tyvar = visitor.requestTypeVariable(node);
				if (tyvar != null)
					if (tyvar.get() == PrimitiveType.UNKNOWN || tyvar.get() == PrimitiveType.ANY) {
						tyvar.set(type);
						return true;
					} else
						return false;
				return true;
			case Force:
				tyvar = visitor.requestTypeVariable(node);
				if (tyvar != null) {
					tyvar.set(type);
					return true;
				} else
					return false;
			case Hint:
				tyvar = visitor.findTypeVariable(node);
				return tyvar == null || tyvar.hint(type);
			case Unify:
				tyvar = visitor.requestTypeVariable(node);
				if (tyvar != null) {
					tyvar.set(TypeUnification.unify(tyvar.get(), type));
					return true;
				} else
					return false;
			default:
				return false;
			}
		}

		public void assignment(T leftSide, ASTNode rightSide, Visitor visitor) {
			switch (visitor.processor.typing) {
			case Static:
				IType leftTy = ty(leftSide, this, visitor);
				IType rightTy = ty(rightSide, visitor);
				if (!leftTy.canBeAssignedFrom(rightTy))
					visitor.incompatibleTypesMarker(rightSide, rightSide, leftTy, rightTy);
				break;
			case ParametersOptionallyTyped:
				judgement(leftSide, ty(rightSide, visitor), TypingJudgementMode.Force, visitor);
				break;
			case Dynamic:
				break;
			}
		}

		public ITypeVariable createTypeVariable(T node, Visitor visitor) {
			ITypeable d = ExpressionTypeVariable.typeableFromExpression(node, visitor);
			if (d != null && !d.staticallyTyped())
				return new ExpressionTypeVariable(node, visitor);
			return null;
		}

		@Override
		public String toString() { return String.format("Expert<%s>", cls.getSimpleName()); }

		public boolean isModifiable(T node, Visitor visitor) { return true; }
	}

	class AccessDeclarationExpert<T extends AccessDeclaration> extends Expert<T> {
		public AccessDeclarationExpert(Class<T> cls) { super(cls); }
		protected Declaration obtainDeclaration(T node, Visitor visitor) { return null; }
		@Override
		public void visitor(T node, Visitor visitor) throws ParsingException {
			super.visitor(node, visitor);
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
		public ITypeVariable createTypeVariable(T node, Visitor visitor) {
			if (node.declaration() instanceof ITypeable && ((ITypeable)node.declaration()).staticallyTyped())
				return null;
			else
				return super.createTypeVariable(node, visitor);
		}
	}

	class ConditionalStatementExpert<T extends ConditionalStatement> extends Expert<T> {
		public ConditionalStatementExpert(Class<T> cls) { super(cls); }
		@Override
		public boolean skipReportingProblemsForSubElements() {return true;}
		@Override
		public void visitor(ConditionalStatement node, Visitor visitor) throws ParsingException {
			ControlFlow t = visitor.controlFlow;
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
			ASTNode condition = node.condition();
			if (node.body() == null || condition == null || !(node instanceof ILoop))
				return;
			Object condEv = PrimitiveType.BOOL.convert(condition == null ? true : condition.evaluateStatic(node.parentOfType(Function.class)));
			if (Boolean.FALSE.equals(condEv))
				visitor.markers().warning(visitor, Problem.ConditionAlwaysFalse, condition, condition, Markers.NO_THROW, condition);
			else if (Boolean.TRUE.equals(condEv)) {
				EnumSet<ControlFlow> flows = node.body().possibleControlFlows();
				if (!(flows.contains(ControlFlow.BreakLoop) || flows.contains(ControlFlow.Return)))
					visitor.markers().warning(visitor, Problem.InfiniteLoop, node, node, Markers.NO_THROW);
			}
		}
	}

	private final Expert<ASTNode> MASTER_OF_NONE = new Expert<ASTNode>(ASTNode.class) {
		@Override
		public IType type(ASTNode node, Visitor visitor) {
			return PrimitiveType.UNKNOWN;
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

	public final IType ty(ASTNode node, Visitor visitor) {
		return node != null ? ty(node, expert(node), visitor) : null;
	}

	public final <T extends ASTNode> IType ty(T node, Expert<T> expert, Visitor visitor) {
		IType type = expert.type(node, visitor);
		node.inferredType(type);
		return type;
	}

	public final void judgement(ASTNode node, IType type, TypingJudgementMode mode, Visitor visitor) {
		expert(node).typingJudgement(node, type, visitor, mode);
	}

	private final Map<Class<? extends ASTNode>, Expert<? extends ASTNode>> committee = new HashMap<Class<? extends ASTNode>, Expert<?>>();
	{
		@SuppressWarnings("rawtypes")
		Expert<?>[] classes = new Expert[] {

			new AccessDeclarationExpert<AccessDeclaration>(AccessDeclaration.class),

			new AccessDeclarationExpert<AccessVar>(AccessVar.class) {
				private Declaration findUsingType(Visitor visitor, AccessVar node, ASTNode predecessor, IType type) {
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
							FindDeclarationInfo info = new FindDeclarationInfo(visitor.script().index());
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
					ASTNode p = node.predecessorInSequence();
					if (p == null && node.name().equals(Variable.THIS.name()))
						return Variable.THIS;
					IType type = visitor.script();
					if (p != null)
						type = ty(p, visitor);
					if (p == null) {
						Function f = node.parentOfType(Function.class);
						if (f != null) {
							Variable v = f.findVariable(node.name());
							if (v != null)
								return v;
						}
						Declaration v = visitor.processor.variableMap.get(node.name());
						if (v == null && !visitor.processor.variableMap.containsKey(node.name())) {
							v = findUsingType(visitor, node, null, type);
							visitor.processor.variableMap.put(node.name(), v);
						}
						return v;
					}
					else
						return findUsingType(visitor, node, p, type);
				}
				@Override
				public IType type(AccessVar node, Visitor visitor) {
					Declaration d = internalObtainDeclaration(node, visitor);
					// declarationFromContext(context) ensures that declaration is not null (if there is actually a variable) which is needed for queryTypeOfExpression for example
					if (d == Variable.THIS)
						return visitor.processor.thisType;
					ITypeVariable stored = visitor.findTypeVariable(node);
					if (stored != null)
						return stored.get();
					if (d instanceof Function)
						return new FunctionType((Function)d);
					else if (d instanceof Variable) {
						Variable v = (Variable)d;
						Map<Variable, IType> typesMap= null;
						if (v.scope() == Scope.LOCAL) {
							if (node.predecessorInSequence() == null)
								typesMap = visitor.processor.variableTypes;
							else {
								IType targetType = ty(node.predecessorInSequence(), visitor);
								if (targetType instanceof Script) {
									ScriptProcessor other = visitor.processor.shared.processors.get(targetType);
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
				public boolean typingJudgement(AccessVar node, IType type, Visitor visitor, TypingJudgementMode mode) {
					if (node.declaration() == Variable.THIS)
						return true;
					return super.typingJudgement(node, type, visitor, mode);
				}
				@Override
				public void visitor(AccessVar node, Visitor visitor) throws ParsingException {
					super.visitor(node, visitor);
					ASTNode pred = node.predecessorInSequence();
					Declaration declaration = node.declaration();
					if (declaration == null && pred == null)
						visitor.markers().error(visitor, Problem.UndeclaredIdentifier, node, node, Markers.NO_THROW, node.name());
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
									visitor.markers().error(visitor, Problem.LocalUsedInGlobal, node, node, Markers.NO_THROW);
							}
							break;
						case STATIC: case CONST:
							visitor.script().addUsedScript(var.script());
							break;
						case VAR:
							Function currentFunction = node.parentOfType(Function.class);
							if (currentFunction != null && var.parentDeclaration() == currentFunction) {
								int locationUsed = currentFunction.bodyLocation().getOffset()+node.start();
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
				public void initializeFromAssignment(Variable var, ASTNode referee, ASTNode expression, Visitor visitor) {
					IType type = ty(expression, visitor);
					var.expectedToBeOfType(type, TypingJudgementMode.Expect);
					var.setLocation(visitor.absoluteSourceLocationFromExpr(referee));
					var.forceType(type);
					var.setInitializationExpression(expression);
				}
				@Override
				public void assignment(AccessVar leftSide, ASTNode rightSide, Visitor visitor) {
					Declaration declaration = leftSide.declaration();
					if (declaration == Variable.THIS)
						return;
					if (declaration == null) {
						IType predType = predecessorType(leftSide, visitor);
						if (predType != null && predType.canBeAssignedFrom(PrimitiveType.PROPLIST))
							if (predType instanceof IProplistDeclaration) {
								IProplistDeclaration proplDecl = (IProplistDeclaration) predType;
								if (proplDecl.isAdHoc()) {
									Variable var = proplDecl.addComponent(
										new Variable(leftSide.name(), Variable.Scope.VAR),
										true
									);
									declaration = var;
									initializeFromAssignment(var, leftSide, rightSide, visitor);
								}
							} else for (IType t : predType)
								if (t == visitor.script()) {
									Variable var = new Variable(leftSide.name(), Variable.Scope.LOCAL);
									initializeFromAssignment(var, leftSide, rightSide, visitor);
									visitor.script().addDeclaration(var);
									declaration = var;
									break;
								}
					}
					super.assignment(leftSide, rightSide, visitor);
				}
				@Override
				public ITypeVariable createTypeVariable(AccessVar node, Visitor visitor) {
					if (node.declaration() instanceof Variable && node.predecessorInSequence() == null)
						return new VariableTypeVariable(node);
					else
						return super.createTypeVariable(node, visitor);
				}
				@Override
				public boolean isModifiable(AccessVar node, Visitor visitor) {
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
				public void assignment(VarInitializationAccess leftSide, ASTNode rightSide, Visitor visitor) {
					supr.assignment(leftSide, rightSide, visitor);
					if (leftSide.declaration() instanceof Variable && ((Variable)leftSide.declaration()).scope() == Scope.CONST && !rightSide.isConstant())
						try {
							visitor.markers().error(visitor, Problem.NonConstGlobalVarAssignment, rightSide, rightSide, Markers.NO_THROW);
						} catch (ParsingException e) { }
				}
				@Override
				public boolean isModifiable(VarInitializationAccess node, Visitor visitor) { return true; /* sudo */ }
			},

			new Expert<ArrayExpression>(ArrayExpression.class) {
				@Override
				public IType type(ArrayExpression node, final Visitor visitor) {
					return new ArrayType(
						null,
						ArrayUtil.map(node.subElements(), IType.class, new IConverter<ASTNode, IType>() {
							@Override
							public IType convert(ASTNode from) {
								return from != null ? ty(from, visitor) : PrimitiveType.UNKNOWN;
							}
						})
					);
				}
				@Override
				public boolean isModifiable(ArrayExpression node, Visitor visitor) { return false; }
			},

			new Expert<ArrayElementExpression>(ArrayElementExpression.class) {
				@Override
				public IType type(ArrayElementExpression node, Visitor visitor) {
					IType t = supr.type(node, visitor);
					if (t != PrimitiveType.UNKNOWN && t != PrimitiveType.ANY)
						return t;
					ASTNode pred = node.predecessorInSequence();
					if (pred != null) {
						IType predTy = ty(pred, visitor);
						for (IType ty : predTy) {
							ArrayType at = as(ty, ArrayType.class);
							if (at != null)
								return at.typeForElementWithIndex(ASTNode.evaluateStatic(node.argument(), visitor));
						}
					}
					return PrimitiveType.ANY;
				}
				@Override
				public void assignment(ArrayElementExpression leftSide, ASTNode rightSide, Visitor visitor) {
					IType predType_ = predecessorType(leftSide, visitor);
					for (IType predType : predType_) {
						ArrayType arrayType = as(predType, ArrayType.class);
						IType rightSideType = ty(rightSide, visitor);
						ASTNode pred = leftSide.predecessorInSequence();
						if (arrayType != null) {
							Object argEv = ASTNode.evaluateStatic(leftSide.argument(), visitor);
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
							judgement(pred, mutation, TypingJudgementMode.Expect, visitor);
							break;
						} else if (predType == PrimitiveType.UNKNOWN || predType == PrimitiveType.ARRAY)
							judgement(
								pred,
								new ArrayType(rightSideType, ArrayType.NO_PRESUMED_LENGTH),
								TypingJudgementMode.Force,
								visitor
							);
					}
				}
				@Override
				public void visitor(ArrayElementExpression node, Visitor visitor) throws ParsingException {
					supr.visitor(node, visitor);
					IType type = predecessorType(node, visitor);
					if (type == null)
						type = PrimitiveType.UNKNOWN;
					ASTNode arg = node.argument();
					if (arg == null)
						visitor.markers().warning(visitor, Problem.MissingExpression, node, node, 0);
					else if (PrimitiveType.UNKNOWN != type && PrimitiveType.ANY != type) {
						IType argType = ty(arg, visitor);
						ASTNode pred = node.predecessorInSequence();
						if (argType == PrimitiveType.STRING) {
							if (TypeUnification.unifyNoChoice(PrimitiveType.PROPLIST, type) == null)
								visitor.markers().warning(visitor, Problem.NotAProplist, node, pred, 0);
							else
								judgement(pred, PrimitiveType.PROPLIST, TypingJudgementMode.Unify, visitor);
						}
						else if (argType == PrimitiveType.INT)
							if (TypeUnification.unifyNoChoice(PrimitiveType.ARRAY, type) == null)
								visitor.markers().warning(visitor, Problem.NotAnArrayOrProplist, node, pred, 0);
							//else
							//	expert(pred).typingJudgement(pred, PrimitiveType.ARRAY, processor, TypingJudgementMode.Unify);
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
				public void visitor(ArraySliceExpression node, Visitor visitor) throws ParsingException {
					supr.visitor(node, visitor);
					IType type = predecessorType(node, visitor);
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
						IType leftSideType = ty(node.leftSide(), visitor);
						IType rightSideType = ty(node.rightSide(), visitor);
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
				public void visitor(BinaryOp node, Visitor visitor) throws ParsingException {
					final Operator op = node.operator();
					// sanity
					ASTNode left = node.leftSide();
					ASTNode right = node.rightSide();
					node.setLocation(left.start(), right.end());
					// i'm an assignment operator and i can't modify my left side :C
					if (op.modifiesArgument() && !expert(left).isModifiable(left, visitor))
						visitor.markers().error(visitor, Problem.ExpressionNotModifiable, node, left, Markers.NO_THROW);
					// obsolete operators in #strict 2impor
					if ((op == Operator.StringEqual || op == Operator.ne) && (visitor.processor.strictLevel >= 2))
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
					default:
						break;
					}

					if (expectedLeft != null)
						judgement(left, expectedLeft, TypingJudgementMode.Unify, visitor);
					if (expectedRight != null)
						judgement(right, expectedRight, TypingJudgementMode.Unify, visitor);
				}
				@Override
				public ITypeVariable createTypeVariable(BinaryOp node, Visitor visitor) {
					ASTNode leftSide = node.leftSide();
					if (node.operator() == Operator.Assign && leftSide != null)
						return expert(leftSide).createTypeVariable(leftSide, visitor);
					return super.createTypeVariable(node, visitor);
				}
			},

			new Expert<UnaryOp>(UnaryOp.class) {
				@Override
				public void visitor(UnaryOp node, Visitor visitor) throws ParsingException {
					supr.visitor(node, visitor);
					ASTNode arg = node.argument();
					if (node.operator().modifiesArgument() && !expert(arg).isModifiable(arg, visitor))
						visitor.markers().error(visitor, Problem.ExpressionNotModifiable, node, arg, Markers.NO_THROW);
					Expert<? super ASTNode> rarg = expert(arg);
					PrimitiveType firstArgType = node.operator().firstArgType();
					if (rarg.unifyDeclaredAndGiven(arg, firstArgType, visitor) == null)
						visitor.incompatibleTypesMarker(node, arg, firstArgType,
							ty(arg, rarg, visitor));
					if (firstArgType != PrimitiveType.ANY)
						rarg.typingJudgement(arg, firstArgType, visitor, TypingJudgementMode.Expect);
				}
				@Override
				public boolean isModifiable(UnaryOp node, Visitor visitor) {
					return node.placement() == Placement.Prefix && node.operator().returnsRef();
				}
			},

			new Expert<BoolLiteral>(BoolLiteral.class) {
				@Override
				public void visitor(BoolLiteral node, Visitor visitor) throws ParsingException {
					supr.visitor(node, visitor);
					if (node.parent() instanceof BinaryOp) {
						Operator op = ((BinaryOp) node.parent()).operator();
						if (op == Operator.And || op == Operator.Or)
							visitor.markers().warning(visitor, Problem.BoolLiteralAsOpArg, node, node, 0, this.toString());
					}
				}
			},

			new Expert<ContinueStatement>(ContinueStatement.class) {
				@Override
				public void visitor(ContinueStatement node, Visitor visitor) throws ParsingException {
					if (node.parentOfType(ILoop.class) == null)
						visitor.markers().error(visitor, Problem.KeywordInWrongPlace, node, node, Markers.NO_THROW, node.keyword());
					supr.visitor(node, visitor);
				}
			},

			new Expert<BreakStatement>(BreakStatement.class) {
				@Override
				public void visitor(BreakStatement node, Visitor visitor) throws ParsingException {
					if (node.parentOfType(ILoop.class) == null)
						visitor.markers().error(visitor, Problem.KeywordInWrongPlace, node, node, Markers.NO_THROW, node.keyword());
					supr.visitor(node, visitor);
				}
			},

			new Expert<ReturnStatement>(ReturnStatement.class) {
				private void warnAboutTupleInReturnExpr(Visitor visitor, ASTNode node, boolean tupleIsError) throws ParsingException {
					if (node == null)
						return;
					if (node instanceof Tuple)
						if (tupleIsError)
							visitor.markers().error(visitor, Problem.TuplesNotAllowed, node, node, Markers.NO_THROW);
						else if (visitor.processor.strictLevel >= 2)
							visitor.markers().error(visitor, Problem.ReturnAsFunction, node, node, Markers.NO_THROW);
					ASTNode[] subElms = node.subElements();
					for (ASTNode e : subElms)
						warnAboutTupleInReturnExpr(visitor, e, true);
				}
				@Override
				public void visitor(ReturnStatement node, Visitor visitor) throws ParsingException {
					supr.visitor(node, visitor);
					ASTNode returnExpr = node.returnExpr();
					warnAboutTupleInReturnExpr(visitor, returnExpr, false);
					Function currentFunction = node.parentOfType(Function.class);
					if (currentFunction == null)
						visitor.markers().error(visitor, Problem.NotAllowedHere, node, node, Markers.NO_THROW, Keywords.Return);
					else if (returnExpr != null)
						if (visitor.processor.typing == Typing.Static && currentFunction.staticallyTyped()) {
							if (expert(returnExpr).unifyDeclaredAndGiven(returnExpr, currentFunction.returnType(), visitor) == null)
								visitor.incompatibleTypesMarker(node,
									returnExpr, currentFunction.returnType(), ty(returnExpr, visitor));
						}
						else {
							IType type = ty(returnExpr, visitor);
							judgement(node, type, TypingJudgementMode.Unify, visitor);
							//parser.linkTypesOf(dummy, returnExpr);
						}
				}
				@Override
				public ITypeVariable createTypeVariable(ReturnStatement node, Visitor visitor) {
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
					Visitor visitor,
					CallDeclaration node, String functionName,
					IType type
				) {
					IType lookIn = type != null ? type : visitor.script();
					if (lookIn != null) for (IType ty : lookIn) {
						Script script = as(ty, Script.class);
						if (script == null && ty instanceof MetaDefinition)
							script = ((MetaDefinition)ty).definition();
						if (script == null)
							continue;
						FindDeclarationInfo info = new FindDeclarationInfo(visitor.script().index());
						info.searchOrigin = visitor.script();
						info.contextFunction = node.parentOfType(Function.class);
						info.findGlobalVariables = type == null;
						Declaration dec = script.findDeclaration(functionName, info);
						// parse function before this one
						if (dec instanceof Function && node.parentOfType(Function.class) != null)
							visitor.visitFunction((Function)dec, node.parentOfType(Function.class));
						if (dec != null)
							return dec;
					}
					if (type != null) {
						// find global function
						Declaration declaration;
						try {
							declaration = visitor.script().index().findGlobal(Declaration.class, functionName);
						} catch (Exception e) {
							e.printStackTrace();
							return null;
						}
						// find engine function
						if (declaration == null)
							declaration = visitor.script().index().engine().findFunction(functionName);

						List<Declaration> allFromLocalIndex = visitor.script().index().declarationMap().get(functionName);
						Declaration decl = visitor.script().engine().findLocalFunction(functionName, false);
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
					String declarationName = node.name();
					if (declarationName.equals(Keywords.Return))
						return null;
					ASTNode p = node.predecessorInSequence();
					if (p == null) {
						Declaration f = visitor.processor.functionMap.get(node.name());
						if (f == null && !visitor.processor.functionMap.containsKey(node.name())) {
							f = findUsingType(visitor, node, declarationName, visitor.script());
							visitor.processor.functionMap.put(declarationName, f);
						}
						return f;
					}
					else
						return findUsingType(visitor, node, declarationName, ty(p, visitor));
				}
				private IType declarationType(CallDeclaration node, Visitor visitor) {
					Declaration d = internalObtainDeclaration(node, visitor);

					// look for gathered type information
					ITypeVariable tyvar = visitor.findTypeVariable(node);
					if (tyvar != null)
						return tyvar.get();

					// calling this() as function -> return object type belonging to script
					if (node.params().length == 0 && d != null && (d == visitor.cachedEngineDeclarations().This || d == Variable.THIS))
						return visitor.processor.thisType;

					if (d instanceof Function) {
						// Some special rule applies and the return type is set accordingly
						SpecialFuncRule rule = node.specialRuleFromContext(visitor, SpecialEngineRules.RETURNTYPE_MODIFIER);
						if (rule != null) {
							IType type = rule.returnType(visitor, node);
							if (type != null)
								return type;
						}
						Function f = (Function)d;
						Map<String, IType> typesMap = null;
						if (f.visibility() != FunctionScope.GLOBAL)
							if (node.predecessorInSequence() == null)
								typesMap = visitor.processor.functionReturnTypes;
							else {
								IType targetType = ty(node.predecessorInSequence(), visitor);
								if (targetType instanceof Script) {
									ScriptProcessor other = visitor.processor.shared.processors.get(targetType);
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

					return supr != null ? supr.type(node, visitor) : PrimitiveType.UNKNOWN;
				}
				private boolean unknownFunctionShouldBeError(CallDeclaration node, Visitor visitor) {
					ASTNode pred = node.predecessorInSequence();
					// stand-alone function? always bark!
					if (pred == null)
						return true;
					// not typed? weird
					IType predType = ty(pred, visitor);
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
					if (ad != null && (ad.declaration() == Variable.THIS || ad.declaration() == visitor.cachedEngineDeclarations().This))
						return false;
					boolean anyDefinitions = false;
					for (IType t : predType)
						if (t instanceof Definition)
							anyDefinitions = true;
					return anyDefinitions;
				}
				@Override
				public IType type(CallDeclaration node, Visitor visitor) {
					IType type = declarationType(node, visitor);
					if (type instanceof FunctionType)
						return ((FunctionType)type).prototype().returnType();
					else
						return type;
				}
				@Override
				public void visitor(CallDeclaration node, Visitor visitor) throws ParsingException {
					super.visitor(node, visitor);

					CachedEngineDeclarations cachedEngineDeclarations = visitor.cachedEngineDeclarations();
					String declarationName = node.name();
					Declaration declaration = node.declaration();
					ASTNode[] params = node.params();
					ASTNode predecessor = node.predecessorInSequence();

					// return as function
					if (declarationName.equals(Keywords.Return)) {
						if (visitor.processor.strictLevel >= 2)
							visitor.markers().error(visitor, Problem.ReturnAsFunction, node, node, Markers.NO_THROW);
						else
							visitor.markers().warning(visitor, Problem.ReturnAsFunction, node, node, 0);
					} else if (declaration instanceof Variable) {
						// variable as function
						((Variable)declaration).setUsed(true);
						IType type = declarationType(node, visitor);
						// no warning when in #strict mode
						if (visitor.processor.strictLevel >= 2)
							if (declaration != cachedEngineDeclarations.This && declaration != Variable.THIS && !PrimitiveType.FUNCTION.canBeAssignedFrom(type))
								visitor.markers().warning(visitor, Problem.VariableCalled, node, node, 0, declaration.name(), type.typeName(false));
					} else if (declaration instanceof Function) {
						Function f = (Function)declaration;
						if (f.visibility() == FunctionScope.GLOBAL || predecessor != null)
							visitor.script().addUsedScript(f.script());

						SpecialFuncRule rule = visitor.specialRuleFor(node, SpecialEngineRules.ARGUMENT_VALIDATOR);
						boolean specialCaseHandled =
							rule != null &&
							rule.validateArguments(node, params, visitor);

						// not a special case... check regular parameter types
						if (!specialCaseHandled) {
							int givenParam = 0;
							for (Variable parm : f.parameters()) {
								if (givenParam >= params.length)
									break;
								ASTNode given = params[givenParam++];
								if (given == null)
									continue;
								IType unified = unifyDeclaredAndGiven(given, parm.type(), visitor);
								if (unified == null)
									visitor.incompatibleTypesMarker(node, given, parm.type(), ty(given, visitor));
								else
									judgement(given, unified, TypingJudgementMode.Unify, visitor);
							}
						}
					} else if (declaration == null && unknownFunctionShouldBeError(node, visitor)) {
						int start = node.start();
						visitor.markers().error(visitor, Problem.UndeclaredIdentifier, node, start, start+declarationName.length(), Markers.NO_THROW, declarationName);
					}
				}
				@Override
				public ITypeVariable createTypeVariable(CallDeclaration node, Visitor visitor) {
					Declaration d = node.declaration();
					CachedEngineDeclarations cache = visitor.cachedEngineDeclarations();
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
						return new ExpressionTypeVariable(node, visitor);
					return super.createTypeVariable(node, visitor);
				}
				@Override
				public boolean isModifiable(CallDeclaration node, Visitor visitor) {
					Declaration declaration = node.declaration();
					IType t = declaration instanceof Function ? ((Function)declaration).returnType() : PrimitiveType.UNKNOWN;
					return t.canBeAssignedFrom(PrimitiveType.REFERENCE) || t.canBeAssignedFrom(PrimitiveType.UNKNOWN);
				}
			},

			new AccessDeclarationExpert<CallInherited>(CallInherited.class) {
				@Override
				public IType type(CallInherited node, Visitor visitor) {
					ITypeVariable tyVar = visitor.findTypeVariable(node);
					if (tyVar != null)
						return tyVar.get();
					Function inherited = node.parentOfType(Function.class).inheritedFunction();
					if (inherited != null) {
						visitor.startRoaming();
						ITypeVariable ty = visitor.visitFunction(inherited);
						visitor.endRoaming();
						if (ty != null) {
							judgement(node, ty.get(), TypingJudgementMode.Force, visitor);
							return ty.get();
						}
					}
					return PrimitiveType.UNKNOWN;
				}
				@Override
				public void visitor(CallInherited node, Visitor visitor) throws ParsingException {
					if (!visitor.roaming || node.parentOfType(Script.class) == visitor.script()) {
						// inherited/_inherited not allowed in non-strict mode
						if (visitor.processor.strictLevel <= 0)
							visitor.markers().error(visitor, Problem.InheritedDisabledInStrict0, node, node, Markers.NO_THROW);

						node.setDeclaration(node.parentOfType(Function.class).inheritedFunction());
						if (node.declaration() == null && !node.failsafe())
							visitor.markers().error(visitor, Problem.NoInheritedFunction, node, node, Markers.NO_THROW, node.parentOfType(Function.class).name());
					}
				}
				@Override
				public ITypeVariable createTypeVariable(CallInherited node, Visitor visitor) {
					Function inherited = node.parentOfType(Function.class).inheritedFunction();
					return inherited != null ? new InheritedFunctionReturnTypeVariable(inherited) : null;
				}
			},

			new Expert<Sequence>(Sequence.class) {
				@Override
				public IType type(Sequence node, Visitor visitor) {
					ASTNode[] elements = node.subElements();
					return (elements == null || elements.length == 0)
						? PrimitiveType.UNKNOWN
						: ty(elements[elements.length-1], visitor);
				}
				@Override
				public void assignment(Sequence leftSide, ASTNode rightSide, Visitor visitor) {
					ASTNode lastElement = leftSide.lastElement();
					expert(lastElement).assignment(lastElement, rightSide, visitor);
				}
				@Override
				public void visitor(Sequence node, Visitor visitor) throws ParsingException {
					supr.visitor(node, visitor);
					ASTNode p = null;
					for (ASTNode e : node.subElements()) {
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
					ASTNode[] elements = node.subElements();
					if (elements != null && elements.length > 0) {
						ASTNode last = elements[elements.length-1];
						return expert(last).isModifiable(last, visitor);
					} else
						return false;
				}
			},

			new Expert<ArraySliceExpression>(ArraySliceExpression.class) {
				@Override
				public IType type(ArraySliceExpression node, Visitor visitor) {
					ArrayType arrayType = as(predecessorType(node, visitor), ArrayType.class);
					if (arrayType != null)
						return node.lo() == null && node.hi() == null ? arrayType : arrayType.typeForSlice(
							ASTNode.evaluateStatic(node.lo(), visitor),
							ASTNode.evaluateStatic(node.hi(), visitor)
						);
					else
						return PrimitiveType.ARRAY;
				}
				@Override
				public void assignment(ArraySliceExpression leftSide, ASTNode rightSide, Visitor visitor) {
					ArrayType arrayType = as(predecessorType(leftSide, visitor), ArrayType.class);
					IType sliceType = ty(rightSide, visitor);
					if (arrayType != null)
						judgement(leftSide.predecessorInSequence(), arrayType.modifiedBySliceAssignment(
							ASTNode.evaluateStatic(leftSide.lo(), visitor),
							ASTNode.evaluateStatic(leftSide.hi(), visitor),
							sliceType
						), TypingJudgementMode.Expect, visitor);
				}
			},

			new Expert<Literal>(Literal.class) {
				@Override
				public boolean typingJudgement(Literal node, IType type, Visitor visitor, TypingJudgementMode mode) {
					// constantly steadfast do i resist the pressure of expectancy lied upon me
					return true;
				}
				@Override
				public void assignment(Literal leftSide, ASTNode rightSide, Visitor visitor) { /* don't care */ }
				@Override
				public ITypeVariable createTypeVariable(Literal node, Visitor visitor) { return null; /* nope */ }
				@Override
				public boolean isModifiable(Literal node, Visitor visitor) { return false; }
			},

			new Expert<Nil>(Nil.class) {
				@Override
				public IType type(Nil node, Visitor visitor) {
					return PrimitiveType.UNKNOWN;
				}
				@Override
				public void visitor(Nil node, Visitor visitor) throws ParsingException {
					if (!visitor.script().engine().settings().supportsNil)
						visitor.markers().error(visitor, Problem.NotSupported, node, node, Markers.NO_THROW, Keywords.Nil, visitor.script().engine().name());
				}
			},

			new Expert<StringLiteral>(StringLiteral.class) {
				@Override
				public IType type(StringLiteral node, Visitor visitor) { return PrimitiveType.STRING; }
				@Override
				public void visitor(StringLiteral node, Visitor visitor) throws ParsingException {
					// warn about overly long strings
					long max = visitor.script().index().engine().settings().maxStringLen;
					String lit = node.literal();
					if (max != 0 && lit.length() > max)
						visitor.markers().warning(visitor, Problem.StringTooLong, node, node, lit.length(), max);

					// stringtbl entries
					// don't warn in #appendto scripts because those will inherit their string tables from the scripts they are appended to
					// and checking for the existence of the table entries there is overkill
					if (visitor.processor.hasAppendTo || visitor.script().resource() == null)
						return;
					String value = lit;
					int valueLen = value.length();
					// warn when using non-declared string tbl entries
					for (int i = 0; i < valueLen;) {
						if (i+1 < valueLen && value.charAt(i) == '$') {
							EntityRegion region = StringTbl.entryRegionInString(lit, node.start(), (i+1));
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

			new Expert<IntegerLiteral>(IntegerLiteral.class) {
				@Override
				public IType type(IntegerLiteral node, Visitor visitor) {
					if (node.longValue() == 0 && visitor.script().engine().settings().zeroIsAny)
						return PrimitiveType.ANY;
					else
						return PrimitiveType.INT;
				}
			},

			new Expert<FloatLiteral>(FloatLiteral.class) {
				@Override
				public void visitor(FloatLiteral node, Visitor visitor) throws ParsingException {
					if (!visitor.script().engine().settings().supportsFloats)
						visitor.markers().error(visitor, Problem.FloatNumbersNotSupported, node, node, Markers.NO_THROW);
					supr.visitor(node, visitor);
				}
			},

			new Expert<IDLiteral>(IDLiteral.class) {
				@Override
				public IType type(IDLiteral node, Visitor visitor) {
					Definition obj = visitor.script().nearestDefinitionWithId(node.idValue());
					return obj != null ? obj.metaDefinition() : PrimitiveType.ID;
				}
			},

			new Expert<BoolLiteral>(BoolLiteral.class) {
				@Override
				public IType type(BoolLiteral node, Visitor visitor) {
					return PrimitiveType.BOOL;
				}
			},

			new Expert<CallExpr>(CallExpr.class) {
				@Override
				public IType type(CallExpr node, Visitor visitor) {
					ASTNode pred = node.predecessorInSequence();
					IType type = ty(pred, visitor);
					if (type instanceof FunctionType)
						return ((FunctionType)type).prototype().returnType();
					else
						return PrimitiveType.ANY;
				}
				@Override
				public void visitor(CallExpr node, Visitor visitor) throws ParsingException {
					if (!visitor.script().engine().settings().supportsFunctionRefs)
						visitor.markers().error(visitor, Problem.FunctionRefNotAllowed, node, node, Markers.NO_THROW, visitor.script().engine().name());
					else {
						IType type = expert(node.predecessorInSequence()).type(node.predecessorInSequence(), visitor);
						if (!PrimitiveType.FUNCTION.canBeAssignedFrom(type))
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
				 * @param processor The processor
				 */
				public void warnIfNoSideEffects(Statement node, Visitor visitor) {
					if (node.parent() instanceof IterateArrayStatement && ((IterateArrayStatement)node.parent()).elementExpr() == node)
						return;
					if (!node.hasSideEffects())
						visitor.markers().warning(visitor, Problem.NoSideEffects, node, node, 0);
				}
				@Override
				public void visitor(Statement node, Visitor visitor) throws ParsingException {
					supr.visitor(node, visitor);
					warnIfNoSideEffects(node, visitor);
					if (visitor.controlFlow != ControlFlow.Continue)
						visitor.markers().warning(visitor, Problem.NeverReached, node, node, 0);
				}
			},

			new Expert<VarDeclarationStatement>(VarDeclarationStatement.class) {
				@Override
				public void visitor(VarDeclarationStatement node, Visitor visitor) throws ParsingException {
					supr.visitor(node, visitor);
					for (VarInitialization initialization : node.variableInitializations())
						if (initialization.variable != null)
							if (initialization.expression != null) {
								IType initializationType = ty(initialization.expression, visitor);
								if (
									initialization.variable.staticallyTyped() &&
									!initialization.variable.type().canBeAssignedFrom(initializationType)
								)
									visitor.incompatibleTypesMarker(
										node,
										initialization.expression,
										initialization.variable.type(), initializationType
									);
								else {
									AccessVar av = new AccessVar(initialization.variable);
									judgement(av, initializationType, TypingJudgementMode.Unify, visitor);
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
				public void visitor(PropListExpression node, Visitor visitor) throws ParsingException {
					supr.visitor(node, visitor);
					if (!visitor.script().engine().settings().supportsProplists)
						visitor.markers().error(visitor, Problem.NotSupported, node, node, Markers.NO_THROW,
							net.arctics.clonk.parser.c4script.ast.Messages.PropListExpression_ProplistsFeature,
							visitor.script().engine().name());
					for (Variable v : node.components())
						if (v.initializationExpression() != null)
							judgement(new AccessVar(v), ty(v.initializationExpression(), visitor), TypingJudgementMode.Unify, visitor);
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
				@Override
				public IType type(MemberOperator node, Visitor visitor) {
					if (node.id() != null)
						return visitor.script().nearestDefinitionWithId(node.id());
					// stuff before -> decides
					ASTNode pred = node.predecessorInSequence();
					return pred != null ? ty(pred, visitor) : supr.type(node, visitor);
				}
				@Override
				public boolean typingJudgement(MemberOperator node, IType type, Visitor visitor, TypingJudgementMode mode) {
					ASTNode p = node.predecessorInSequence();
					return p != null ? expert(p).typingJudgement(p, type, visitor, mode) : false;
				}
				@Override
				public void visitor(MemberOperator node, Visitor visitor) throws ParsingException {
					supr.visitor(node, visitor);
					ASTNode pred = node.predecessorInSequence();
					EngineSettings settings = visitor.script().engine().settings();
					if (pred != null) {
						IType requiredType = node.dotNotation() ? PrimitiveType.PROPLIST : TypeChoice.make(PrimitiveType.OBJECT, PrimitiveType.ID);
						ASTNode sequenceTilMe = pred.sequenceTilMe();
						Expert<? super ASTNode> stmReporter = expert(sequenceTilMe);
						if (!stmReporter.typingJudgement(sequenceTilMe, requiredType, visitor, TypingJudgementMode.Hint))
							visitor.markers().warning(visitor, node.dotNotation() ? Problem.NotAProplist : Problem.CallingMethodOnNonObject, node, node, 0,
								ty(sequenceTilMe, stmReporter, visitor).typeName(false));
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
				public void visitor(IterateArrayStatement node, Visitor visitor) throws ParsingException {
					ControlFlow t = visitor.controlFlow;
					visitor.controlFlow = ControlFlow.Continue;
					Variable loopVariable;
					AccessVar accessVar;
					ASTNode elementExpr = node.elementExpr();
					ASTNode arrayExpr = node.arrayExpr();
					if (elementExpr instanceof VarDeclarationStatement)
						loopVariable = ((VarDeclarationStatement)elementExpr).variableInitializations()[0].variable;
					else if ((accessVar = as(SimpleStatement.unwrap(elementExpr), AccessVar.class)) != null) {
						Declaration d = visitor.obtainDeclaration(accessVar);
						if (d == null) {
							// implicitly create loop variable declaration if not found
							SourceLocation varPos = visitor.absoluteSourceLocationFromExpr(accessVar);
							loopVariable = visitor.processor.script.createVarInScope(Variable.DEFAULT_VARIABLE_FACTORY, node.parentOfType(Function.class), accessVar.name(), Scope.VAR, varPos.start(), varPos.end(), null);
						} else
							loopVariable = as(d, Variable.class);
					} else
						loopVariable = null;

					visitor.visitNode(elementExpr, true);
					visitor.visitNode(arrayExpr, true);

					IType type = ty(arrayExpr, visitor);
					if (!type.canBeAssignedFrom(PrimitiveType.ARRAY))
						visitor.incompatibleTypesMarker(node, arrayExpr, type, PrimitiveType.ARRAY);
					IType elmType = ArrayType.elementTypeSet(type);
					TypeEnvironment env = visitor.newTypeEnvironment();
					{
						if (loopVariable != null) {
							loopVariable.setUsed(true);
							judgement(new AccessVar(loopVariable), elmType, TypingJudgementMode.Unify, visitor);
						}
						visitor.visitNode(node.body(), true);
					}
					visitor.endTypeEnvironment(env, true, false);
					visitor.controlFlow = t;
				}
			},

			new Expert<SimpleStatement>(SimpleStatement.class) {
				@Override
				public void visitor(SimpleStatement node, Visitor visitor) throws ParsingException {
					BinaryOp op = as(node.expression(), BinaryOp.class);
					if (op != null && !op.operator().modifiesArgument())
						visitor.markers().warning(visitor, Problem.NoAssignment, node, op, 0);
					supr.visitor(node, visitor);
				}
			},

			new ConditionalStatementExpert<IfStatement>(IfStatement.class) {
				@Override
				public void visitor(IfStatement node, Visitor visitor) throws ParsingException {
					ControlFlow old = visitor.controlFlow;
					ASTNode condition = node.condition();
					visitor.visitNode(condition, true);
					// use two separate type environments for if and else statement, merging
					// gathered information afterwards
					TypeEnvironment ifEnvironment = visitor.newTypeEnvironment();
					visitor.visitNode(node.body(), true);
					visitor.endTypeEnvironment(ifEnvironment, false, false);
					visitor.controlFlow = old;
					if (node.elseExpression() != null) {
						TypeEnvironment elseEnvironment = visitor.newTypeEnvironment();
						visitor.visitNode(node.elseExpression(), true);
						visitor.endTypeEnvironment(elseEnvironment, false, false);
						ifEnvironment.inject(elseEnvironment, false);
					}
					if (ifEnvironment.up != null)
						ifEnvironment.up.inject(ifEnvironment, false);
					visitor.controlFlow = old;

					if (!condition.containsConst()) {
						Object condEv = PrimitiveType.BOOL.convert(condition.evaluateStatic(node.parentOfType(Function.class)));
						if (condEv != null && condEv != ASTNode.EVALUATION_COMPLEX)
							visitor.markers().warning(visitor,
								condEv.equals(true) ? Problem.ConditionAlwaysTrue : Problem.ConditionAlwaysFalse,
								condition, condition, 0, condition);
					}
				};
			},

			new ConditionalStatementExpert<ForStatement>(ForStatement.class) {
				@Override
				public void visitor(ForStatement node, Visitor visitor) throws ParsingException {
					if (node.initializer() != null)
						visitor.visitNode(node.initializer(), true);
					super.visitor(node, visitor);
					if (node.increment() != null)
						visitor.visitNode(node.increment(), true);
				}
			},

			new ConditionalStatementExpert<WhileStatement>(WhileStatement.class),

			new Expert<NewProplist>(NewProplist.class) {
				@Override
				public void visitor(NewProplist node, Visitor visitor) throws ParsingException {
					node.definedDeclaration().setPrototype(as(ty(node.prototype(), visitor), ProplistDeclaration.class));
				}
			},

			new Expert<Placeholder>(Placeholder.class) {
				@Override
				public void visitor(Placeholder node, Visitor visitor) throws ParsingException {
					StringTbl.reportMissingStringTblEntries(visitor, new EntityRegion(null, node, node.entryName()), node);
				}
			},

			new Expert<MissingStatement>(MissingStatement.class) {
				@Override
				public void visitor(MissingStatement node, Visitor visitor) throws ParsingException {
					visitor.markers().error(visitor, Problem.MissingStatement, node, node, Markers.NO_THROW);
				}
			},

			new Expert<GarbageStatement>(GarbageStatement.class) {
				@Override
				public void visitor(GarbageStatement node, Visitor visitor) throws ParsingException {
					visitor.markers().error(visitor, Problem.Garbage, node, node, Markers.NO_THROW, node.garbage());
				}
			},

			new Expert<FunctionDescription>(FunctionDescription.class) {
				@Override
				public void visitor(FunctionDescription node, Visitor visitor) throws ParsingException {
					if (visitor.processor.hasAppendTo)
						return;
					int off = 1;
					for (String part : node.contents().split("\\|")) { //$NON-NLS-1$
						if (part.startsWith("$") && part.endsWith("$")) { //$NON-NLS-1$ //$NON-NLS-2$
							StringTbl stringTbl = visitor.script().localStringTblMatchingLanguagePref();
							String entryName = part.substring(1, part.length()-1);
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
				public void visitor(Comment node, Visitor visitor) throws ParsingException {
					if (!visitor.roaming || node.parentOfType(Script.class) == visitor.script()) {
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
				public void visitor(Unfinished node, Visitor visitor) throws ParsingException {
					visitor.markers().error(visitor, Problem.NotFinished, node, node, Markers.NO_THROW, node);
				}
			}

		};
		for (Expert<?> expert : classes)
			committee.put(expert.cls(), expert);
		for (Expert<?> expert : classes)
			expert.findSuper();
	}

}
