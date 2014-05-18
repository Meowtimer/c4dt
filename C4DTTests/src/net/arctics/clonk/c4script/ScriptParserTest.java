package net.arctics.clonk.c4script;

import static net.arctics.clonk.util.Utilities.as;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import net.arctics.clonk.Core;
import net.arctics.clonk.DefinitionInfo;
import net.arctics.clonk.ProblemException;
import net.arctics.clonk.TestBase;
import net.arctics.clonk.ast.ASTComparisonDelegate;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.c4script.ast.AccessVar;
import net.arctics.clonk.c4script.ast.BinaryOp;
import net.arctics.clonk.c4script.ast.Block;
import net.arctics.clonk.c4script.ast.CallDeclaration;
import net.arctics.clonk.c4script.ast.ForStatement;
import net.arctics.clonk.c4script.ast.IntegerLiteral;
import net.arctics.clonk.c4script.ast.SimpleStatement;
import net.arctics.clonk.c4script.ast.StringLiteral;
import net.arctics.clonk.c4script.ast.UnaryOp;
import net.arctics.clonk.c4script.ast.UnaryOp.Placement;
import net.arctics.clonk.c4script.ast.VarDeclarationStatement;
import net.arctics.clonk.c4script.ast.VarInitialization;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.ID;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.Markers;
import net.arctics.clonk.util.SelfcontainedStorage;

import org.eclipse.core.resources.IStorage;
import org.junit.Assert;
import org.junit.Test;

public class ScriptParserTest extends TestBase {

	public static class Setup {
		public Index index;
		public List<Script> scripts;
		public List<ScriptParser> parsers;
		public Script script;
		public ScriptParser parser;
		public final Markers parserMarkers = new Markers();
		@SuppressWarnings("serial")
		public Setup(final Object... sources) {
			this.index = new Index() {
				private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
				@Override
				public Engine engine() { return Core.instance().loadEngine(TestBase.ENGINE); }
			};
			this.scripts = Arrays.stream(sources).map(source -> {
				if (source instanceof String)
					return new Script(index) {
						@Override
						public IStorage source() { return new SelfcontainedStorage(name(), (String)source); }
					};
				else if (source instanceof DefinitionInfo) {
					final DefinitionInfo info = as(source, DefinitionInfo.class);
					return new Definition(index, ID.get(info.name), info.name) {
						@Override
						public IStorage source() { return new SelfcontainedStorage(name(), info.source); }
					};
				} else
					throw new IllegalArgumentException(source.toString());
			}).collect(Collectors.toList());
			this.scripts.forEach(this.index::addScript);
			this.parsers = this.scripts.stream().map(script -> new ScriptParser(
				((SelfcontainedStorage)script.source()).contentsAsString(), script, null
			)).collect(Collectors.toList());
			this.script = this.scripts.get(0);
			this.parser = this.parsers.get(0);
		}
	}

	@Test
	public void testForLoopParsingParsing() {
		final ASTNode body = new ForStatement(
			new VarDeclarationStatement(Variable.Scope.VAR,
				new VarInitialization("i", IntegerLiteral.ZERO, 0, 0, null, null)),
					new BinaryOp(Operator.Smaller, new AccessVar("i"),
						new IntegerLiteral(100)), new UnaryOp(Operator.Increment, Placement.Postfix, new AccessVar("i")),
						new Block(new SimpleStatement(
							new CallDeclaration("Log", new StringLiteral("Hello")))));

		final Setup setup = new Setup(String.format("func Test() {%s}", body.printed()));
		try {
			setup.parser.parse();
			setup.script.deriveInformation();
		} catch (final ProblemException e) {
			e.printStackTrace();
		}
		Assert.assertTrue(setup.parserMarkers.size() == 0);
		Assert.assertTrue(setup.script.findFunction("Test") != null);
		Assert.assertTrue(setup.script.findFunction("Test").body().statements().length == 1);
		Assert.assertTrue(setup.script.findFunction("Test").body().statements()[0].compare(body, new ASTComparisonDelegate(body)));
	}

}
