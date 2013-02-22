package net.arctics.clonk.parser.c4script.inference.dabble;

import static net.arctics.clonk.util.ArrayUtil.map;
import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.isAnyOf;
import static net.arctics.clonk.util.Utilities.threadPool;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import net.arctics.clonk.util.StringUtil;

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
					pool.execute(processor);
			}
		}, 20);
		for (ScriptProcessor processor : shared.processors.values())
			processor.clearReporters(processor.script());
	}

	@Override
	public ProblemReportingContext localTypingContext(Script script) {
		return localTypingContext(new C4ScriptParser(script));
	}

	@Override
	public ProblemReportingContext localTypingContext(C4ScriptParser parser) {
		markers = parser.markers();
		return new ScriptProcessor(parser, new Shared());
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

	public final class ScriptProcessor implements Runnable, ProblemReportingContext, IEvaluationContext {

		static final int
			MAX_PAR = 10,
			MAX_NUMVAR = 20,
			UNKNOWN_PARAMETERNUM = MAX_PAR+1;
		static final boolean
			UNUSEDPARMWARNING = false;

		private final C4ScriptParser parser;
		private final Script script;
		private final Index index;
		private final Typing typing;
		private final CachedEngineDeclarations cachedEngineDeclarations;
		private final Shared shared;
		private final Object working = new Object();
		private final int strictLevel;
		private final boolean hasAppendTo;
		private final Set<Function> finishedFunctions = new HashSet<>();
		private final Map<String, IType> functionReturnTypes = new HashMap<>();
		private final Map<Variable, IType> variableTypes = new HashMap<>();
		private final IType thisType;

		private final Map<String, Declaration> variableMap = new HashMap<>();
		private final Map<String, Declaration> functionMap = new HashMap<>();

		private ASTNode reportingNode;
		private TypeEnvironment typeEnvironment;
		private boolean finished = false;
		private ControlFlow controlFlow;
		private Markers markers;
		private List<Script> visitees;
		private ITypeVariable returnType, inheritedReturnType;

		public ScriptProcessor(C4ScriptParser parser, Shared shared) {
			this.markers = DabbleInference.this.markers();
			this.shared = shared;
			this.parser = parser;
			this.script = parser.script();
			this.index = script.index();
			this.typing = parser.typing();
			this.cachedEngineDeclarations = this.script().engine().cachedDeclarations();
			this.strictLevel = script().strictLevel();
			this.thisType = TypeChoice.make(
				script(),
				script() instanceof Definition ? ((Definition)script()).metaDefinition() : PrimitiveType.ID
			);
			boolean hasAppendTo = false;
			for (Directive d : script().directives())
				if (d.type() == DirectiveType.APPENDTO) {
					hasAppendTo = true;
					break;
				}
			this.hasAppendTo = hasAppendTo;
		}

		public final SpecialFuncRule specialRuleFor(CallDeclaration node, int role) {
			Engine engine = script().engine();
			if (engine != null && engine.specialRules() != null)
				return engine.specialRules().funcRuleFor(node.name(), role);
			else
				return null;
		}

		private void assignDefaultParmTypesToFunction(Function function) {
			if (parser.specialEngineRules() != null)
				for (SpecialFuncRule funcRule : parser.specialEngineRules().defaultParmTypeAssignerRules())
					if (funcRule.assignDefaultParmTypes(this, function))
						return;
		}


		@Override
		public ITypeVariable visitFunction(Function function) {
			if (function == null || function.body() == null)
				return null;
			Script funScript = function.script();
			if (roaming || (visitees != null && visitees.contains(funScript)) || script() == funScript) {
				if (!finishedFunctions.add(function))
					return null;
				ITypeVariable oldReturnType = returnType;
				returnType = new CurrentFunctionReturnTypeVariable(function);
				ITypeVariable oldInheritedReturnType = inheritedReturnType;
				inheritedReturnType = null;
				try {
					ASTNode[] statements = function.body().statements();
					newTypeEnvironment();
					{
						typeEnvironment.add(returnType);
						boolean ownedFunction = !roaming || funScript == script();
						if (ownedFunction)
							assignDefaultParmTypesToFunction(function);
						else
							for (Variable l : function.locals()) {
								ITypeVariable ti = requestTypeInfo(new AccessVar(l));
								if (ti != null)
									ti.storeType(PrimitiveType.UNKNOWN);
							}
						List<Variable> parameters = function.parameters();
						Function baseFunction = function.baseFunction();
						for (int i = 0; i < parameters.size(); i++) {
							Variable p = parameters.get(i);
							IType t = p.type();
							if ((t == PrimitiveType.UNKNOWN) || (t == PrimitiveType.OBJECT) && typing == Typing.ParametersOptionallyTyped) {
								t = PrimitiveType.UNKNOWN;
								if (ownedFunction) {
									List<CallDeclaration> calls = index.callsTo(function.name());
									if (calls != null)
										for (CallDeclaration call : calls) {
											Script other = call.parentOfType(Script.class);
											ScriptProcessor processor = shared.processors.get(other);
											if (processor != null)
												processor.reportProblems();
											if (call.params().length > i) {
												Function f = as(processor != null ? processor.obtainDeclaration(call) : call.declaration(), Function.class);
												if (f != null) {
													f = f.baseFunction();
													if (f != null)
														f = (Function)f.latestVersion();
												}
												if (f == baseFunction) {
													script.addUsedScript(other);
													t = TypeUnification.unify(t, processor != null ? processor.typeOf(call.params()[i]) : call.params()[i].inferredType());
												}
											}
										}
								}
								if (t == PrimitiveType.UNKNOWN)
									t = p.parameterType();

								if (ownedFunction)
									p.assignType(t, true);
								ITypeVariable varTypeInfo = requestTypeInfo(new AccessVar(p));
								if (varTypeInfo != null)
									varTypeInfo.storeType(t);
							}
						}
						ControlFlow old = controlFlow;
						controlFlow = ControlFlow.Continue;
						for (ASTNode s : statements)
							visitNode(s, true);
						controlFlow = old;
					}
					if (!roaming)
						typeEnvironment.apply(this, false);
					endTypeEnvironment(true, true);
					warnAboutPossibleProblemsWithFunctionLocalVariables(function, statements);
					clearReporters(function);
				}
				catch (ParsingException e) { return null; }
				finally {
					function.assignType(returnType.type(), false);
					ITypeVariable r = returnType;
					returnType = oldReturnType;
					oldReturnType = r;
					inheritedReturnType = oldInheritedReturnType;
				}
				return oldReturnType;
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
					typing == Typing.Static ? IMarker.SEVERITY_ERROR : IMarker.SEVERITY_WARNING,
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
			ASTNode saved = reportingNode;
			reportingNode = expression;
			{
				Expert<? super T> reporter = expert(expression);
				ControlFlow old = controlFlow;
				if (recursive && !reporter.skipReportingProblemsForSubElements())
					for (ASTNode e : expression.subElements())
						if (e != null)
							visitNode(e, true);
				controlFlow = old;
				reporter.visit(expression, this);
				if (controlFlow == ControlFlow.Continue)
					controlFlow = expression.controlFlow();
			}
			reportingNode = saved;
			return expression;
		}

		public TypeEnvironment newTypeEnvironment() {
			TypeEnvironment l = new TypeEnvironment();
			l.up = this.typeEnvironment;
			this.typeEnvironment = l;
			return l;
		}

		public void endTypeEnvironment(boolean inject, boolean ignoreLocals) {
			if (inject && typeEnvironment.up != null)
				typeEnvironment.up.inject(typeEnvironment, ignoreLocals);
			typeEnvironment = typeEnvironment.up;
		}

		/**
		 * Requests type information for an expression
		 * @param expression the expression
		 * @return the type information or null if none has been stored
		 */
		public ITypeVariable requestTypeInfo(ASTNode expression) {
			if (typeEnvironment == null || typing == Typing.Static || typing == Typing.Dynamic)
				return null;
			boolean topMostLayer = true;
			ITypeVariable base = null;
			for (TypeEnvironment list = typeEnvironment; list != null; list = list.up) {
				for (ITypeVariable info : list)
					if (info.storesTypeInformationFor(expression, this))
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
				typeEnvironment.add(newlyCreated);
			}
			return newlyCreated;
		}

		/**
		 * Query the type of an arbitrary expression. With some luck the parser will be able to give an answer.
		 * @param expression the expression to query the type of
		 * @return The typeinfo or null if nothing was found
		 */
		public ITypeVariable queryTypeInfo(ASTNode expression) {
			if (typeEnvironment == null)
				return null;
			for (TypeEnvironment list = typeEnvironment; list != null; list = list.up)
				for (ITypeVariable info : list)
					if (info.storesTypeInformationFor(expression, this))
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
			return info != null ? info.type() : defaultType;
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
							ASTNode old = reportingNode;
							reportingNode = decl;
							this.markers().warning(this, code, initialization, initialization, 0, format);
							reportingNode = old;
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

		private final void startRoaming(Script visitee) {
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
				startRoaming(script);
			for (Function f : script.functions()) {
				// skip function that have been overridden
				if (roaming && !script().seesFunction(f))
					continue;
				visitFunction(f);
			}
			if (roaming)
				endRoaming();
		}

		@Profiled
		@Override
		public void reportProblems() {
			synchronized (shared) {
				if (!finished) {
					finished = true;
					internalWork();
					System.out.println(String.format("%s: used scripts: %s", script.name(), StringUtil.blockString("", "", ",  ", map(script.usedScripts(), new IConverter<Script, String>() {
						@Override
						public String convert(Script from) {
							return from.name();
						}
					}))));
				}
			}
		}

		private void internalWork() {
			// revisit all inherited scripts since that is the only way to
			// accurately type inherited functions with respect to added things from this script
			newTypeEnvironment();
			{
				visitees = script().conglomerate();
				for (Script include : script().includes(GatherIncludesOptions.Recursive))
					visit(include, true);
				visitees = Arrays.asList(script());
				storeTypings(typeEnvironment);
				for (ITypeVariable ti : typeEnvironment) {
					Declaration d = ti.declaration(this);
					if (d != null && d.containedIn(script()))
						ti.apply(false, this);
				}
			}
			endTypeEnvironment(false, false);

			newTypeEnvironment();
			{
				visit(script(), false);
				storeTypings(typeEnvironment);
				script().setTypings(variableTypes, functionReturnTypes);
				typeEnvironment.apply(this, false);
			}
			endTypeEnvironment(false, false);
		}
		private void storeTypings(TypeEnvironment typeEnvironment) {
			for (ITypeVariable info : typeEnvironment) {
				VariableTypeVariable vti = as(info, VariableTypeVariable.class);
				if (vti != null && vti.variable().scope() == Scope.LOCAL)
					variableTypes.put(vti.variable(), vti.type());
				FunctionReturnTypeVariable ftri = as(info, FunctionReturnTypeVariable.class);
				if (ftri != null && ftri.function().visibility() != FunctionScope.GLOBAL)
					functionReturnTypes.put(ftri.function().name(), ftri.type());
			}
		}
		private void clearReporters(ASTNode node) {
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
					node.temporaryProblemReportingObject = findReporter(node);
					return TraversalContinuation.Continue;
				}
			}, null);
		}

		@Override
		public void run() {
			if (finished)
				return;
			if (shared.monitor.isCanceled())
				return;
			shared.monitor.subTask(String.format("Reporting problems for '%s'", script().name()));
			reportProblems();
			shared.monitor.worked(1);
		}

		@Override
		public void storeType(ASTNode node, IType type) {
			ITypeVariable requested = requestTypeInfo(node);
			if (requested != null)
				requested.storeType(type);
		}

		@Override
		public Definition definition() { return parser.definition(); }
		@Override
		public SourceLocation absoluteSourceLocationFromExpr(ASTNode expression) {
			Function f = expression.parentOfType(Function.class);
			int bodyOffset = f != null ? f.bodyLocation().start() : 0;
			return parser.absoluteSourceLocation(expression.start()+bodyOffset, expression.end()+bodyOffset);
		}
		@Override
		public CachedEngineDeclarations cachedEngineDeclarations() { return cachedEngineDeclarations; }
		@Override
		public Script script() { return script; }
		@Override
		public IFile file() { return script().scriptFile(); }
		@Override
		public Declaration container() { return script(); }
		@Override
		public int fragmentOffset() { return parser.fragmentOffset(); }
		@Override
		public IType typeOf(ASTNode node) { return ty(node, this); }
		@Override
		public boolean isModifiable(ASTNode node) { return expert(node).isModifiable(node, this); }
		@Override
		public BufferedScanner scanner() { return parser; }

		@Override
		public <T extends AccessDeclaration> Declaration obtainDeclaration(T access) {
			@SuppressWarnings("unchecked")
			AccessDeclarationExpert<T> reporter = (AccessDeclarationExpert<T>)expert(access);
			return reporter.obtainDeclaration(access, this);
		}

		@Override
		public boolean validForType(ASTNode node, IType type) {
			return expert(node).validForType(node, type, this);
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
	 * 	<li>typing ({@link Expert#type(ASTNode, ScriptProcessor)})</li>
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
				Expert<? super T> sup = (Expert<? super T>)problemReporters.get(s);
				if (sup != null) {
					supr = sup;
					return;
				}
			}
			supr = NULL_REPORTER;
		}

		/**
		 * Returning true tells the {@link ScriptProcessor} to not recursively call {@link #visit(ASTNode, ScriptProcessor)} on {@link ASTNode#subElements()}
		 * @return Do you just show up, play the music,
		 */
		public boolean skipReportingProblemsForSubElements() {return false;}
		public void visit(T node, ScriptProcessor processor) throws ParsingException {}

		public IType type(T node, ScriptProcessor processor) { return processor.queryTypeOfExpression(node, PrimitiveType.UNKNOWN); }

		public IType callerType(T node, ScriptProcessor processor) {
			ASTNode pred = node.predecessorInSequence();
			if (pred != null)
				return ty(pred, processor);
			else
				return processor.script();
		}

		public final IType predecessorType(ASTNode node, ScriptProcessor processor) {
			ASTNode p = node.predecessorInSequence();
			return p != null ? ty(p, processor) : null;
		}

		public final <X extends IType> X predecessorTypeAs(ASTNode node, Class<X> cls, ScriptProcessor processor) {
			return as(predecessorType(node, processor), cls);
		}

		/**
		 * Return whether this expression is valid as a value of the specified type.
		 * @param type The type to test against
		 * @param context Script parser context
		 * @return True if valid, false if not.
		 */
		public final boolean validForType(ASTNode node, IType type, ScriptProcessor processor) {
			if (type == null)
				return true;
			IType myType = ty(node, processor);
			return type.canBeAssignedFrom(myType);
		}

		public boolean typingJudgement(T node, IType type, ScriptProcessor processor, TypingJudgementMode mode) {
			ITypeVariable info;
			switch (mode) {
			case Expect:
				info = processor.requestTypeInfo(node);
				if (info != null)
					if (info.type() == PrimitiveType.UNKNOWN || info.type() == PrimitiveType.ANY) {
						info.storeType(type);
						return true;
					} else
						return false;
				return true;
			case Force:
				info = processor.requestTypeInfo(node);
				if (info != null) {
					info.storeType(type);
					return true;
				} else
					return false;
			case Hint:
				info = processor.queryTypeInfo(node);
				return info == null || info.hint(type);
			case Unify:
				info = processor.requestTypeInfo(node);
				if (info != null) {
					info.storeType(TypeUnification.unify(info.type(), type));
					return true;
				} else
					return false;
			default:
				return false;
			}
		}

		public void assignment(T leftSide, ASTNode rightSide, ScriptProcessor processor) {
			if (processor.typing == Typing.Static) {
				IType leftTy = ty(leftSide, this, processor);
				IType rightTy = ty(rightSide, processor);
				if (!leftTy.canBeAssignedFrom(rightTy))
					processor.incompatibleTypes(rightSide, rightSide, leftTy, rightTy);
			} else
				judgement(leftSide, ty(rightSide, processor), TypingJudgementMode.Force, processor);
		}

		public ITypeVariable createTypeVariable(T node, ScriptProcessor processor) {
			ITypeable d = ExpressionTypeVariable.typeableFromExpression(node, processor);
			if (d != null && !d.staticallyTyped())
				return new ExpressionTypeVariable(node, processor);
			return null;
		}

		@Override
		public String toString() { return String.format("Expert<%s>", cls.getSimpleName()); }

		public boolean isModifiable(T node, ScriptProcessor processor) { return true; }
	}

	class AccessDeclarationExpert<T extends AccessDeclaration> extends Expert<T> {
		public AccessDeclarationExpert(Class<T> cls) { super(cls); }
		protected Declaration obtainDeclaration(T node, ScriptProcessor processor) { return null; }
		@Override
		public void visit(T node, ScriptProcessor processor) throws ParsingException {
			super.visit(node, processor);
			internalObtainDeclaration(node, processor);
		}
		protected final Declaration internalObtainDeclaration(T node, ScriptProcessor processor) {
			if (!processor.roaming || processor.script() == node.parentOfType(Script.class)) {
				if (node.declaration() == null)
					node.setDeclaration(obtainDeclaration(node, processor));
				if (node.declaration() == null) {
					processor.script().index().loadScriptsContainingDeclarationsNamed(node.name());
					node.setDeclaration(obtainDeclaration(node, processor));
				}
				return node.declaration();
			} else
				return obtainDeclaration(node, processor);
		}
		@Override
		public ITypeVariable createTypeVariable(T node, ScriptProcessor processor) {
			if (node.declaration() instanceof ITypeable && ((ITypeable)node.declaration()).staticallyTyped())
				return null;
			else
				return super.createTypeVariable(node, processor);
		}
	}

	class ConditionalStatementProblemReporter<T extends ConditionalStatement> extends Expert<T> {
		public ConditionalStatementProblemReporter(Class<T> cls) { super(cls); }
		@Override
		public boolean skipReportingProblemsForSubElements() {return true;}
		@Override
		public void visit(ConditionalStatement node, ScriptProcessor processor) throws ParsingException {
			ControlFlow t = processor.controlFlow;
			processor.controlFlow = ControlFlow.Continue;
			processor.newTypeEnvironment();
			processor.visitNode(node.condition(), true);
			processor.endTypeEnvironment(true, false);
			processor.newTypeEnvironment();
			processor.visitNode(node.body(), true);
			processor.endTypeEnvironment(true, false);
			loopConditionWarnings(node, processor);
			processor.controlFlow = t;
		}
		/**
		 * Emit warnings about loop conditions that could result in loops never executing or never ending.
		 * @param body The loop body. If the condition looks like it will always be true, checks are performed whether the body contains loop control flow statements.
		 * @param condition The loop condition to check
		 */
		protected void loopConditionWarnings(ConditionalStatement node, ScriptProcessor processor) {
			ASTNode condition = node.condition();
			if (node.body() == null || condition == null || !(node instanceof ILoop))
				return;
			Object condEv = PrimitiveType.BOOL.convert(condition == null ? true : condition.evaluateStatic(node.parentOfType(Function.class)));
			if (Boolean.FALSE.equals(condEv))
				processor.markers().warning(processor, Problem.ConditionAlwaysFalse, condition, condition, Markers.NO_THROW, condition);
			else if (Boolean.TRUE.equals(condEv)) {
				EnumSet<ControlFlow> flows = node.body().possibleControlFlows();
				if (!(flows.contains(ControlFlow.BreakLoop) || flows.contains(ControlFlow.Return)))
					processor.markers().warning(processor, Problem.InfiniteLoop, node, node, Markers.NO_THROW);
			}
		}
	}

	private final Expert<ASTNode> NULL_REPORTER = new Expert<ASTNode>(ASTNode.class) {
		@Override
		public IType type(ASTNode node, ScriptProcessor processor) {
			return PrimitiveType.UNKNOWN;
		}
		@Override
		public IType callerType(ASTNode node, ScriptProcessor processor) {
			return PrimitiveType.ANY;
		}
	};

	private final <T extends ASTNode> Expert<? super T> findReporter(T node) {
		for (Class<?> cls = node.getClass(); cls != null; cls = cls.getSuperclass()) {
			@SuppressWarnings("unchecked")
			Expert<? super T> reporter = (Expert<? super T>)problemReporters.get(cls);
			if (reporter != null)
				return reporter;
		}
		return NULL_REPORTER;
	}

	@SuppressWarnings("unchecked")
	private final <T extends ASTNode> Expert<? super T> expert(T node) {
		if (node.temporaryProblemReportingObject != null)
			return (Expert<? super T>)node.temporaryProblemReportingObject;
		else
			return findReporter(node);
	}

	public final IType ty(ASTNode node, ScriptProcessor processor) {
		return node != null ? ty(node, expert(node), processor) : null;
	}

	public final <T extends ASTNode> IType ty(T node, Expert<T> reporter, ScriptProcessor processor) {
		IType type = reporter.type(node, processor);
		node.inferredType(type);
		return type;
	}

	public final void judgement(ASTNode node, IType type, TypingJudgementMode mode, ScriptProcessor processor) {
		expert(node).typingJudgement(node, type, processor, mode);
	}

	private final Map<Class<? extends ASTNode>, Expert<? extends ASTNode>> problemReporters = new HashMap<Class<? extends ASTNode>, Expert<?>>();
	{
		@SuppressWarnings("rawtypes")
		Expert<?>[] reporters = new Expert[] {

			new AccessDeclarationExpert<AccessDeclaration>(AccessDeclaration.class),

			new AccessDeclarationExpert<AccessVar>(AccessVar.class) {
				private Declaration findUsingType(ScriptProcessor processor, AccessVar node, ASTNode predecessor, IType type) {
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
							FindDeclarationInfo info = new FindDeclarationInfo(processor.script().index());
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
										processor.visitFunction(p);
								}
								return v;
							}
						}
					}
					return null;
				}
				@Override
				protected Declaration obtainDeclaration(AccessVar node, ScriptProcessor processor) {
					ASTNode p = node.predecessorInSequence();
					if (p == null && node.name().equals(Variable.THIS.name()))
						return Variable.THIS;
					IType type = processor.script();
					if (p != null)
						type = ty(p, processor);
					if (p == null) {
						Function f = node.parentOfType(Function.class);
						if (f != null) {
							Variable v = f.findVariable(node.name());
							if (v != null)
								return v;
						}
						Declaration v = processor.variableMap.get(node.name());
						if (v == null && !processor.variableMap.containsKey(node.name())) {
							v = findUsingType(processor, node, null, type);
							processor.variableMap.put(node.name(), v);
						}
						return v;
					}
					else
						return findUsingType(processor, node, p, type);
				}
				@Override
				public IType type(AccessVar node, ScriptProcessor processor) {
					Declaration d = internalObtainDeclaration(node, processor);
					// declarationFromContext(context) ensures that declaration is not null (if there is actually a variable) which is needed for queryTypeOfExpression for example
					if (d == Variable.THIS)
						return processor.thisType;
					IType stored = processor.queryTypeOfExpression(node, null);
					if (stored != null)
						return stored;
					if (d instanceof Function)
						return new FunctionType((Function)d);
					else if (d instanceof Variable) {
						Variable v = (Variable)d;
						Map<Variable, IType> typesMap= null;
						if (v.scope() == Scope.LOCAL) {
							if (node.predecessorInSequence() == null)
								typesMap = processor.variableTypes;
							else {
								IType targetType = ty(node.predecessorInSequence(), processor);
								if (targetType instanceof Script) {
									ScriptProcessor other = processor.shared.processors.get(targetType);
									if (other != null) {
										other.reportProblems();
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
				public IType callerType(AccessVar node, ScriptProcessor processor) {
					Variable v = as(node.declaration(), Variable.class);
					if (v != null) switch (v.scope()) {
					case CONST: case STATIC:
						return null;
					default:
						break;
					}
					return super.callerType(node, processor);
				}
				@Override
				public boolean typingJudgement(AccessVar node, IType type, ScriptProcessor processor, TypingJudgementMode mode) {
					if (node.declaration() == Variable.THIS)
						return true;
					return super.typingJudgement(node, type, processor, mode);
				}
				@Override
				public void visit(AccessVar node, ScriptProcessor processor) throws ParsingException {
					super.visit(node, processor);
					ASTNode pred = node.predecessorInSequence();
					Declaration declaration = node.declaration();
					if (declaration == null && pred == null)
						processor.markers().error(processor, Problem.UndeclaredIdentifier, node, node, Markers.NO_THROW, node.name());
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
									processor.markers().error(processor, Problem.LocalUsedInGlobal, node, node, Markers.NO_THROW);
							}
							break;
						case STATIC: case CONST:
							processor.script().addUsedScript(var.script());
							break;
						case VAR:
							Function currentFunction = node.parentOfType(Function.class);
							if (currentFunction != null && var.parentDeclaration() == currentFunction) {
								int locationUsed = currentFunction.bodyLocation().getOffset()+node.start();
								if (locationUsed < var.start())
									processor.markers().warning(processor, Problem.VarUsedBeforeItsDeclaration, node, node, 0, var.name());
							}
							break;
						case PARAMETER:
							break;
						}
					} else if (declaration instanceof Function)
						if (!processor.script().engine().settings().supportsFunctionRefs)
							processor.markers().error(processor, Problem.FunctionRefNotAllowed, node, node, Markers.NO_THROW, processor.script().engine().name());
				}
				public void initializeFromAssignment(Variable var, ASTNode referee, ASTNode expression, ScriptProcessor processor) {
					IType type = ty(expression, processor);
					var.expectedToBeOfType(type, TypingJudgementMode.Expect);
					var.setLocation(processor.absoluteSourceLocationFromExpr(referee));
					var.forceType(type);
					var.setInitializationExpression(expression);
				}
				@Override
				public void assignment(AccessVar leftSide, ASTNode rightSide, ScriptProcessor processor) {
					Declaration declaration = leftSide.declaration();
					if (declaration == Variable.THIS)
						return;
					if (declaration == null) {
						IType predType = predecessorType(leftSide, processor);
						if (predType != null && predType.canBeAssignedFrom(PrimitiveType.PROPLIST))
							if (predType instanceof IProplistDeclaration) {
								IProplistDeclaration proplDecl = (IProplistDeclaration) predType;
								if (proplDecl.isAdHoc()) {
									Variable var = proplDecl.addComponent(
										new Variable(leftSide.name(), Variable.Scope.VAR),
										true
									);
									declaration = var;
									initializeFromAssignment(var, leftSide, rightSide, processor);
								}
							} else for (IType t : predType)
								if (t == processor.script()) {
									Variable var = new Variable(leftSide.name(), Variable.Scope.LOCAL);
									initializeFromAssignment(var, leftSide, rightSide, processor);
									processor.script().addDeclaration(var);
									declaration = var;
									break;
								}
					}
					super.assignment(leftSide, rightSide, processor);
				}
				@Override
				public ITypeVariable createTypeVariable(AccessVar node, ScriptProcessor processor) {
					if (node.declaration() instanceof Variable && node.predecessorInSequence() == null)
						return new VariableTypeVariable(node);
					else
						return super.createTypeVariable(node, processor);
				}
				@Override
				public boolean isModifiable(AccessVar node, ScriptProcessor processor) {
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
				public void assignment(VarInitializationAccess leftSide, ASTNode rightSide, ScriptProcessor processor) {
					supr.assignment(leftSide, rightSide, processor);
					if (leftSide.declaration() instanceof Variable && ((Variable)leftSide.declaration()).scope() == Scope.CONST && !rightSide.isConstant())
						try {
							processor.markers().error(processor, Problem.NonConstGlobalVarAssignment, rightSide, rightSide, Markers.NO_THROW);
						} catch (ParsingException e) { }
				}
				@Override
				public boolean isModifiable(VarInitializationAccess node, ScriptProcessor processor) { return true; /* sudo */ }
			},

			new Expert<ArrayExpression>(ArrayExpression.class) {
				@Override
				public IType type(ArrayExpression node, final ScriptProcessor processor) {
					return new ArrayType(
						null,
						ArrayUtil.map(node.subElements(), IType.class, new IConverter<ASTNode, IType>() {
							@Override
							public IType convert(ASTNode from) {
								return from != null ? ty(from, processor) : PrimitiveType.UNKNOWN;
							}
						})
					);
				}
				@Override
				public boolean isModifiable(ArrayExpression node, ScriptProcessor processor) { return false; }
			},

			new Expert<ArrayElementExpression>(ArrayElementExpression.class) {
				@Override
				public IType type(ArrayElementExpression node, ScriptProcessor processor) {
					IType t = supr.type(node, processor);
					if (t != PrimitiveType.UNKNOWN && t != PrimitiveType.ANY)
						return t;
					ASTNode pred = node.predecessorInSequence();
					if (pred != null) {
						IType predTy = ty(pred, processor);
						for (IType ty : predTy) {
							ArrayType at = as(ty, ArrayType.class);
							if (at != null)
								return at.typeForElementWithIndex(ASTNode.evaluateStatic(node.argument(), processor));
						}
					}
					return PrimitiveType.ANY;
				}
				@Override
				public void assignment(ArrayElementExpression leftSide, ASTNode rightSide, ScriptProcessor processor) {
					IType predType_ = predecessorType(leftSide, processor);
					for (IType predType : predType_) {
						ArrayType arrayType = as(predType, ArrayType.class);
						IType rightSideType = ty(rightSide, processor);
						ASTNode pred = leftSide.predecessorInSequence();
						if (arrayType != null) {
							Object argEv = ASTNode.evaluateStatic(leftSide.argument(), processor);
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
							processor.storeType(pred, mutation);
							break;
						} else if (predType == PrimitiveType.UNKNOWN || predType == PrimitiveType.ARRAY)
							judgement(
								pred,
								new ArrayType(rightSideType, ArrayType.NO_PRESUMED_LENGTH),
								TypingJudgementMode.Force,
								processor
							);
					}
				}
				@Override
				public void visit(ArrayElementExpression node, ScriptProcessor processor) throws ParsingException {
					supr.visit(node, processor);
					IType type = predecessorType(node, processor);
					if (type == null)
						type = PrimitiveType.UNKNOWN;
					ASTNode arg = node.argument();
					if (arg == null)
						processor.markers().warning(processor, Problem.MissingExpression, node, node, 0);
					else if (PrimitiveType.UNKNOWN != type && PrimitiveType.ANY != type) {
						IType argType = ty(arg, processor);
						ASTNode pred = node.predecessorInSequence();
						if (argType == PrimitiveType.STRING) {
							if (TypeUnification.unifyNoChoice(PrimitiveType.PROPLIST, type) == null)
								processor.markers().warning(processor, Problem.NotAProplist, node, pred, 0);
							else
								judgement(pred, PrimitiveType.PROPLIST, TypingJudgementMode.Unify, processor);
						}
						else if (argType == PrimitiveType.INT)
							if (TypeUnification.unifyNoChoice(PrimitiveType.ARRAY, type) == null)
								processor.markers().warning(processor, Problem.NotAnArrayOrProplist, node, pred, 0);
							//else
							//	reporter(pred).typingJudgement(pred, PrimitiveType.ARRAY, processor, TypingJudgementMode.Unify);
					}
				}
				@Override
				public boolean isModifiable(ArrayElementExpression node, ScriptProcessor processor) { return true; }
			},

			new Expert<ArraySliceExpression>(ArraySliceExpression.class) {
				private void warnIfNotArray(ASTNode node, ScriptProcessor processor, IType type) {
					if (type != null && type != PrimitiveType.UNKNOWN && type != PrimitiveType.ANY &&
						TypeUnification.unifyNoChoice(PrimitiveType.ARRAY, type) == null &&
						TypeUnification.unifyNoChoice(PrimitiveType.PROPLIST, type) == null)
						processor.markers().warning(processor, Problem.NotAnArrayOrProplist, node, node, 0);
				}
				@Override
				public void visit(ArraySliceExpression node, ScriptProcessor processor) throws ParsingException {
					supr.visit(node, processor);
					IType type = predecessorType(node, processor);
					warnIfNotArray(node.predecessorInSequence(), processor, type);
				}
				@Override
				public boolean isModifiable(ArraySliceExpression node, ScriptProcessor processor) { return false; }
			},

			new Expert<OperatorExpression>(OperatorExpression.class) {
				@Override
				public IType type(OperatorExpression node, ScriptProcessor processor) {
					return node.operator().resultType();
				}
				@Override
				public boolean isModifiable(OperatorExpression node, ScriptProcessor processor) { return node.operator().returnsRef(); }
			},

			new Expert<BinaryOp>(BinaryOp.class) {
				@Override
				public IType type(BinaryOp node, ScriptProcessor processor) {
					switch (node.operator()) {
					// &&/|| special: they return either the left or right side of the operator so the return type is the lowest common denominator of the argument types
					case And: case Or: case JumpNotNil:
						IType leftSideType = ty(node.leftSide(), processor);
						IType rightSideType = ty(node.rightSide(), processor);
						if (leftSideType == rightSideType)
							return leftSideType;
						else
							return TypeUnification.unify(leftSideType, rightSideType);
					case Assign:
						return ty(node.rightSide(), processor);
					default:
						return supr.type(node, processor);
					}
				}
				@Override
				public void visit(BinaryOp node, ScriptProcessor processor) throws ParsingException {
					final Operator op = node.operator();
					// sanity
					ASTNode left = node.leftSide();
					ASTNode right = node.rightSide();
					node.setLocation(left.start(), right.end());
					// i'm an assignment operator and i can't modify my left side :C
					if (op.modifiesArgument() && !expert(left).isModifiable(left, processor))
						processor.markers().error(processor, Problem.ExpressionNotModifiable, node, left, Markers.NO_THROW);
					// obsolete operators in #strict 2impor
					if ((op == Operator.StringEqual || op == Operator.ne) && (processor.strictLevel >= 2))
						processor.markers().warning(processor, Problem.ObsoleteOperator, node, node, 0, op.operatorName());
					// wrong parameter types
					if (!validForType(left, op.firstArgType(), processor))
						processor.incompatibleTypes(node, left, op.firstArgType(), ty(left, processor));
					if (!validForType(right, op.secondArgType(), processor))
						processor.incompatibleTypes(node, right, op.secondArgType(), ty(right, processor));

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
						expert(left).assignment(left, right, processor);
						break;
					default:
						break;
					}

					if (expectedLeft != null)
						judgement(left, expectedLeft, TypingJudgementMode.Unify, processor);
					if (expectedRight != null)
						judgement(right, expectedRight, TypingJudgementMode.Unify, processor);
				}
				@Override
				public ITypeVariable createTypeVariable(BinaryOp node, ScriptProcessor processor) {
					ASTNode leftSide = node.leftSide();
					if (node.operator() == Operator.Assign && leftSide != null)
						return expert(leftSide).createTypeVariable(leftSide, processor);
					return super.createTypeVariable(node, processor);
				}
			},

			new Expert<UnaryOp>(UnaryOp.class) {
				@Override
				public void visit(UnaryOp node, ScriptProcessor processor) throws ParsingException {
					supr.visit(node, processor);
					ASTNode arg = node.argument();
					if (node.operator().modifiesArgument() && !expert(arg).isModifiable(arg, processor))
						processor.markers().error(processor, Problem.ExpressionNotModifiable, node, arg, Markers.NO_THROW);
					Expert<? super ASTNode> rarg = expert(arg);
					PrimitiveType firstArgType = node.operator().firstArgType();
					if (!rarg.validForType(arg, firstArgType, processor))
						processor.incompatibleTypes(node, arg, firstArgType,
							ty(arg, rarg, processor));
					if (firstArgType != PrimitiveType.ANY)
						rarg.typingJudgement(arg, firstArgType, processor, TypingJudgementMode.Expect);
				}
				@Override
				public boolean isModifiable(UnaryOp node, ScriptProcessor processor) {
					return node.placement() == Placement.Prefix && node.operator().returnsRef();
				}
			},

			new Expert<BoolLiteral>(BoolLiteral.class) {
				@Override
				public void visit(BoolLiteral node, ScriptProcessor processor) throws ParsingException {
					supr.visit(node, processor);
					if (node.parent() instanceof BinaryOp) {
						Operator op = ((BinaryOp) node.parent()).operator();
						if (op == Operator.And || op == Operator.Or)
							processor.markers().warning(processor, Problem.BoolLiteralAsOpArg, node, node, 0, this.toString());
					}
				}
			},

			new Expert<ContinueStatement>(ContinueStatement.class) {
				@Override
				public void visit(ContinueStatement node, ScriptProcessor processor) throws ParsingException {
					if (node.parentOfType(ILoop.class) == null)
						processor.markers().error(processor, Problem.KeywordInWrongPlace, node, node, Markers.NO_THROW, node.keyword());
					supr.visit(node, processor);
				}
			},

			new Expert<BreakStatement>(BreakStatement.class) {
				@Override
				public void visit(BreakStatement node, ScriptProcessor processor) throws ParsingException {
					if (node.parentOfType(ILoop.class) == null)
						processor.markers().error(processor, Problem.KeywordInWrongPlace, node, node, Markers.NO_THROW, node.keyword());
					supr.visit(node, processor);
				}
			},

			new Expert<ReturnStatement>(ReturnStatement.class) {
				private void warnAboutTupleInReturnExpr(ScriptProcessor processor, ASTNode node, boolean tupleIsError) throws ParsingException {
					if (node == null)
						return;
					if (node instanceof Tuple)
						if (tupleIsError)
							processor.markers().error(processor, Problem.TuplesNotAllowed, node, node, Markers.NO_THROW);
						else if (processor.strictLevel >= 2)
							processor.markers().error(processor, Problem.ReturnAsFunction, node, node, Markers.NO_THROW);
					ASTNode[] subElms = node.subElements();
					for (ASTNode e : subElms)
						warnAboutTupleInReturnExpr(processor, e, true);
				}
				@Override
				public void visit(ReturnStatement node, ScriptProcessor processor) throws ParsingException {
					supr.visit(node, processor);
					ASTNode returnExpr = node.returnExpr();
					warnAboutTupleInReturnExpr(processor, returnExpr, false);
					Function currentFunction = node.parentOfType(Function.class);
					if (currentFunction == null)
						processor.markers().error(processor, Problem.NotAllowedHere, node, node, Markers.NO_THROW, Keywords.Return);
					else if (returnExpr != null)
						if (processor.typing == Typing.Static && currentFunction.staticallyTyped()) {
							if (!expert(returnExpr).validForType(returnExpr, currentFunction.returnType(), processor))
								processor.incompatibleTypes(node,
									returnExpr, currentFunction.returnType(), ty(returnExpr, processor));
						}
						else {
							IType type = ty(returnExpr, processor);
							judgement(node, type, TypingJudgementMode.Unify, processor);
							//parser.linkTypesOf(dummy, returnExpr);
						}
				}
				@Override
				public ITypeVariable createTypeVariable(ReturnStatement node, ScriptProcessor processor) {
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
					ScriptProcessor processor,
					CallDeclaration node, String functionName,
					IType type
				) {
					IType lookIn = type != null ? type : processor.script();
					if (lookIn != null) for (IType ty : lookIn) {
						Script script = as(ty, Script.class);
						if (script == null && ty instanceof MetaDefinition)
							script = ((MetaDefinition)ty).definition();
						if (script == null)
							continue;
						FindDeclarationInfo info = new FindDeclarationInfo(processor.script().index());
						info.searchOrigin = processor.script();
						info.contextFunction = node.parentOfType(Function.class);
						info.findGlobalVariables = type == null;
						Declaration dec = script.findDeclaration(functionName, info);
						// parse function before this one
						if (dec instanceof Function && node.parentOfType(Function.class) != null)
							processor.visitFunction((Function)dec);
						if (dec != null)
							return dec;
					}
					if (type != null) {
						// find global function
						Declaration declaration;
						try {
							declaration = processor.script().index().findGlobal(Declaration.class, functionName);
						} catch (Exception e) {
							e.printStackTrace();
							return null;
						}
						// find engine function
						if (declaration == null)
							declaration = processor.script().index().engine().findFunction(functionName);

						List<Declaration> allFromLocalIndex = processor.script().index().declarationMap().get(functionName);
						Declaration decl = processor.script().engine().findLocalFunction(functionName, false);
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
				protected Declaration obtainDeclaration(CallDeclaration node, ScriptProcessor processor) {
					String declarationName = node.name();
					if (declarationName.equals(Keywords.Return))
						return null;
					ASTNode p = node.predecessorInSequence();
					if (p == null) {
						Declaration f = processor.functionMap.get(node.name());
						if (f == null && !processor.functionMap.containsKey(node.name())) {
							f = findUsingType(processor, node, declarationName, processor.script());
							processor.functionMap.put(declarationName, f);
						}
						return f;
					}
					else
						return findUsingType(processor, node, declarationName, ty(p, processor));
				}
				private IType declarationType(CallDeclaration node, ScriptProcessor processor) {
					Declaration d = internalObtainDeclaration(node, processor);

					// look for gathered type information
					IType stored = processor.queryTypeOfExpression(node, null);
					if (stored != null)
						return stored;

					// calling this() as function -> return object type belonging to script
					if (node.params().length == 0 && d != null && (d == processor.cachedEngineDeclarations().This || d == Variable.THIS))
						return processor.thisType;

					if (d instanceof Function) {
						// Some special rule applies and the return type is set accordingly
						SpecialFuncRule rule = node.specialRuleFromContext(processor, SpecialEngineRules.RETURNTYPE_MODIFIER);
						if (rule != null) {
							IType type = rule.returnType(processor, node);
							if (type != null)
								return type;
						}
						Function f = (Function)d;
						Map<String, IType> typesMap = null;
						if (f.visibility() != FunctionScope.GLOBAL)
							if (node.predecessorInSequence() == null)
								typesMap = processor.functionReturnTypes;
							else {
								IType targetType = ty(node.predecessorInSequence(), processor);
								if (targetType instanceof Script) {
									ScriptProcessor other = processor.shared.processors.get(targetType);
									if (other != null) {
										other.reportProblems();
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

					return supr != null ? supr.type(node, processor) : PrimitiveType.UNKNOWN;
				}
				private boolean unknownFunctionShouldBeError(CallDeclaration node, ScriptProcessor processor) {
					ASTNode pred = node.predecessorInSequence();
					// stand-alone function? always bark!
					if (pred == null)
						return true;
					// not typed? weird
					IType predType = ty(pred, processor);
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
					if (ad != null && (ad.declaration() == Variable.THIS || ad.declaration() == processor.cachedEngineDeclarations().This))
						return false;
					boolean anyDefinitions = false;
					for (IType t : predType)
						if (t instanceof Definition)
							anyDefinitions = true;
					return anyDefinitions;
				}
				@Override
				public IType type(CallDeclaration node, ScriptProcessor processor) {
					IType type = declarationType(node, processor);
					if (type instanceof FunctionType)
						return ((FunctionType)type).prototype().returnType();
					else
						return type;
				}
				@Override
				public void visit(CallDeclaration node, ScriptProcessor processor) throws ParsingException {
					super.visit(node, processor);

					CachedEngineDeclarations cachedEngineDeclarations = processor.cachedEngineDeclarations();
					String declarationName = node.name();
					Declaration declaration = node.declaration();
					ASTNode[] params = node.params();
					ASTNode predecessor = node.predecessorInSequence();

					// return as function
					if (declarationName.equals(Keywords.Return)) {
						if (processor.strictLevel >= 2)
							processor.markers().error(processor, Problem.ReturnAsFunction, node, node, Markers.NO_THROW);
						else
							processor.markers().warning(processor, Problem.ReturnAsFunction, node, node, 0);
					} else // variable as function
						if (declaration instanceof Variable) {
							((Variable)declaration).setUsed(true);
							IType type = declarationType(node, processor);
							// no warning when in #strict mode
							if (processor.strictLevel >= 2)
								if (declaration != cachedEngineDeclarations.This && declaration != Variable.THIS && !PrimitiveType.FUNCTION.canBeAssignedFrom(type))
									processor.markers().warning(processor, Problem.VariableCalled, node, node, 0, declaration.name(), type.typeName(false));
						} else if (declaration instanceof Function) {
							Function f = (Function)declaration;
							if (f.visibility() == FunctionScope.GLOBAL || predecessor != null)
								processor.script().addUsedScript(f.script());

							SpecialFuncRule rule = processor.specialRuleFor(node, SpecialEngineRules.ARGUMENT_VALIDATOR);
							boolean specialCaseHandled =
								rule != null &&
								rule.validateArguments(node, params, processor);

							// not a special case... check regular parameter types
							if (!specialCaseHandled) {
								int givenParam = 0;
								for (Variable parm : f.parameters()) {
									if (givenParam >= params.length)
										break;
									ASTNode given = params[givenParam++];
									if (given == null)
										continue;
									if (!validForType(given, parm.type(), processor)) {
										validForType(given, parm.type(), processor);
										processor.incompatibleTypes(node, given, parm.type(), ty(given, processor));
									}
									//else
										//judgement(given, parm.type(), TypingJudgementMode.Unify, processor);
								}
							}
					}
					else if (declaration == null)
						if (unknownFunctionShouldBeError(node, processor)) {
							int start = node.start();
							processor.markers().error(processor, Problem.UndeclaredIdentifier, node, start, start+declarationName.length(), Markers.NO_THROW, declarationName);
						}
				}
				@Override
				public ITypeVariable createTypeVariable(CallDeclaration node, ScriptProcessor processor) {
					Declaration d = node.declaration();
					CachedEngineDeclarations cache = processor.cachedEngineDeclarations();
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
						return new ExpressionTypeVariable(node, processor);
					return super.createTypeVariable(node, processor);
				}
				@Override
				public boolean isModifiable(CallDeclaration node, ScriptProcessor processor) {
					Declaration declaration = node.declaration();
					IType t = declaration instanceof Function ? ((Function)declaration).returnType() : PrimitiveType.UNKNOWN;
					return t.canBeAssignedFrom(PrimitiveType.REFERENCE) || t.canBeAssignedFrom(PrimitiveType.UNKNOWN);
				}
			},

			new AccessDeclarationExpert<CallInherited>(CallInherited.class) {
				@Override
				public IType type(CallInherited node, ScriptProcessor processor) {
					Function inherited = node.parentOfType(Function.class).inheritedFunction();
					if (inherited != null) {
						processor.startRoaming(inherited.script());
						ITypeVariable ty = processor.visitFunction(inherited);
						if (ty != null)
							processor.inheritedReturnType = ty;
						else
							ty = processor.inheritedReturnType;
						processor.endRoaming();
						return ty != null ? ty.type() : PrimitiveType.UNKNOWN;
					} else
						return PrimitiveType.UNKNOWN;
				}
				@Override
				public void visit(CallInherited node, ScriptProcessor processor) throws ParsingException {
					if (!processor.roaming || node.parentOfType(Script.class) == processor.script()) {
						// inherited/_inherited not allowed in non-strict mode
						if (processor.strictLevel <= 0)
							processor.markers().error(processor, Problem.InheritedDisabledInStrict0, node, node, Markers.NO_THROW);

						node.setDeclaration(node.parentOfType(Function.class).inheritedFunction());
						if (node.declaration() == null && !node.failsafe())
							processor.markers().error(processor, Problem.NoInheritedFunction, node, node, Markers.NO_THROW, node.parentOfType(Function.class).name());
					}
				}
			},

			new Expert<Sequence>(Sequence.class) {
				@Override
				public IType type(Sequence node, ScriptProcessor processor) {
					ASTNode[] elements = node.subElements();
					return (elements == null || elements.length == 0)
						? PrimitiveType.UNKNOWN
						: ty(elements[elements.length-1], processor);
				}
				@Override
				public void assignment(Sequence leftSide, ASTNode rightSide, ScriptProcessor processor) {
					ASTNode lastElement = leftSide.lastElement();
					expert(lastElement).assignment(lastElement, rightSide, processor);
				}
				@Override
				public void visit(Sequence node, ScriptProcessor processor) throws ParsingException {
					supr.visit(node, processor);
					ASTNode p = null;
					for (ASTNode e : node.subElements()) {
						if (
							(e != null && !e.isValidInSequence(p)) ||
							(p != null && !p.allowsSequenceSuccessor(e))
						)
							processor.markers().error(processor, Problem.NotAllowedHere, node, e, Markers.NO_THROW, e);
						p = e;
					}
					if (p != null && !p.isValidAtEndOfSequence())
						processor.markers().error(processor, Problem.NotFinished, node, node, Markers.NO_THROW, node.printed());
				}
				@Override
				public boolean isModifiable(Sequence node, ScriptProcessor processor) {
					ASTNode[] elements = node.subElements();
					if (elements != null && elements.length > 0) {
						ASTNode last = elements[elements.length-1];
						return expert(last).isModifiable(last, processor);
					} else
						return false;
				}
			},

			new Expert<ArraySliceExpression>(ArraySliceExpression.class) {
				@Override
				public IType type(ArraySliceExpression node, ScriptProcessor processor) {
					ArrayType arrayType = predecessorTypeAs(node, ArrayType.class, processor);
					if (arrayType != null)
						return node.lo() == null && node.hi() == null ? arrayType : arrayType.typeForSlice(
							ASTNode.evaluateStatic(node.lo(), processor),
							ASTNode.evaluateStatic(node.hi(), processor)
						);
					else
						return PrimitiveType.ARRAY;
				}
				@Override
				public void assignment(ArraySliceExpression leftSide, ASTNode rightSide, ScriptProcessor processor) {
					ArrayType arrayType = predecessorTypeAs(leftSide, ArrayType.class, processor);
					IType sliceType = ty(rightSide, processor);
					if (arrayType != null)
						processor.storeType(leftSide.predecessorInSequence(), arrayType.modifiedBySliceAssignment(
							ASTNode.evaluateStatic(leftSide.lo(), processor),
							ASTNode.evaluateStatic(leftSide.hi(), processor),
							sliceType
							));
				}
			},

			new Expert<Literal>(Literal.class) {
				@Override
				public boolean typingJudgement(Literal node, IType type, ScriptProcessor processor, TypingJudgementMode mode) {
					// constantly steadfast do i resist the pressure of expectancy lied upon me
					return true;
				}
				@Override
				public void assignment(Literal leftSide, ASTNode rightSide, ScriptProcessor processor) { /* don't care */ }
				@Override
				public ITypeVariable createTypeVariable(Literal node, ScriptProcessor processor) { return null; /* nope */ }
				@Override
				public boolean isModifiable(Literal node, ScriptProcessor processor) { return false; }
			},

			new Expert<Nil>(Nil.class) {
				@Override
				public IType type(Nil node, ScriptProcessor processor) {
					return PrimitiveType.UNKNOWN;
				}
				@Override
				public void visit(Nil node, ScriptProcessor processor) throws ParsingException {
					if (!processor.script().engine().settings().supportsNil)
						processor.markers().error(processor, Problem.NotSupported, node, node, Markers.NO_THROW, Keywords.Nil, processor.script().engine().name());
				}
			},

			new Expert<StringLiteral>(StringLiteral.class) {
				@Override
				public IType type(StringLiteral node, ScriptProcessor processor) { return PrimitiveType.STRING; }
				@Override
				public void visit(StringLiteral node, ScriptProcessor processor) throws ParsingException {
					// warn about overly long strings
					long max = processor.script().index().engine().settings().maxStringLen;
					String lit = node.literal();
					if (max != 0 && lit.length() > max)
						processor.markers().warning(processor, Problem.StringTooLong, node, node, lit.length(), max);

					// stringtbl entries
					// don't warn in #appendto scripts because those will inherit their string tables from the scripts they are appended to
					// and checking for the existence of the table entries there is overkill
					if (processor.hasAppendTo || processor.script().resource() == null)
						return;
					String value = lit;
					int valueLen = value.length();
					// warn when using non-declared string tbl entries
					for (int i = 0; i < valueLen;) {
						if (i+1 < valueLen && value.charAt(i) == '$') {
							EntityRegion region = StringTbl.entryRegionInString(lit, node.start(), (i+1));
							if (region != null) {
								StringTbl.reportMissingStringTblEntries(processor, region, node);
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
				public IType type(IntegerLiteral node, ScriptProcessor processor) {
					if (node.longValue() == 0 && processor.script().engine().settings().zeroIsAny)
						return PrimitiveType.ANY;
					else
						return PrimitiveType.INT;
				}
			},

			new Expert<FloatLiteral>(FloatLiteral.class) {
				@Override
				public void visit(FloatLiteral node, ScriptProcessor processor) throws ParsingException {
					if (!processor.script().engine().settings().supportsFloats)
						processor.markers().error(processor, Problem.FloatNumbersNotSupported, node, node, Markers.NO_THROW);
					supr.visit(node, processor);
				}
			},

			new Expert<IDLiteral>(IDLiteral.class) {
				@Override
				public IType type(IDLiteral node, ScriptProcessor processor) {
					Definition obj = processor.script().nearestDefinitionWithId(node.idValue());
					return obj != null ? obj.metaDefinition() : PrimitiveType.ID;
				}
			},

			new Expert<BoolLiteral>(BoolLiteral.class) {
				@Override
				public IType type(BoolLiteral node, ScriptProcessor processor) {
					return PrimitiveType.BOOL;
				}
			},

			new Expert<CallExpr>(CallExpr.class) {
				@Override
				public IType type(CallExpr node, ScriptProcessor processor) {
					ASTNode pred = node.predecessorInSequence();
					IType type = ty(pred, processor);
					if (type instanceof FunctionType)
						return ((FunctionType)type).prototype().returnType();
					else
						return PrimitiveType.ANY;
				}
				@Override
				public void visit(CallExpr node, ScriptProcessor processor) throws ParsingException {
					if (!processor.script().engine().settings().supportsFunctionRefs)
						processor.markers().error(processor, Problem.FunctionRefNotAllowed, node, node, Markers.NO_THROW, processor.script().engine().name());
					else {
						IType type = expert(node.predecessorInSequence()).type(node.predecessorInSequence(), processor);
						if (!PrimitiveType.FUNCTION.canBeAssignedFrom(type))
							processor.markers().error(processor, Problem.CallingExpression, node, node, Markers.NO_THROW);
					}
				}
			},

			new Expert<Statement>(Statement.class) {
				@Override
				public IType type(Statement node, ScriptProcessor processor) {
					return PrimitiveType.UNKNOWN;
				}
				/**
				 * Emit a warning if this expression is erroneously used at a place where only expressions with side effects are allowed.
				 * @param processor The processor
				 */
				public void warnIfNoSideEffects(Statement node, ScriptProcessor processor) {
					if (node.parent() instanceof IterateArrayStatement && ((IterateArrayStatement)node.parent()).elementExpr() == node)
						return;
					if (!node.hasSideEffects())
						processor.markers().warning(processor, Problem.NoSideEffects, node, node, 0);
				}
				@Override
				public void visit(Statement node, ScriptProcessor processor) throws ParsingException {
					supr.visit(node, processor);
					warnIfNoSideEffects(node, processor);
					if (processor.controlFlow != ControlFlow.Continue)
						processor.markers().warning(processor, Problem.NeverReached, node, node, 0);
				}
			},

			new Expert<VarDeclarationStatement>(VarDeclarationStatement.class) {
				@Override
				public void visit(VarDeclarationStatement node, ScriptProcessor processor) throws ParsingException {
					supr.visit(node, processor);
					for (VarInitialization initialization : node.variableInitializations())
						if (initialization.variable != null)
							if (initialization.expression != null) {
								IType initializationType = ty(initialization.expression, processor);
								if (
									initialization.variable.staticallyTyped() &&
									!initialization.variable.type().canBeAssignedFrom(initializationType)
								)
									processor.incompatibleTypes(
										node,
										initialization.expression,
										initialization.variable.type(), initializationType
									);
								else {
									AccessVar av = new AccessVar(initialization.variable);
									judgement(av, initializationType, TypingJudgementMode.Unify, processor);
								}
							}
				}
			},

			new Expert<PropListExpression>(PropListExpression.class) {
				@Override
				public IType type(PropListExpression node, ScriptProcessor processor) {
					return node.definedDeclaration();
				}
				@Override
				public void visit(PropListExpression node, ScriptProcessor processor) throws ParsingException {
					supr.visit(node, processor);
					if (!processor.script().engine().settings().supportsProplists)
						processor.markers().error(processor, Problem.NotSupported, node, node, Markers.NO_THROW,
							net.arctics.clonk.parser.c4script.ast.Messages.PropListExpression_ProplistsFeature,
							processor.script().engine().name());
					for (Variable v : node.components())
						if (v.initializationExpression() != null)
							judgement(new AccessVar(v), ty(v.initializationExpression(), processor), TypingJudgementMode.Unify, processor);
				}
				@Override
				public boolean isModifiable(PropListExpression node, ScriptProcessor processor) { return false; }
			},

			new Expert<Parenthesized>(Parenthesized.class) {
				@Override
				public IType type(Parenthesized node, ScriptProcessor processor) {
					return ty(node.innerExpression(), processor);
				}
				@Override
				public boolean isModifiable(Parenthesized node, ScriptProcessor processor) {
					return expert(node.innerExpression()).isModifiable(node.innerExpression(), processor);
				}
			},

			new Expert<MemberOperator>(MemberOperator.class) {
				@Override
				public IType type(MemberOperator node, ScriptProcessor processor) {
					if (node.id() != null)
						return processor.script().nearestDefinitionWithId(node.id());
					// stuff before -> decides
					ASTNode pred = node.predecessorInSequence();
					return pred != null ? ty(pred, processor) : supr.type(node, processor);
				}
				@Override
				public boolean typingJudgement(MemberOperator node, IType type, ScriptProcessor processor, TypingJudgementMode mode) {
					ASTNode p = node.predecessorInSequence();
					return p != null ? expert(p).typingJudgement(p, type, processor, mode) : false;
				}
				@Override
				public void visit(MemberOperator node, ScriptProcessor processor) throws ParsingException {
					supr.visit(node, processor);
					ASTNode pred = node.predecessorInSequence();
					EngineSettings settings = processor.script().engine().settings();
					if (pred != null) {
						IType requiredType = node.dotNotation() ? PrimitiveType.PROPLIST : TypeChoice.make(PrimitiveType.OBJECT, PrimitiveType.ID);
						ASTNode sequenceTilMe = pred.sequenceTilMe();
						Expert<? super ASTNode> stmReporter = expert(sequenceTilMe);
						if (!stmReporter.typingJudgement(sequenceTilMe, requiredType, processor, TypingJudgementMode.Hint))
							processor.markers().warning(processor, node.dotNotation() ? Problem.NotAProplist : Problem.CallingMethodOnNonObject, node, node, 0,
								ty(sequenceTilMe, stmReporter, processor).typeName(false));
					}
					if (node.getLength() > 3 && !settings.spaceAllowedBetweenArrowAndTilde)
						processor.markers().error(processor, Problem.MemberOperatorWithTildeNoSpace, node, node, Markers.NO_THROW);
					if (node.dotNotation() && !settings.supportsProplists)
						processor.markers().error(processor, Problem.DotNotationNotSupported, node, node, Markers.NO_THROW, node);
				}
			},

			new Expert<IterateArrayStatement>(IterateArrayStatement.class) {
				@Override
				public boolean skipReportingProblemsForSubElements() { return true; }
				@Override
				public void visit(IterateArrayStatement node, ScriptProcessor processor) throws ParsingException {
					ControlFlow t = processor.controlFlow;
					processor.controlFlow = ControlFlow.Continue;
					Variable loopVariable;
					AccessVar accessVar;
					ASTNode elementExpr = node.elementExpr();
					ASTNode arrayExpr = node.arrayExpr();
					if (elementExpr instanceof VarDeclarationStatement)
						loopVariable = ((VarDeclarationStatement)elementExpr).variableInitializations()[0].variable;
					else if ((accessVar = as(SimpleStatement.unwrap(elementExpr), AccessVar.class)) != null) {
						Declaration d = processor.obtainDeclaration(accessVar);
						if (d == null) {
							// implicitly create loop variable declaration if not found
							SourceLocation varPos = processor.absoluteSourceLocationFromExpr(accessVar);
							loopVariable = processor.parser.createVarInScope(node.parentOfType(Function.class), accessVar.name(), Scope.VAR, varPos.start(), varPos.end(), null);
						} else
							loopVariable = as(d, Variable.class);
					} else
						loopVariable = null;

					processor.visitNode(elementExpr, true);
					processor.visitNode(arrayExpr, true);

					IType type = ty(arrayExpr, processor);
					if (!type.canBeAssignedFrom(PrimitiveType.ARRAY))
						processor.incompatibleTypes(node, arrayExpr, type, PrimitiveType.ARRAY);
					IType elmType = ArrayType.elementTypeSet(type);
					processor.newTypeEnvironment();
					{
						if (loopVariable != null) {
							loopVariable.setUsed(true);
							judgement(new AccessVar(loopVariable), elmType, TypingJudgementMode.Unify, processor);
						}
						processor.visitNode(node.body(), true);
					}
					processor.endTypeEnvironment(true, false);
					processor.controlFlow = t;
				}
			},

			new Expert<SimpleStatement>(SimpleStatement.class) {
				@Override
				public void visit(SimpleStatement node, ScriptProcessor processor) throws ParsingException {
					BinaryOp op = as(node.expression(), BinaryOp.class);
					if (op != null && !op.operator().modifiesArgument())
						processor.markers().warning(processor, Problem.NoAssignment, node, op, 0);
					supr.visit(node, processor);
				}
			},

			new ConditionalStatementProblemReporter<IfStatement>(IfStatement.class) {
				@Override
				public void visit(IfStatement node, ScriptProcessor processor) throws ParsingException {
					ControlFlow old = processor.controlFlow;
					ASTNode condition = node.condition();
					processor.visitNode(condition, true);
					// use two separate type environments for if and else statement, merging
					// gathered information afterwards
					TypeEnvironment ifEnvironment = processor.newTypeEnvironment();
					processor.visitNode(node.body(), true);
					processor.endTypeEnvironment(false, false);
					processor.controlFlow = old;
					if (node.elseExpression() != null) {
						TypeEnvironment elseEnvironment = processor.newTypeEnvironment();
						processor.visitNode(node.elseExpression(), true);
						processor.endTypeEnvironment(false, false);
						ifEnvironment.inject(elseEnvironment, false);
					}
					if (ifEnvironment.up != null)
						ifEnvironment.up.inject(ifEnvironment, false);
					processor.controlFlow = old;

					if (!condition.containsConst()) {
						Object condEv = PrimitiveType.BOOL.convert(condition.evaluateStatic(node.parentOfType(Function.class)));
						if (condEv != null && condEv != ASTNode.EVALUATION_COMPLEX)
							processor.markers().warning(processor,
								condEv.equals(true) ? Problem.ConditionAlwaysTrue : Problem.ConditionAlwaysFalse,
								condition, condition, 0, condition);
					}
				};
			},

			new ConditionalStatementProblemReporter<ForStatement>(ForStatement.class) {
				@Override
				public void visit(ForStatement node, ScriptProcessor processor) throws ParsingException {
					if (node.initializer() != null)
						processor.visitNode(node.initializer(), true);
					super.visit(node, processor);
					if (node.increment() != null)
						processor.visitNode(node.increment(), true);
				}
			},

			new ConditionalStatementProblemReporter<WhileStatement>(WhileStatement.class),

			new Expert<NewProplist>(NewProplist.class) {
				@Override
				public void visit(NewProplist node, ScriptProcessor processor) throws ParsingException {
					node.definedDeclaration().setPrototype(as(ty(node.prototype(), processor), ProplistDeclaration.class));
				}
			},

			new Expert<Placeholder>(Placeholder.class) {
				@Override
				public void visit(Placeholder node, ScriptProcessor processor) throws ParsingException {
					StringTbl.reportMissingStringTblEntries(processor, new EntityRegion(null, node, node.entryName()), node);
				}
			},

			new Expert<MissingStatement>(MissingStatement.class) {
				@Override
				public void visit(MissingStatement node, ScriptProcessor processor) throws ParsingException {
					processor.markers().error(processor, Problem.MissingStatement, node, node, Markers.NO_THROW);
				}
			},

			new Expert<GarbageStatement>(GarbageStatement.class) {
				@Override
				public void visit(GarbageStatement node, ScriptProcessor processor) throws ParsingException {
					processor.markers().error(processor, Problem.Garbage, node, node, Markers.NO_THROW, node.garbage());
				}
			},

			new Expert<FunctionDescription>(FunctionDescription.class) {
				@Override
				public void visit(FunctionDescription node, ScriptProcessor processor) throws ParsingException {
					if (processor.hasAppendTo)
						return;
					int off = 1;
					for (String part : node.contents().split("\\|")) { //$NON-NLS-1$
						if (part.startsWith("$") && part.endsWith("$")) { //$NON-NLS-1$ //$NON-NLS-2$
							StringTbl stringTbl = processor.script().localStringTblMatchingLanguagePref();
							String entryName = part.substring(1, part.length()-1);
							if (stringTbl == null || stringTbl.map().get(entryName) == null)
								processor.markers().warning(processor, Problem.UndeclaredIdentifier, node,
									new Region(node.start()+off, part.length()), 0, entryName);
						}
						off += part.length()+1;
					}
				}
			},

			new Expert<Comment>(Comment.class) {
				@Override
				public void visit(Comment node, ScriptProcessor processor) throws ParsingException {
					if (!processor.roaming || node.parentOfType(Script.class) == processor.script()) {
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
								processor.markers().todo(processor.file(), node, s.substring(todoIndex, lineEnd), node.start()+2+todoIndex, node.start()+2+lineEnd, markerPriority);
							}
						} while (markerPriority > IMarker.PRIORITY_LOW);
					}
				}
			},

			new Expert<Unfinished>(Unfinished.class) {
				@Override
				public IType type(Unfinished node, ScriptProcessor processor) {
					return ty(node.expression(), processor);
				}
				@Override
				public void visit(Unfinished node, ScriptProcessor processor) throws ParsingException {
					processor.markers().error(processor, Problem.NotFinished, node, node, Markers.NO_THROW, node);
				}
			}

		};
		for (Expert<?> reporter : reporters)
			problemReporters.put(reporter.cls(), reporter);
		for (Expert<?> reporter : reporters)
			reporter.findSuper();
	}

}
