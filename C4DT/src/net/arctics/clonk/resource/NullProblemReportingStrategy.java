package net.arctics.clonk.resource;

import static net.arctics.clonk.util.Utilities.as;
import net.arctics.clonk.index.CachedEngineDeclarations;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.Markers;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.ProblemReportingContext;
import net.arctics.clonk.parser.c4script.ProblemReportingStrategy;
import net.arctics.clonk.parser.c4script.ProblemReportingStrategy.Capabilities;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.parser.c4script.ast.AccessDeclaration;
import net.arctics.clonk.parser.c4script.ast.TypingJudgementMode;
import net.arctics.clonk.resource.ProjectSettings.Typing;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IRegion;

@Capabilities(capabilities=Capabilities.ISSUES|Capabilities.TYPING)
final class NullProblemReportingStrategy extends ProblemReportingStrategy {
	@Override
	public void run() {}
	
	@Override
	public ProblemReportingContext localTypingContext(Script script) { return localTypingContext(new C4ScriptParser(script)); }

	@Override
	public ProblemReportingContext localTypingContext(final C4ScriptParser parser) {
		return new ProblemReportingContext() {
			@Override
			public boolean validForType(ASTNode node, IType type) { return true; }
			@Override
			public void typingJudgement(ASTNode node, IType type, TypingJudgementMode mode) {}
			@Override
			public Typing typing() { return Typing.Dynamic; }
			@Override
			public <T extends IType> T typeOf(ASTNode node, Class<T> cls) { return as((IType)PrimitiveType.ANY, cls); }
			@Override
			public IType typeOf(ASTNode node) { return PrimitiveType.ANY; }
			@Override
			public void storeType(ASTNode exprElm, IType type) {}
			@Override
			public IType queryTypeOfExpression(ASTNode exprElm, IType defaultType) { return PrimitiveType.ANY; }
			@Override
			public <T extends AccessDeclaration> Declaration obtainDeclaration(T access) { return null; }
			@Override
			public void incompatibleTypes(ASTNode node, IRegion region, IType left, IType right) {}
			@Override
			public void assignment(ASTNode leftSide, ASTNode rightSide) {}
			@Override
			public int fragmentOffset() { return 0; }
			@Override
			public IFile file() { return parser.script().scriptFile(); }
			@Override
			public Declaration container() { return parser.script(); }
			@Override
			public Object valueForVariable(String varName) { return ASTNode.EVALUATION_COMPLEX; }
			@Override
			public Script script() { return parser.script(); }
			@Override
			public void reportOriginForExpression(ASTNode expression, IRegion location, IFile file) { }
			@Override
			public Function function() { return null; }
			@Override
			public int codeFragmentOffset() { return 0; }
			@Override
			public Object[] arguments() { return null; }
			@Override
			public BufferedScanner scanner() { return parser; }
			@Override
			public void reportProblemsOfFunction(Function function) {}
			@Override
			public void reportProblems() {}
			@Override
			public Markers markers() { return parser.markers(); }
			@Override
			public Definition definition() { return parser.definition(); }
			@Override
			public CachedEngineDeclarations cachedEngineDeclarations() { return null; }
			@Override
			public SourceLocation absoluteSourceLocationFromExpr(ASTNode expression) { return expression; }
		};
	}
}