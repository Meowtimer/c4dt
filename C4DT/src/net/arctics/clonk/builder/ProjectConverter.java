package net.arctics.clonk.builder;

import static java.lang.String.format;
import static java.lang.System.out;
import static net.arctics.clonk.util.ArrayUtil.concat;
import static net.arctics.clonk.util.StreamUtil.ofType;
import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.defaulting;
import static net.arctics.clonk.util.Utilities.findMemberCaseInsensitively;
import static net.arctics.clonk.util.Utilities.runWithoutAutoBuild;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ast.Structure;
import net.arctics.clonk.ast.TraversalContinuation;
import net.arctics.clonk.c4group.C4Group.GroupType;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.Operator;
import net.arctics.clonk.c4script.ProplistDeclaration;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.c4script.Variable.Scope;
import net.arctics.clonk.c4script.ast.AccessDeclaration;
import net.arctics.clonk.c4script.ast.AccessVar;
import net.arctics.clonk.c4script.ast.BinaryOp;
import net.arctics.clonk.c4script.ast.CallDeclaration;
import net.arctics.clonk.c4script.ast.FunctionDescription;
import net.arctics.clonk.c4script.ast.IDLiteral;
import net.arctics.clonk.c4script.ast.IfStatement;
import net.arctics.clonk.c4script.ast.IntegerLiteral;
import net.arctics.clonk.c4script.ast.Nil;
import net.arctics.clonk.c4script.ast.PropListExpression;
import net.arctics.clonk.c4script.ast.StringLiteral;
import net.arctics.clonk.c4script.effect.Effect;
import net.arctics.clonk.c4script.typing.PrimitiveType;
import net.arctics.clonk.index.CodeTransformer;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.ID;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.IndexEntity;
import net.arctics.clonk.ini.ActMapUnit;
import net.arctics.clonk.ini.DefCoreUnit;
import net.arctics.clonk.ini.IniEntry;
import net.arctics.clonk.ini.IniUnit;
import net.arctics.clonk.landscapescript.LandscapeScript;
import net.arctics.clonk.util.Pair;
import net.arctics.clonk.util.StringUtil;
import net.arctics.clonk.util.TaskExecution;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

/**
 * Converts projects for one engine into projects for another engine.
 * @author madeen
 *
 */
public class ProjectConverter implements IResourceVisitor, Runnable {

	class DeclarationConversion implements Runnable {
		final IResource origin;
		final IResource target;
		final Declaration declaration;
		public DeclarationConversion(IResource origin, IResource target, Declaration declaration) {
			super();
			this.origin = origin;
			this.target = target;
			this.declaration = declaration;
		}
		class FileConversion {
			final Structure original;
			final IFile target;
			Structure converted;
			public FileConversion(Structure original, IFile target) {
				super();
				this.original = original;
				this.target = target;
			}
			FileConversion convert() {
				converted = as(transformer.convert(original, original), Structure.class);
				return this;
			}
			void write() {
				if (converted instanceof IndexEntity)
					((IndexEntity)converted).forceIndex(destinationIndex);
				converted.forceParents();
				final String convString = converted.printed();
				try {
					target.setContents(new ByteArrayInputStream(convString.getBytes()), true, true, null);
				} catch (final CoreException e) {
					e.printStackTrace();
				}
			}
		}
		@Override
		public void run() {
			try {
				final IFolder originFolder = as(origin, IFolder.class);
				final IFolder targetFolder = as(target, IFolder.class);
				final Map<IFile, FileConversion> sub = new HashMap<>();
				if (originFolder != null && targetFolder != null)
					Arrays.stream(originFolder.members(IResource.FILE)).filter(r -> r instanceof IFile).forEach(res -> {
						final IFile file = (IFile)res;
						final Structure struct = Structure.pinned(file, false, false);
						if (struct != null) {
							if (struct instanceof LandscapeScript)
								return; // can't handle that
							final IFile target = targetFolder.getFile(file.getName());
							if (target.exists())
								sub.put(file, new FileConversion(struct, target));
						}
					});
				else
					sub.put((IFile) origin, new FileConversion((Structure) declaration, (IFile)target).convert());

				sub.values().forEach(FileConversion::convert);

				final Script script = as(declaration, Script.class);
				final FileConversion scriptFile = script != null ? sub.get(script.file()) : null;

				if (originFolder != null && targetFolder != null) {
					final Definition definition = as(declaration, Definition.class);
					if (definition != null && originFolder != null) {
						final Supplier<ID> mapID = () -> transformer.idMap().getOrDefault(definition.id(), null);
						final ID mapped = mapID.get();
						if (mapped != definition.id()) {
							final FileConversion defCore = sub.get(findMemberCaseInsensitively(originFolder, DefCoreUnit.FILE_NAME));
							final IniUnit defCoreUnit = defCore != null ? as(defCore.converted, IniUnit.class) : null;
							if (defCoreUnit != null) {
								final IniEntry entry = defCoreUnit.entryInSection("DefCore", "id");
								if (entry != null)
									entry.value(new IDLiteral(mapped));
							}
						}
						final FileConversion actMap = sub.get(findMemberCaseInsensitively(originFolder, ActMapUnit.FILE_NAME));
						final ActMapUnit actMapUnit = actMap != null ? as(actMap.converted, ActMapUnit.class) : null;
						if (actMapUnit != null)
							if (scriptFile != null && scriptFile.converted instanceof Script) {
								((Script)scriptFile.converted).addDeclaration(
									new Variable(Scope.LOCAL, "ActMap", new PropListExpression(actMapUnit.toProplist()))
								);
								actMap.target.delete(true, null);
								sub.remove(actMap.original.file());
							}
					}
				}

				if (script != null) {
					final List<Pair<Function, FunctionDescription>> descs = new ArrayList<>(5);
					script.traverse(node -> {
						if (node instanceof FunctionDescription)
							descs.add(Pair.pair(node.parent(Function.class), (FunctionDescription) node));
						return TraversalContinuation.Continue;
					});
					if (!descs.isEmpty()) {
						final ProplistDeclaration dec = new ProplistDeclaration(descs.stream()
							.map(d -> new Variable(
								null,
								d.first().name(), new PropListExpression(new ProplistDeclaration(
									Arrays.stream(d.second().splitContents())
										.map(p -> {
											final String n = defaulting(p.first(), "Desc");
											ASTNode v;
											switch (n) {
											case "Method":
												v = new AccessVar("METHOD_"+p.second());
												break;
											default:
												v = new StringLiteral(p.second());
												break;
											}
											return new Variable(Scope.VAR, n, v);
										})
										.collect(Collectors.toList())
								))
							))
							.collect(Collectors.toList())
						);
						descs.forEach(d -> d.second().parent().removeSubElement(d.second()));
						((Script)scriptFile.converted).addDeclaration(new Variable(Scope.STATIC, "FunctionDescriptions", new PropListExpression(dec)));
					}
				}

				sub.values().forEach(FileConversion::write);
			} catch (final CoreException e) {
				e.printStackTrace();
			}
		}
	}

	private final ClonkProjectNature sourceProject, destinationProject;
	private final CodeTransformer transformer;
	private Engine sourceEngine() { return sourceProject.index().engine(); }
	private Engine targetEngine() { return destinationProject.index().engine(); }
	private IProgressMonitor monitor;
	private final List<DeclarationConversion> conversions = new LinkedList<>();
	private final List<Declaration> needResaving = Collections.synchronizedList(new LinkedList<>());
	private Index destinationIndex;

	public CodeTransformer configuration() { return transformer; }
	/**
	 * Create a new converter by specifying source and destination.
	 * @param sourceProject Source project
	 * @param destinationProject Destination project
	 */
	public ProjectConverter(final IProject sourceProject, final IProject destinationProject) {
		this.sourceProject = ClonkProjectNature.get(sourceProject);
		this.destinationProject = ClonkProjectNature.get(destinationProject);
		this.transformer = targetEngine().loadCodeTransformer(sourceEngine());
		assert(sourceEngine() != targetEngine());
		this.destinationIndex = new Index() {
			private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
			@Override
			public Engine engine() { return ProjectConverter.this.destinationProject.index().engine(); }
		};
	}
	public static IPath convertPath(Engine sourceEngine, Engine targetEngine, final IPath path) {
		IPath result = new Path("");
		for (int i = 0; i < path.segmentCount(); i++) {
			String segment = path.segment(i);
			final GroupType groupType = sourceEngine.groupTypeForFileName(segment);
			if (groupType != GroupType.OtherGroup)
				segment = targetEngine.groupName(StringUtil.rawFileName(segment), groupType);
			result = result.append(segment);
		}
		return result;
	}
	@Override
	public void run() {
		try {
			sourceProject.getProject().accept(this);
			TaskExecution.threadPool(conversions, 20);
		} catch (final CoreException e) {
			e.printStackTrace();
		}
	}
	/**
	 * Perform the conversion, reporting progress back to a {@link IProgressMonitor}
	 * @param monitor The monitor to report progress to
	 */
	public void convert(final IProgressMonitor monitor) {
		this.monitor = monitor;
		this.needResaving.clear();
		runWithoutAutoBuild(this);
		copyCompatibilityFiles();
		postProcess();
		saveThoseInNeedOfResaving();
	}

	public void saveThoseInNeedOfResaving() {
		needResaving.forEach(dec -> {
			try {
				Core.instance().performActionsOnFileDocument(dec.file(), d -> {
					d.set(dec.printed());
					return null;
				}, true);
			} catch (final Exception e) {
				e.printStackTrace();
			}
		});
	}

	private static void forceCreate(IFile file, InputStream value, NullProgressMonitor npm) {
		final List<IContainer> containers = new LinkedList<>();
		for (IContainer c = file.getParent(); c != null; c = c.getParent())
			containers.add(0, c);
		ofType(containers.stream(), IFolder.class).forEach(f -> {
			try {
				f.create(true, true, npm);
			} catch (final CoreException e) {}
		});
		try {
			file.create(value, true, npm);
		} catch (final CoreException e) {
			e.printStackTrace();
		}
	}

	private void copyCompatibilityFiles() {
		final NullProgressMonitor npm = new NullProgressMonitor();
		transformer.compatibilityFiles().forEach((key, value) -> {
			final Path path = new Path(key);
			try {
				final IFile file = destinationProject.getProject().getFile(path);
				if (file.exists())
					file.setContents(new ByteArrayInputStream(value), IResource.FORCE, npm);
				else
					forceCreate(file, new ByteArrayInputStream(value), npm);
			} catch (final Exception e) {
				e.printStackTrace();
			}
		});
	}

	public void postProcess() {
		final NullProgressMonitor npm = new NullProgressMonitor();
		try {
			destinationProject.getProject().build(IncrementalProjectBuilder.CLEAN_BUILD, npm);
			destinationProject.getProject().build(IncrementalProjectBuilder.FULL_BUILD, npm);
		} catch (final CoreException ce) {}
		final List<Runnable> list = new LinkedList<>();
		destinationProject.index().allScripts(s -> list.add(() -> {
			final boolean[] doSave = new boolean[1];
			s.traverse((node, ctx) -> {
				if (
					applyFix(node, CallDeclaration.class, ProjectConverter::fixSomeArgumentTypes) ||
					applyFix(node, IfStatement.class, ProjectConverter::fixIfEffect) ||
					applyFix(node, Function.class, ProjectConverter::fixParms) ||
					applyFix(node, AccessDeclaration.class, ProjectConverter::fixUndeclaredIdentifier)
				)
					doSave[0] = true;
				return TraversalContinuation.Continue;
			}, null);
			if (doSave[0])
				needResaving.add(s);
		}));
		TaskExecution.threadPool(list, 20);
	}

	@FunctionalInterface
	public interface Fix<T> {
		boolean fix(T node);
	}
	@SuppressWarnings("unchecked")
	private static <T> boolean applyFix(ASTNode node, Class<? extends ASTNode> cls, Fix<T> fix) {
		if (cls.isInstance(node))
			return fix.fix((T)node);
		else
			return false;
	}

	private static boolean fixParms(Function node) {
		final Set<String> got = new HashSet<>();
		final boolean fixedParms = node.parameters().stream().reduce(false, (num, v) -> {
			if (v.type() == PrimitiveType.REFERENCE) {
				v.forceType(PrimitiveType.ANY);
				return true;
			} else if (!got.add(v.name())) {
				final String mut = IntStream.iterate(2, x -> x + 1)
					.mapToObj(x -> v.name()+Integer.valueOf(x))
					.filter(got::add)
					.findFirst()
					.orElseThrow(() -> new IllegalStateException());
				v.setName(mut);
				return true;
			} else
				return false;
		}, (a, b) -> a || b);
		if (node.returnType() == PrimitiveType.REFERENCE) {
			node.forceType(PrimitiveType.ANY);
			return true;
		}
		return fixedParms;
	}

	private static boolean fixSomeArgumentTypes(CallDeclaration node) {
		final Function fn = node.function();
		if (fn != null) {
			final ASTNode[] converted = IntStream.range(0, node.params().length).mapToObj(x -> {
				final Variable par = fn.parameter(x);
				final ASTNode arg = node.params()[x];
				if (arg != null && arg.equals(IntegerLiteral.ZERO) && par != null && par.type() != PrimitiveType.INT)
					return new Nil();
				else
					return arg;
			}).toArray(l -> new ASTNode[l]);
			if (!Arrays.equals(node.params(), converted)) {
				node.setParams(converted);
				return true;
			}
		}
		return false;
	}

	private static boolean fixUndeclaredIdentifier(AccessDeclaration v) {
		if (v.declaration() == null && v.predecessor() == null) {
			final CallDeclaration call = as(v, CallDeclaration.class);
			if (v instanceof AccessVar)
				v.parent().replaceSubElement(v, new CallDeclaration("EvaluateID", new StringLiteral(v.name())), 0);
			else if (call != null) {
				call.setParams(concat(new StringLiteral(call.name()), call.params()));
				call.setName("UnknownFunction");
			}
			return true;
		} else
			return false;
	}

	private static boolean fixIfEffect(IfStatement check) {
		final Script script = check.parent(Script.class);
		final BinaryOp comp = as(check.condition(), BinaryOp.class);
		if (comp != null) {
			final boolean leftSideIsEffect = script.typings().get(comp.leftSide()) instanceof Effect;
			final boolean rightSideIsZero = IntegerLiteral.ZERO.equals(comp.rightSide());
			final boolean elligible = leftSideIsEffect && rightSideIsZero;
			if (elligible) {
				Operator conv;
				switch (comp.operator()) {
				case SmallerEqual:
					conv = Operator.Equal;
					break;
				case Larger:
					conv = Operator.NotEqual;
					break;
				default:
					conv = null;
				}
				if (conv != null) {
					comp.operator(conv);
					comp.setRightSide(new Nil());
					return true;
				} else
					return false;
			} else
				return false;
		} else
			return false;
	}

	/**
	 * By letting this converter visit the source project the actual conversion is performed.
	 */
	@Override
	public boolean visit(final IResource origin) throws CoreException {
		if (origin instanceof IProject || skipResource(origin))
			return true;
		final IPath path = convertPath(sourceEngine(), targetEngine(), origin.getProjectRelativePath());
		if (origin instanceof IFile) {
			final IFile sourceFile = (IFile) origin;
			final IFile targetFile = destinationProject.getProject().getFile(path);
			try (InputStream contents = sourceFile.getContents()) {
				if (targetFile.exists())
					targetFile.setContents(contents, true, true, monitor);
				else
					targetFile.create(contents, true, monitor);
				if (sourceFile.getParent() == null || Definition.at(sourceFile.getParent()) == null) {
					final Structure struct = Structure.pinned(sourceFile, true, false);
					if (struct != null)
						conversions.add(new DeclarationConversion(sourceFile, targetFile, struct));
				}
			} catch (final Exception e) {
				out.println(format("Failed to convert contents of %s: %s", origin.getFullPath(), e.getMessage()));
				e.printStackTrace();
			}
			return true;
		} else if (origin instanceof IFolder) {
			final IFolder container = destinationProject.getProject().getFolder(path);
			if (!container.exists())
				container.create(true, true, monitor);
			final Definition def = Definition.at((IContainer) origin);
			if (def != null)
				conversions.add(new DeclarationConversion(origin, container, def));
			return true;
		} else
			return false;
	}
	private boolean skipResource(final IResource sourceResource) {
		return sourceResource.getName().equals(".project");
	}
}
