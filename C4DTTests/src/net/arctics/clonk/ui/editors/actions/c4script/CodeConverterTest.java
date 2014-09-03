package net.arctics.clonk.ui.editors.actions.c4script;

import net.arctics.clonk.TestBase;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.builder.CodeConverter;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.ScriptParser;
import net.arctics.clonk.c4script.ScriptParserTest.Setup;
import net.arctics.clonk.c4script.ast.Tidy;

import org.junit.Test;

public class CodeConverterTest extends TestBase {
	@Test
	public void testOldStyleToNewStyleConversion() {
		final String source = "Initialize:\n\treturn 1;";
		final Setup setup = new Setup(source);
		setup.parsers.forEach(ScriptParser::run);
		setup.index.refresh();
		setup.scripts.forEach(Script::deriveInformation);

		final CodeConverter converter = new CodeConverter() {
			@Override
			public ASTNode performConversion(ASTNode node, Declaration owner, ICodeConverterContext context) {
				try {
					return new Tidy(null, 2).tidyExhaustive(node);
				} catch (final CloneNotSupportedException e) {
					e.printStackTrace();
					throw new RuntimeException();
				}
			}
		};

		converter.runOnDocument(setup.script, documentMock(source));
	}
}
