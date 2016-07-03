package net.arctics.clonk.c4script.typing;

import static net.arctics.clonk.util.Utilities.as;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ast.Sequence;
import net.arctics.clonk.ast.SourceLocation;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.ProblemReporter;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.ast.AccessDeclaration;
import net.arctics.clonk.index.CachedEngineDeclarations;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.parser.Markers;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IRegion;

public class TypeUtil {
	public static ProblemReporter problemReportingContext(final Declaration context) {
		return new ProblemReporter() {
			@Override
			public Definition definition() { return script() instanceof Definition ? (Definition)script() : null; }
			@Override
			public SourceLocation absoluteSourceLocationFromExpr(final ASTNode expression) {
				final int soff = context.sectionOffset();
				return new SourceLocation(expression.start()+soff, expression.end()+soff);
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
			public <T extends AccessDeclaration> Declaration obtainDeclaration(final T access) { return access.declaration(); }
			@Override
			public IType typeOf(final ASTNode node) {
				final AccessDeclaration ad = as(node, AccessDeclaration.class);
				return ad != null && ad.declaration() instanceof ITypeable
					? ((ITypeable)ad.declaration()).type() : PrimitiveType.UNKNOWN;
			}
			@Override
			public <T extends IType> T typeOf(final ASTNode node, final Class<T> cls) { return as(typeOf(node), cls); }
			@Override
			public Markers markers() { return null; }
			@Override
			public boolean judgement(final ASTNode node, final IType type, final TypingJudgementMode mode) { return false; }
			@Override
			public void incompatibleTypesMarker(final ASTNode node, final IRegion region, final IType left, final IType right) {}
			@Override
			public boolean isModifiable(final ASTNode node) { return false; }
			@Override
			public Function function() { return null; }
			@Override
			public Declaration declarationOf(final ASTNode node) {
				final ASTNode last = node instanceof Sequence ? ((Sequence)node).lastElement() : node;
				if (last instanceof AccessDeclaration) {
					final AccessDeclaration ad = (AccessDeclaration) last;
					return ad.declaration();
				}
				return null;
			}
		};
	}
	public static Definition definition(final IType type) {
		for (final IType t : type)
			if (t instanceof Definition)
				return (Definition) t; // return the first one found
		return null;
	}
	public static boolean convertToBool(final Object value) {
		return !Boolean.FALSE.equals(PrimitiveType.BOOL.convert(value));
	}
	public static IType inferredType(final ASTNode node) {
		final Function function = node.parent(Function.class);
		final Script script = function.parent(Script.class);
		final Function.Typing typing = script.typings().get(function);
		return typing != null ? typing.nodeTypes[node.localIdentifier()] : PrimitiveType.UNKNOWN;
	}
}