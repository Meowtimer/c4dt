package net.arctics.clonk.parser.c4script;

import static net.arctics.clonk.util.Utilities.as;
import net.arctics.clonk.index.CachedEngineDeclarations;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.Markers;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.c4script.ast.AccessDeclaration;
import net.arctics.clonk.parser.c4script.ast.TypingJudgementMode;
import net.arctics.clonk.resource.ProjectSettings.Typing;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IRegion;

public class TypeUtil {
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
			public Script script() {
				return context.script();
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
					? ((ITypeable)ad.declaration()).type() : PrimitiveType.UNKNOWN;
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