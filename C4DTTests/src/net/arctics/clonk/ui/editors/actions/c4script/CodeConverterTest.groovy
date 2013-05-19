package net.arctics.clonk.ui.editors.actions.c4script;

import static org.junit.Assert.*;

import org.eclipse.jface.text.IDocument;
import org.junit.Test;

import net.arctics.clonk.TestBase;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.c4script.ScriptParser
import net.arctics.clonk.c4script.ScriptParserTest.Setup
import net.arctics.clonk.c4script.typing.TypeUtil;
import net.arctics.clonk.ui.editors.actions.c4script.CodeConverter.ICodeConverterContext;

public class CodeConverterTest extends TestBase {
	@Test
	public void testOldStyleToNewStyleConversion() {
		String source =
"""
Initialize:
	return 1;
"""
		def setup = new Setup(source)
		setup.parsers.each { it.run() }
		setup.index.refresh()
		setup.scripts.each { it.deriveInformation() }

		try {
			def converter = new CodeConverter() {
				@Override
				protected ASTNode performConversion(ScriptParser parser, ASTNode node, Declaration owner, ICodeConverterContext context) {
					node.exhaustiveOptimize(TypeUtil.problemReportingContext(parser.script()))
				}
			}

			converter.runOnDocument(setup.script, setup.parser, documentMock(source))
		} catch (e) {
			e.printStackTrace()
		}
	}
}
