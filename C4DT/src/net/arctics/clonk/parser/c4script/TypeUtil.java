package net.arctics.clonk.parser.c4script;

import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.defaulting;

import java.util.HashSet;
import java.util.Set;

import net.arctics.clonk.index.CachedEngineDeclarations;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.IIndexEntity;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.Markers;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.c4script.ast.AccessDeclaration;
import net.arctics.clonk.parser.c4script.ast.TypingJudgementMode;
import net.arctics.clonk.resource.ProjectSettings.Typing;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IRegion;

public class TypeUtil {
	private static ThreadLocal<Set<IResolvableType>> recursion = new ThreadLocal<Set<IResolvableType>>() {
		@Override
		protected Set<IResolvableType> initialValue() {
			return new HashSet<IResolvableType>();
		};
	};
	public static IType resolveInternal(IType type, ProblemReportingContext context, IType callerType, Set<IResolvableType> recursionCatcher) {
		boolean makingProgress;
		IType[] schluss = new IType[5];
		int passes = 0;
		try {
			do {
				makingProgress = false;
				If: if (type instanceof IResolvableType) {
					IResolvableType rt = (IResolvableType)type;
					if (recursionCatcher.contains(rt))
						break If;
					recursionCatcher.add(rt);
					schluss[passes++] = rt;
					IType resolved = rt.resolve(context, callerType);
					if (!Utilities.objectsEqual(resolved, rt)) {
						makingProgress = true;
						type = resolved;
					}
				}
			} while (makingProgress && passes < schluss.length-1);
		} finally {
			for (int i = passes-1; i >= 0; i--)
				recursionCatcher.remove(schluss[i]);
		}
		return type;
	}
	public static IType resolve(IType type, ProblemReportingContext context, IType callerType) {
		return resolveInternal(type, context, callerType, recursion.get());
	}
	public static IType resolve(IType type, IIndexEntity context, Declaration defaultDeclaration) {
		Declaration dec = defaulting(as(context, Declaration.class), defaultDeclaration);
		return resolve(type, problemReportingContext(dec), dec.script());
	}

	public static ProblemReportingContext problemReportingContext(final Declaration context) {
		return new ProblemReportingContext() {

			@Override
			public IType queryTypeOfExpression(ASTNode exprElm, IType defaultType) {
				return null;
			}

			@Override
			public Definition definition() {
				return script() instanceof Definition ? (Definition)script() : null;
			}

			@Override
			public void storeType(ASTNode exprElm, IType type) {
				// yeah right
			}

			@Override
			public SourceLocation absoluteSourceLocationFromExpr(ASTNode expression) {
				int bodyOffset = context.absoluteExpressionsOffset();
				return new SourceLocation(expression.start()+bodyOffset, expression.end()+bodyOffset);
			}

			@Override
			public Object[] arguments() {
				return new Object[0];
			}

			@Override
			public Function function() {
				return as(context, Function.class);
			}

			@Override
			public Script script() {
				return context.script();
			}

			@Override
			public int codeFragmentOffset() {
				return 0;
			}

			@Override
			public void reportOriginForExpression(ASTNode expression, IRegion location, IFile file) {}

			@Override
			public Object valueForVariable(String varName) {
				return null;
			}

			@Override
			public CachedEngineDeclarations cachedEngineDeclarations() {
				return context.engine().cachedDeclarations();
			}

			@Override
			public IFile file() { return as(context.resource(), IFile.class); }
			@Override
			public Declaration container() { return context; }
			@Override
			public int fragmentOffset() { return 0; }

			@Override
			public <T extends AccessDeclaration> Declaration obtainDeclaration(T access) {
				return access.declaration();
			}

			@Override
			public IType typeOf(ASTNode node) {
				AccessDeclaration ad = as(node, AccessDeclaration.class);
				return ad != null && ad.declaration() instanceof ITypeable
					? ((ITypeable)ad).type() : PrimitiveType.UNKNOWN;
			}

			@Override
			public BufferedScanner scanner() {
				return null;
			}

			@Override
			public <T extends IType> T typeOf(ASTNode node, Class<T> cls) {
				return as(typeOf(node), cls);
			}

			@Override
			public boolean validForType(ASTNode node, IType type) {
				return type.canBeAssignedFrom(typeOf(node));
			}

			@Override
			public Typing typing() {
				return Typing.ParametersOptionallyTyped;
			}

			@Override
			public Markers markers() {
				return null;
			}

			@Override
			public void reportProblems() {}
			@Override
			public void reportProblemsOfFunction(Function function) {}
			@Override
			public void assignment(ASTNode leftSide, ASTNode rightSide) {}
			@Override
			public void typingJudgement(ASTNode node, IType type, TypingJudgementMode mode) {}
			@Override
			public void incompatibleTypes(ASTNode node, IRegion region, IType left, IType right) {}
		};
	}
	public static Definition definition(IType type) {
		for (IType t : type)
			if (t instanceof Definition)
				return (Definition) t; // return the first one found
		return null;
	}

}