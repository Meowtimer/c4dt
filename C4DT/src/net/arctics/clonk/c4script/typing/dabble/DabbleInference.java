package net.arctics.clonk.c4script.typing.dabble;

import static java.lang.String.format;
import static net.arctics.clonk.Flags.DEBUG;
import static net.arctics.clonk.util.ArrayUtil.filteredIterable;
import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.defaulting;
import static net.arctics.clonk.util.Utilities.eq;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;

import net.arctics.clonk.Core;
import net.arctics.clonk.Problem;
import net.arctics.clonk.ProblemException;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ControlFlow;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ast.EntityRegion;
import net.arctics.clonk.ast.IASTPositionProvider;
import net.arctics.clonk.ast.IASTVisitor;
import net.arctics.clonk.ast.IEvaluationContext;
import net.arctics.clonk.ast.Placeholder;
import net.arctics.clonk.ast.Sequence;
import net.arctics.clonk.ast.SourceLocation;
import net.arctics.clonk.ast.Structure;
import net.arctics.clonk.ast.TraversalContinuation;
import net.arctics.clonk.c4script.FindDeclarationInfo;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.Function.FunctionScope;
import net.arctics.clonk.c4script.IProplistDeclaration;
import net.arctics.clonk.c4script.InitializationFunction;
import net.arctics.clonk.c4script.InitializationFunction.VarInitializationAccess;
import net.arctics.clonk.c4script.Keywords;
import net.arctics.clonk.c4script.Operator;
import net.arctics.clonk.c4script.ProblemReporter;
import net.arctics.clonk.c4script.ProblemReportingStrategy;
import net.arctics.clonk.c4script.ProblemReportingStrategy.Capabilities;
import net.arctics.clonk.c4script.ProplistDeclaration;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.SpecialEngineRules;
import net.arctics.clonk.c4script.SpecialEngineRules.SpecialFuncRule;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.c4script.Variable.Scope;
import net.arctics.clonk.c4script.ast.AccessDeclaration;
import net.arctics.clonk.c4script.ast.AccessVar;
import net.arctics.clonk.c4script.ast.ArrayElementExpression;
import net.arctics.clonk.c4script.ast.ArrayExpression;
import net.arctics.clonk.c4script.ast.ArraySliceExpression;
import net.arctics.clonk.c4script.ast.BinaryOp;
import net.arctics.clonk.c4script.ast.BoolLiteral;
import net.arctics.clonk.c4script.ast.BreakStatement;
import net.arctics.clonk.c4script.ast.CallDeclaration;
import net.arctics.clonk.c4script.ast.CallExpr;
import net.arctics.clonk.c4script.ast.CallInherited;
import net.arctics.clonk.c4script.ast.CastExpression;
import net.arctics.clonk.c4script.ast.Comment;
import net.arctics.clonk.c4script.ast.ConditionalStatement;
import net.arctics.clonk.c4script.ast.ContinueStatement;
import net.arctics.clonk.c4script.ast.EmptyStatement;
import net.arctics.clonk.c4script.ast.FloatLiteral;
import net.arctics.clonk.c4script.ast.ForStatement;
import net.arctics.clonk.c4script.ast.FunctionBody;
import net.arctics.clonk.c4script.ast.FunctionDescription;
import net.arctics.clonk.c4script.ast.GarbageStatement;
import net.arctics.clonk.c4script.ast.IDLiteral;
import net.arctics.clonk.c4script.ast.ILoop;
import net.arctics.clonk.c4script.ast.IfStatement;
import net.arctics.clonk.c4script.ast.IntegerLiteral;
import net.arctics.clonk.c4script.ast.IterateArrayStatement;
import net.arctics.clonk.c4script.ast.Literal;
import net.arctics.clonk.c4script.ast.MemberOperator;
import net.arctics.clonk.c4script.ast.MissingStatement;
import net.arctics.clonk.c4script.ast.NewProplist;
import net.arctics.clonk.c4script.ast.Nil;
import net.arctics.clonk.c4script.ast.OperatorExpression;
import net.arctics.clonk.c4script.ast.Parenthesized;
import net.arctics.clonk.c4script.ast.PropListExpression;
import net.arctics.clonk.c4script.ast.ReturnStatement;
import net.arctics.clonk.c4script.ast.SimpleStatement;
import net.arctics.clonk.c4script.ast.Statement;
import net.arctics.clonk.c4script.ast.StringLiteral;
import net.arctics.clonk.c4script.ast.TempAccessVar;
import net.arctics.clonk.c4script.ast.This;
import net.arctics.clonk.c4script.ast.Tuple;
import net.arctics.clonk.c4script.ast.UnaryOp;
import net.arctics.clonk.c4script.ast.UnaryOp.Placement;
import net.arctics.clonk.c4script.ast.Unfinished;
import net.arctics.clonk.c4script.ast.VarDeclarationStatement;
import net.arctics.clonk.c4script.ast.VarInitialization;
import net.arctics.clonk.c4script.ast.WhileStatement;
import net.arctics.clonk.c4script.ast.evaluate.IVariable;
import net.arctics.clonk.c4script.typing.ArrayType;
import net.arctics.clonk.c4script.typing.CallTargetType;
import net.arctics.clonk.c4script.typing.FunctionType;
import net.arctics.clonk.c4script.typing.IType;
import net.arctics.clonk.c4script.typing.ITypeable;
import net.arctics.clonk.c4script.typing.PrimitiveType;
import net.arctics.clonk.c4script.typing.TypeChoice;
import net.arctics.clonk.c4script.typing.TypeVariable;
import net.arctics.clonk.c4script.typing.Typing;
import net.arctics.clonk.c4script.typing.TypingJudgementMode;
import net.arctics.clonk.c4script.typing.dabble.DabbleInference.Input.Visit;
import net.arctics.clonk.c4script.typing.dabble.DabbleInference.Input.Visitor;
import net.arctics.clonk.index.CachedEngineDeclarations;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Definition.ProxyVar;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.EngineFunction;
import net.arctics.clonk.index.EngineSettings;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.IndexEntity;
import net.arctics.clonk.index.MetaDefinition;
import net.arctics.clonk.index.Scenario;
import net.arctics.clonk.parser.Markers;
import net.arctics.clonk.stringtbl.StringTbl;
import net.arctics.clonk.util.Pair;
import net.arctics.clonk.util.PerClass;
import net.arctics.clonk.util.TaskExecution;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

@Capabilities(capabilities=Capabilities.ISSUES|Capabilities.TYPING)
public class DabbleInference extends ProblemReportingStrategy {

	static final boolean UNUSEDPARMWARNING = false;

	final Map<Script, Input> input = new HashMap<>();
	Typing typing;

	private Input script(final String script) {
		for (final Input i : input.values())
			if (i.script().qualifiedName().equals(script))
				return i;
		return null;
	}

	Visit visitFor(String identification) {
		final String[] parts = identification.split("::");
		if (parts.length != 2)
			return null;
		final String script = parts[0];
		final String func = parts[1];
		final Input input = script(script);
		if (input != null) {
			final Function fn = input.script().function(func);
			if (fn != null)
				return input.visits.get(fn);
		}
		return null;
	}


	Visit visitFor(final Function function, final Script script) {
		if (function.body() == null)
			return null;
		final Input input = DabbleInference.this.input.get(script);
		return input != null ? input.visits.get(function) : null;
	}

	private boolean noticeParameterCountMismatch;

	@Override
	public DabbleInference configure(final Index index, final String args) {
		super.configure(index, args);
		for (final String a : args.split("\\|")) //$NON-NLS-1$
			switch (a) {
			case "noticeParameterCountMismatch": //$NON-NLS-1$
				noticeParameterCountMismatch = true;
				break;
			}
		assembleCommittee();
		return this;
	}

	@Override
	public DabbleInference initialize(final Markers markers, final IProgressMonitor progressMonitor, final Script[] scripts) {
		synchronized (projectName) {
			typing = defaulting(index != null ? index.typing() : null, Typing.INFERRED);
			super.initialize(markers, progressMonitor, scripts);
			gatherInput(scripts);
			return this;
		}
	}

	@Override
	public ProblemReportingStrategy initialize(final Markers markers, final IProgressMonitor progressMonitor, final Collection<Pair<Script, Function>> functions) {
		synchronized (projectName) {
			typing = defaulting(index != null ? index.typing() : null, Typing.INFERRED);
			super.initialize(markers, progressMonitor, functions);
			gatherInputFromRestrictedFunctionSet(functions);
			return this;
		}
	}

	private void gatherInputFromRestrictedFunctionSet(final Collection<Pair<Script, Function>> functions_) {
		final ArrayList<Pair<Script, Function>> functions = new ArrayList<>(functions_);
		input.clear();
		while (functions.size() > 0) {
			final Pair<Script, Function> frst = functions.get(0);
			Input i = input.get(frst.first());
			if (i == null) {
				final List<Function> funcs = new LinkedList<>();
				final Script s = frst.first().script();
				for (final Iterator<Pair<Script, Function>> it = functions.iterator(); it.hasNext();) {
					final Pair<Script, Function> f = it.next();
					if (f.first() == s) {
						it.remove();
						funcs.add(f.second());
					}
				}
				i = new Input(s, 0, funcs.toArray(new Function[funcs.size()]));
				input.put(s, i);
			}
		}
	}

	@Override
	public void run() {
		subTask(Messages.ComputingGraph);
		final Plan plan = new Plan(this);
		// prepare
		TaskExecution.threadPool(plan.visits.values(), 3);
		if (plan.total > 0) {
			subTask(Messages.RunInference);
			threadPool = TaskExecution.newPool(plan.total);
			try {
				remainingRuns = new AtomicInteger(plan.total);
				for (final Visit r : plan.roots)
					runVisit(r);
				try {
					threadPool.awaitTermination(3, TimeUnit.MINUTES);
				} catch (final InterruptedException e) {
					e.printStackTrace();
				}
			} finally {
				threadPool = null;
			}
		}
		// double takes
		for (final Visit v : plan.doubleTakes)
			v.doubleTake = false;
		TaskExecution.threadPool(plan.doubleTakes, 3);
	}

	@Override
	public void run2() {
		validateParameters();
	}

	static class ParameterValidation {
		final Function called;
		final CallDeclaration node;
		final Visitor visitor;
		public ParameterValidation(final Visitor visitor, final CallDeclaration node, final Function called) {
			super();
			this.visitor = visitor;
			this.node = node;
			this.called = called;
		}
		void validateTypes() {
			final ASTNode[] params = node.params();
			final Script script = visitor.script();
			final DabbleInference inference = visitor.inference();
			final IType[] nodeTypes = visitor.visit.inferredTypes;
			final IType pred = node.predecessor() != null
				? nodeTypes[node.predecessor().localIdentifier()]
				: script;
			if (pred == null)
				return;
			for (int p = 0; p < params.length; p++) {
				final ASTNode given = params[p];
				if (given == null)
					continue;
				final IType parmTy = called.parameterType(p, pred);
				final IType givenTy = nodeTypes[given.localIdentifier()];
				final IType unified = inference.typing.unifyNoChoice(parmTy, givenTy);
				if (unified == null)
					try {
						inference.markers().marker(script, Problem.IncompatibleArgTypes, given, given.start(), given.end(), Markers.NO_THROW,
							inference.typing == Typing.STATIC ? IMarker.SEVERITY_ERROR : IMarker.SEVERITY_WARNING,
							called.name(), called.parameter(p).name(), parmTy.typeName(true), givenTy.typeName(true)
						);
					} catch (final ProblemException e) {}
				else if (visitor != null && givenTy == PrimitiveType.UNKNOWN)
					visitor.judgment(given, unified, TypingJudgementMode.UNIFY);
			}
			if (inference.noticeParameterCountMismatch)
				validateParameterCount();
		}
		private void validateParameterCount() {
			final ASTNode[] params = node.params();
			final Script script = visitor.script();
			final DabbleInference inference = visitor.inference();
			if (called.index() == script.index() && params.length > called.numParameters() && !(called.script() instanceof Engine))
				try {
					inference.markers().error(script, Problem.ParameterCountMismatch, node, node, Markers.NO_THROW,
						called.numParameters(), params.length, called.name());
				} catch (final ProblemException e) {}
		}
		@Override
		public String toString() { return node.printed(); }
	}

	final List<ParameterValidation> parameterValidations = Collections.synchronizedList(new LinkedList<ParameterValidation>());

	@Override
	public void apply() {
		for (final Input input : this.input.values())
			input.apply();
	}

	private void validateParameters() {
		for (final ParameterValidation pv : parameterValidations)
			try {
				pv.validateTypes();
			} catch (final Exception e) {
				e.printStackTrace();
			}
		parameterValidations.clear();
	}

	private void subTask(final String text) {
		progressMonitor.subTask(String.format("%s: %s", projectName, text)); //$NON-NLS-1$
	}

	private void gatherInput(final Script[] scripts) {
		input.clear();
		for (final Script p : scripts)
			if (p != null) {
				final Input info = new Input(p, 0);
				input.put(p.script(), info);
			}
	}

	ExecutorService threadPool;
	AtomicInteger remainingRuns;

	void runVisit(final Visit visit) {
		if (threadPool != null) {
			threadPool.execute(visit);
			if (remainingRuns.decrementAndGet() == 0)
				threadPool.shutdown();
		}
		else
			visit.run();
	}

	void runPlan(final Plan plan) {
		if (plan.total > 0) {
			subTask(Messages.RunInference);
			threadPool = TaskExecution.newPool(plan.total);
			try {
				remainingRuns = new AtomicInteger(plan.total);
				for (final Visit r : plan.roots)
					runVisit(r);
				try {
					threadPool.awaitTermination(3, TimeUnit.MINUTES);
				} catch (final InterruptedException e) {
					e.printStackTrace();
				}
			} finally { threadPool = null; }
		}
	}

	/**
	 * One script and associated information.
	 * @author madeen
	 *
	 */
	protected final class Input {

		/**
		 * Type variable for the return type of a function that was visited.
		 * {@link FunctionReturnTypeVariable}s are pre-created ({@link Input#makePlan()}) and determined (set to state {@link State#FINISHED}) in a not quite deterministic order.
		 * Synchronization of multiple visitor threads will be performed by locking on objects of this type.
		 * @author madeen
		 */
		final class Visit extends FunctionReturnTypeVariable implements Runnable {

			Visitor visitor;
			IType[] inferredTypes;
			Expert<?>[] experts;
			Declaration[] declarations;
			boolean doubleTake;

			final Set<Visit> dependents = new HashSet<>();
			final Set<Visit> dependencies = new HashSet<>();
			final int hash;

			@Override
			public int hashCode() { return hash; }
			public Input input() { return Input.this; }
			@Override
			public String toString() { return function.qualifiedName(script); }
			@Override
			public void run() {
				visitor.visit();
				for (final Visit d : dependents) {
					boolean doRun;
					synchronized (d.dependencies) {
						doRun = d.dependencies.remove(this) && d.dependencies.size() == 0;
					}
					if (doRun)
						runVisit(d);
				}
			}

			public Visit(final Function function) {
				super(function);
				if (typing == Typing.INFERRED)
					set(PrimitiveType.VOID);
				this.hash = function.qualifiedName(script).hashCode();
			}

			void prepare() {
				visitor = new Visitor(this);
				inferredTypes = new IType[function.totalNumASTNodes()];
				experts = new Expert<?>[function.totalNumASTNodes()];
				declarations = new Declaration[function.totalNumASTNodes()];
				final boolean owns = function.containedIn(script);
				function.body().traverse((node, nothing) -> {
					if (owns && node instanceof AccessDeclaration)
						((AccessDeclaration)node).setDeclaration(null);
					final Expert<? super ASTNode> e = findExpert(node);
					final int nid = node.localIdentifier();
					if (nid >= 0) {
						experts[nid] = e;
						if (e.providesInherentType)
							inferredTypes[nid] = e.type(node, visitor);
					}
					return TraversalContinuation.Continue;
				}, null);
			}

			public boolean matches(final Function function, final Script script) {
				return this.function == function && input().script == script;
			}
		}

		final class Visitor extends Markers implements ProblemReporter, IEvaluationContext {

			final Input input() { return Input.this; }
			final DabbleInference inference() { return DabbleInference.this; }

			ControlFlow controlFlow;
			TypeEnvironment environment;
			final Visit visit;
			int roaming;

			public Visitor(final Visit visit) { this.visit = visit; }

			@SuppressWarnings("unchecked")
			private final <T extends ASTNode> Expert<? super T> expert(final T node) {
				final int localID = node.localIdentifier();
				if (visit != null && localID >= 0) {
					final Expert<?> expert = visit.experts[localID];
					return (Expert<? super T>) expert;
				} else
					return findExpert(node);
			}

			public final IType ty(final ASTNode node) { return node != null ? ty(node, expert(node)) : null; }

			public final <T extends ASTNode> IType ty(final T node, final Expert<T> expert) {
				final IType type = expert.type(node, this);
				if (visit != null)
					visit.inferredTypes[node.localIdentifier()] = type;
				return type;
			}

			@Override
			public final boolean judgment(final ASTNode node, final IType type, final TypingJudgementMode mode) {
				return expert(node).judgment(node, type, null, this, mode);
			}

			public final boolean judgment(final ASTNode node, final IType type, final ASTNode origin, final TypingJudgementMode mode) {
				return expert(node).judgment(node, type, origin, this, mode);
			}

			@Override
			public String toString() {
				final String func = visit != null ? visit.function().qualifiedName() : "<no function>"; //$NON-NLS-1$
				return String.format("Visitor (%s, %s)", input().toString(), func); //$NON-NLS-1$
			}

			public final SpecialFuncRule specialRuleFor(final CallDeclaration node, final int role) {
				final Engine engine = script().engine();
				if (engine != null && engine.specialRules() != null)
					return engine.specialRules().funcRuleFor(node.name(), role);
				else
					return null;
			}

			private void typeParametersFromCalls(final Function function, final Function baseFunction, final TypeVariable[] parameterTypes) {
				final List<CallDeclaration> calls = index.callsTo(function.name());
				if (calls != null) {

					final IType[][] types = new IType[parameterTypes.length][calls.size()];
					gatherCallTypes(function, baseFunction, parameterTypes, calls, types);

					for (int pa = 0; pa < parameterTypes.length; pa++) {
						final Variable par = function.parameter(pa);
						if (par.staticallyTyped())
							continue;
						parameterTypes[pa].set(typing, types[pa]);
					}
				}
			}

			private void gatherCallTypes(
				final Function function,
				final Function baseFunction,
				final TypeVariable[] parameterTypes,
				final List<? extends CallDeclaration> calls,
				final IType[][] types
			) {
				final Function base = (Function) function.baseFunction().latestVersion();
				for (int ci = 0; ci < calls.size(); ci++) {
					final CallDeclaration call = calls.get(ci);
					if (call.predecessor() instanceof MemberOperator && ((MemberOperator) call.predecessor()).hasTilde())
						continue;
					final Function f = call.parent(Function.class);
					final Script other = f.script();
					final Script ds = call.predecessor() == null && conglomerate.contains(other) ? script : other;
					final Visit v = visitFor(f, ds);
					final Visitor vtor = v != null ? v.visitor : null;

					Function ref = as(call.declaration(), Function.class);
					if (ref == null)
						continue;
					// not related - short circuit skip
					ref = (Function)ref.latestVersion();
					if (ref.baseFunction().latestVersion() != base)
						continue;
					RelevanceCheck: if (ref != null) {
						final IType predTy = call.predecessor() != null
							? nodeType(f, other, v, vtor, call.predecessor())
							: call.parent(Script.class);
						if (predTy == null)
							continue;
						for (final IType t : predTy) {
							if (t == script)
								break RelevanceCheck;
							if (t instanceof Script) {
								final Script calledOn = (Script)t;
								if (script.doesInclude(index, calledOn))
									break RelevanceCheck;
								if (calledOn.seesFunction(function) && calledOn.doesInclude(index, script))
									break RelevanceCheck;
							}
						}
						continue;
					}
					final int parNum = Math.min(parameterTypes.length, call.params().length);
					for (int pa = 0; pa < parNum; pa++)
						if (!function.parameter(pa).staticallyTyped()) {
							final ASTNode concretePar = call.params()[pa];
							if (concretePar != null) {
								script().addUsedScript(other);
								types[pa][ci] = nodeType(f, other, v, vtor, concretePar);
							}
						}
				}
				if (DEBUG)
					log("Call types: %s", Arrays.deepToString(types)); //$NON-NLS-1$
			}

			private IType nodeType(
				final Function containing,
				final Script other,
				final Visit containingVisit,
				final Visitor visitor,
				final ASTNode node
			) {
				IType ty = containingVisit != null ? containingVisit.inferredTypes[node.localIdentifier()] : null;
				if (ty == null) {
					final Function.Typing typing = other.typings().get(containing);
					if (typing != null)
						ty = typing.nodeTypes[node.localIdentifier()];
				}
				return ty;
			}

			public Visit visit() {
				try {
					innerVisit();
				} catch (final Exception e) {
					log("Error visiting '%s'", visit.function.qualifiedName()); //$NON-NLS-1$
					e.printStackTrace();
				}
				return visit;
			}

			private void innerVisit() {

				final Function function = visit.function();
				final Script funScript = function.script();
				if (DEBUG)
					log("Visiting %s", function.qualifiedName(script())); //$NON-NLS-1$
				final boolean ownedFunction = funScript == script();
				final ASTNode[] statements = function.body().statements();
				final List<Variable> parameters = function.parameters();
				final Function baseFunction = function.baseFunction();

				if (!ownedFunction)
					startRoaming();
				try {
					TypeEnvironment env = newTypeEnvironment();
					{
						env.add(visit);
						if (typing == Typing.STATIC)
							visit.set(function.returnType());
						createFunctionLocalsTypeVariables(function);
						final TypeVariable[] parTypes = createParameterTypeVariables(function, parameters);
						for (final TypeVariable pt : parTypes)
							env.add(pt);
						env = newTypeEnvironment();
						{
							if (
								!assignParameterTypesOfInheritedEngineFunction(function, parTypes) &&
								!setParameterTypesBasedOnRules(function, parTypes) &&
								shouldTypeFromCalls(function)
							) {
								preliminaryVisit(function, statements);
								typeParametersFromCalls(function, baseFunction, parTypes);
								env.clear();
							}
							actualVisit(ownedFunction, statements, parTypes);
							warnAboutUnusedLocals(function, statements);
							DabbleInference.this.markers().take(this);
						}
						env = endTypeEnvironment();
						typeUntypedParametersByUsage(parTypes);
					}
					endTypeEnvironment();
				}
				catch (final ProblemException e) {}
				finally {
					if (!ownedFunction)
						endRoaming();
				}
			}

			private void typeUntypedParametersByUsage(final TypeVariable[] parTypes) {
				class CallsIntersectionAssigner implements IASTVisitor<Void> {
					final Declaration parameter;
					final Set<String> requiredMethods = new HashSet<>();
					@Override
					public TraversalContinuation visitNode(final ASTNode node, final Void v) {
						final ASTNode pred = node.predecessor();
						if (node instanceof CallDeclaration && pred != null) {
							final Declaration dec = declarationOf(pred);
							if (dec == parameter)
								requiredMethods.add(((CallDeclaration) node).name());
						}
						return TraversalContinuation.Continue;
					}

					private Set<Script> scriptsSupporting(final String name) {
						if (index.engine().findFunction(name) != null || index.findGlobal(Function.class, name) != null)
							return Collections.emptySet();
						final List<Function> funcs = script.index().declarationsWithName(name, Function.class);
						final Set<Script> scripts = new HashSet<>(funcs.size());
						for (final Function f : funcs)
							scripts.add(f.script());
						return scripts;
					}

					public CallsIntersectionAssigner(final TypeVariable tyVar, final ASTNode body) {
						this.parameter = tyVar.declaration();
						body.traverse(this, null);
						Set<Script> remaining = null;
						for (final String m : requiredMethods) {
							final Set<Script> supporting = scriptsSupporting(m);
							remaining = remaining != null ? intersection(remaining, supporting) : supporting;
						}
						if (remaining != null && !remaining.isEmpty()) {
							final IType ty = TypeChoice.make(remaining);
							if (!eq(ty, PrimitiveType.UNKNOWN))
								//System.out.println(format("%s: %s", parameter.qualifiedName(), ty.typeName(true)));
								tyVar.set(ty);
						}
					}
					private <T> Set<T> intersection(final Set<T> a, final Set<T> b) {
						final Set<T> tmp = new HashSet<T>();
						for (final T x : a)
							if (b.contains(x))
								tmp.add(x);
						return tmp;
					}
				};
				final FunctionBody body = visit.function().body();
				for (final TypeVariable parTy : parTypes)
					if (eq(parTy.get(), PrimitiveType.OBJECT))
						new CallsIntersectionAssigner(parTy, body);
			}

			private void createFunctionLocalsTypeVariables(final Function function) {
				for (final Variable l : function.locals()) {
					final AccessVar av = new TempAccessVar(l, function.body());
					final TypeVariable ti = expert(av).requestTypeVariable(av, this);
					if (ti != null)
						ti.set(PrimitiveType.UNKNOWN);
				}
			}

			private TypeVariable[] createParameterTypeVariables(final Function function, final List<Variable> parameters) {
				// create type variables for parameters
				final TypeVariable[] callTypes = new TypeVariable[parameters.size()];
				for (int i = 0; i < callTypes.length; i++) {
					final Variable p = function.parameter(i);
					final TypeVariable tyvar = new VariableTypeVariable(p);
					tyvar.set(p.staticallyTyped() ? p.type() : PrimitiveType.UNKNOWN);
					environment.add(tyvar);
					callTypes[i] = tyvar;
				}
				return callTypes;
			}

			private void actualVisit(final boolean ownedFunction, final ASTNode[] statements, final TypeVariable[] callTypes) throws ProblemException {
				if (ownedFunction)
					for (int i = 0; i < callTypes.length; i++)
						callTypes[i].apply(false);
				final ControlFlow old = controlFlow;
				controlFlow = ControlFlow.Continue;
				for (final ASTNode s : statements)
					visit(s, true);
				controlFlow = old;
			}

			boolean preliminary;
			private void preliminaryVisit(final Function function, final ASTNode[] statements) throws ProblemException {
				preliminary = true;
				// when taking parameter types from calls to the function visit the function body preliminarily
				// before visiting the functions the calls are in.
				// Merge insights about types from visiting the body with the types of concrete parameters.
				// This way, the functions calling this function - which also call visit - also will have
				// some preliminary return type for this function.
				// Also, merging call types with how the parameter is actually used inside the body improves
				// the chance of correctly deciding which kind of parameters are the 'right' ones to pass to the function.
				if (DEBUG)
					log("%s: Preliminary visit", toString()); //$NON-NLS-1$
				startRoaming();
				{
					final ControlFlow old = controlFlow;
					controlFlow = ControlFlow.Continue;
					for (final ASTNode s : statements)
						visit(s, true);
					controlFlow = old;
				}
				endRoaming();
				visit.declarations = new Declaration[visit.declarations.length];
				preliminary = false;
			}

			public void concreteArgumentMismatch(final ASTNode argument, final Variable parameter, final Function callee, final IType expected, final IType got) {
				//try {
				//	DabbleInference.this.markers().marker(this,
				//		Problem.ConcreteArgumentMismatch,
				//		argument, argument.start(), argument.end(),
				//		Markers.NO_THROW, IMarker.SEVERITY_WARNING,
				//		argument, parameter.name(), callee.qualifiedName(), expected.typeName(true), got.typeName(true)
				//	);
				//} catch (final ProblemException e) {}
			}

			@Override
			public void incompatibleTypesMarker(final ASTNode node, final IRegion region, IType left, IType right) {
				try {
					if (left == null)
						left = PrimitiveType.ANY;
					if (right == null)
						right = PrimitiveType.ANY;
					this.markers().marker(this, Problem.IncompatibleTypes, node, region.getOffset(), region.getOffset()+region.getLength(), Markers.NO_THROW,
						typing == Typing.STATIC ? IMarker.SEVERITY_ERROR : IMarker.SEVERITY_WARNING,
						left.typeName(true), right.typeName(true)
					);
				} catch (final ProblemException e) {}
			}

			public final <T extends ASTNode> T visit(final T expression, final boolean recursive) throws ProblemException {
				if (expression == null)
					return null;
				final Expert<? super T> expert = expert(expression);
				final ControlFlow old = controlFlow;
				if (recursive && !expert.skipReportingProblemsForSubElements())
					for (final ASTNode e : expression.subElements())
						if (e != null)
							visit(e, true);
				controlFlow = old;
				expert.visit(expression, this);
				if (observer != null && !preliminary)
					observer.visitNode(expression, this);
				if (controlFlow == ControlFlow.Continue)
					controlFlow = expression.controlFlow();
				return expression;
			}

			public TypeEnvironment newTypeEnvironment() {
				return this.environment = new TypeEnvironment(typing, defaulting(environment, input().typeEnvironment));
			}

			public TypeEnvironment endTypeEnvironment() {
				environment.up.inject(environment);
				return environment = environment.up == input().typeEnvironment ? null : environment.up;
			}

			private void createWarningAtDeclarationOfVariable(
				final ASTNode body,
				final Variable variable,
				final Problem code,
				final Object... args
			) {
				body.traverse((node, context) -> {
					if (node instanceof VarInitialization) {
						final VarInitialization vi = (VarInitialization) node;
						if (vi.variable == variable) {
							markers().warning(Visitor.this, code, vi, vi, 0, args);
							return TraversalContinuation.Cancel;
						}
					}
					return TraversalContinuation.Continue;
				}, null);
			}

			/**
			 * Warn about variables declared inside the given block that have not been referenced elsewhere ({@link Variable#isUsed() == false})
			 * @param func The function the block belongs to.
			 * @param block The {@link Block}
			 */
			public void warnAboutUnusedLocals(final Function func, final ASTNode[] statements) {
				if (func == null)
					return;
				if (UNUSEDPARMWARNING)
					for (final Variable p : func.parameters())
						if (!p.isUsed())
							this.markers().warning(this, Problem.UnusedParameter, null, p, Markers.ABSOLUTE_MARKER_LOCATION, p.name());
				for (final Variable v : func.locals()) {
					if (!v.isUsed())
						createWarningAtDeclarationOfVariable(func.body(), v, Problem.Unused, v.name());
					final Variable shadowed = script().findVariable(v.name());
					// ignore those pesky static variables from scenario scripts
					if (shadowed != null && !(shadowed.parentDeclaration() instanceof Scenario))
						createWarningAtDeclarationOfVariable(func.body(), v, Problem.IdentShadowed,
							v.qualifiedName(), shadowed.qualifiedName());
				}
			}

			private final void startRoaming() { roaming++; }
			private final void endRoaming()   { --roaming; }

			@Override
			public void marker(final IASTPositionProvider positionProvider, final Problem code, final ASTNode node, final int markerStart, final int markerEnd, final int flags, final int severity, final Object... args) throws ProblemException {
				if (node == null || node.parent(Script.class) != script || (preliminary && node.containedIn(visit.function)))
					return;
				else {
					erroneous |= severity >= IMarker.SEVERITY_ERROR;
					super.marker(positionProvider, code, node, markerStart, markerEnd, flags, severity, args);
				}
			}

			@Override
			public Definition definition() { return as(input().script, Definition.class); }
			@Override
			public SourceLocation absoluteSourceLocationFromExpr(final ASTNode expression) {
				int fragmentOffset = input().fragmentOffset;
				if (fragmentOffset == 0) {
					final Function f = expression.parent(Function.class);
					fragmentOffset = f != null ? f.bodyLocation().start() : 0;
				}
				return new SourceLocation(
					fragmentOffset+expression.start(),
					fragmentOffset+expression.end()
				);
			}

			@Override
			public <T extends AccessDeclaration> Declaration obtainDeclaration(final T access) {
				final Expert<? super T> e = expert(access);
				@SuppressWarnings("unchecked")
				final AccessDeclarationExpert<T> expert = (AccessDeclarationExpert<T>)e;
				return expert.obtainDeclaration(access, this);
			}

			public boolean validForType(final ASTNode node, final IType type) {
				return expert(node).unifyDeclaredAndGiven(node, type, this) != null;
			}

			@Override
			@SuppressWarnings("unchecked")
			public final <T extends IType> T typeOf(final ASTNode node, final Class<T> cls) {
				for (final IType t : typeOf(node))
					if (cls.isInstance(t))
						return (T)t;
				return null;
			}

			private ASTNode lastNonMemberOperator(final Sequence seq) {
				final ASTNode[] subs = seq.subElements();
				for (int i = subs.length - 1; i >= 0; i--) {
					final ASTNode s = subs[i];
					if (!(s instanceof MemberOperator))
						return s;
				}
				return null;
			}

			@Override
			public Declaration declarationOf(final ASTNode node) {
				if (visit != null) {
					final ASTNode last =
						node instanceof MemberOperator ? ((MemberOperator)node).predecessor()
						: node instanceof Sequence ? lastNonMemberOperator((Sequence) node)
						: node;
					if (last instanceof AccessDeclaration) {
						final AccessDeclaration ad = (AccessDeclaration) last;
						return ((AccessDeclarationExpert<? super AccessDeclaration>)expert(ad)).declaration(ad, this);
					}
				}
				return null;
			}

			void log(final String msg, final Object... args) {
				final StringBuilder b = new StringBuilder(10+msg.length()+args.length*5);
				b.append(this.toString());
				b.append(": "); //$NON-NLS-1$
				b.append(String.format(msg, args));
				final String t = b.toString();
				System.out.println(t);
				//log(t);
			}

			@Override
			public CachedEngineDeclarations cachedEngineDeclarations() { return input().cachedEngineDeclarations; }
			@Override
			public Script script() { return input().script; }
			@Override
			public IFile file() { return script().file(); }
			@Override
			public Declaration container() { return script(); }
			@Override
			public int fragmentOffset() { return input().fragmentOffset; }
			@Override
			public IType typeOf(final ASTNode node) { return ty(node); }
			@Override
			public boolean isModifiable(final ASTNode node) { return expert(node).isModifiable(node, this); }
			@Override
			public Markers markers() { return preliminary || visit.doubleTake ? NULL_MARKERS : this; }
			@Override
			public IVariable variable(final AccessDeclaration access, final Object obj) { return null; }
			@Override
			public Object[] arguments() { return null; }
			@Override
			public Function function() { return visit != null ? visit.function : null; }
			@Override
			public int codeFragmentOffset() { return 0; }
			@Override
			public void reportOriginForExpression(final ASTNode expression, final IRegion location, final IFile file) {}
			@Override
			public Object self() { return null; }
		}

		final Script script;
		final List<Script> conglomerate;
		final CachedEngineDeclarations cachedEngineDeclarations;
		final int strictLevel;
		final boolean hasAppendTo;
		final Map<Function, Visit> visits;
		final IType thisType;
		final SpecialEngineRules rules;
		final int fragmentOffset;
		final TypeEnvironment typeEnvironment;
		final boolean partial;
		boolean erroneous = false;

		public Script script() { return script; }

		private HashMap<Function, Visit> makeVisits(final Function[] restrict) {
			final HashMap<Function, Visit> result = new LinkedHashMap<>();
			if (restrict != null && restrict.length > 0) {
				for (final Function f : restrict)
					if (f.body() != null)
						result.put(f, new Visit(f));
			} else {
				final List<? extends Function> localFuncs = script.functions();
				@SuppressWarnings({ "serial" })
				class Revisitor extends HashSet<String> implements IASTVisitor<Function> {
					public Revisitor() { super(localFuncs.size()); }
					private TraversalContinuation revisit(final Function f) {
						if (DEBUG)
							System.out.println(format("%s: Revisiting %s", script.qualifiedName(), f.qualifiedName()));
						add(f.name());
						for (final List<AccessVar> vrs : f.script().varReferences().values())
							for (final AccessVar vr : vrs)
								if (vr.predecessor() == null && vr.containedIn(f) && inAssignment(vr))
									add(vr.name());
						f.findInherited();
						result.put(f, new Visit(f));
						return TraversalContinuation.Continue;
					}
					private boolean inAssignment(final AccessVar vr) {
						for (BinaryOp bop = vr.parent(BinaryOp.class); bop != null; bop = bop.parent(BinaryOp.class))
							if (bop.operator().isAssignment() && vr.containedIn(bop.leftSide()))
								return true;
						return false;
					}
					@Override
					public TraversalContinuation visitNode(final ASTNode node, final Function context) {
						if (node instanceof This)
							return revisit(context);
						if (node.predecessor() == null && node instanceof AccessDeclaration) {
							final AccessDeclaration ad = (AccessDeclaration) node;
							final String name = ad.name();
							switch (name) {
							case "GetID":
								if (ad instanceof CallDeclaration && ((CallDeclaration)ad).params().length == 0)
									return revisit(context);
								break;
							default:
								final Declaration d = ad instanceof CallDeclaration ? script.function(name) : script.variable(name);
								if (d != null && contains(d.name()))
									return revisit(context);
							}
						}
						return TraversalContinuation.Continue;
					}
					public void maybeRevisit(final Function function) {
						if (!contains(function.name()))
							function.traverse(this, function);
					}
					public void loop() {
						for (final Function f : localFuncs)
							this.add(f.name());
						final Collection<Function> functions = script.functionMap().values();
						int old;
						do {
							old = size();
							for (final Function f : functions) {
								if (f.body() == null || f.script() == script)
									continue;
								maybeRevisit(f);
							}
						}
						while (old != size());
					}
				}
				new Revisitor().loop();
				for (final Function f : localFuncs)
					if (f.body() != null) {
						f.findInherited();
						result.put(f, new Visit(f));
					}
			}
			return result;
		}


		boolean setParameterTypesBasedOnRules(final Function function, final TypeVariable[] parameterTypeVariables) {
			if (rules != null)
				for (final SpecialFuncRule funcRule : rules.defaultParmTypeAssignerRules())
					if (funcRule.assignDefaultParmTypes(script, function, parameterTypeVariables))
						return true;
			return false;
		}

		boolean assignParameterTypesOfInheritedEngineFunction(final Function function, final TypeVariable[] parameterTypeVariables) {
			if (function.isGlobal()) {
				final Function ef = function.engine().findFunction(function.name());
				if (ef != null) {
					for (int i = 0; i < parameterTypeVariables.length && i < ef.numParameters(); i++)
						parameterTypeVariables[i].set(ef.parameter(i).type());
					return true;
				}
			}
			return false;
		}

		boolean shouldTypeFromCalls(final Function function) {
			final boolean typeFromCalls =
				typing == Typing.INFERRED &&
				//script instanceof Definition &&
				function.numParameters() > 0 &&
				(function.typeFromCallsHint() || !allParametersStaticallyTyped(function));
			function.setTypeFromCallsHint(typeFromCalls);
			return typeFromCalls;
		}

		final boolean allParametersStaticallyTyped(final Function function) {
			for (final Variable p : function.parameters())
				if (!p.staticallyTyped())
					return false;
			return true;
		}

		public Input(final Script script, final int sourceFragmentOffset, final Function... restrict) {
			this.script = script;
			this.conglomerate = script.conglomerate();
			this.rules = script.engine().specialRules();
			this.cachedEngineDeclarations = script.engine().cachedDeclarations();
			this.strictLevel = script.strictLevel();
			this.thisType = script instanceof Definition ? script : typing.unify(filteredIterable(script.includes(0), Definition.class));
			this.fragmentOffset = sourceFragmentOffset;
			this.hasAppendTo = script.hasAppendTo();
			this.typeEnvironment = TypeEnvironment.newSynchronized(typing);
			this.visits = Collections.synchronizedMap(makeVisits(restrict));
			this.partial = restrict != null && restrict.length > 0;
		}

		public void apply() {
			final Map<String, IType> variableTypes = new HashMap<>();
			final Map<String, Function.Typing> functionTypings = new HashMap<>();
			fillTypingMaps(variableTypes, functionTypings);
			if (partial)
				script.typings().update(variableTypes, functionTypings);
			else
				script.setTypings(new Script.Typings(variableTypes, functionTypings));
			for (final TypeVariable tv : typeEnvironment.values())
				if (tv.declaration().containedIn(script))
					tv.apply(false);
		}

		private void fillTypingMaps(final Map<String, IType> variableTypes, final Map<String, Function.Typing> functionTypings) {
			if (typing == Typing.INFERRED)
				if (!partial)
					for (final Script s : script.conglomerate())
						for (final Variable v : s.variables()) {
							final TypeVariable tyVar = typeEnvironment.get(v);
							if (tyVar != null)
								variableTypes.put(v.name(), tyVar.get());
						}
			for (final Visit entry : visits.values())
				putFunctionTyping(functionTypings, entry);
		}

		private void putFunctionTyping(final Map<String, Function.Typing> functionTypings, final Visit visit) {
			final Function fun = visit.function;
			final TypeVariable retTy = typeEnvironment.get(fun);
			IType[] parameterTypes;
			if (erroneous && typing == Typing.INFERRED) {
				final Function.Typing oldTyping = script.typings().get(fun);
				parameterTypes = oldTyping != null ? oldTyping.parameterTypes : new IType[0];
			}
			else {
				parameterTypes = new IType[fun.numParameters()];
				final List<Variable> parms = fun.parameters();
				for (int i = 0; i < parms.size(); i++) {
					final Variable p = parms.get(i);
					final TypeVariable parTy = typeEnvironment.get(p);
					parameterTypes[i] = parTy != null ? parTy.get() : p.type();
				}
			}
			functionTypings.put(fun.name(),
				new Function.Typing(
					parameterTypes,
					retTy != null ? retTy.get() : fun.returnType(),
					visit.inferredTypes
				)
			);
		}

		@Override
		public String toString() { return script.name(); }
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
	 * @param <T> Type of node the class has expertise for
	 */
	class Expert<T extends ASTNode> extends PerClass<ASTNode, T, Expert<? super T>> {
		boolean providesInherentType = false;

		public Expert(final Class<T> cls) { super(cls); }
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
		public void visit(final T node, final Visitor visitor) throws ProblemException {}
		public IType type(final T node, final Visitor visitor) { return PrimitiveType.UNKNOWN; }
		public Declaration typeEnvironmentKey(final T node, final Visitor visitor) { return null; }
		public TypeVariable createTypeVariable(final T node, final Visitor visitor) { return null; }
		public boolean isModifiable(final T node, final Visitor visitor) { return true; }

		@Override
		public String toString() { return String.format("Expert<%s>", cls.getSimpleName()); } //$NON-NLS-1$

		public final IType predecessorType(final ASTNode node, final Visitor visitor) {
			final ASTNode p = node.predecessor();
			return p != null ? visitor.ty(p) : null;
		}

		/**
		 * Return whether this expression is valid as a value of the specified type.
		 * @param type The type to test against
		 * @param context Script parser context
		 * @return True if valid, false if not.
		 */
		public final IType unifyDeclaredAndGiven(final ASTNode node, final IType type, final Visitor visitor) {
			final IType myType = visitor.ty(node);
			if (type == null)
				return myType;
			return typing.unifyNoChoice(type, myType);
		}

		public TypeVariable findTypeVariable(final T node, final Visitor visitor) {
			final Declaration key = typeEnvironmentKey(node, visitor);
			return findTypeVariable(key, visitor);
		}

		/**
		 * Query the type variable of an arbitrary expression. With some luck the inference engine will be able to give an answer.
		 * @param node the expression to query the type of
		 * @return The {@link TypeVariable} or null if nothing was found
		 */
		public final TypeVariable findTypeVariable(final Declaration key, final Visitor visitor) {
			if (key == null)
				return null;
			for (TypeEnvironment e = visitor.environment; e != null; e = e.up) {
				final TypeVariable tv = e.get(key);
				if (tv != null)
					return tv;
			}
			return null;
		}

		public TypeVariable findParameterTypeVariable(final Variable parameter, final Visitor visitor) {
			return findTypeVariable(parameter, visitor);
		}

		/**
		 * Requests type information for an expression
		 * @param expression the expression
		 * @return the type information or null if none has been stored
		 */
		public final TypeVariable requestTypeVariable(final T node, final Visitor visitor) {
			switch (typing) {
			case STATIC: case DYNAMIC:
				return null;
			default:
				break;
			}

			final TypeEnvironment env = visitor.environment;
			if (env == null)
				return null;
			final Declaration key = typeEnvironmentKey(node, visitor);
			if (key == null)
				return null;

			boolean topMostLayer = true;
			TypeVariable base = null;
			for (TypeEnvironment list = env; list != null; list = list.up) {
				final TypeVariable tyvar = list.get(key);
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
				else if (visitor.input().partial)
					if (key instanceof Variable)
						newtyvar.set(((Variable)key).type(visitor.script()));
				env.add(newtyvar);
			}
			return newtyvar;
		}

		public boolean judgment(final T node, final IType type, final ASTNode origin, final Visitor visitor, final TypingJudgementMode mode) {
			if (DEBUG)
				visitor.warning(visitor, Problem.TypingJudgment, node, node, Markers.NO_THROW, node.printed(), type.typeName(true));
			final TypeVariable tyvar = requestTypeVariable(node, visitor);
			if (tyvar != null) {
				switch (mode) {
				case OVERWRITE:
					tyvar.set(type);
					break;
				case UNIFY:
					tyvar.set(typing.unify(tyvar.get(), type));
					break;
				}
				return true;
			} else
				return false;
		}

		public final void assignment(final T leftSide, final ASTNode rightSide, final Visitor visitor) {
			switch (typing) {
			case STATIC:
				final IType leftTy = visitor.ty(leftSide, this);
				final IType rightTy = visitor.ty(rightSide);
				if (!typing.compatible(leftTy, rightTy))
					visitor.incompatibleTypesMarker(rightSide, rightSide, leftTy, rightTy);
				break;
			case INFERRED:
				visitor.judgment(leftSide, visitor.ty(rightSide), rightSide, TypingJudgementMode.OVERWRITE);
				break;
			case DYNAMIC:
				break;
			}
		}
	}

	private final Expert<ASTNode> MASTER_OF_NONE = new Expert<ASTNode>(ASTNode.class) {
		@Override
		public IType type(final ASTNode node, final Visitor visitor) { return PrimitiveType.UNKNOWN; }
	};

	private final <T extends ASTNode> Expert<? super T> findExpert(final T node) {
		for (Class<?> cls = node.getClass(); cls != null; cls = cls.getSuperclass()) {
			@SuppressWarnings("unchecked")
			final
			Expert<? super T> expert = (Expert<? super T>)committee.get(cls);
			if (expert != null)
				return expert;
		}
		return MASTER_OF_NONE;
	}

	private final Map<Class<? extends ASTNode>, Expert<? extends ASTNode>> committee = new HashMap<Class<? extends ASTNode>, Expert<?>>();

	class AccessDeclarationExpert<T extends AccessDeclaration> extends Expert<T> {
		public AccessDeclarationExpert(final Class<T> cls) { super(cls); }
		protected Declaration obtainDeclaration(final T node, final Visitor visitor) { return null; }
		@Override
		public void visit(final T node, final Visitor visitor) throws ProblemException {
			super.visit(node, visitor);
			internalObtainDeclaration(node, visitor);
		}
		protected final Declaration internalObtainDeclaration(final T node, final Visitor visitor) {
			Declaration d = declaration(node, visitor);
			if (d == null)
				d = obtainDeclaration(node, visitor);
			if (d == null) {
				visitor.script().index().loadScriptsContainingDeclarationsNamed(node.name());
				d = obtainDeclaration(node, visitor);
			}
			setDeclaration(node, visitor, d);
			if (!visitor.preliminary && visitor.script() == node.parent(Script.class))
				node.setDeclaration(d);
			return d;
		}
		final void setDeclaration(final T node, final Visitor visitor, final Declaration d) {
			final int x = node.localIdentifier();
			if (x != -1)
				visitor.visit.declarations[x] = d;
		}
		final Declaration declaration(final T node, final Visitor visitor) {
			final int x = node.localIdentifier();
			return x > -1 ? visitor.visit.declarations[x] : null;
		}
		@Override
		public TypeVariable createTypeVariable(final T node, final Visitor visitor) {
			final Declaration d = declaration(node, visitor);
			return d instanceof ITypeable && ((ITypeable)d).staticallyTyped() ? null : super.createTypeVariable(node, visitor);
		}
		@Override
		public IType type(final T node, final Visitor visitor) {
			final TypeVariable tyvar = findTypeVariable(node, visitor);
			return tyvar != null ? tyvar.get() : PrimitiveType.UNKNOWN;
		}
		@Override
		public Declaration typeEnvironmentKey(final T node, final Visitor visitor) {
			return internalObtainDeclaration(node, visitor);
		}
	}

	private synchronized void assembleCommittee() {

		class AccessVarExpert<T extends AccessVar> extends AccessDeclarationExpert<T> {
			private AccessVarExpert(final Class<T> cls) { super(cls); }
			private Declaration findUsingType(final Visitor visitor, final AccessVar node, final ASTNode predecessor, final IType type) {
				if (type == null)
					return null;
				for (final IType t : type) {
					final Script scriptToLookIn = as(t, Script.class);
					if (scriptToLookIn != null) {
						final FindDeclarationInfo info = new FindDeclarationInfo(node.name(), visitor.script().index());
						info.searchOrigin(scriptToLookIn);
						info.findGlobalVariables = predecessor == null;
						Declaration v = scriptToLookIn.findDeclaration(info);
						if (v instanceof Definition)
							v = ((Definition)v).proxyVar();
						return v;
					} else
						// find pseudo-variable from proplist expression
						if (t instanceof IProplistDeclaration) {
							final Variable proplistComponent = ((IProplistDeclaration)t).findComponent(node.name());
							if (proplistComponent != null)
								return proplistComponent;
						}
				}
				return null;
			}

			@Override
			protected Declaration obtainDeclaration(final AccessVar node, final Visitor visitor) {
				final ASTNode p = node.predecessor();
				if (p == null) {
					final Function f = node.parent(Function.class);
					if (f != null) {
						final Variable v = f.findVariable(node.name());
						if (v != null)
							return v;
					}
				}
				return findUsingType(visitor, node, p, p != null ? visitor.ty(p) : visitor.script());
			}

			@Override
			public IType type(final T node, final Visitor visitor) {
				final Declaration d = internalObtainDeclaration(node, visitor);
				//if (d instanceof Variable && ((Variable)d).staticallyTyped())
				//	return ((Variable)d).type();
				if (d instanceof Function)
					return new FunctionType((Function)d);
				final TypeVariable stored = findTypeVariable(node, visitor);
				if (stored != null)
					return stored.get();
				else if (d instanceof Variable) {
					final Variable v = (Variable)d;
					switch (v.scope()) {
					case LOCAL:
						return unifyVariableTypesForAllPredecessorTypes(node, visitor, d);
					default:
						return v.type();
					}
				}
				else if (d instanceof ITypeable)
					return ((ITypeable) d).type();
				return PrimitiveType.UNKNOWN;
			}
			private IType unifyVariableTypesForAllPredecessorTypes(final T node, final Visitor visitor, final Declaration d) {
				IType t = PrimitiveType.UNKNOWN;
				if (node.predecessor() != null) {
					final IType predTy = visitor.ty(node.predecessor());
					if (predTy != null)
						for (final IType _t : predTy)
							if (_t instanceof Script) {
								final IType frt = ((Variable)d).type((Script)_t);
								t = typing.unify(t, frt);
							}
				}
				return t != PrimitiveType.UNKNOWN ? t : ((Variable)d).type(visitor.script());
			}

			@Override
			public void visit(final T node, final Visitor visitor) throws ProblemException {
				super.visit(node, visitor);
				final ASTNode pred = node.predecessor();
				final Declaration declaration = internalObtainDeclaration(node, visitor);
				if (declaration == null && pred == null)
					visitor.markers().error(visitor, Problem.UndeclaredIdentifier, node, node, Markers.NO_THROW, node.name());
				// local variable used in global function
				else if (declaration instanceof Variable) {
					final Variable var = (Variable) declaration;
					handleVariable(node, visitor, pred, var);
				} else if (declaration instanceof Function) {
					if (!visitor.script().engine().settings().supportsFunctionRefs)
						visitor.markers().error(visitor, Problem.FunctionRefNotAllowed, node, node, Markers.NO_THROW, visitor.script().engine().name());
					if (pred instanceof MemberOperator && !((MemberOperator)pred).dotNotation())
						visitor.markers().error(visitor, Problem.FunctionRefAfterArrow, node, node, Markers.NO_THROW, node.name());
				}
			}
			private void handleVariable(final T node, final Visitor visitor, final ASTNode pred, final Variable var) throws ProblemException {
				var.setUsed(true);
				switch (var.scope()) {
				case LOCAL:
					handleField(node, visitor, pred);
					break;
				case STATIC: case CONST:
					handleStatic(visitor, var);
					break;
				case VAR:
					handleFunctionLocal(node, visitor, var);
					break;
				case PARAMETER:
					break;
				}
			}
			private void handleFunctionLocal(final T node, final Visitor visitor, final Variable var) {
				final Function currentFunction = node.parent(Function.class);
				if (currentFunction != null && var.parentDeclaration() == currentFunction) {
					final int locationUsed = currentFunction.bodyLocation().getOffset()+node.start();
					if (locationUsed < var.start())
						visitor.markers().warning(visitor, Problem.VarUsedBeforeItsDeclaration, node, node, 0, var.name());
				}
			}
			private void handleStatic(final Visitor visitor, final Variable var) {
				visitor.script().addUsedScript(var.script());
			}
			private void handleField(final T node, final Visitor visitor, final ASTNode pred) throws ProblemException {
				final Declaration d = node.parent(Declaration.class);
				if (d != null && pred == null) {
					final Function f = d.topLevelParent(Function.class);
					final Variable v = d.topLevelParent(Variable.class);
					if (
						(f != null && f.visibility() == FunctionScope.GLOBAL) ||
						(f == null && v != null && v.scope() != Scope.LOCAL)
					)
						visitor.markers().error(visitor, Problem.LocalUsedInGlobal, node, node, Markers.NO_THROW);
				}
			}

			@Override
			public boolean judgment(final T node, final IType type, final ASTNode origin, final Visitor visitor, final TypingJudgementMode mode) {
				final Declaration declaration = internalObtainDeclaration(node, visitor);
				if (declaration == null && origin != null) {
					final IType predType = predecessorType(node, visitor);
					if (predType != null) {
						boolean typeAsProplist = true;
						for (final IType t : predType)
							if (t == visitor.script() && visitor.script() == node.parent(Script.class)) {
								addFieldToScript(node, type, origin, visitor);
								typeAsProplist = false;
							}
							else if (t instanceof Declaration && t instanceof IProplistDeclaration) {
								final IProplistDeclaration proplDecl = (IProplistDeclaration) t;
								final Declaration d = (Declaration) t;
								if (d.parent(IndexEntity.class) == visitor.script())
									addComponentToProplist(node, type, origin, visitor, proplDecl);
								else if (DEBUG)
									visitor.log("Won't add '%s' to '%s'", node.name(), d.qualifiedName()); //$NON-NLS-1$
								typeAsProplist = false;
							}
							else if (t instanceof CallTargetType)
								/*
								 * steps->|cursor|
								 *
								 * asd = 123;
								 *
								 * Prevent typing steps as proplist {asd}
								 */
								typeAsProplist = false;
						if (typeAsProplist)
							typeAsProplist(node, type, origin, visitor);
					}
				}
				return super.judgment(node, type, origin, visitor, mode);
			}

			private void addFieldToScript(final T node, final IType type, final ASTNode origin, final Visitor visitor) {
				final Variable var = new Variable(Variable.Scope.LOCAL, node.name());
				initializeFromAssignment(node, type, origin, visitor, var);
				visitor.script().addDeclaration(var);
				node.setDeclaration(var);
			}

			private void addComponentToProplist(final T node, final IType type, final ASTNode origin, final Visitor visitor, final IProplistDeclaration proplDecl) {
				final Variable var = proplDecl.addComponent(new Variable(Variable.Scope.VAR, node.name()), true);
				var.setLocation(node.absolute());
				node.setDeclaration(var);
				initializeFromAssignment(node, type, origin, visitor, var);
			}

			private void typeAsProplist(final T node, final IType type, final ASTNode origin, final Visitor visitor) {
				final ProplistDeclaration proplDecl = new ProplistDeclaration(new ArrayList<Variable>());
				final Variable var = proplDecl.addComponent(new Variable(Variable.Scope.VAR, node.name()), true);
				proplDecl.setParent(visitor.script());
				var.setLocation(node);
				node.setDeclaration(var);
				initializeFromAssignment(node, type, origin, visitor, var);
				visitor.judgment(node.predecessor(), proplDecl, TypingJudgementMode.UNIFY);
				visitor.script().addDeclaration(proplDecl);
			}

			private void initializeFromAssignment(final T node, final IType type, final ASTNode origin, final Visitor visitor, final Variable var) {
				var.setLocation(visitor.absoluteSourceLocationFromExpr(node));
				var.forceType(type);
				var.setInitializationExpression(origin);
			}

			@Override
			public TypeVariable createTypeVariable(final T node, final Visitor visitor) {
				final Declaration dec = internalObtainDeclaration(node, visitor);
				return dec instanceof Variable && !(dec instanceof ProxyVar)
					? new VariableTypeVariable((Variable) dec) : null;
			}

			@Override
			public boolean isModifiable(final T node, final Visitor visitor) {
				final Declaration declaration = declaration(node, visitor);
				final ASTNode pred = node.predecessor();
				if (pred == null)
					return declaration == null || ((Variable)declaration).scope() != Scope.CONST;
				else
					return true; // you can never be so sure
			}
		}

		class LiteralExpert<T extends Literal<?>> extends Expert<T> {
			{ providesInherentType = true; }
			public LiteralExpert(final Class<T> cls) { super(cls); }
			@Override
			public boolean judgment(final T node, final IType type, final ASTNode origin, final Visitor visitor, final TypingJudgementMode mode) {
				// constantly steadfast do i resist the pressure of expectancy lied upon me
				return true;
			}
			@Override
			public boolean isModifiable(final T node, final Visitor visitor) { return false; }
		}

		class ConditionalStatementExpert<T extends ConditionalStatement> extends Expert<T> {
			public ConditionalStatementExpert(final Class<T> cls) { super(cls); }
			@Override
			public boolean skipReportingProblemsForSubElements() {return true;}
			@Override
			public void visit(final ConditionalStatement node, final Visitor visitor) throws ProblemException {
				final ControlFlow t = visitor.controlFlow;
				visitor.controlFlow = ControlFlow.Continue;
				visitor.newTypeEnvironment();
				visitor.visit(node.condition(), true);
				visitor.endTypeEnvironment();
				visitor.newTypeEnvironment();
				visitor.visit(node.body(), true);
				visitor.endTypeEnvironment();
				loopConditionWarnings(node, visitor);
				visitor.controlFlow = t;
			}
			/**
			 * Emit warnings about loop conditions that could result in loops never executing or never ending.
			 * @param body The loop body. If the condition looks like it will always be true, checks are performed whether the body contains loop control flow statements.
			 * @param condition The loop condition to check
			 */
			protected void loopConditionWarnings(final ConditionalStatement node, final Visitor visitor) {
				final ASTNode condition = node.condition();
				if (node.body() == null || condition == null || !(node instanceof ILoop))
					return;
				if (node.body() instanceof EmptyStatement && !condition.hasSideEffects())
					visitor.markers().warning(visitor, Problem.EmptyBody, node.body(), node.body(), Markers.NO_THROW, condition);
				final Object condEv = PrimitiveType.BOOL.convert(condition == null ? true : condition.evaluateStatic(node.parent(Function.class)));
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
				public boolean judgment(final VarInitializationAccess node, final IType type, final ASTNode origin, final Visitor visitor, final TypingJudgementMode mode) {
					super.judgment(node, type, origin, visitor, mode);
					if (origin != null) {
						final Declaration d = declaration(node, visitor);
						if (d instanceof Variable && ((Variable)d).scope() == Scope.CONST && !origin.isConstant())
							try {
								visitor.markers().error(visitor, Problem.NonConstGlobalVarAssignment, origin, origin, Markers.NO_THROW);
							} catch (final ProblemException e) { }
					}
					return true;
				}
				@Override
				public boolean isModifiable(final VarInitializationAccess node, final Visitor visitor) { return true; /* sudo */ }
			},

			new AccessVarExpert<TempAccessVar>(TempAccessVar.class) {
				@Override
				protected Declaration obtainDeclaration(final AccessVar node, final Visitor visitor) { return node.declaration(); }
			},

			new Expert<ArrayExpression>(ArrayExpression.class) {
				@Override
				public IType type(final ArrayExpression node, final Visitor visitor) {
					IType elmType = PrimitiveType.UNKNOWN;
					for (final ASTNode e : node.subElements())
						if (e != null)
							elmType = typing.unify(elmType, visitor.ty(e));
					return new ArrayType(elmType);
				}
				@Override
				public boolean isModifiable(final ArrayExpression node, final Visitor visitor) { return false; }
			},

			new Expert<ArrayElementExpression>(ArrayElementExpression.class) {
				@Override
				public IType type(final ArrayElementExpression node, final Visitor visitor) {
					final IType t = supr.type(node, visitor);
					if (t != PrimitiveType.UNKNOWN && t != PrimitiveType.ANY)
						return t;
					final ASTNode pred = node.predecessor();
					if (pred != null) {
						final IType predTy = visitor.ty(pred);
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
				public boolean judgment(final ArrayElementExpression leftSide, final IType rightSideType, final ASTNode origin, final Visitor visitor, final TypingJudgementMode mode) {
					final IType predType_ = predecessorType(leftSide, visitor);
					final ASTNode pred = leftSide.predecessor();
					if (predType_ == null)
						return false;
					if (origin != null) {
						final IType elmTy = visitor.ty(leftSide.argument());
						if (elmTy != null)
							for (final IType e : elmTy)
								if (eq(e, PrimitiveType.STRING))
									return visitor.judgment(pred, PrimitiveType.PROPLIST, mode);
						return visitor.judgment(pred, new ArrayType(rightSideType), TypingJudgementMode.UNIFY);
					}
					return true;
				}
				@Override
				public void visit(final ArrayElementExpression node, final Visitor visitor) throws ProblemException {
					supr.visit(node, visitor);
					IType type = predecessorType(node, visitor);
					if (type == null)
						type = PrimitiveType.UNKNOWN;
					final ASTNode arg = node.argument();
					if (arg == null)
						visitor.markers().warning(visitor, Problem.MissingExpression, node, node, 0);
					else {
						final IType _argType = visitor.ty(arg);
						final ASTNode pred = node.predecessor();
						for (final IType argType : _argType)
							if (eq(argType, PrimitiveType.STRING)) {
								if (typing.unifyNoChoice(PrimitiveType.PROPLIST, type) == null)
									visitor.markers().warning(visitor, Problem.NotAProplist, node, pred, 0);
								else
									visitor.judgment(pred, PrimitiveType.PROPLIST, TypingJudgementMode.UNIFY);
							} else {
								final IType u = typing.unifyNoChoice(PrimitiveType.ARRAY, type);
								if (eq(argType, PrimitiveType.INT))
									if (u == null)
										visitor.markers().warning(visitor, Problem.NotAnArrayOrProplist, node, pred, 0);
									else
										visitor.judgment(pred, u, TypingJudgementMode.UNIFY);
							}
					}
				}
				@Override
				public boolean isModifiable(final ArrayElementExpression node, final Visitor visitor) { return true; }
			},

			new Expert<ArraySliceExpression>(ArraySliceExpression.class) {
				private void warnIfNotArray(final ASTNode node, final Visitor visitor, final IType type) {
					if (type != null && type != PrimitiveType.UNKNOWN && type != PrimitiveType.ANY &&
						typing.unifyNoChoice(PrimitiveType.ARRAY, type) == null &&
						typing.unifyNoChoice(PrimitiveType.PROPLIST, type) == null)
						visitor.markers().warning(visitor, Problem.NotAnArrayOrProplist, node, node, 0);
				}
				@Override
				public void visit(final ArraySliceExpression node, final Visitor visitor) throws ProblemException {
					supr.visit(node, visitor);
					final IType type = predecessorType(node, visitor);
					warnIfNotArray(node.predecessor(), visitor, type);
				}
				@Override
				public boolean isModifiable(final ArraySliceExpression node, final Visitor visitor) { return false; }
			},

			new Expert<OperatorExpression>(OperatorExpression.class) {
				@Override
				public IType type(final OperatorExpression node, final Visitor visitor) {
					return node.operator().returnType();
				}
				@Override
				public boolean isModifiable(final OperatorExpression node, final Visitor visitor) { return node.operator().returnsRef(); }
			},

			new Expert<BinaryOp>(BinaryOp.class) {
				@Override
				public IType type(final BinaryOp node, final Visitor visitor) {
					switch (node.operator()) {
					// &&/|| special: they return either the left or right side of the operator so the return type is the lowest common denominator of the argument types
					case And: case Or: case JumpNotNil:
						final IType leftSideType = visitor.ty(node.leftSide());
						final IType rightSideType = visitor.ty(node.rightSide());
						if (leftSideType == rightSideType)
							return leftSideType;
						else
							return typing.unify(leftSideType, rightSideType);
					case Assign:
						return visitor.ty(node.rightSide());
					default:
						return supr.type(node, visitor);
					}
				}
				@Override
				public boolean skipReportingProblemsForSubElements() { return true; }
				@Override
				public void visit(final BinaryOp node, final Visitor visitor) throws ProblemException {
					final Operator op = node.operator();
					final ASTNode left = node.leftSide();
					final ASTNode right = node.rightSide();

					// right first
					visitor.visit(right, true);
					visitor.visit(left, true);

					detectProblems(node, visitor, op, left, right);

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
					case Assign:
						if (left instanceof AccessVar && right instanceof AccessVar && visitor.declarationOf(left) == visitor.declarationOf(right))
							if (visitor.declarationOf(left) != null)
								visitor.warning(visitor, Problem.NoopAssignment, left, left, Markers.NO_THROW, left.printed());
						//$FALL-THROUGH$
					case AssignAdd:
					case AssignSubtract:
					case AssignMultiply:
					case AssignModulo:
					case AssignDivide:
						visitor.expert(left).assignment(left, right, visitor);
						break;
					case JumpNotNil:
						expectedLeft = visitor.ty(right);
						expectedRight = null;
						break;
					default:
						break;
					}

					if (expectedLeft != null)
						visitor.judgment(left, expectedLeft, TypingJudgementMode.UNIFY);
					if (expectedRight != null)
						visitor.judgment(right, expectedRight, TypingJudgementMode.UNIFY);
				}
				private void detectProblems(final BinaryOp node, final Visitor visitor, final Operator op, final ASTNode left, final ASTNode right) throws ProblemException {
					// i'm an assignment operator and i can't modify my left side :C
					if (op.modifiesArgument() && !visitor.expert(left).isModifiable(left, visitor))
						visitor.markers().error(visitor, Problem.ExpressionNotModifiable, node, left, Markers.NO_THROW);
					// obsolete operators in #strict 2 import
					if ((op == Operator.StringEqual || op == Operator.ne) && (visitor.input().strictLevel >= 2))
						visitor.markers().warning(visitor, Problem.ObsoleteOperator, node, node, 0, op.operatorName());
					// wrong parameter types
					if (unifyDeclaredAndGiven(left, op.firstArgType(), visitor) == null)
						visitor.incompatibleTypesMarker(node, left, op.firstArgType(), visitor.ty(left));
					if (unifyDeclaredAndGiven(right, op.secondArgType(), visitor) == null)
						visitor.incompatibleTypesMarker(node, right, op.secondArgType(), visitor.ty(right));
				}
				@Override
				public TypeVariable createTypeVariable(final BinaryOp node, final Visitor visitor) {
					final ASTNode leftSide = node.leftSide();
					if (node.operator().isAssignment() && leftSide != null)
						return visitor.expert(leftSide).createTypeVariable(leftSide, visitor);
					return super.createTypeVariable(node, visitor);
				}
				@Override
				public Declaration typeEnvironmentKey(final BinaryOp node, final Visitor visitor) {
					final ASTNode leftSide = node.leftSide();
					if (node.operator().isAssignment() && leftSide != null)
						return visitor.expert(leftSide).typeEnvironmentKey(leftSide, visitor);
					else
						return null;
				}
				@Override
				public boolean isModifiable(final BinaryOp node, final Visitor visitor) { return node.operator().returnsRef(); }
			},

			new Expert<UnaryOp>(UnaryOp.class) {
				@Override
				public IType type(final UnaryOp node, final Visitor visitor) { return node.operator().returnType(); }
				@Override
				public void visit(final UnaryOp node, final Visitor visitor) throws ProblemException {
					supr.visit(node, visitor);
					final ASTNode arg = node.argument();
					if (node.operator().modifiesArgument() && !visitor.expert(arg).isModifiable(arg, visitor))
						visitor.markers().error(visitor, Problem.ExpressionNotModifiable, node, arg, Markers.NO_THROW);
					final Expert<? super ASTNode> xprt = visitor.expert(arg);
					final PrimitiveType firstArgType = node.operator().firstArgType();
					if (xprt.unifyDeclaredAndGiven(arg, firstArgType, visitor) == null)
						visitor.incompatibleTypesMarker(node, arg, firstArgType, visitor.ty(arg, xprt));
					if (firstArgType != PrimitiveType.UNKNOWN)
						xprt.judgment(arg, firstArgType, node, visitor, TypingJudgementMode.UNIFY);
				}
				@Override
				public boolean isModifiable(final UnaryOp node, final Visitor visitor) {
					return node.placement() == Placement.Prefix && node.operator().returnsRef();
				}
			},

			new Expert<BoolLiteral>(BoolLiteral.class) {
				@Override
				public void visit(final BoolLiteral node, final Visitor visitor) throws ProblemException {
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
				public void visit(final ContinueStatement node, final Visitor visitor) throws ProblemException {
					if (node.parent(ILoop.class) == null)
						visitor.markers().error(visitor, Problem.KeywordInWrongPlace, node, node, Markers.NO_THROW, node.keyword());
					supr.visit(node, visitor);
				}
			},

			new Expert<BreakStatement>(BreakStatement.class) {
				@Override
				public void visit(final BreakStatement node, final Visitor visitor) throws ProblemException {
					if (node.parent(ILoop.class) == null)
						visitor.markers().error(visitor, Problem.KeywordInWrongPlace, node, node, Markers.NO_THROW, node.keyword());
					supr.visit(node, visitor);
				}
			},

			new Expert<ReturnStatement>(ReturnStatement.class) {
				private void warnAboutTupleInReturnExpr(final Visitor visitor, final ASTNode node, final boolean tupleIsError) throws ProblemException {
					if (node == null)
						return;
					if (node instanceof Tuple)
						if (tupleIsError)
							visitor.markers().error(visitor, Problem.TuplesNotAllowed, node, node, Markers.NO_THROW);
						else if (visitor.input().strictLevel >= 2)
							visitor.markers().error(visitor, Problem.ReturnAsFunction, node, node, Markers.NO_THROW);
					final ASTNode[] subElms = node.subElements();
					for (final ASTNode e : subElms)
						warnAboutTupleInReturnExpr(visitor, e, true);
				}
				@Override
				public void visit(final ReturnStatement node, final Visitor visitor) throws ProblemException {
					supr.visit(node, visitor);
					final ASTNode returnExpr = node.returnExpr();
					warnAboutTupleInReturnExpr(visitor, returnExpr, false);
					final Function currentFunction = node.parent(Function.class);
					if (currentFunction == null)
						visitor.markers().error(visitor, Problem.NotAllowedHere, node, node, Markers.NO_THROW, Keywords.Return);
					else if (returnExpr != null)
						if (typing == Typing.STATIC && currentFunction.staticallyTyped()) {
							if (visitor.expert(returnExpr).unifyDeclaredAndGiven(returnExpr, currentFunction.returnType(), visitor) == null)
								visitor.incompatibleTypesMarker(node,
									returnExpr, currentFunction.returnType(), visitor.ty(returnExpr));
						}
						else {
							final IType type = visitor.ty(returnExpr);
							visitor.judgment(node, type, TypingJudgementMode.UNIFY);
						}
				}
				@Override
				public TypeVariable createTypeVariable(final ReturnStatement node, final Visitor visitor) {
					return new FunctionReturnTypeVariable(node.parent(Function.class));
				}
				@Override
				public Declaration typeEnvironmentKey(final ReturnStatement node, final Visitor visitor) {
					return node.parent(Function.class);
				}
			},

			new AccessDeclarationExpert<CallDeclaration>(CallDeclaration.class) {
				/**
				 * Find a {@link Function} for some hypothetical {@link CallDeclaration}, using contextual information such as the {@link ASTNode#type(ProblemReporter)} of the {@link ASTNode} preceding this {@link CallDeclaration} in the {@link Sequence}.
				 * @param input Context to use for searching
				 * @param functionName Name of the function to look for. Would correspond to the hypothetical {@link CallDeclaration}'s {@link #name()}
				 * @param pred The predecessor of the hypothetical {@link CallDeclaration} ({@link ASTNode#predecessor()})
				 * @param listToAddPotentialDeclarationsTo When supplying a non-null value to this parameter, potential declarations will be added to the collection. Such potential declarations would be obtained by querying the {@link Index}'s {@link Index#declarationMap()}.
				 * @return The {@link Function} that is very likely to be the one actually intended to be referenced by the hypothetical {@link CallDeclaration}.
				 */
				private Declaration findUsingType(
					final Visitor visitor,
					final CallDeclaration node,
					final IType type
				) {
					final String functionName = node.name();
					final IType lookIn = type != null ? type : visitor.script();
					if (lookIn != null) for (final IType ty : lookIn) {
						Script script = as(ty, Script.class);
						if (script == null && ty instanceof MetaDefinition)
							script = ((MetaDefinition)ty).definition();
						if (script == null)
							continue;
						script.requireLoaded();
						final FindDeclarationInfo info = new FindDeclarationInfo(functionName, visitor.script().index());
						info.searchOrigin(visitor.script());
						info.contextFunction = node.parent(Function.class);
						info.findGlobalVariables = type == null;
						final Declaration dec = script.findDeclaration(info);
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

					// find engine function
					final Function global = visitor.script().index().engine().findFunction(functionName);
					if (global != null) {
						final int numCandidates = globalCompetitors(visitor, global);
						// only return found global function if it's the only choice
						if (numCandidates == 0)
							return global;
					} else {
						final Declaration indexGlobal = visitor.script().index().findGlobal(Declaration.class, functionName);
						if (indexGlobal != null)
							return indexGlobal;
					}
					return null;
				}
				private int globalCompetitors(final Visitor visitor, final Function global) {
					int num = 0;
					final List<Declaration> allFromLocalIndex = visitor.script().index().declarationMap().get(global.name());
					if (allFromLocalIndex != null)
						for (final Declaration d : allFromLocalIndex)
							if (d instanceof Function && ((Function)d).baseFunction() != global)
								num++;
					return num;
				}
				@Override
				protected Declaration obtainDeclaration(final CallDeclaration node, final Visitor visitor) {
					final String declarationName = node.name();
					if (declarationName.equals(Keywords.Return))
						return null;
					final ASTNode p = node.predecessor();
					return findUsingType(visitor, node, p != null ? visitor.ty(p) : visitor.script());
				}
				private IType declarationType(final CallDeclaration node, final Visitor visitor) {
					final Declaration d = internalObtainDeclaration(node, visitor);

					// look for gathered type information
					final TypeVariable tyvar = findTypeVariable(node, visitor);
					if (tyvar != null)
						return tyvar.get();

					// calling this() as function -> return object type belonging to script
					if (node.params().length == 0 && d != null && (d == visitor.cachedEngineDeclarations().This))
						return node.parent(Function.class).isGlobal() ? CallTargetType.INSTANCE : visitor.input().thisType;

					if (d instanceof Function) {
						final Function fn = (Function) d;
						// Some special rule applies and the return type is set accordingly
						final SpecialFuncRule rule = visitor.script().engine().specialRules().funcRuleFor(node.name(), SpecialEngineRules.RETURNTYPE_MODIFIER);
						if (rule != null) {
							final IType type = rule.returnType(visitor, node);
							if (type != null)
								return type;
						}
						return returnTypeFromPredecessorType(node, visitor, fn);
					}
					if (d instanceof Variable) {
						final Variable v = (Variable)d;
						switch (v.scope()) {
						case LOCAL:
							final IType t = node.predecessor() == null
								? visitor.script().typings().variableTypes.get(v.name())
								: null;
							return t != null ? t : v.type();
						default:
							return v.type();
						}
					}
					return supr != null ? supr.type(node, visitor) : PrimitiveType.UNKNOWN;
				}
				private IType returnTypeFromPredecessorType(final CallDeclaration node, final Visitor visitor, final Function fn) {
					IType t = PrimitiveType.UNKNOWN;
					if (node.predecessor() != null) {
						final IType predTy = visitor.ty(node.predecessor());
						if (predTy != null)
							for (final IType _t : predTy)
								if (_t instanceof Script) {
									final Script _s = (Script)_t;
									final TypeVariable rtv = visitFor(_s.override(fn), _s);
									t = typing.unify(t, rtv != null ? rtv.get() : fn.returnType(_s));
								}
					} else {
						final Visit v = visitFor(fn, visitor.script());
						if (v != null)
							t = v.get();
					}
					return t != PrimitiveType.UNKNOWN ? t : fn.returnType(visitor.script());
				}
				private IType unknownFunctionShouldBeError(final CallDeclaration node, final Visitor visitor) {
					ASTNode pred = node.predecessor();
					// stand-alone function? always bark!
					if (pred == null)
						return visitor.script();
					// not typed? weird
					final IType predType = visitor.ty(pred);
					if (predType == null)
						return null;
					// called via ~? ok
					if (pred instanceof MemberOperator)
						if (((MemberOperator)pred).hasTilde())
							return null;
						else
							pred = pred.predecessor();
					// allow this->Unknown()
					if (pred instanceof This)
						return null;
					if (predType instanceof Definition)
						return predType;
					boolean allScripts = true;
					for (final IType t : predType)
						if (!(t instanceof Definition)) {
							allScripts = false;
							break;
						}
					return allScripts ? predType : null;
				}
				@Override
				public IType type(final CallDeclaration node, final Visitor visitor) {
					final IType type = declarationType(node, visitor);
					if (type instanceof FunctionType)
						return ((FunctionType)type).prototype().returnType();
					else
						return type;
				}
				@Override
				public boolean skipReportingProblemsForSubElements() { return true; }
				@Override
				public void visit(final CallDeclaration node, final Visitor visitor) throws ProblemException {

					// call ty() for parameter nodes to store ensure types are stored in inferredType
					for (final ASTNode e : node.params())
						if (e != null) {
							visitor.visit(e, true);
							visitor.ty(e);
						}

					super.visit(node, visitor);

					final CachedEngineDeclarations cachedEngineDeclarations = visitor.cachedEngineDeclarations();
					final String declarationName = node.name();
					final Declaration declaration = internalObtainDeclaration(node, visitor);
					final ASTNode[] params = node.params();
					final ASTNode predecessor = node.predecessor();

					if (predecessor instanceof MemberOperator && ((MemberOperator)predecessor).dotNotation())
						visitor.error(visitor, Problem.FunctionCallAfterDot, node, node, Markers.NO_THROW, node.name());

					if (predecessor != null && !typing.compatible(visitor.ty(predecessor), PrimitiveType.PROPLIST)) {
						final ASTNode loc = predecessor instanceof MemberOperator ? predecessor.predecessor() : predecessor;
						if (loc != null)
							visitor.error(visitor, Problem.CallingMethodOnNonObject, loc, loc, Markers.NO_THROW,
								visitor.ty(predecessor).typeName(true));
					}
					if (declarationName.equals(Keywords.Return))
						returnsAsFunctionWarning(node, visitor);
					else if (declaration instanceof Variable)
						variableAsFunctionWarning(node, visitor, cachedEngineDeclarations, declaration);
					else if (declaration instanceof Function) {
						runtimeTypeCheckLeniency(visitor, node);
						validateParameters(visitor, node, declaration, params, predecessor);
					}
					else if (declaration == null)
						maybeUnknownMarker(node, visitor, declarationName);
				}
				private void runtimeTypeCheckLeniency(final Visitor visitor, final CallDeclaration node) {
					if (
						node instanceof CallDeclaration &&
						node.params().length >= 1 &&
						node.name().equals("GetType") //$NON-NLS-1$
					)
						visitor.judgment(
							node.params()[0],
							typing.unify(PrimitiveType.ANY, visitor.ty(node.params()[0])),
							TypingJudgementMode.UNIFY
						);
				}
				private void validateParameters(final Visitor visitor, final CallDeclaration node, final Declaration declaration, final ASTNode[] params, final ASTNode predecessor) throws ProblemException {
					final Function f = (Function)declaration;

					if (f.visibility() == FunctionScope.GLOBAL || predecessor != null)
						visitor.script().addUsedScript(f.script());

					// not a special case... check regular parameter types
					if (!visitor.preliminary && !visitor.visit.doubleTake)
						if (!applyRuleBasedValidation(node, visitor, params))
							if (node.params().length > 0 && visitor.visit.function.script() == visitor.script()) {
								final ParameterValidation pv = new ParameterValidation(visitor, node, f);
								if (f.baseFunction() instanceof EngineFunction)
									pv.validateTypes();
								else
									parameterValidations.add(pv);
							}
				}
				private void maybeUnknownMarker(final CallDeclaration node, final Visitor visitor, final String declarationName) throws ProblemException {
					final IType container = unknownFunctionShouldBeError(node, visitor);
					if (container != null) {
						final int start = node.start();
						visitor.markers().error(visitor, Problem.DeclarationNotFound,
							node, start, start+declarationName.length(), Markers.NO_THROW, declarationName, container.typeName(true));
					}
				}
				private boolean applyRuleBasedValidation(final CallDeclaration node, final Visitor visitor, final ASTNode[] params) throws ProblemException {
					final SpecialFuncRule rule = visitor.specialRuleFor(node, SpecialEngineRules.ARGUMENT_VALIDATOR);
					final boolean specialCaseHandled =
						rule != null &&
						rule.validateArguments(node, params, visitor);
					return specialCaseHandled;
				}
				private void variableAsFunctionWarning(final CallDeclaration node, final Visitor visitor, final CachedEngineDeclarations cachedEngineDeclarations, final Declaration declaration) {
					// variable as function
					((Variable)declaration).setUsed(true);
					final IType type = declarationType(node, visitor);
					// no warning when in #strict mode
					if (visitor.input().strictLevel >= 2)
						if (declaration != cachedEngineDeclarations.This && !typing.compatible(PrimitiveType.FUNCTION, type))
							visitor.markers().warning(visitor, Problem.VariableCalled, node, node, 0, declaration.name(), type.typeName(false));
				}
				private void returnsAsFunctionWarning(final CallDeclaration node, final Visitor visitor) throws ProblemException {
					// return as function
					if (visitor.input().strictLevel >= 2)
						visitor.markers().error(visitor, Problem.ReturnAsFunction, node, node, Markers.NO_THROW);
					else
						visitor.markers().warning(visitor, Problem.ReturnAsFunction, node, node, 0);
				}
				@Override
				public TypeVariable createTypeVariable(final CallDeclaration node, final Visitor visitor) {
					final Declaration d = declaration(node, visitor);
					if (d instanceof Function) {
						final Function f = (Function) d;
						if (f.staticallyTyped() || f.isEngineDeclaration() || f != node.parent(Function.class))
							return null;
						return new FunctionReturnTypeVariable((Function)d);
					} else
						return null;
				}
				@Override
				public Declaration typeEnvironmentKey(final CallDeclaration node, final Visitor visitor) {
					return null;
				}
				@Override
				public boolean isModifiable(final CallDeclaration node, final Visitor visitor) {
					final Declaration declaration = declaration(node, visitor);
					final IType t = declaration instanceof Function ? ((Function)declaration).returnType() : PrimitiveType.UNKNOWN;
					return
						typing.compatible(t, PrimitiveType.REFERENCE) ||
						typing.compatible(t, PrimitiveType.UNKNOWN);
				}
			},

			new AccessDeclarationExpert<CallInherited>(CallInherited.class) {
				@Override
				public IType type(final CallInherited node, final Visitor visitor) {
					final TypeVariable tyVar = findTypeVariable(node, visitor);
					if (tyVar != null)
						return tyVar.get();
					final Function fn = node.parent(Function.class);
					final Function inherited = fn.inheritedFunction();
					if (inherited != null && !inherited.inheritsFrom(fn, new HashSet<Function>()))
						if (inherited.body() != null) {
							final Visit inhv = visitor.input().new Visit(inherited);
							inhv.prepare();
							inhv.run();
							visitor.judgment(node, inhv.get(), TypingJudgementMode.OVERWRITE);
							return inhv.get();
						} else
							return inherited.returnType();
					return PrimitiveType.UNKNOWN;
				}
				@Override
				public void visit(final CallInherited node, final Visitor visitor) throws ProblemException {
					if (node.parent(Script.class) == visitor.script()) {
						// inherited/_inherited not allowed in non-strict mode
						if (visitor.input().strictLevel <= 0)
							visitor.markers().error(visitor, Problem.InheritedDisabledInStrict0, node, node, Markers.NO_THROW);

						final Function function = node.parent(Function.class);
						final Function inh = function.inheritedFunction();
						setDeclaration(node, visitor, inh);
						if (!visitor.preliminary && visitor.script() == node.parent(Script.class))
							node.setDeclaration(inh);
						if (inh == null && !node.failsafe())
							visitor.markers().error(visitor, Problem.NoInheritedFunction, node, node, Markers.NO_THROW, function.name());
					}
				}
				@Override
				public Declaration typeEnvironmentKey(final CallInherited node, final Visitor visitor) {
					final Function f = node.parent(Function.class);
					return f != null ? f.baseFunction() : null;
				}
			},

			new Expert<Sequence>(Sequence.class) {
				@Override
				public IType type(final Sequence node, final Visitor visitor) {
					final ASTNode[] elements = node.subElements();
					return (elements == null || elements.length == 0)
						? PrimitiveType.UNKNOWN
						: visitor.ty(elements[elements.length-1]);
				}
				@Override
				public boolean judgment(final Sequence node, final IType type, final ASTNode origin, final Visitor visitor, final TypingJudgementMode mode) {
					return visitor.judgment(node.lastElement(), type, origin, mode);
				}
				@Override
				public void visit(final Sequence node, final Visitor visitor) throws ProblemException {
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
				public boolean isModifiable(final Sequence node, final Visitor visitor) {
					final ASTNode[] elements = node.subElements();
					if (elements != null && elements.length > 0) {
						final ASTNode last = elements[elements.length-1];
						return visitor.expert(last).isModifiable(last, visitor);
					} else
						return false;
				}
			},

			new Expert<ArraySliceExpression>(ArraySliceExpression.class) {
				@Override
				public IType type(final ArraySliceExpression node, final Visitor visitor) {
					final ArrayType arrayType = as(predecessorType(node, visitor), ArrayType.class);
					return defaulting(arrayType, PrimitiveType.ARRAY);
				}
			},

			new Expert<Nil>(Nil.class) {
				@Override
				public IType type(final Nil node, final Visitor visitor) {
					return PrimitiveType.UNKNOWN;
				}
				@Override
				public void visit(final Nil node, final Visitor visitor) throws ProblemException {
					if (!visitor.script().engine().settings().supportsNil)
						visitor.markers().error(visitor, Problem.NotSupported, node, node, Markers.NO_THROW, Keywords.Nil, visitor.script().engine().name());
				}
			},

			new Expert<This>(This.class) {
				@Override
				public IType type(final This node, final net.arctics.clonk.c4script.typing.dabble.DabbleInference.Input.Visitor visitor) {
					final Function fn = node.parent(Function.class);
					return fn != null && fn.isGlobal()
						? CallTargetType.INSTANCE // global func - don't assume this to be typed as this script
						: visitor.input().thisType;
				}
				@Override
				public boolean isModifiable(final This node, final Visitor visitor) { return false; }
			},

			new LiteralExpert<StringLiteral>(StringLiteral.class) {
				@Override
				public IType type(final StringLiteral node, final Visitor visitor) { return PrimitiveType.STRING; }
				@Override
				public void visit(final StringLiteral node, final Visitor visitor) throws ProblemException {
					// warn about overly long strings
					final long max = visitor.script().index().engine().settings().maxStringLen;
					final String lit = node.literal();
					if (max != 0 && lit.length() > max)
						visitor.markers().warning(visitor, Problem.StringTooLong, node, node, lit.length(), max);

					// stringtbl entries
					// don't warn in #appendto scripts because those will inherit their string tables from the scripts they are appended to
					// and checking for the existence of the table entries there is overkill
					if (visitor.input().hasAppendTo || visitor.script().resource() == null)
						return;
					final String value = lit;
					final int valueLen = value.length();
					// warn when using non-declared string tbl entries
					for (int i = 0; i < valueLen;) {
						if (i+1 < valueLen && value.charAt(i) == '$') {
							final EntityRegion region = StringTbl.entryRegionInString(lit, node.start(), (i+1));
							if (region != null) {
								reportMissingStringTblEntries(visitor, region, node);
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
				public IType type(final IntegerLiteral node, final Visitor visitor) {
					if (node.longValue() == 0 && visitor.script().engine().settings().zeroIsAny)
						return PrimitiveType.ANY;
					else
						return PrimitiveType.INT;
				}
			},

			new LiteralExpert<FloatLiteral>(FloatLiteral.class) {
				@Override
				public IType type(final FloatLiteral node, final Visitor visitor) { return PrimitiveType.FLOAT; }
				@Override
				public void visit(final FloatLiteral node, final Visitor visitor) throws ProblemException {
					if (!visitor.script().engine().settings().supportsFloats)
						visitor.markers().error(visitor, Problem.FloatNumbersNotSupported, node, node, Markers.NO_THROW);
					supr.visit(node, visitor);
				}
			},

			new LiteralExpert<IDLiteral>(IDLiteral.class) {
				@Override
				public IType type(final IDLiteral node, final Visitor visitor) {
					final Definition obj = visitor.script().nearestDefinitionWithId(node.idValue());
					return obj != null ? obj.metaDefinition() : PrimitiveType.ID;
				}
			},

			new LiteralExpert<BoolLiteral>(BoolLiteral.class) {
				@Override
				public IType type(final BoolLiteral node, final Visitor visitor) { return PrimitiveType.BOOL; }
			},

			new Expert<CallExpr>(CallExpr.class) {
				@Override
				public IType type(final CallExpr node, final Visitor visitor) {
					final ASTNode pred = node.predecessor();
					final IType type = visitor.ty(pred);
					if (type instanceof FunctionType)
						return ((FunctionType)type).prototype().returnType();
					else
						return PrimitiveType.ANY;
				}
				@Override
				public void visit(final CallExpr node, final Visitor visitor) throws ProblemException {
					if (!visitor.script().engine().settings().supportsFunctionRefs)
						visitor.markers().error(visitor, Problem.FunctionRefNotAllowed, node, node, Markers.NO_THROW, visitor.script().engine().name());
					else {
						final IType type = visitor.expert(node.predecessor()).type(node.predecessor(), visitor);
						if (!typing.compatible(PrimitiveType.FUNCTION, type))
							visitor.markers().error(visitor, Problem.CallingExpression, node, node, Markers.NO_THROW);
					}
				}
			},

			new Expert<Statement>(Statement.class) {
				@Override
				public IType type(final Statement node, final Visitor visitor) { return PrimitiveType.UNKNOWN; }
				/**
				 * Emit a warning if this expression is erroneously used at a place where only expressions with side effects are allowed.
				 * @param input The info
				 */
				public void warnIfNoSideEffects(final Statement node, final Visitor visitor) {
					if (node.parent() instanceof IterateArrayStatement && ((IterateArrayStatement)node.parent()).elementExpr() == node)
						return;
					if (!node.hasSideEffects())
						visitor.markers().warning(visitor, Problem.NoSideEffects, node, node, 0);
				}
				@Override
				public void visit(final Statement node, final Visitor visitor) throws ProblemException {
					supr.visit(node, visitor);
					warnIfNoSideEffects(node, visitor);
					if (visitor.controlFlow != ControlFlow.Continue)
						visitor.markers().warning(visitor, Problem.NeverReached, node, node, 0);
				}
			},

			new Expert<Comment>(Comment.class) {
				@Override
				public void visit(final Comment node, final Visitor visitor) throws ProblemException {}
			},

			new Expert<VarDeclarationStatement>(VarDeclarationStatement.class) {
				@Override
				public void visit(final VarDeclarationStatement node, final Visitor visitor) throws ProblemException {
					supr.visit(node, visitor);
					for (final VarInitialization initialization : node.variableInitializations())
						if (initialization.variable != null)
							if (initialization.expression != null) {
								final IType initializationType = visitor.ty(initialization.expression);
								if (
									initialization.variable.staticallyTyped() &&
									!typing.compatible(
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
									final AccessVar r = new TempAccessVar(initialization.variable, initialization);
									final AccessVar av = r;
									visitor.judgment(av, initializationType, TypingJudgementMode.OVERWRITE);
								}
							}
				}
			},

			new Expert<PropListExpression>(PropListExpression.class) {
				@Override
				public IType type(final PropListExpression node, final Visitor visitor) {
					return node.definedDeclaration();
				}
				@Override
				public boolean skipReportingProblemsForSubElements() { return true; }
				@Override
				public void visit(final PropListExpression node, final Visitor visitor) throws ProblemException {
					supr.visit(node, visitor);
					if (!visitor.script().engine().settings().supportsProplists)
						visitor.markers().error(visitor, Problem.NotSupported, node, node, Markers.NO_THROW,
							"",
							visitor.script().engine().name());

					for (final Variable v : node.components())
						if (v.initializationExpression() != null) {
							visitor.visit(v.initializationExpression(), true);
							visitor.judgment(new TempAccessVar(v, node), visitor.ty(v.initializationExpression()), TypingJudgementMode.UNIFY);
						}
				}
				@Override
				public boolean isModifiable(final PropListExpression node, final Visitor visitor) { return false; }
			},

			new Expert<Parenthesized>(Parenthesized.class) {
				@Override
				public IType type(final Parenthesized node, final Visitor visitor) {
					return visitor.ty(node.innerExpression());
				}
				@Override
				public boolean isModifiable(final Parenthesized node, final Visitor visitor) {
					return visitor.expert(node.innerExpression()).isModifiable(node.innerExpression(), visitor);
				}
			},

			new Expert<MemberOperator>(MemberOperator.class) {
				final IType OBJECTISH = CallTargetType.INSTANCE;
				@Override
				public IType type(final MemberOperator node, final Visitor visitor) {
					if (node.id() != null)
						return visitor.script().nearestDefinitionWithId(node.id());
					// stuff before -> decides
					final ASTNode pred = node.predecessor();
					return pred != null ? visitor.ty(pred) : supr.type(node, visitor);
				}
				@Override
				public boolean judgment(final MemberOperator node, final IType type, final ASTNode origin, final Visitor visitor, final TypingJudgementMode mode) {
					final ASTNode p = node.predecessor();
					return p != null ? visitor.expert(p).judgment(p, type, origin, visitor, mode) : false;
				}
				@Override
				public void visit(final MemberOperator node, final Visitor visitor) throws ProblemException {
					supr.visit(node, visitor);
					final ASTNode pred = node.predecessor();
					final EngineSettings settings = visitor.script().engine().settings();
					if (pred != null) {
						final IType requiredType = node.dotNotation() ? PrimitiveType.PROPLIST : PrimitiveType.OBJECT;
						final Expert<? super ASTNode> stmReporter = visitor.expert(pred);
						if (!typing.compatible(OBJECTISH, visitor.ty(pred)))
							visitor.markers().warning(visitor, node.dotNotation() ? Problem.NotAProplist : Problem.CallingMethodOnNonObject, node, node, 0,
								visitor.ty(pred, stmReporter).typeName(false));
						else {
							final IType predTy = visitor.ty(pred);
							if (eq(predTy, PrimitiveType.UNKNOWN) || eq(predTy, PrimitiveType.ANY))
								visitor.judgment(pred, requiredType, TypingJudgementMode.UNIFY);
						}
					}
					if (node.getLength() > 3 && !settings.spaceAllowedBetweenArrowAndTilde)
						visitor.markers().error(visitor, Problem.MemberOperatorWithTildeNoSpace, node, node, Markers.NO_THROW);
					if (node.dotNotation() && !settings.supportsProplists)
						visitor.markers().error(visitor, Problem.DotNotationNotSupported, node, node, Markers.NO_THROW, node);
				}
			},

			new Expert<IterateArrayStatement>(IterateArrayStatement.class) {
				IType elementTypeSet(final IType arrayTypes) {
					List<IType> elementTypes = null;
					for (final IType t : arrayTypes) {
						final ArrayType at = as(t, ArrayType.class);
						if (at != null) {
							if (elementTypes == null)
								elementTypes = new ArrayList<IType>();
							elementTypes.add(at.elementType());
						}
					}
					return elementTypes != null
						? typing.unify(elementTypes)
						: null;
				}
				@Override
				public boolean skipReportingProblemsForSubElements() { return true; }
				@Override
				public void visit(final IterateArrayStatement node, final Visitor visitor) throws ProblemException {
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
							loopVariable = visitor.input().script.createVarInScope(Variable.DEFAULT_VARIABLE_FACTORY, node.parent(Function.class), accessVar.name(), Scope.VAR, varPos.start(), varPos.end(), null);
						} else
							loopVariable = as(d, Variable.class);
					} else
						loopVariable = null;

					visitor.judgment(arrayExpr, PrimitiveType.ARRAY, TypingJudgementMode.UNIFY);

					visitor.visit(elementExpr, true);
					visitor.visit(arrayExpr, true);

					final IType type = visitor.ty(arrayExpr);
					if (!typing.compatible(type, PrimitiveType.ARRAY))
						visitor.incompatibleTypesMarker(node, arrayExpr, type, PrimitiveType.ARRAY);
					final IType elmType = elementTypeSet(type);
					visitor.newTypeEnvironment();
					{
						if (loopVariable != null) {
							loopVariable.setUsed(true);
							visitor.judgment(new TempAccessVar(loopVariable, node), elmType, TypingJudgementMode.OVERWRITE);
						}
						visitor.visit(node.body(), true);
					}
					visitor.endTypeEnvironment();
					visitor.controlFlow = t;
				}
			},

			new Expert<SimpleStatement>(SimpleStatement.class) {
				@Override
				public void visit(final SimpleStatement node, final Visitor visitor) throws ProblemException {
					final BinaryOp op = as(node.expression(), BinaryOp.class);
					if (op != null && !op.operator().modifiesArgument())
						visitor.markers().warning(visitor, Problem.NoAssignment, node, op, 0);
					supr.visit(node, visitor);
				}
			},

			new ConditionalStatementExpert<IfStatement>(IfStatement.class) {
				/**
				 * Check whether the given expression contains a reference to a constant.
				 * @param condition The expression to check
				 * @return Whether the expression contains a constant.
				 */
				boolean containsConst(final ASTNode node) {
					if (node instanceof AccessVar && ((AccessVar)node).constCondition())
						return true;
					for (final ASTNode expression : node.subElements())
						if(expression != null && containsConst(expression))
							return true;
					return false;
				}
				@Override
				public void visit(final IfStatement node, final Visitor visitor) throws ProblemException {
					final ControlFlow old = visitor.controlFlow;
					final ASTNode condition = node.condition();
					visitor.visit(condition, true);
					visitor.ty(condition);
					// use two separate type environments for if and else statement, merging
					// gathered information afterwards
					visitor.newTypeEnvironment();
					{
						visitor.newTypeEnvironment();
						{
							visitor.visit(node.body(), true);
						}
						visitor.endTypeEnvironment();
						visitor.controlFlow = old;
						if (node.elseExpression() != null) {
							visitor.newTypeEnvironment();
							{
								visitor.visit(node.elseExpression(), true);
							}
							visitor.endTypeEnvironment();
						}
					}
					visitor.endTypeEnvironment();
					visitor.controlFlow = old;

					if (!containsConst(condition)) {
						final Object condEv = PrimitiveType.BOOL.convert(condition.evaluateStatic(node.parent(Function.class)));
						if (condEv != null && condEv != ASTNode.EVALUATION_COMPLEX)
							visitor.markers().warning(visitor,
								condEv.equals(true) ? Problem.ConditionAlwaysTrue : Problem.ConditionAlwaysFalse,
								condition, condition, 0, condition);
					}
				};
			},

			new ConditionalStatementExpert<ForStatement>(ForStatement.class) {
				IASTVisitor<List<VarInitialization>> variableGatherer = (node, context) -> {
					if (node instanceof VarInitialization && ((VarInitialization) node).variable != null)
						context.add((VarInitialization) node);
					return TraversalContinuation.Continue;
				};
				List<VarInitialization> gatherVariables(final ForStatement fs) {
					final List<VarInitialization> result = new ArrayList<>(3);
					if (fs.initializer() != null)
						fs.initializer().traverse(variableGatherer, result);
					return result;
				}
				@Override
				public void visit(final ForStatement node, final Visitor visitor) throws ProblemException {
					detectVariablesUsedInMultipleLoops(visitor, node);
					if (node.initializer() != null)
						visitor.visit(node.initializer(), true);
					super.visit(node, visitor);
					if (node.increment() != null)
						visitor.visit(node.increment(), true);
				}
				private void detectVariablesUsedInMultipleLoops(final Visitor visitor, final ForStatement node) {
					final List<VarInitialization> own = gatherVariables(node);
					for (ForStatement p = node.parent(ForStatement.class); p != null; p = p.parent(ForStatement.class)) {
						final List<VarInitialization> others = gatherVariables(p);
						for (final VarInitialization ot : others)
							for (final VarInitialization ow : own)
								if (ot.variable != null && ot.variable == ow.variable)
									visitor.warning(visitor, Problem.LoopVariableUsedInMultipleLoops, ow, ow, Markers.NO_THROW,
										ot.variable.name());
					}
				}
			},

			new ConditionalStatementExpert<WhileStatement>(WhileStatement.class),

			new Expert<NewProplist>(NewProplist.class) {
				@Override
				public void visit(final NewProplist node, final Visitor visitor) throws ProblemException {
					node.definedDeclaration().setPrototype(as(visitor.ty(node.prototype()), ProplistDeclaration.class));
				}
			},

			new Expert<Placeholder>(Placeholder.class) {
				@Override
				public void visit(final Placeholder node, final Visitor visitor) throws ProblemException {
					reportMissingStringTblEntries(visitor, new EntityRegion(null, node, node.entryName()), node);
				}
			},

			new Expert<MissingStatement>(MissingStatement.class) {
				@Override
				public void visit(final MissingStatement node, final Visitor visitor) throws ProblemException {
					visitor.markers().error(visitor, Problem.MissingStatement, node, node, Markers.NO_THROW);
				}
			},

			new Expert<GarbageStatement>(GarbageStatement.class) {
				@Override
				public void visit(final GarbageStatement node, final Visitor visitor) throws ProblemException {
					visitor.markers().error(visitor, Problem.Garbage, node, node, Markers.NO_THROW, node.garbage());
				}
			},

			new Expert<FunctionDescription>(FunctionDescription.class) {
				@Override
				public void visit(final FunctionDescription node, final Visitor visitor) throws ProblemException {
					if (visitor.input().hasAppendTo)
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

			new Expert<Unfinished>(Unfinished.class) {
				@Override
				public IType type(final Unfinished node, final Visitor visitor) {
					return visitor.ty(node.expression());
				}
				@Override
				public void visit(final Unfinished node, final Visitor visitor) throws ProblemException {
					visitor.markers().error(visitor, Problem.NotFinished, node, node, Markers.NO_THROW, node);
				}
			},

			new Expert<CastExpression>(CastExpression.class) {
				@Override
				public IType type(final CastExpression node, final Visitor visitor) {
					return node.targetType();
				}
			}

		};
		committee.clear();
		for (final Expert<?> expert : classes)
			committee.put(expert.cls(), expert);
		for (final Expert<?> expert : classes)
			expert.findSuper();
	}

	@Override
	public void captureMarkers() {
		final List<IMarker> markers = new ArrayList<>(20);
		for (final Input input : this.input.values())
			try {
				for (final IMarker m : input.script.file().findMarkers(Core.MARKER_C4SCRIPT_ERROR, true, IResource.DEPTH_ONE)) {
					final int start = m.getAttribute(IMarker.CHAR_START, 0);
					//final int end = m.getAttribute(IMarker.CHAR_END, 0);
					for (final Visit visit : input.visits.values())
						if (visit.function.bodyLocation().containsOffset(start)) {
							markers.add(m);
							break;
						}
				}
			} catch (final CoreException e) {
				//e.printStackTrace();
			}
		this.markers.capture(markers);
	}

	/**
	 * Create error markers in scripts for StringTbl references where the entry is missing from some of the StringTbl**.txt files
	 * @param context Problem reporter
	 * @param region The region describing the string table reference in question
	 * @param node Node in which the reference to the string table entry is contained
	 */
	private static void reportMissingStringTblEntries(final ProblemReporter context, final EntityRegion region, final ASTNode node) {
		StringBuilder miss = null;
		try {
			for (final IResource r : (context.script().resource() instanceof IContainer ? (IContainer)context.script().resource() : context.script().resource().getParent()).members()) {
				if (!(r instanceof IFile))
					continue;
				final IFile f = (IFile) r;
				final Matcher m = StringTbl.PATTERN.matcher(r.getName());
				if (m.matches()) {
					final String lang = m.group(1);
					final StringTbl tbl = (StringTbl)Structure.pinned(f, true, false);
					if (tbl != null)
						if (tbl.map().get(region.text()) == null) {
							if (miss == null)
								miss = new StringBuilder(10);
							if (miss.length() > 0)
								miss.append(", "); //$NON-NLS-1$
							miss.append(lang);
						}
				}
			}
		} catch (final CoreException e) {}
		if (miss != null)
			context.markers().warning(context, Problem.MissingLocalizations, node, region.region(), 0, region.text(), miss);
	}

}
