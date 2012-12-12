package net.arctics.clonk.parser.c4script;

import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.defaulting;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import net.arctics.clonk.index.CachedEngineDeclarations;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.IIndexEntity;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.IFunctionCall;
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
	public static IType resolveInternal(IType type, DeclarationObtainmentContext context, IType callerType, Set<IResolvableType> recursionCatcher) {
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
	public static IType resolve(IType type, DeclarationObtainmentContext context, IType callerType) {
		return resolveInternal(type, context, callerType, recursion.get());
	}
	public static IType resolve(IType type, IIndexEntity context, Declaration defaultDeclaration) {
		Declaration dec = defaulting(as(context, Declaration.class), defaultDeclaration);
		return resolve(type, declarationObtainmentContext(dec), dec.script());
	}
	
	public static DeclarationObtainmentContext declarationObtainmentContext(final Declaration context) {
		return new DeclarationObtainmentContext() {
			
			@Override
			public IType queryTypeOfExpression(ExprElm exprElm, IType defaultType) {
				return null;
			}
			
			@Override
			public void reportProblems(Function function) {
			}

			@Override
			public Function currentFunction() {
				return context instanceof Function ? (Function)context : null;
			}
			
			@Override
			public Definition definition() {
				return script() instanceof Definition ? (Definition)script() : null;
			}

			@Override
			public void storeType(ExprElm exprElm, IType type) {
				// yeah right
			}

			@Override
			public Declaration currentDeclaration() {
				return context;
			}

			@Override
			public SourceLocation absoluteSourceLocationFromExpr(ExprElm expression) {
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
			public void reportOriginForExpression(ExprElm expression, IRegion location, IFile file) {}

			@Override
			public Object valueForVariable(String varName) {
				return null;
			}

			@Override
			public CachedEngineDeclarations cachedEngineDeclarations() {
				return context.engine().cachedDeclarations();
			}

			@Override
			public void setCurrentFunction(Function function) {
				// ignore
			}

			private final Stack<IFunctionCall> functionCall = new Stack<IFunctionCall>();
			@Override
			public void pushCurrentFunctionCall(IFunctionCall call) { functionCall.push(call); }
			@Override
			public void popCurrentFunctionCall() { functionCall.pop(); }
			@Override
			public IFunctionCall currentFunctionCall() { return functionCall.isEmpty() ? null : functionCall.peek(); }
		};
	}
	public static Definition definition(IType type) {
		for (IType t : type)
			if (t instanceof Definition)
				return (Definition) t; // return the first one found
		return null;
	}

}