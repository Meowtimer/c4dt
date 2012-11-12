package net.arctics.clonk.parser.c4script;

import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.defaulting;

import java.util.HashSet;
import java.util.Set;

import net.arctics.clonk.index.CachedEngineDeclarations;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.IIndexEntity;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.c4script.ast.ExprElm;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IRegion;

public class TypeUtil {
	private static ThreadLocal<Set<IResolvableType>> recursion = new ThreadLocal<Set<IResolvableType>>();
	public static IType resolve(IType t, DeclarationObtainmentContext context, IType callerType) {
		if (t instanceof IResolvableType) {
			IResolvableType rt = (IResolvableType)t;
			Set<IResolvableType> r = recursion.get();
			if (r == null)
				recursion.set(r = new HashSet<IResolvableType>());
			else if (r.contains(rt))
				return t;
			r.add(rt);
			try {
				return rt.resolve(context, callerType);
			} finally {
				r.remove(rt);
			}
		} else
			return t;
	}
	public static IType resolve(IType type, IIndexEntity context, Declaration defaultDeclaration) {
		Declaration dec = defaulting(as(context, Declaration.class), defaultDeclaration);
		return TypeUtil.resolve(type, declarationObtainmentContext(dec), dec.script());
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
		};
	}
	public static IType combineTypes(IType first, IType second) {
		return TypeSet.create(first, second);
	}
}