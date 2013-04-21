package net.arctics.clonk.c4script.typing;

import static net.arctics.clonk.util.Utilities.as;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.IType;
import net.arctics.clonk.c4script.ITypeable;
import net.arctics.clonk.c4script.PrimitiveType;
import net.arctics.clonk.c4script.ProblemReportingContext;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.ast.AccessDeclaration;
import net.arctics.clonk.index.CachedEngineDeclarations;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.IASTVisitor;
import net.arctics.clonk.parser.Markers;
import net.arctics.clonk.parser.SourceLocation;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IRegion;

public class TypeUtil {
	public static ProblemReportingContext problemReportingContext(final Declaration context) {
		return new ProblemReportingContext() {
			@Override
			public Definition definition() { return script() instanceof Definition ? (Definition)script() : null; }
			@Override
			public SourceLocation absoluteSourceLocationFromExpr(ASTNode expression) {
				final int bodyOffset = context.absoluteExpressionsOffset();
				return new SourceLocation(expression.start()+bodyOffset, expression.end()+bodyOffset);
			}
			@Override
			public Script script() { return context.script(); }
			@Override
			public CachedEngineDeclarations cachedEngineDeclarations() { return context.engine().cachedDeclarations(); }
			@Override
			public IFile file() { return as(context.resource(), IFile.class); }
			@Override
			public Declaration container() { return context; }
			@Override
			public int fragmentOffset() { return 0; }
			@Override
			public <T extends AccessDeclaration> Declaration obtainDeclaration(T access) { return access.declaration(); }
			@Override
			public IType typeOf(ASTNode node) {
				final AccessDeclaration ad = as(node, AccessDeclaration.class);
				return ad != null && ad.declaration() instanceof ITypeable
					? ((ITypeable)ad.declaration()).type() : PrimitiveType.UNKNOWN;
			}
			@Override
			public <T extends IType> T typeOf(ASTNode node, Class<T> cls) { return as(typeOf(node), cls); }
			@Override
			public Markers markers() { return null; }
			@Override
			public void setMarkers(Markers markers) { /* ignore */ }
			@Override
			public void reportProblems() {}
			@Override
			public Object visitFunction(Function function) { return null; }
			@Override
			public void judgement(ASTNode node, IType type, TypingJudgementMode mode) {}
			@Override
			public void incompatibleTypesMarker(ASTNode node, IRegion region, IType left, IType right) {}
			@Override
			public boolean isModifiable(ASTNode node) { return false; }
			@Override
			public boolean triggersRevisit(Function function, Function called) { return true; }
			@Override
			public void setObserver(IASTVisitor<ProblemReportingContext> observer) {}
		};
	}
	public static Definition definition(IType type) {
		for (final IType t : type)
			if (t instanceof Definition)
				return (Definition) t; // return the first one found
		return null;
	}

}