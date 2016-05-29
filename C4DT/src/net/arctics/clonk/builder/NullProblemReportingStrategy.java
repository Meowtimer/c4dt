package net.arctics.clonk.builder;

import static java.util.Arrays.stream;
import static net.arctics.clonk.util.Utilities.as;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IRegion;

import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ast.SourceLocation;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.ProblemReporter;
import net.arctics.clonk.c4script.ProblemReportingStrategy;
import net.arctics.clonk.c4script.ProblemReportingStrategy.Capabilities;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.ast.AccessDeclaration;
import net.arctics.clonk.c4script.typing.IType;
import net.arctics.clonk.c4script.typing.PrimitiveType;
import net.arctics.clonk.c4script.typing.TypingJudgementMode;
import net.arctics.clonk.index.CachedEngineDeclarations;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.Markers;
import net.arctics.clonk.util.Pair;

@Capabilities(capabilities=Capabilities.ISSUES|Capabilities.TYPING)
public final class NullProblemReportingStrategy extends ProblemReportingStrategy {
	
	public NullProblemReportingStrategy(Index index, String args) {
		super(index, args);
	}

	private final class NullReporter implements ProblemReporter {
		private final Function f;
		public NullReporter(Function f) { this.f = f; }
		@Override
		public IFile file() { return f.script().file(); }
		@Override
		public Declaration container() { return f.script(); }
		@Override
		public int fragmentOffset() { return 0; }
		@Override
		public IType typeOf(ASTNode node) { return PrimitiveType.UNKNOWN; }
		@Override
		public <T extends IType> T typeOf(ASTNode node, Class<T> cls) { return as(PrimitiveType.UNKNOWN, cls); }
		@Override
		public <T extends AccessDeclaration> Declaration obtainDeclaration(T access) { return null; }
		@Override
		public boolean judgement(ASTNode node, IType type, TypingJudgementMode mode) { return false; }
		@Override
		public void incompatibleTypesMarker(ASTNode node, IRegion region, IType left, IType right) {}
		@Override
		public boolean isModifiable(ASTNode node) { return true; }
		@Override
		public Declaration declarationOf(ASTNode node) { return null;}
		@Override
		public Definition definition() { return as(f.script(), Definition.class); }
		@Override
		public SourceLocation absoluteSourceLocationFromExpr(ASTNode expression) { return null; }
		@Override
		public CachedEngineDeclarations cachedEngineDeclarations() { return null; }
		@Override
		public Markers markers() { return null; }
		@Override
		public Script script() { return f.script(); }
		@Override
		public Function function() { return f; }
	}
	
	private Set<Function> functions;
	
	@Override
	public ProblemReportingStrategy initialize(Markers markers, IProgressMonitor progressMonitor, Collection<Pair<Script, Function>> functions) {
		super.initialize(markers, progressMonitor, functions);
		this.functions = functions.stream().map(p -> p.second()).collect(Collectors.toSet());
		return this;
	}
	
	@Override
	public ProblemReportingStrategy initialize(Markers markers, IProgressMonitor progressMonitor, Script[] scripts) {
		super.initialize(markers, progressMonitor, scripts);
		this.functions = stream(scripts).flatMap(s -> s.functions().stream()).collect(Collectors.toSet());
		return this;
	}
	
	@Override
	public void run() {
		if (observer != null) {
			functions.forEach(f -> f.traverse(observer, new NullReporter(f)));
		}
	}

}